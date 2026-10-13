package net.yadaframework.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import net.yadaframework.components.YadaDataTableFactory;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.web.YadaDatatablesRequest;
import net.yadaframework.web.datatables.proxy.YadaDataTableProxy;

@Repository
@Transactional(readOnly = true) 
//TODO non considera il search su singola colonna "columns[i][search][value]", nel senso che usa solo il global search su tutte le colonne searchable
public class YadaDataTableDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	public static final String COLUMN_SELECTION = "_yadaSelectionColumn";
	public static final String COLUMN_COMMAND = "_yadaCommandColumn";

	@Autowired MessageSource messageSource;
	@Autowired YadaUtil yadaUtil;
	
    @PersistenceContext
	EntityManager em;
	
	/**
	 * Returns one page of data in the format needed by DataTables, reading only what the policy allows.
	 * The request is checked against the policy before any query: a column, search, order or extra attribute that
	 * the policy does not allow rejects the whole request with a "yadaError" and no data. Each row contains
	 * the requested policy columns, the policy extra outputs, "DT_RowId" and "DT_RowClass" (from getDT_RowClass(), if any).
	 * There is no need to call a json converter because everything is a map already.
	 * @param yadaDatatablesRequest the request parameters sent by DataTables. Conditions added by the server to
	 * 	{@link YadaDatatablesRequest#getYadaSql()} are kept.
	 * @param entityClass the entity class to fetch from the database
	 * @param locale the locale to use for localized values
	 * @param policy the server-side policy for this table, e.g. {@code yadaDataTable.getQueryPolicy()} or one built
	 * 	with {@link YadaDataTableQueryPolicy#forEntity(Class)}; it must have been built for entityClass or a superclass
	 * @return one page of data, or a map with "yadaError"
	 */
	public <T> Map<String, Object> getConvertedJsonPage(YadaDatatablesRequest yadaDatatablesRequest, Class<T> entityClass, Locale locale, YadaDataTableQueryPolicy policy) {
		if (policy == null || entityClass == null || !policy.getEntityClass().isAssignableFrom(entityClass)) {
			throw new YadaInvalidUsageException("A DataTable policy for {} is required", entityClass);
		}
		Map<String, Object> json = new HashMap<String, Object>();
		YadaDataTableQueryPolicy.Resolved resolved;
		try {
			resolved = policy.resolve(yadaDatatablesRequest);
		} catch (YadaDataTableQueryPolicy.RejectedRequestException e) {
			log.warn("DataTable request rejected for {}: {}", entityClass.getSimpleName(), e.getMessage());
			Map<String, String> inner = new HashMap<>();
			inner.put("error", "Invalid table request");
			json.put("yadaError", inner);
			return json;
		}
		try {
			List<T> entityPage = getPage(yadaDatatablesRequest, entityClass, locale, resolved);
			List<Map<String, Object>> jsonData = convertToJson(entityPage, yadaDatatablesRequest, entityClass, resolved);
			json.put("draw", yadaDatatablesRequest.getDraw());
			json.put("recordsTotal", yadaDatatablesRequest.getRecordsTotal());
			json.put("recordsFiltered", yadaDatatablesRequest.getRecordsFiltered());
			json.put("data", jsonData);
		} catch (Exception e) {
			log.error("Can't retrieve data", e);
			// The error object must have the same structure of the one inside ajaxError.html
			Map<String, String> inner = new HashMap<>();
			inner.put("error", e.toString());
			json.put("yadaError", inner);
		}
		return json;
	}

	/**
	 * Retrieve all values allowed by the policy and put them in a json-like structure
	 * @return never null
	 */
	private <T> List<Map<String, Object>> convertToJson(List<T> entityPage, YadaDatatablesRequest yadaDatatablesRequest, Class<?> entityClass, YadaDataTableQueryPolicy.Resolved resolved) {
		Field idField = yadaUtil.getFieldNoTraversing(entityClass, "id");
		idField.setAccessible(true);
		//
		List<Map<String, Object>> json = new ArrayList<Map<String, Object>>();
		for (T entity : entityPage) {
			Map<String, Object> entityJson = new HashMap<String, Object>();
			json.add(entityJson);
			// Conversion from java to map, only for the paths allowed by the policy
			for (String attributePath : resolved.outputPaths()) {
				addAttributeValue(entity, entityJson, attributePath);
			}
			// Add DT_RowClass to the value returned by getDT_RowClass() if any
			addAttributeValue(entity, entityJson, "DT_RowClass"); // See YadaUserMessage for an example
			// Add DT_RowId for DataTables id
			String dataTableId = yadaDatatablesRequest.getDataTableId();
			try {
				Long id = (Long) idField.get(entity);
				if (yadaDatatablesRequest.isLegacy()) {
					entityJson.put("DT_RowId", entityClass.getSimpleName()+"#"+id); // UserProfile#22
				} else {
					String rowId = dataTableId+"_"+entityClass.getSimpleName()+"_"+id; // "UserTable_UserProfile_22"
					entityJson.put("DT_RowId", rowId);
				}
			} catch (Exception e) {
				log.error("Failed to set DT_RowId for entity {} (ignored)", entity);
			}
		}
		return json;
	}

	/**
	 * 
	 * @param entity The object containing the data
	 * @param entityJson the map that will be populated with values
	 * @param attributePath the path in the entity that contains the value. Can be an attribute path or a method path
	 */
	private <entityClass> void addAttributeValue(entityClass entity, Map<String, Object> entityJson, String attributePath) {
		try {
			if (entity==null) {
				return;
			}
			Object value = "";
			String[] parts = attributePath.split("\\.", 2);
			String attributeName = parts[0];
			if (COLUMN_SELECTION.equals(attributeName) || COLUMN_COMMAND.equals(attributeName)) {
				return;
			}
			if (!(entity instanceof java.util.Map) && YadaDataTableQueryPolicy.isSecretName(attributeName)) {
				// Second safeguard after the policy validation: credentials are never returned
				log.warn("Credential attribute {} not returned", attributeName);
				return;
			}
			if (entity instanceof java.util.Map) {
				Map<Object,Object> mapEntity = (Map<Object,Object>) entity;
				// The old version was generating a key from the String value, but needed to know how to do that,
				// so only String and Locale keys were supported.
				//	// If the field is a map, handle String and Locale keys only
				//	if (keyType.getTypeName().equals(String.class.getName())) {
				//		value = mapEntity.get(attributeName);
				//	} else if (keyType.getTypeName().equals(Locale.class.getName())) {
				//		value = mapEntity.get(new Locale(attributeName));
				//	} else {
				//		log.debug("Invalid map key type {} - value ignored", keyType);
				//	}
				
				// The new version is a bit less efficient but can cope with any key type because
				// it relies on the toString iterating on all the keys
				Set<Entry<Object, Object>> entrySet = mapEntity.entrySet();
				for (Entry<Object, Object> entry : entrySet) {
					if (entry.getKey().toString().equals(attributeName)) {
						value = entry.getValue();
						break;
					}
				}
			} else {
				Field field = yadaUtil.getFieldNoTraversing(entity.getClass(), attributeName);
				Type valueType = null;
				if (field!=null) {
					field.setAccessible(true);
					value = field.get(entity);
					valueType = field.getType();
					// If the value is null it might be a lazy attribute, so try the getter too
					if (value==null) {
						try {
							String prefix = (valueType==boolean.class || valueType==Boolean.class)?"is":"get";
							String getterName = prefix + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
							Method getter = entity.getClass().getMethod(getterName);
							getter.setAccessible(true);
							value = getter.invoke(entity);
						} catch (Exception e) {
							log.debug("Getter invocation exception (ignored): {}", e.getMessage());
						}
					}
				} else {
					try {
						// Not a field, but it could be a method
						value = org.apache.commons.beanutils.PropertyUtils.getProperty(entity, attributeName);
						valueType = value.getClass();
					} catch (Exception e) {
						// Not even a method
						if (!"DT_RowClass".equals(attributeName)) {
							log.debug("Field {} in entity {} not found (ignored)", attributeName, entity.getClass());
						}
						return;
					}
				}
				if (valueType.equals(YadaPersistentEnum.class)) {
					if (value==null) {
						ParameterizedType type = (ParameterizedType) field.getGenericType();
						log.debug("null value for {}.{} - did you add enum {} to the YadaSetup.setupApplication() method?", entity.getClass().getSimpleName(), field.getName(), type.getActualTypeArguments()[0]);
					} else {
						value = ((YadaPersistentEnum)value).getLocalText();
					}
				}
				// The old version
				//	if (value instanceof java.util.Map) {
				//		ParameterizedType type = (ParameterizedType) field.getGenericType();
				//		keyType = type.getActualTypeArguments()[0];
				//	}
			}
				
			if (parts.length==1) {
				// End of the path
				// TODO value.toString() is not always appropriate, for example for dates.
				//      Maybe pass a map of converters from some type to String?
				if (isEntityValue(value)) {
					// Never return the toString() of a related entity
					log.warn("Entity value of {} not returned: add the attribute name to the path", attributeName);
					value = null;
				}
				entityJson.put(attributeName, value==null?null:value.toString());
				return;
			}
			// Recurse into the path
			Map<String, Object> jsonValue = (Map<String, Object>) entityJson.get(attributeName);
			if (jsonValue==null) {
				jsonValue = new HashMap<>();
				entityJson.put(attributeName, jsonValue);
			}
			addAttributeValue(value, jsonValue, parts[1] /*, keyType*/);
		} catch (Exception e) {
			log.error("Can't get value of {} for entity {} - ignored", attributePath, entity, e);
		}
	}

	/**
	 * @return true for an entity, or a collection or map holding entities
	 */
	private boolean isEntityValue(Object value) {
		if (value==null) {
			return false;
		}
		if (value instanceof java.util.Collection<?> collection) {
			return collection.stream().anyMatch(this::isEntityValue);
		}
		if (value instanceof Map<?,?> map) {
			return map.values().stream().anyMatch(this::isEntityValue);
		}
		return YadaDataTableQueryPolicy.isEntityClass(value.getClass());
	}

	/**
	 * Fetches one page of results using only the paths that the policy has allowed.
	 * @param yadaDatatablesRequest the request: only paging and the global search value are read from it, plus the server conditions in getYadaSql().
	 * 	It also receives the final counts.
	 * @param targetClass the entity class to query
	 * @param resolved the trusted paths and directions returned by {@link YadaDataTableQueryPolicy}
	 * @return the page of entities. The total is in yadaDatatablesRequest.recordsFiltered
	 */
	@SuppressWarnings("rawtypes")
	protected <targetClass> List<targetClass> getPage(YadaDatatablesRequest yadaDatatablesRequest, Class targetClass, Locale locale, YadaDataTableQueryPolicy.Resolved resolved) {
		String requestSearchValue = yadaDatatablesRequest.getSearch().getValue();
		String globalSearchString = requestSearchValue!=null?StringUtils.trimToNull(requestSearchValue.toLowerCase(locale)):null;
		Long globalSearchNumber = null;
		boolean globalSearchEnabled = globalSearchString!=null;
		if (globalSearchEnabled) {
			try {
				globalSearchNumber = Long.parseLong(globalSearchString);
			} catch (NumberFormatException e) {
				// Ignore
			}
		}
		YadaSql yadaSql = yadaDatatablesRequest.getYadaSql();
		YadaSql countSql = (YadaSql) YadaUtil.copyEntity(yadaSql, true); // Copy any conditions set before calling
		YadaSql searchSql = YadaSql.instance();
		
		yadaSql.selectFrom("select distinct e from "+targetClass.getSimpleName()+" e");
		countSql.selectFromReplace("select count(distinct e.id) from "+targetClass.getSimpleName()+" e");
		
		// Searching: the policy has already removed the columns that are not searchable
		for (String attributePath : resolved.searchPaths()) {
			if (!globalSearchEnabled) {
				break;
			}
			Class attributeType;
			boolean isCollection;
			try {
				attributeType = yadaUtil.getType(targetClass, attributePath);
				isCollection = yadaUtil.isCollection(targetClass, attributePath);
			} catch (Exception e) {
				// The policy has validated the path, so this should never happen. No fallback type: the column is not searched.
				log.error("Can't find type for {}.{} (not searched)", targetClass.getSimpleName(), attributePath, e);
				continue;
			}
			boolean isNumber = attributeType == Long.class || attributeType == long.class 
				|| attributeType == Integer.class || attributeType == int.class 
				|| attributeType == BigInteger.class || attributeType == BigDecimal.class
				|| attributeType == Float.class || attributeType == float.class
				|| attributeType == Double.class || attributeType == double.class
				|| attributeType == Short.class || attributeType == short.class;
			// Add left joins otherwise Hibernate creates cross joins hence it doesn't return rows with null values
			String attributeName = addLeftJoins(attributePath, yadaSql, targetClass);
			if (isCollection) {
				// Collections require "member of" operator in JPQL
				if (attributeType == String.class) {
					searchSql.where(":globalSearchStringExact member of " + attributeName).or();
				} else if (globalSearchNumber!=null) {
					// For any numeric collection element type, use the parsed number
					searchSql.where(":globalSearchNumber member of " + attributeName).or();
				}
			} else if (attributeType == String.class) {
				searchSql.where(attributeName + " like :globalSearchString").or();
			} else if (attributeType == Boolean.class || attributeType == boolean.class) {
				if ("true".indexOf(globalSearchString)>-1) {
					searchSql.where(attributeName + " = true").or();
				} else if ("false".indexOf(globalSearchString)>-1) {
					searchSql.where(attributeName + " = false").or();
				}
			} else if (attributeType == YadaPersistentEnum.class) {
				// No need for KEY(" + attributeName + ") = :yadalang because it's added by addLeftJoins()
				searchSql.where(attributeName + " like :globalSearchString").or();
				searchSql.setParameter("yadalang", LocaleContextHolder.getLocale().getLanguage());
			} else if (attributeType.isEnum()) {
				// Loop on all enum constants to find the ones that contain the search key
				List<Integer> enumValues = new ArrayList<Integer>();
				for (Object enumConstant : attributeType.getEnumConstants()) {
					String enumText = enumConstant.toString().toLowerCase();
					if (enumText.indexOf(globalSearchString)>-1) {
						enumValues.add(((Enum)enumConstant).ordinal());
					}
				}
				searchSql.whereIn(attributeName, enumValues).or();
			} else if (isNumber) {
				// JPA doesn't support "like" on numbers, so only exact matches are found
				searchSql.where(globalSearchNumber!=null, attributeName + " = :globalSearchNumber").or();
			} else {
				log.debug("Attribute type {} of {} is not searchable (skipped)", attributeType, attributePath);
			}
		}
		String searchConditions = searchSql.getWhere();
		if (StringUtils.isNotBlank(searchConditions)) {
			if (StringUtils.isNotBlank(yadaSql.getWhere())) {
				yadaSql.and();
			}
			yadaSql.where("(" + searchConditions + ")").and();
		}
		// Sorting
		boolean needsExtraction = false;
		// If targetClass has an id attribute, add it to the "order by" clause for deterministic pagination
		boolean needsOrderById = Arrays.stream(targetClass.getDeclaredFields()).anyMatch(field -> field.getName().equalsIgnoreCase("id"));
		for (String[] order : resolved.orders()) {
			String attributeName = order[0];
			String direction = order[1]; // Only "asc" or "desc"
			if ("id".equalsIgnoreCase(attributeName)) {
				needsOrderById = false; // No need to add it twice
			}
			// Add left joins otherwise Hibernate creates cross joins hence it doesn't return rows with null values
			String sortColumn = addLeftJoins(attributeName, yadaSql, targetClass);
			yadaSql.orderBy(sortColumn + " " + direction);
			if (attributeName.indexOf('.')>-1) {
				yadaSql.selectFrom(sortColumn); // Needed to avoid "ORDER BY clause is not in SELECT list"
				needsExtraction = true;
			}
		}
		// Always order by id (too) to have deterministic pagination
		if (needsOrderById) {
			yadaSql.orderBy("e.id"); // No need to specify the direction
		}
		yadaSql.setParameter("globalSearchString", "%"+globalSearchString+"%");
		yadaSql.setParameter("globalSearchStringExact", globalSearchString);
		yadaSql.setParameter("globalSearchNumber", globalSearchNumber);

	    	Query query = yadaSql.query(em);
	    	// Set the page size. When paging is disabled on the FE, limit is -1
	    	int limit = yadaDatatablesRequest.getLength();
	    	if (limit>-1) {
		    	query.setMaxResults(limit);
	    	}
		query.setFirstResult(yadaDatatablesRequest.getStart());
		@SuppressWarnings("unchecked")
		List<targetClass> result = query.getResultList();
		// An outer query (two-phase ID approach) was considered but doesn't help: the inner query
		// would still need DISTINCT + ORDER BY on a joined column, hitting the same MySQL limitation.
		if (needsExtraction) {
			// When doing an "order by" on a joined column we add the column to the select clause to prevent the "ORDER BY clause is not in SELECT list" error.
			// This means that the result now is a list of Object[] where only the first element is what we need.
			@SuppressWarnings("unchecked")
			List<Object[]> realResult = (List<Object[]>) result; // Just a type cast
			List<targetClass> extractedResult = new ArrayList<>();
			for (Object[] arrayResult : realResult) {
				extractedResult.add((targetClass) arrayResult[0]);
			}
			result = extractedResult;
		}
	    	// Count with the search conditions
	    	yadaSql.toCount();
	    	query = yadaSql.query(em);
	    	long count = (long) query.getSingleResult();
	    	yadaDatatablesRequest.setRecordsFiltered(count);
	    	// Count without the search conditions
	    	query = countSql.query(em);
	    	count = (long) query.getSingleResult();
	    	yadaDatatablesRequest.setRecordsTotal(count);
	    	return result;
	}
	
	/**
	 * From an attribute with a path, like "location.company.name", inserts needed left joins and returns the last segment "company.name".
	 * Joins are inserted for all elements before the last dot: "location" and "company" in the example.
	 * The path must have been validated by {@link YadaDataTableQueryPolicy}.
	 */
	private String addLeftJoins(String attributePath, YadaSql yadaSqlBuilder, Class targetClass) {
		if (attributePath==null) {
			return null;
		}
		return addLeftJoinsRecurse(attributePath, "e", yadaSqlBuilder, targetClass);
	}

	private String addLeftJoinsRecurse(String attributePath, String context, YadaSql yadaSql, Class<?> contextClass) {
		String[] parts = attributePath.split("\\.");
		String current = parts[0]; // location
		Field currentField = yadaUtil.getFieldNoTraversing(contextClass, current);
		Class<?> currentClass = null;
		if (currentField!=null) {
			currentClass = currentField.getType();
		}
		if (parts.length>1) { // location.company.name
			String alias = context + "_" + current; // Adding the context to the alias to prevent name clashes - TODO maybe all the preceding path should be added?
			yadaSql.join("left join " + context + "." + current + " " + alias); // e.location location
			if (Map.class.equals(currentClass)) {
				// Need to add a condition on the map key. Need to check for null to handle absent data.
				// The key is always bound as a parameter, never written in the query.
				String keyValue = parts[1]; // en
				String keyParameter = mapKeyParameter(alias, keyValue);
				String whereToAdd = "(KEY("+alias+") is null or KEY("+alias+")=:" + keyParameter + ")"; // (KEY(e_name) is null or KEY(e_name)=:yadaKey_e_name_656e);
				if (yadaSql.getWhere().indexOf(whereToAdd)<0) {
					yadaSql.where(whereToAdd).and();
					yadaSql.setParameter(keyParameter, toMapKey(keyValue, currentField));
				}
				// If the current attribute is a map, continue only if there is something more after the map key
				if (parts.length>2) {
					// Need to find the type of the map value in order to set the proper join
					ParameterizedType type = (ParameterizedType) currentField.getGenericType();
					currentClass = (Class<?>) type.getActualTypeArguments()[1];
					// Also skip the key in the path
					attributePath = StringUtils.substringAfter(attributePath, "."); // Here we skip the current name, so that below we skip the key of the map
				} else {
					// No need to go deeper, just return the current attribute alias for sorting.
					return alias;
				}
			}
			return addLeftJoinsRecurse(StringUtils.substringAfter(attributePath, "."), alias, yadaSql, currentClass);
		} else {
			// Last element of the path - if it's a YadaPersistentEnum we still need a join for the map
			if (YadaPersistentEnum.class.equals(currentClass)) {
				String alias = attributePath + "_langToText";
				yadaSql.join("left join " + context + "." + attributePath + " " + attributePath); 	// left join user.status status
				yadaSql.join("left join " + attributePath + ".langToText " + alias);	// left join status.langToText status_langToText
				String whereToAdd = "(KEY("+alias+") is null or KEY("+alias+")=:yadalang)";
				if (yadaSql.getWhere().indexOf(whereToAdd)<0) {
					yadaSql.where(whereToAdd).and();
					yadaSql.setParameter("yadalang", LocaleContextHolder.getLocale().getLanguage());
				}
				return alias;
			}
			return context + "." + attributePath; // e.phone, company.name
		}
	}

	/**
	 * Converts the key segment of a path to the key type of the map, so that it can be bound as a query parameter.
	 * @param keyValue the key as written in the path, e.g. "en" or "en_US"
	 * @param mapField the Map field
	 * @return the key as a Locale, enum constant, number or String depending on the declared key type
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object toMapKey(String keyValue, Field mapField) {
		Class<?> keyType = String.class;
		if (mapField.getGenericType() instanceof ParameterizedType mapType && mapType.getActualTypeArguments()[0] instanceof Class<?> keyClass) {
			keyType = keyClass;
		}
		if (Locale.class.equals(keyType)) {
			return org.apache.commons.lang3.LocaleUtils.toLocale(keyValue.replace('-', '_'));
		} else if (keyType.isEnum()) {
			return Enum.valueOf((Class<? extends Enum>) keyType, keyValue);
		} else if (Integer.class.equals(keyType) || int.class.equals(keyType)) {
			return Integer.valueOf(keyValue);
		} else if (Long.class.equals(keyType) || long.class.equals(keyType)) {
			return Long.valueOf(keyValue);
		}
		return keyValue;
	}

	/**
	 * @return a query parameter name for a map key, unique for each alias and key: the key is hex-encoded,
	 * 	so "en-US" and "en_US" get different names, e.g. "yadaKey_e_title_656e2d5553"
	 */
	private String mapKeyParameter(String alias, String keyValue) {
		return "yadaKey_" + alias + "_" + java.util.HexFormat.of().formatHex(keyValue.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

}
