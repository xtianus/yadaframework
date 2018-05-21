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
			json.put("error", e.toString()); // TODO handle the error in javascript 
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
				String attributePath = column.getNameOrData();
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
				if (field==null) {
					log.debug("Field {} in entity {} not found (ignored)", attributeName, entity.getClass());
					return;
				}
				field.setAccessible(true);
				value = field.get(entity);
				// The old version
				//	if (value instanceof java.util.Map) {
				//		ParameterizedType type = (ParameterizedType) field.getGenericType();
				//		keyType = type.getActualTypeArguments()[0];
				//	}
			}
				
			if (parts.length==1) {
				// End of the path
				entityJson.put(attributeName, value.toString());
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
		String globalSearchString = StringUtils.trimToNull(yadaDatatablesRequest.getSearch().getValue().toLowerCase(locale));
//		String globalCondition = StringUtils.trimToNull(yadaDatatablesRequest.getGlobalCondition());
		Long globalSearchNumber = null;
		boolean globalSearchEnabled = globalSearchString!=null;
		if (globalSearchEnabled) {
			try {
				globalSearchNumber = Long.parseLong(globalSearchString);
			} catch (NumberFormatException e) {
				log.debug("Invalid globalSearchString '{}'", globalSearchString);
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
		List<YadaDatatablesOrder> orderList = yadaDatatablesRequest.getOrder();
		for (YadaDatatablesOrder yadaDatatablesOrder : orderList) {
			int columnIndex = yadaDatatablesOrder.getColumnIndex();
			if (columnIndex>=0) {
				YadaDatatablesColumn yadaDatatablesColumn = yadaDatatablesColumns.get(columnIndex);
				if (yadaDatatablesColumn.isOrderable()) {
					String attributeName = yadaDatatablesColumn.getNameOrData();
					if (attributeName!=null) {
						// Add left joins otherwise Hibernate creates cross joins hence it doesn't return rows with null values
						attributeName = addLeftJoins(attributeName, yadaSql, targetClass);
						yadaSql.orderBy(attributeName + " " + yadaDatatablesOrder.getDir());
						// Class attributeType = yadaUtil.getType(targetClass, attributeName);
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

	private String addLeftJoinsRecurse(String attributePath, String context, YadaSql yadaSql, Class targetClass) {
		String[] parts = attributePath.split("\\.");
		if (parts.length>1) { // location.company.name
			String current = parts[0]; // location
			yadaSql.join("left join " + context + "." + current + " " + current); // e.location location
			return addLeftJoinsRecurse(StringUtils.substringAfter(attributePath, "."), current, yadaSql, targetClass);
		} else {
			try {
				// Last element of the path - if it's a YadaPersistentEnum we still need a join for the map
				if (yadaUtil.getType(targetClass, attributePath) == YadaPersistentEnum.class) {
					yadaSql.join("left join " + context + "." + attributePath + " " + attributePath); 	// left join user.status status
					yadaSql.join("left join " + attributePath + ".langToText langToText");				// left join status.langToText langToText
					String whereToAdd = "KEY(langToText)=:yadalang";
					if (yadaSql.getWhere().indexOf(whereToAdd)<0) {
						yadaSql.where(whereToAdd).and();
						yadaSql.setParameter("yadalang", LocaleContextHolder.getLocale().getLanguage());
					}
					return "langToText";
				}
			} catch (NoSuchFieldException e) {
				log.error("No field {} found on class {} (ignored)", attributePath, targetClass.getName());
			}
			return context + "." + attributePath; // e.phone, company.name
		}
	}

}
