package net.yadaframework.persistence;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.web.YadaDatatablesColumn;
import net.yadaframework.web.YadaDatatablesOrder;
import net.yadaframework.web.YadaDatatablesRequest;

/**
 * Server-owned, immutable description of what a DataTables request may read from one entity class.
 * <p>
 * Every value sent by the browser (column name and data, search and order flags, order index and direction,
 * extra json attributes) is untrusted. {@link YadaDataTableDao#getConvertedJsonPage(YadaDatatablesRequest, Class, Locale, YadaDataTableQueryPolicy)}
 * only accepts a request whose columns are all declared here: the attribute path of each column is both the entity path
 * used in the query and the json path of the returned value. Search and sort are allowed only where this policy allows them,
 * the order direction must be "asc" or "desc", and the response contains only the declared columns and extra outputs.
 * <p>
 * Paths are validated against the entity class when the policy is built: a queryable (searchable or orderable) path
 * must resolve to persistent fields, map keys included; an output-only path may end with a getter that returns no
 * entity, collection or map. Paths that end
 * with an entity (except {@link YadaPersistentEnum}), a collection or map of entities, or that contain a credential
 * field name (see {@link #isSecretName(String)}) are refused.
 * <p>
 * Example: <pre>
 * YadaDataTableQueryPolicy policy = YadaDataTableQueryPolicy.forEntity(UserProfile.class)
 *     .column("id").column("userCredentials.username")
 *     .column("userCredentials.lastSuccessfulLogin", false, true)
 *     .extraOutput("registrationDate").build();
 * </pre>
 * The default /yadaDataTableData endpoint derives the policy from the YadaDataTable configuration.
 */
public final class YadaDataTableQueryPolicy {
	/** A field name segment of an attribute path */
	private static final Pattern FIELD_SEGMENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
	/** A map key segment of an attribute path, e.g. a language. Map keys are always bound as query parameters. */
	private static final Pattern KEY_SEGMENT = Pattern.compile("[A-Za-z0-9_-]+");
	/** Accepted DataTables table ids, which are echoed in DT_RowId */
	private static final Pattern TABLE_ID = Pattern.compile("[A-Za-z0-9_-]{1,100}");
	/** Field names that are never queried or returned, whatever the policy says. Lowercase. */
	private static final Set<String> SECRET_NAMES = Set.of("password", "newpassword", "oldpassword", "confirmpassword", "currentpassword",
		"passwordhash", "token", "tokenhash", "secret", "salt");

	private final Class<?> entityClass;
	private final Map<String, Column> columns; // path --> permissions
	private final Set<String> extraOutputs;

	private record Column(boolean searchable, boolean orderable) {}

	/**
	 * Result of checking a request against the policy: the trusted paths to query and return
	 * @param outputPaths the paths to return for each row, in order, without duplicates
	 * @param searchPaths the paths to use for the global search
	 * @param orders the (path, direction) pairs to sort on; the direction is "asc" or "desc"
	 */
	record Resolved(List<String> outputPaths, List<String> searchPaths, List<String[]> orders) {}

	/**
	 * Thrown when a request asks for something that the policy does not allow
	 */
	static final class RejectedRequestException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		RejectedRequestException(String message) {
			super(message);
		}
	}

	private YadaDataTableQueryPolicy(Builder builder) {
		this.entityClass = builder.entityClass;
		this.columns = Collections.unmodifiableMap(new LinkedHashMap<>(builder.columns));
		this.extraOutputs = Collections.unmodifiableSet(new LinkedHashSet<>(builder.extraOutputs));
	}

	/**
	 * Starts a policy for the given entity class
	 * @param entityClass the class that the table reads, the same passed to getConvertedJsonPage()
	 * @return a builder
	 */
	public static Builder forEntity(Class<?> entityClass) {
		return new Builder(entityClass);
	}

	/**
	 * @return the entity class that the paths have been validated against
	 */
	public Class<?> getEntityClass() {
		return entityClass;
	}

	/**
	 * @return the column paths, in declaration order
	 */
	public Set<String> getColumnPaths() {
		return columns.keySet();
	}

	/**
	 * @return the extra paths returned for each row in addition to the requested columns
	 */
	public Set<String> getExtraOutputPaths() {
		return extraOutputs;
	}

	public boolean isSearchable(String path) {
		Column column = columns.get(path);
		return column != null && column.searchable();
	}

	public boolean isOrderable(String path) {
		Column column = columns.get(path);
		return column != null && column.orderable();
	}

	/**
	 * Tells whether a field name is a credential that must never be queried or returned, e.g. "password" or "tokenHash"
	 * @param fieldName a single segment of an attribute path
	 * @return true for credential names
	 */
	public static boolean isSecretName(String fieldName) {
		return fieldName != null && SECRET_NAMES.contains(fieldName.toLowerCase(Locale.ROOT));
	}

	/**
	 * Checks a DataTables request against this policy, before any query.
	 * @param request the request sent by the browser
	 * @return the trusted paths to use
	 * @throws RejectedRequestException when the request asks for anything that is not allowed
	 */
	Resolved resolve(YadaDatatablesRequest request) {
		String dataTableId = request.getDataTableId();
		if (dataTableId != null && !TABLE_ID.matcher(dataTableId).matches()) {
			throw new RejectedRequestException("invalid dataTableId");
		}
		List<YadaDatatablesColumn> requestColumns = request.getColumns() == null ? List.of() : request.getColumns();
		List<String> boundPaths = new ArrayList<>(); // Same index as requestColumns, null for columns without data
		Set<String> outputPaths = new LinkedHashSet<>();
		boolean searching = request.getSearch() != null && StringUtils.isNotBlank(request.getSearch().getValue());
		Set<String> searchPaths = new LinkedHashSet<>();
		for (int i = 0; i < requestColumns.size(); i++) {
			YadaDatatablesColumn requestColumn = requestColumns.get(i);
			String path = requestColumn == null ? null : boundPath(requestColumn);
			boundPaths.add(path);
			if (path == null) {
				continue;
			}
			if (!columns.containsKey(path)) {
				throw new RejectedRequestException("column " + i + " is not allowed");
			}
			outputPaths.add(path);
			// The browser flag can only restrict the search
			if (searching && isSearchable(path) && requestColumn.isSearchable()) {
				searchPaths.add(path);
			}
		}
		List<String> requestExtras = request.getExtraJsonAttributes() == null ? List.of() : request.getExtraJsonAttributes();
		for (String extra : requestExtras) {
			String path = StringUtils.trimToNull(extra);
			if (path != null && !extraOutputs.contains(path) && !columns.containsKey(path)) {
				throw new RejectedRequestException("extra json attribute is not allowed");
			}
		}
		outputPaths.addAll(extraOutputs);
		List<String[]> orders = new ArrayList<>();
		List<YadaDatatablesOrder> requestOrders = request.getOrder() == null ? List.of() : request.getOrder();
		if (requestOrders.size() > requestColumns.size()) {
			throw new RejectedRequestException("too many order entries");
		}
		for (YadaDatatablesOrder requestOrder : requestOrders) {
			if (requestOrder == null) {
				throw new RejectedRequestException("invalid order entry");
			}
			int index;
			try {
				index = Integer.parseInt(StringUtils.trimToEmpty(requestOrder.get("column")));
			} catch (NumberFormatException e) {
				throw new RejectedRequestException("invalid order column index");
			}
			if (index < 0 || index >= requestColumns.size()) {
				throw new RejectedRequestException("order column index out of range");
			}
			String path = boundPaths.get(index);
			if (path == null || !isOrderable(path)) {
				throw new RejectedRequestException("column " + index + " is not orderable");
			}
			String direction = requestOrder.getDir();
			if (YadaDatatablesOrder.ASC.equalsIgnoreCase(direction)) {
				direction = YadaDatatablesOrder.ASC;
			} else if (YadaDatatablesOrder.DESC.equalsIgnoreCase(direction)) {
				direction = YadaDatatablesOrder.DESC;
			} else {
				throw new RejectedRequestException("invalid order direction");
			}
			orders.add(new String[] { path, direction });
		}
		return new Resolved(List.copyOf(outputPaths), List.copyOf(searchPaths), List.copyOf(orders));
	}

	/**
	 * @return the attribute path that a request column asks for, or null when the column holds no entity value
	 * 	(selection and command columns, columns without data or rendered by a javascript function)
	 */
	private static String boundPath(YadaDatatablesColumn requestColumn) {
		String name = StringUtils.trimToNull(requestColumn.getName());
		String path = name != null ? name : StringUtils.trimToNull(requestColumn.getData());
		if (path == null || YadaDataTableDao.COLUMN_SELECTION.equals(path) || YadaDataTableDao.COLUMN_COMMAND.equals(path)) {
			return null;
		}
		if (name == null && ("function".equals(path) || StringUtils.isNumeric(path))) {
			// DataTables sends "function" for function data and the index for array data
			return null;
		}
		return path;
	}

	/**
	 * Builds a {@link YadaDataTableQueryPolicy}. Every path is validated against the entity class by {@link #build()}.
	 */
	public static final class Builder {
		private final Class<?> entityClass;
		private final Map<String, Column> columns = new LinkedHashMap<>();
		private final Set<String> extraOutputs = new LinkedHashSet<>();

		private Builder(Class<?> entityClass) {
			if (entityClass == null) {
				throw new YadaInvalidUsageException("The entity class of a DataTable policy can't be null");
			}
			this.entityClass = entityClass;
		}

		/**
		 * Adds a searchable and orderable column.
		 * @param path the entity attribute path, also the json path of the value, e.g. "userCredentials.username"
		 * @return this builder
		 */
		public Builder column(String path) {
			return column(path, true, true);
		}

		/**
		 * Adds a column.
		 * @param path the entity attribute path, also the json path of the value, e.g. "title.en" for a localized map
		 * @param searchable true if the global search may use this column
		 * @param orderable true if the table may be sorted on this column
		 * @return this builder
		 */
		public Builder column(String path, boolean searchable, boolean orderable) {
			String trimmed = StringUtils.trimToNull(path);
			if (trimmed == null) {
				throw new YadaInvalidUsageException("Empty DataTable column path");
			}
			columns.put(trimmed, new Column(searchable, orderable));
			return this;
		}

		/**
		 * Adds a value that is returned for every row without being a table column, e.g. to compute a cell in javascript.
		 * It is never searched or sorted. Browser requests may only ask for declared extras.
		 * @param path the entity attribute or getter path
		 * @return this builder
		 */
		public Builder extraOutput(String path) {
			String trimmed = StringUtils.trimToNull(path);
			if (trimmed == null) {
				throw new YadaInvalidUsageException("Empty DataTable extra output path");
			}
			extraOutputs.add(trimmed);
			return this;
		}

		/**
		 * @return the immutable policy
		 * @throws YadaInvalidUsageException when a path is invalid for the entity class or discloses a credential or an entity
		 */
		public YadaDataTableQueryPolicy build() {
			for (Map.Entry<String, Column> column : columns.entrySet()) {
				boolean queryable = column.getValue().searchable() || column.getValue().orderable();
				validatePath(entityClass, column.getKey(), queryable);
			}
			for (String extra : extraOutputs) {
				validatePath(entityClass, extra, false);
			}
			return new YadaDataTableQueryPolicy(this);
		}
	}

	/**
	 * Checks that a path resolves on the entity class and returns no credential or entity.
	 * @param queryable true when the path is used in the query, so every segment must be a persistent field
	 */
	static void validatePath(Class<?> entityClass, String path, boolean queryable) {
		String[] segments = path.split("\\.", -1);
		Class<?> currentClass = entityClass;
		Type currentType = entityClass;
		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			if (!FIELD_SEGMENT.matcher(segment).matches()) {
				throw new YadaInvalidUsageException("Invalid DataTable path '{}' for {}", path, entityClass.getSimpleName());
			}
			if (isSecretName(segment)) {
				throw new YadaInvalidUsageException("DataTable path '{}' would disclose a credential", path);
			}
			Field field = findField(currentClass, segment);
			if (field == null) {
				boolean last = i == segments.length - 1;
				Class<?> getterType = last && !queryable ? getterType(currentClass, segment) : null;
				if (getterType != null) {
					// Computed value, e.g. getFullName()
					if (isEntityClass(getterType) || (Collection.class.isAssignableFrom(getterType) || Map.class.isAssignableFrom(getterType))) {
						throw new YadaInvalidUsageException("DataTable path '{}' would return an entity or a collection from a getter", path);
					}
					return;
				}
				throw new YadaInvalidUsageException("DataTable path '{}' does not resolve to {} field '{}' of {}", path, queryable ? "a persistent" : "a", segment, currentClass.getName());
			}
			currentClass = field.getType();
			currentType = field.getGenericType();
			if (Map.class.isAssignableFrom(currentClass)) {
				if (i + 1 < segments.length) {
					i++; // Skip the map key
					if (!KEY_SEGMENT.matcher(segments[i]).matches()) {
						throw new YadaInvalidUsageException("Invalid map key in DataTable path '{}'", path);
					}
					currentType = typeArgument(currentType, 1);
					currentClass = rawClass(currentType);
					if (currentClass == null) {
						throw new YadaInvalidUsageException("Can't resolve the map value type of DataTable path '{}'", path);
					}
				} else {
					Class<?> valueClass = rawClass(typeArgument(currentType, 1));
					if (valueClass == null || isEntityClass(valueClass)) {
						throw new YadaInvalidUsageException("DataTable path '{}' would return a map of entities", path);
					}
				}
			} else if (Collection.class.isAssignableFrom(currentClass)) {
				Class<?> elementClass = rawClass(typeArgument(currentType, 0));
				if (elementClass == null) {
					throw new YadaInvalidUsageException("Can't resolve the element type of DataTable path '{}'", path);
				}
				if (i + 1 < segments.length) {
					currentClass = elementClass;
				} else if (isEntityClass(elementClass)) {
					throw new YadaInvalidUsageException("DataTable path '{}' would return a collection of entities", path);
				}
			}
		}
		if (isEntityClass(currentClass) && !YadaPersistentEnum.class.equals(currentClass)) {
			throw new YadaInvalidUsageException("DataTable path '{}' would return a whole entity: add the attribute name", path);
		}
	}

	private static Field findField(Class<?> rootClass, String name) {
		Class<?> current = rootClass;
		while (current != null) {
			try {
				return current.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				current = current.getSuperclass();
			}
		}
		return null;
	}

	/**
	 * @return the return type of the getter of a bean property, or null if there is no getter
	 */
	private static Class<?> getterType(Class<?> type, String name) {
		try {
			for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
				if (descriptor.getName().equals(name) && descriptor.getReadMethod() != null) {
					return descriptor.getReadMethod().getReturnType();
				}
			}
		} catch (IntrospectionException e) {
			return null;
		}
		return null;
	}

	private static Type typeArgument(Type type, int position) {
		if (type instanceof ParameterizedType parameterized) {
			Type[] arguments = parameterized.getActualTypeArguments();
			if (arguments.length > position) {
				return arguments[position];
			}
		}
		return null;
	}

	private static Class<?> rawClass(Type type) {
		if (type instanceof Class<?> result) {
			return result;
		}
		if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> result) {
			return result;
		}
		return null;
	}

	/**
	 * @return true if the class or one of its superclasses is an entity or a mapped superclass
	 */
	static boolean isEntityClass(Class<?> type) {
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			if (current.isAnnotationPresent(Entity.class) || current.isAnnotationPresent(MappedSuperclass.class)) {
				return true;
			}
		}
		return false;
	}
}
