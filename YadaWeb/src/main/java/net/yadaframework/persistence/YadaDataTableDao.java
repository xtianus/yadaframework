package net.yadaframework.persistence;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.web.YadaDatatablesColumn;
import net.yadaframework.web.YadaDatatablesOrder;
import net.yadaframework.web.YadaDatatablesRequest;

@Repository
@Transactional(readOnly = true) 
//TODO non considera il search su singola colonna "columns[i][search][value]", nel senso che usa solo il global search su tutte le colonne searchable
//TODO gestire il search/sort per gli enum (vedi OneNote RIN TODO) 
//TODO gestire i parametri nestati più di 2 livelli (e.g. company.location.name)
public class YadaDataTableDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired MessageSource messageSource;
	@Autowired YadaUtil yadaUtil;
	
    @PersistenceContext EntityManager em;
	
    /**
     * Ritorna la mappa dei valori come richiesta da DataTables
     * @param yadaDatatablesRequest
     * @param entityClass
     * @param locale
     * @return
     */
	public Map<String, Object> getJsonPage(YadaDatatablesRequest yadaDatatablesRequest, Class<?> entityClass, Locale locale) {
		Map<String, Object> json = new HashMap<String, Object>();
		try {
			List data = getPage(yadaDatatablesRequest, entityClass, em, locale);
			json.put("draw", yadaDatatablesRequest.getDraw());
			json.put("recordsTotal", yadaDatatablesRequest.getRecordsTotal());
			json.put("recordsFiltered", yadaDatatablesRequest.getRecordsFiltered());
			json.put("data", data);
		} catch (Exception e) {
			log.error("Impossibile recuperare la pagina di dati", e);
			json.put("error", e.toString()); 
		}
		return json;
	}
 
	/**
	 * Preleva una pagina di risultati che rispettino i parametri inviati via web.
	 * Il nome delle colonne su cui fare le query e il sort viene preso da "name" oppure da "data" di DataTables.
	 * @param yadaDatatablesRequest parametri della query, conterrà anche il count finale
	 * @param targetClass tipo dell'oggetto da cercare
	 * @param em
	 * @return Pagina di oggetti di tipo targetClass. Il numero totale è reperibile in yadaDatatablesRequest.recordsFiltered
	 */
	@SuppressWarnings("rawtypes")
	protected List getPage(YadaDatatablesRequest yadaDatatablesRequest, Class targetClass, EntityManager em, Locale locale) {
		//
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
		
		yadaSql.selectFrom("select e from "+targetClass.getSimpleName()+" e");
		countSql.selectFrom("select count(*) from "+targetClass.getSimpleName()+" e");
		
		// Searching
		List<YadaDatatablesColumn> yadaDatatablesColumns = yadaDatatablesRequest.getColumns();
		for (YadaDatatablesColumn yadaDatatablesColumn : yadaDatatablesColumns) {
			boolean columnSearchable = yadaDatatablesColumn.isSearchable();
			if (globalSearchEnabled && columnSearchable) {
				// First use the name as the attribute, then use the data
				String attributeName = StringUtils.trimToNull(yadaDatatablesColumn.getNameOrData());
				if (attributeName!=null) {
					try {
						// attributeName could be composite like company.name so we can't get the Field directly
//						Field field = targetClass.getDeclaredField(attributeName);
//						Class attributeType = field.getType();
						Class attributeType = yadaUtil.getType(targetClass, attributeName);
						// Add left joins otherwise Hibernate creates cross joins hence it doesn't return rows with null values
						attributeName = addLeftJoins(attributeName, yadaSql);
						if (attributeType == String.class) {
							yadaSql.where(attributeName + " like :globalSearchString").or();
						} else if (attributeType == Boolean.class || attributeType == boolean.class) {
							// TODO localizzare "true"/"false" (ma anche no)
							if ("true".indexOf(globalSearchString)>-1) {
								yadaSql.where(attributeName + " = true").or();
							} else if ("false".indexOf(globalSearchString)>-1) {
								yadaSql.where(attributeName + " = false").or();
							}
						} else if (attributeType.isEnum()) {
							// Per gli enum devo ciclare su tutte le chiavi, prendere i valori nel locale corrente, e scoprire quali enum contengono la searchkey
							List<Integer> enumValues = new ArrayList<Integer>();
							for (Object enumConstant : attributeType.getEnumConstants()) {
								String enumText = enumConstant.toString().toLowerCase();
								if (enumText.indexOf(globalSearchString)>-1) {
									enumValues.add(((Enum)enumConstant).ordinal());
								}
							}
							yadaSql.whereIn(attributeName, enumValues).or();
						} else if (attributeType == Long.class || attributeType == long.class 
							|| attributeType == Integer.class || attributeType == int.class 
							|| attributeType == BigInteger.class || attributeType == BigDecimal.class
							|| attributeType == Float.class || attributeType == float.class
							|| attributeType == Double.class || attributeType == double.class
							|| attributeType == Short.class || attributeType == short.class
							) {
							// Vorrei fare il match parziale della stringa digitata dentro al valore numerico ma JPA non supporta il like e CONVERT causa un 
							// java.lang.NullPointerException dentro a org.hibernate.hql.internal.antlr.HqlBaseParser.identPrimary(HqlBaseParser.java:4285)
							yadaSql.where(globalSearchNumber!=null, attributeName + " = :globalSearchNumber").or();
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
		// Sorting
		List<YadaDatatablesOrder> orderList = yadaDatatablesRequest.getOrder();
		for (YadaDatatablesOrder yadaDatatablesOrder : orderList) {
			int columnIndex = yadaDatatablesOrder.getColumnIndex();
			if (columnIndex>=0) {
				YadaDatatablesColumn yadaDatatablesColumn = yadaDatatablesColumns.get(columnIndex);
				if (yadaDatatablesColumn.isOrderable()) {
					String attributeName = StringUtils.trimToNull(yadaDatatablesColumn.getNameOrData());
					if (attributeName!=null) {
						// Add left joins otherwise Hibernate creates cross joins hence it doesn't return rows with null values
						attributeName = addLeftJoins(attributeName, yadaSql);
						yadaSql.orderBy(attributeName + " " + yadaDatatablesOrder.getDir());
					}
				}
			}
		}
		yadaSql.setParameter("globalSearchString", "%"+globalSearchString+"%");
		yadaSql.setParameter("globalSearchNumber", globalSearchNumber);

    	Query query = yadaSql.query(em);
		query.setMaxResults(yadaDatatablesRequest.getLength());
		query.setFirstResult(yadaDatatablesRequest.getStart());
    	List result = query.getResultList();
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
	
	private String addLeftJoins(String attributePath, YadaSql yadaSqlBuilder) {
		if (attributePath==null) {
			return null;
		}
		return addLeftJoinsRecurse(attributePath, "e", yadaSqlBuilder);
	}

	private String addLeftJoinsRecurse(String attributePath, String context, YadaSql yadaSql) {
		String[] parts = attributePath.split("\\.");
		if (parts.length>1) { // location.company.name
			String current = parts[0]; // location
			yadaSql.join("left join " + context + "." + current + " " + current); // e.location location
			return addLeftJoinsRecurse(StringUtils.substringAfter(attributePath, "."), current, yadaSql);
		} else {
			return context + "." + attributePath; // e.phone, company.name
		}
	}

}