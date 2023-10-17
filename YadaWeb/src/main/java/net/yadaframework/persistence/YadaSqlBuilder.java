package net.yadaframework.persistence;
import java.awt.print.Pageable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yadaframework.core.CloneableFiltered;
import net.yadaframework.exceptions.InternalException;
import net.yadaframework.web.YadaPageRequest;
import net.yadaframework.web.YadaPageSort;

/**
 * Classe di utilità che costruisce una stringa sql o jpql partendo dagli elementi che la compongono, opzionalmente presenti.
 * Si utilizza istanziandola con new() visto che non può essere un singleton.
 * @see YadaSql
 */
@Deprecated // Use YadaSql instead
public class YadaSqlBuilder implements CloneableFiltered {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	String select = null;
	String from = null;
	String joins = "";
	String update = null;
	String set = null;
	List<String> whereConditions = new ArrayList<String>();
	List<String> whereOperators = new ArrayList<String>();
	String groupby = null;
	List<String> havingConditions = new ArrayList<String>();
	List<String> havingOperators = new ArrayList<String>();
	String orderAndLimit = null;
	Map<String, Object> parameters = new HashMap<>();
	
	/**
	 * Set a query parameter if the parameter itself has actually been used in the query.
	 * @param paramName like "company"
	 * @param value
	 * @param query
	 */
	public void setParameter(String paramName, Object value, Query query) {
		if (hasParameter(paramName)) {
			query.setParameter(paramName, value);
		}
	}
	
	/**
	 * Set all parameters previously added, but ony if predent in the query
	 * @param query
	 * @see addParameter(String name, Object value)
	 */
	public void setParameters(Query query) {
		for (String name : parameters.keySet()) {
			this.setParameter(name, parameters.get(name), query);
		}
	}
	
	/**
	 * Add a parameter to be later set on the query
	 * @param name the parameter name without leading :
	 * @param value
	 */
	public void addParameter(String name, Object value) {
		parameters.put(name, value);
	}
	
	/**
	 * Clears all where conditions
	 * @return true if there were any where conditions to clear
	 */
	public boolean clearWhere() {
		boolean result = !this.whereConditions.isEmpty();
		this.whereConditions.clear();
		this.whereOperators.clear();
		return result;
	}
	
	/**
	 * 
	 * @param updateAndTablename e.g. "update Answer"
	 * @param set e.g. "set timegap = (1431001452060 - timestamp)"
	 */
	public void setUpdateAndSet(String updateAndTablename, String set) {
		update = updateAndTablename;
		this.set = set;
	}
	
	/**
	 * Aggiunge un pezzo alla select.
	 * @param selectSegment il pezzo da aggiungere, per esempio "a.name"
	 */
	public void addSelect(String selectSegment) {
		addSelect(true, selectSegment);
	}
	
	/**
	 * Aggiunge un pezzo alla select.
	 * @param condition espressione che indica se aggiungere il pezzo
	 * @param selectSegment il pezzo da aggiungere, per esempio "a.name"
	 */
	public void addSelect(Boolean enabled, String selectSegment) {
		if (enabled) {
			if (StringUtils.trimToNull(select)==null) {
				select = selectSegment;
			} else {
				select = select + ", " + selectSegment;
			}
		}
	}
	
	/**
	 * Do not use for JPA queries because "limit" is not supported. Use query.setMaxResults() instead.
	 * @param pageable
	 */
	public void setOrderAndLimit(YadaPageRequest pageable) {
		setOrder(pageable);
		StringBuffer sqlLimit = new StringBuffer(" limit ");
		sqlLimit.append(pageable.getOffset()).append(",").append(pageable.getPageSize());
		this.orderAndLimit = this.orderAndLimit + sqlLimit.toString();
	}
	
	public void setOrder(YadaPageRequest pageable) {
		StringBuffer sqlOrder = new StringBuffer("order by ");
		Iterator<YadaPageSort.Order> orders = pageable.getPageSort().iterator();
		boolean orderPresent=orders.hasNext();
		while (orders.hasNext()) {
			YadaPageSort.Order order = orders.next();
			sqlOrder.append(order.getProperty()).append(" ").append(order.getDirection());
			if (orders.hasNext()) {
				sqlOrder.append(",");
			}
		}
		this.orderAndLimit = orderPresent?sqlOrder.toString():"";
	}
	
	/**
	 * Shortcut for setting order and limit in one go. Do not use addOrder if limit is set here.
	 * For JPA queries use query.setMaxResults() for limit.
	 * @param orderAndLimit example: "order by xxx asc limit 10"
	 */
	public void setOrderAndLimit(String orderAndLimit) {
		this.orderAndLimit = orderAndLimit;
	}
	
	/**
	 * Append an order. Do not use after setOrderAndLimit.
	 * @param columnName
	 * @param direction
	 */
	public void addOrder(String columnName, String direction) {
		if (StringUtils.trimToNull(this.orderAndLimit)==null) {
			this.orderAndLimit = "order by ";
		} else {
			this.orderAndLimit += ", ";
		}
		this.orderAndLimit += columnName + " " + direction;
	}
	
	/**
	 * 
	 * @param enabled
	 * @param condition
	 * @param operator "and" usually. Can be null.
	 * @return the value of condition
	 */
	public boolean addWhere(boolean enabled, String condition, String operator) {
		if (enabled) {
			addWhere(condition, operator);
		}
		return enabled;
	}

	public void addWhere(String condition) {
		addWhere(condition, null);
	}

	/**
	 * 
	 * @param condition
	 * @param operator "and" usually. Can be null.
	 */
	public void addWhere(String condition, String operator) {
		if (condition!=null) {
			if (condition.toLowerCase().startsWith("where")) {
				condition = condition.substring("where".length());
			}
			whereConditions.add(condition);
			whereOperators.add(operator);
		}
	}
	
	public void addHaving(String condition, String operator) {
		havingConditions.add(condition);
		havingOperators.add(operator);
	}
	
	public String getSql() {
		StringBuilder builder = new StringBuilder();
		if (select!=null) {
			builder.append(select).append(" ");
			if (from!=null) {
				builder.append("from ").append(from).append(" ");
			}
		} else if (update!=null) {
			builder.append(update).append(" ");
			if (set!=null) {
				builder.append(set).append(" ");
			}
		}
		builder.append(joins);
		builder.append(buildConditions("where", whereConditions, whereOperators));
		if (StringUtils.trimToNull(groupby)!=null) {
			builder.append(" ").append(groupby);
		}
		builder.append(buildConditions("having", havingConditions, havingOperators));
		if (StringUtils.trimToNull(orderAndLimit)!=null) {
			builder.append(" ").append(orderAndLimit);
		}
		if (log.isDebugEnabled()) {
			log.debug(builder.toString());
		}
		return builder.toString();
	}
	
	/**
	 * Questo si può usare per costruire una stringa di condizioni per uso estemporaneo
	 * @return
	 */
	public String getWhereConditionsString() {
		return buildConditions(null, whereConditions, whereOperators);
	}
	
	private String buildConditions(String prefix, List<String> conditions, List<String> operators) {
		StringBuilder builder = new StringBuilder();
		if (conditions.size()>0) {
			if (StringUtils.trimToNull(prefix)!=null) {
				builder.append(" ").append(prefix).append(" ");
			}
			for (int i=0; i<conditions.size(); i++) {
				String condition = conditions.get(i);
				builder.append(condition).append(" ");
				if (i<conditions.size()-1) {
					String operator = StringUtils.trimToNull(operators.get(i));
					if (operator!=null) {
						builder.append(operator).append(" ");
					}
				}
			}
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param from la from. La keyword iniziale "from" è opzionale
	 */
	public void setFrom(String from) {
		if (this.from!=null) {
			throw new InternalException("From already set in setFrom()");
		}
		if (from.toLowerCase().startsWith("from")) {
			from = from.substring("from".length());
		}
		this.from = from;
	}
	
	/**
	 * Aggiunge una inner join ... on. Può essere usato anche per una left join e anche per un OQL senza on.
	 * @param join
	 */
	public void addJoinOn(String join) {
		if (join.toLowerCase().indexOf("join")==-1) {
			join = "join " + join;
		}
		this.joins = this.joins + join + " ";
	}

	/**
	 * Aggiunge una inner join
	 * @param enabled
	 * @param join
	 */
	public void addJoinOn(boolean enabled, String join) {
		if (enabled) {
			addJoinOn(join);
		}
	}
	
	/**
	 * Trasforma la query in un count(*), tenendo invariato il from ma cancellando group, order e limit
	 */
	public void setSelectCount() {
	    this.setSelect("select count(*)");
	    this.setOrderAndLimit("");
	    this.setGroupby("");
	}

	/**
	 * 
	 * @param select la select fino al from escluso. La keyword iniziale "select" è opzionale
	 */
	public void setSelect(String select) {
		// Inserisco il select iniziale se manca
		if (!select.toLowerCase().startsWith("select")) {
			select = "select " + select;
		}
		this.select = select;
	}

	public void setSelectFrom(String select, String from) {
		setSelect(select);
		setFrom(from);
	}

	/**
	 * 
	 * @param groupby "group by xxx"
	 */
	public void setGroupby(String groupby) {
		this.groupby = groupby;
	}
	
	/**
	 * Add a "where aaa in (x, y, z)" clause
	 * @param attributeName
	 * @param values a list of values (e.g. integers)
	 * @param operator "and", "or", ... or null
	 */
	public void addWhereIn(String attributeName, Collection values, String operator) {
		if (!values.isEmpty()) {
			String valueListString = StringUtils.join(values, ',');
			addWhere(attributeName + " in ("+valueListString+")", operator);
		}
	}

	/**
	 * Gestisce una where tipo "where s.id not in (select distinct sl.storieLette_id from StorieLette sl where sl.UserProfile_id = 1)"
	 * @param enabled
	 * @param clause
	 * @param subquery
	 * @param operator
	 */
	public void addWhereIn(boolean enabled, String clause, YadaSqlBuilder subquery, String operator) {
		if (enabled) {
			this.addWhere(clause + "(" + subquery.getSql() + ")" , operator);
		}
	}

	public void addHaving(String condition) {
		this.addHaving(condition, null);
	}

	/**
	 * Checks if the query contains a ":named" parameter
	 * @param parameter the name of the parameter, without the initial ":", like "username"
	 * @return true if the parameter has been used in the query
	 */
	public boolean hasParameter(String parameter) {
		String regexp = ".*:"+parameter+"\b.*";
		for (String whereCondition : whereConditions) {
			if (whereCondition.matches(regexp)) {
				return true;
			}
		}
		for (String havingCondition : havingConditions) {
			if (havingCondition.matches(regexp)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add e join section if it hasn't already been added.
	 * The check is made on the literal string so be careful of spaces (case is insensitive though).
	 * @param joinPart i.e. "left join e.company company"
	 */
	public void addJoinIfNotPresent(String joinPart) {
		if (this.joins.toLowerCase().indexOf(joinPart.toLowerCase())==-1) {
			addJoinOn(joinPart);
		}
	}

	@Override
	public Field[] getExcludedFields() {
		// TODO Auto-generated method stub
		return null;
	}
	
	// The following private accessors are needed for Deep Cloning

	private String getJoins() {
		return joins;
	}

	private void setJoins(String joins) {
		this.joins = joins;
	}

	private String getUpdate() {
		return update;
	}

	private void setUpdate(String update) {
		this.update = update;
	}

	private String getSet() {
		return set;
	}

	private void setSet(String set) {
		this.set = set;
	}

	private List<String> getWhereOperators() {
		return whereOperators;
	}

	private void setWhereOperators(List<String> whereOperators) {
		this.whereOperators = whereOperators;
	}

	private List<String> getHavingConditions() {
		return havingConditions;
	}

	private void setHavingConditions(List<String> havingConditions) {
		this.havingConditions = havingConditions;
	}

	private List<String> getHavingOperators() {
		return havingOperators;
	}

	private void setHavingOperators(List<String> havingOperators) {
		this.havingOperators = havingOperators;
	}

	private Map<String, Object> getParameters() {
		return parameters;
	}

	private void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	private String getSelect() {
		return select;
	}

	private String getFrom() {
		return from;
	}

	private String getGroupby() {
		return groupby;
	}

	private String getOrderAndLimit() {
		return orderAndLimit;
	}

	private void setWhereConditions(List<String> whereConditions) {
		this.whereConditions = whereConditions;
	}

	private List<String> getWhereConditions() {
		return whereConditions;
	}

	public Query createNativeQuery(EntityManager em) {
		Query query = em.createNativeQuery(this.getSql());
		this.setParameters(query);
		return query;
	}

	public Query createQuery(EntityManager em) {
		Query query = em.createQuery(this.getSql());
		this.setParameters(query);
		return query;
	}
	
}
