package net.yadaframework.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.web.YadaDatatablesColumn;
import net.yadaframework.web.YadaDatatablesOrder;
import net.yadaframework.web.YadaDatatablesRequest;

@Repository
@Transactional(readOnly = true) 
//TODO non considera il search su singola colonna "columns[i][search][value]", nel senso che usa solo il global search su tutte le colonne searchable
//TODO gestire i parametri nestati più di 2 livelli (e.g. company.location.name)
public class YadaDataTableDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired MessageSource messageSource;
	@Autowired YadaUtil yadaUtil;
	
    @PersistenceContext EntityManager em;
	
    /**
	 * Returns a map with the result in the format needed by DataTables.
	 * All values that are included in the annotated json view (on the caller) will end up in the resulting json
     * @param yadaDatatablesRequest
     * @param entityClass
     * @param locale
     * @return
     */
    @Deprecated // Use getConvertedJsonPage instead
	public <entityClass> Map<String, Object> getJsonPage(YadaDatatablesRequest yadaDatatablesRequest, Class<?> entityClass, Locale locale) {
		Map<String, Object> json = new HashMap<String, Object>();
		try {
			List<entityClass> data = getPage(yadaDatatablesRequest, entityClass, locale);
	    	// Eagerly fetching localized strings. This should not be a performance problem as DataTables is generally used in internal admin pages with a few lines.
	    	YadaUtil.prefetchLocalizedStringList(data, entityClass);
			json.put("draw", yadaDatatablesRequest.getDraw());
			json.put("recordsTotal", yadaDatatablesRequest.getRecordsTotal());
			json.put("recordsFiltered", yadaDatatablesRequest.getRecordsFiltered());
			json.put("data", data);
		} catch (Exception e) {
			log.error("Can't retrieve data", e);
			json.put("error", e.toString()); // TODO handle the error in javascript 
		}
		return json;
	}
 
	/**
	 * Returns a map with the result in the format needed by DataTables.
	 * Only requested values are returned in the map. There is no need to call a json converter because everything is a map already.
	 * @param yadaDatatablesRequest
	 * @param entityClass
	 * @param locale
	 * @return
	 */
	public <entityClass> Map<String, Object> getConvertedJsonPage(YadaDatatablesRequest yadaDatatablesRequest, Class<?> entityClass, Locale locale) {
		Map<String, Object> json = new HashMap<String, Object>();
		try {
			List<entityClass> entityPage = getPage(yadaDatatablesRequest, entityClass, locale);
			List<Map<String, Object>> jsonData = convertToJson(entityPage, yadaDatatablesRequest, entityClass, locale);
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
	 * Retrieve all values requested and put them in a json-like structure
	 * @param entityPage
	 * @param yadaDatatablesRequest
	 * @param entityClass
	 * @param locale
	 * @return
	 */
	private <entityClass> List<Map<String, Object>> convertToJson(List<entityClass> entityPage, YadaDatatablesRequest yadaDatatablesRequest, Class<?> entityClass, Locale locale) {
		Field idField = yadaUtil.getFieldNoTraversing(entityClass, "id");
		idField.setAccessible(true);
		//
		List<Map<String, Object>> json = new ArrayList<Map<String, Object>>();
		for (entityClass entity : entityPage) {
			Map<String, Object> entityJson = new HashMap<String, Object>();
			json.add(entityJson);
			// Conversion from java to map
			for (YadaDatatablesColumn  column : yadaDatatablesRequest.getColumns()) {
				String attributePath = column.getNameOrData(); // Name is the database attribute, data is the json attribute and could also be a javascript function
				if (attributePath!=null) {
					addAttributeValue(entity, entityJson, attributePath);
				}
			}
			// Add any extra json attributes
			for (String  attributePath : yadaDatatablesRequest.getExtraJsonAttributes()) {
				if (attributePath!=null) {
					addAttributeValue(entity, entityJson, attributePath);
				}
			}
			// Add DT_RowId for DataTables id
			// TODO when on a single page there are multiple tables with the same object, the ids are not unique.
			// We should prefix them with the table id, if any.
			try {
				Long id = (Long) idField.get(entity);
				entityJson.put("DT_RowId", entityClass.getSimpleName()+"#"+id);
			} catch (Exception e) {
				log.error("Failed to set DT_RowId for entity {} (ignored)", entity);
			}
		}
		return json;
	}

	private <entityClass> void addAttributeValue(entityClass entity, Map<String, Object> entityJson, String attributePath) {
		try {
			if (entity==null) {
				return;
			}
			Object value = "";
			String[] parts = attributePath.split("\\.", 2);
			String attributeName = parts[0];
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
				} else {
					try {
						// Not a field, but it could be a method
						value = org.apache.commons.beanutils.PropertyUtils.getProperty(entity, attributeName);
						valueType = value.getClass();
					} catch (Exception e) {
						// Not even a method
						log.debug("Field {} in entity {} not found (ignored)", attributeName, entity.getClass());
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
				entityJson.put(attributeName, value==null?null:value.toString());
				return;
			}
			// Recurse into the path
			Map<String, Object> jsonValue = new HashMap<>();
			entityJson.put(attributeName, jsonValue);
			addAttributeValue(value, jsonValue, parts[1] /*, keyType*/);
		} catch (Exception e) {
			log.error("Can't get value of {} for entity {} - ignored", attributePath, entity, e);
		}
	}

	/**
	 * Preleva una pagina di risultati che rispettino i parametri inviati via web.
	 * Il nome delle colonne su cui fare le query e il sort viene preso da "name" oppure da "data" di DataTables.
	 * @param yadaDatatablesRequest parametri della query, conterrà anche il count finale
	 * @param targetClass tipo dell'oggetto da cercare
	 * @return Pagina di oggetti di tipo targetClass. Il numero totale è reperibile in yadaDatatablesRequest.recordsFiltered
	 */
	@SuppressWarnings("rawtypes")
	protected <targetClass> List<targetClass> getPage(YadaDatatablesRequest yadaDatatablesRequest, Class targetClass, Locale locale) {
		String requestSearchValue = yadaDatatablesRequest.getSearch().getValue();
		String globalSearchString = requestSearchValue!=null?StringUtils.trimToNull(requestSearchValue.toLowerCase(locale)):null;
//		String globalCondition = StringUtils.trimToNull(yadaDatatablesRequest.getGlobalCondition());
		Long globalSearchNumber = null;
		boolean globalSearchEnabled = globalSearchString!=null;
		if (globalSearchEnabled) {
			try {
				globalSearchNumber = Long.parseLong(globalSearchString);
			} catch (NumberFormatException e) {
				// log.debug("Invalid globalSearchString '{}'", globalSearchString);
				// Ignore
			}
		}
		YadaSql yadaSql = yadaDatatablesRequest.getYadaSql();
		YadaSql countSql = (YadaSql) YadaUtil.copyEntity(yadaSql); // Copy any conditions set before calling
		YadaSql searchSql = YadaSql.instance();
		
		yadaSql.selectFrom("select distinct e from "+targetClass.getSimpleName()+" e");
		countSql.selectFrom("select count(*) from "+targetClass.getSimpleName()+" e");
		
		// Searching
		List<YadaDatatablesColumn> yadaDatatablesColumns = yadaDatatablesRequest.getColumns();
		for (YadaDatatablesColumn yadaDatatablesColumn : yadaDatatablesColumns) {
			boolean columnSearchable = yadaDatatablesColumn.isSearchable();
			if (globalSearchEnabled && columnSearchable) {
				// First use the name as the attribute, then use the data
				String attributeName = yadaDatatablesColumn.getNameOrData();
				if (attributeName!=null) {
					try {
						// attributeName could be composite like company.name so we can't get the Field directly
//						Field field = targetClass.getDeclaredField(attributeName);
//						Class attributeType = field.getType();
						Class attributeType = yadaUtil.getType(targetClass, attributeName);
						// Add left joins otherwise Hibernate creates cross joins hence it doesn't return rows with null values
						attributeName = addLeftJoins(attributeName, yadaSql, targetClass);
						if (attributeType == String.class) {
							searchSql.where(attributeName + " like :globalSearchString").or();
						} else if (attributeType == Boolean.class || attributeType == boolean.class) {
							// TODO localizzare "true"/"false" (ma anche no)
							if ("true".indexOf(globalSearchString)>-1) {
								searchSql.where(attributeName + " = true").or();
							} else if ("false".indexOf(globalSearchString)>-1) {
								searchSql.where(attributeName + " = false").or();
							}
						} else if (attributeType == YadaPersistentEnum.class) {
							// No need for KEY(" + attributeName + ") = :yadalang" because it's added by addLeftJoins()
							searchSql.where(attributeName + " like :globalSearchString").or();
							searchSql.setParameter("yadalang", LocaleContextHolder.getLocale().getLanguage());
						} else if (attributeType.isEnum()) {
							// Per gli enum devo ciclare su tutte le chiavi, prendere i valori nel locale corrente, e scoprire quali enum contengono la searchkey
							List<Integer> enumValues = new ArrayList<Integer>();
							for (Object enumConstant : attributeType.getEnumConstants()) {
								String enumText = enumConstant.toString().toLowerCase();
								if (enumText.indexOf(globalSearchString)>-1) {
									enumValues.add(((Enum)enumConstant).ordinal());
								}
							}
							searchSql.whereIn(attributeName, enumValues).or();
						} else if (attributeType == Long.class || attributeType == long.class 
							|| attributeType == Integer.class || attributeType == int.class 
							|| attributeType == BigInteger.class || attributeType == BigDecimal.class
							|| attributeType == Float.class || attributeType == float.class
							|| attributeType == Double.class || attributeType == double.class
							|| attributeType == Short.class || attributeType == short.class
							) {
							// Vorrei fare il match parziale della stringa digitata dentro al valore numerico ma JPA non supporta il like e CONVERT causa un 
							// java.lang.NullPointerException dentro a org.hibernate.hql.internal.antlr.HqlBaseParser.identPrimary(HqlBaseParser.java:4285)
							searchSql.where(globalSearchNumber!=null, attributeName + " = :globalSearchNumber").or();
//							yadaSqlBuilder.addWhere(globalSearchNumber!=null, "CONVERT("+attributeName + " USING utf8) like :globalSearchString", "or");
						} else {
							log.error("Invalid attribute type for {} (skipped) ", attributeName);
						}
					} catch (Exception e) {
						log.error("Can't get attribute type (skipped)", e);
						continue;
					}
				}
			}
		}
		String searchConditions = searchSql.getWhere();
		if (StringUtils.isNotBlank(searchConditions)) {
			yadaSql.where("(" + searchConditions + ")").and();
		}
		// Sorting
		boolean needsExtraction = false;
		List<YadaDatatablesOrder> orderList = yadaDatatablesRequest.getOrder();
		if (orderList!=null) {
			for (YadaDatatablesOrder yadaDatatablesOrder : orderList) {
				int columnIndex = yadaDatatablesOrder.getColumnIndex();
				if (columnIndex>=0) {
					YadaDatatablesColumn yadaDatatablesColumn = yadaDatatablesColumns.get(columnIndex);
					if (yadaDatatablesColumn.isOrderable()) {
						String attributeName = yadaDatatablesColumn.getNameOrData();
						if (attributeName!=null) {
							// Add left joins otherwise Hibernate creates cross joins hence it doesn't return rows with null values
							String sortColumn = addLeftJoins(attributeName, yadaSql, targetClass);
							yadaSql.orderBy(sortColumn + " " + yadaDatatablesOrder.getDir());
							if (attributeName.indexOf('.')>-1) {
								yadaSql.selectFrom(sortColumn); // Needed to avoid "ORDER BY clause is not in SELECT list"
								needsExtraction = true;
							}
							// Class attributeType = yadaUtil.getType(targetClass, attributeName);
						}
					}
				}
			}
		}
		yadaSql.setParameter("globalSearchString", "%"+globalSearchString+"%");
		yadaSql.setParameter("globalSearchNumber", globalSearchNumber);

    	Query query = yadaSql.query(em);
		query.setMaxResults(yadaDatatablesRequest.getLength());
		query.setFirstResult(yadaDatatablesRequest.getStart());
    	@SuppressWarnings("unchecked")
		List<targetClass> result = query.getResultList();
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
    	// Count con where
    	yadaSql.toCount(); // Trasforma in un count
    	query = yadaSql.query(em);
    	long count = (long) query.getSingleResult();
    	yadaDatatablesRequest.setRecordsFiltered(count);
    	// Count senza search
    	query = countSql.query(em);
    	count = (long) query.getSingleResult();
    	yadaDatatablesRequest.setRecordsTotal(count);
    	return result;
	}
	
	/**
	 * From an attribute with a path, like "location.company.name", inserts needed left joins and returns the last segment "company.name".
	 * Joins are inserted for all elements before the last dot: "location" and "company" in the example.
	 * @param attributePath
	 * @param yadaSqlBuilder
	 * @return
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
			if (currentClass.equals(Map.class)) {
				// Need to add a condition on the map key
				String keyValue = parts[1]; // en
				String whereToAdd = "KEY("+alias+")='" + keyValue + "'"; // KEY(name)='en';
				if (yadaSql.getWhere().indexOf(whereToAdd)<0) {
					yadaSql.where(whereToAdd).and();
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
			return addLeftJoinsRecurse(StringUtils.substringAfter(attributePath, "."), current, yadaSql, currentClass);
//			if (!contextClass.equals(Map.class)) {
//				attributeClass = field.getType();
//			} else {
//				// If the targetClass is a map, then the current attribute is the key of the map so there's no need to add a join
//				// but we need to know the type of the value in the map.
//				// TODO don't we need to add a KEY(current)=:xxx? What would xxx be?
//			}
//			return addLeftJoinsRecurse(StringUtils.substringAfter(attributePath, "."), current, yadaSql, attributeClass);
		} else {
//			try {
				// Last element of the path - if it's a YadaPersistentEnum we still need a join for the map
				// if (yadaUtil.getType(targetClass, attributePath) == YadaPersistentEnum.class) {
				if (YadaPersistentEnum.class.equals(currentClass)) {
					yadaSql.join("left join " + context + "." + attributePath + " " + attributePath); 	// left join user.status status
					yadaSql.join("left join " + attributePath + ".langToText langToText");				// left join status.langToText langToText
					String whereToAdd = "KEY(langToText)=:yadalang";
					if (yadaSql.getWhere().indexOf(whereToAdd)<0) {
						yadaSql.where(whereToAdd).and();
						yadaSql.setParameter("yadalang", LocaleContextHolder.getLocale().getLanguage());
					}
					return "langToText";
				}
//			} catch (NoSuchFieldException e) {
//				log.error("No field {} found on class {} (ignored)", attributePath, targetClass.getName());
//			}
			return context + "." + attributePath; // e.phone, company.name
		}
	}

}
