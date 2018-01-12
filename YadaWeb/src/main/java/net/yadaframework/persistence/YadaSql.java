package net.yadaframework.persistence;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import net.yadaframework.core.CloneableDeep;
import net.yadaframework.exceptions.YadaInternalException;

/**
 * Incrementally and conditionally builds a sql select/update query
 */
// http://dev.mysql.com/doc/refman/5.7/en/select.html
public class YadaSql implements CloneableDeep {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	YadaSql parent = null; // Used for subexpressions
	boolean enabled = true; // Used for subexpressions
	StringBuilder queryBuffer = new StringBuilder();
	StringBuilder joins = new StringBuilder();
	StringBuilder whereConditions = new StringBuilder();
	StringBuilder havingConditions = new StringBuilder();
	String groupBy = "";
	String orderBy = "";
	Integer limit = null;
	boolean nowInHaving = false;
	boolean lastSkipped = false;
	String pendingWhereOperand = null;
	String pendingHavingOperand = null;
	Boolean nativeQuery;
	Map<String, Object> insertValues = new HashMap<>();
	Map<String, Object> parameters = new HashMap<>();
	boolean queryDone = false;
	List<YadaSql> unions = new ArrayList<>();
//	Map<String, String> aliasMap = new HashMap<>();
	
	private YadaSql() {
	}
	
	private YadaSql(YadaSql parent, boolean enabled) {
		this.parent = parent;
		this.enabled = enabled;
	}
	
	/**
	 * Add a query as a union of the current one.
	 * @param unioned the query to add as a union
	 * @return the current object
	 */
	public YadaSql union(YadaSql unioned) {
		unions.add(unioned);
		return this;
	}

	private YadaSql appendQuery(String text) {
		lastSkipped = false;
		queryBuffer.append(text).append(" ");
		return this;
	}
	
	private YadaSql appendSection(StringBuilder builder, String sectionOperand, String text) {
		lastSkipped = false;
		if (this.parent == null) {
			// Not a subexpression: add the operand (e.g. 'where') if not added
			if (builder.indexOf(sectionOperand)<0) {
				builder.append(sectionOperand);
			}
		} else {
			// In a subexpression add the operand if there is a select or an update, otherwise strip it
			if (builder.indexOf(sectionOperand)<0 && (queryBuffer.indexOf("from")>-1 || queryBuffer.indexOf("update")>-1)) {
				builder.append(sectionOperand);
			}
		}
		// Remove the operand if present, because added above if needed
		if (text.toLowerCase().startsWith(sectionOperand)) {
			text = text.substring(sectionOperand.length());
		}
		builder.append(text).append(" ");
		return this;
	}

	private YadaSql appendWhere(String text, String startingWhere) {
		if (StringUtils.isNotEmpty(text)) {
			nowInHaving=false;
			if (pendingWhereOperand!=null) {
				String operand = pendingWhereOperand;
				pendingWhereOperand = null;
				appendWhere(operand, "");
			}
			return appendSection(whereConditions, startingWhere, text);
		}
		return this;
	}
	
	private YadaSql appendHaving(String text) {
		if (StringUtils.isNotEmpty(text)) {
			nowInHaving=true;
			if (pendingHavingOperand!=null) {
				String operand = pendingHavingOperand;
				pendingHavingOperand = null;
				appendHaving(operand);
			}
			return appendSection(havingConditions, "having ", text);
		}
		return this;
	}
	
	/**
	 * Start a "delete from ..." query
	 * @param enabled
	 * @param deleteFrom like "delete from User"
	 * @return
	 */
	public YadaSql deleteFrom(boolean enabled, String deleteFrom) {
		if (enabled) {
			return appendQuery(deleteFrom);
		}
		return this;
	}
	
	/**
	 * Start a "delete from ..." query
	 * @param deleteFrom like "delete from User"
	 * @return
	 */
	public YadaSql deleteFrom(String deleteFrom) {
		return deleteFrom(true, deleteFrom);
	}
	
	/**
	 * Start a "select ... from ..." query
	 * @param enabled
	 * @param selectFrom
	 * @return
	 */
	public YadaSql selectFrom(boolean enabled, String selectFrom) {
		if (enabled) {
			return appendQuery(selectFrom);
		}
		return this;
	}
	
	// insert removed because it should handle more parameter types and most of all special characters like ' in the value
	
//	/**
//	 * Start a "insert ... values ..." query
//	 * @param enabled
//	 * @param tableName the table where to insert
//	 * @return
//	 */
//	public YadaSql insertInto(boolean enabled, String tableName) {
//		if (enabled) {
//			return appendQuery("insert into " + tableName);
//		}
//		return this;
//	}
//	
//	public YadaSql insertInto(boolean enabled, String columnName, String value) {
//		if (enabled) {
//			insertValues.put(columnName, value);
//		}
//		return this;
//	}
//	
//	public YadaSql insertInto(boolean enabled, String columnName, Long value) {
//		if (enabled) {
//			insertValues.put(columnName, value);
//		}
//		return this;
//	}
//	
//	public YadaSql insertInto(boolean enabled, String columnName, Integer value) {
//		if (enabled) {
//			insertValues.put(columnName, value);
//		}
//		return this;
//	}
//	
//	public YadaSql insertInto(boolean enabled, String columnName, Object value) {
//		if (enabled) {
//			insertValues.put(columnName, value);
//		}
//		return this;
//	}
//	
//	// TODO more parameter types
	
	/**
	 * Start a "select ... from ..." query
	 * @param selectFrom
	 * @return
	 */
	public YadaSql selectFrom(String selectFrom) {
		return appendQuery(selectFrom);
	}
	
	/**
	 * Continues an update - set query with name=value, e.g. "name=:usename"
	 * @param nameValue
	 * @return
	 */
	public YadaSql set(String nameValue) {
		String currentQuery = queryBuffer.toString().trim();
		if (!currentQuery.endsWith("set") && !currentQuery.endsWith(",")) {
			queryBuffer.append(", ");
		}
		return appendQuery(nameValue);
	}
	
	/**
	 * Continues an update - set query with name=value, e.g. "name=:usename"
	 * @param enabled
	 * @param nameValue
	 * @return
	 */
	public YadaSql set(boolean enabled, String nameValue) {
		if (enabled) {
			return set(nameValue);
		}
		return this;
	}
	
	/**
	 * Start a "update ... set ..." query
	 * @param updateSet e.g. "update Uset set name=:username"
	 * @return
	 */
	public YadaSql updateSet(String updateSet) {
		return appendQuery(updateSet);
	}
	
	/**
	 * Returns an empty YadaSql for later use
	 * @return
	 */
	public static YadaSql instance() {
		YadaSql yadaSql = new YadaSql();
		return yadaSql;
	}
	
	/**
	 * Adds a join condition if not already added
	 * @param enabled false to prevent the join from being added. Can be null or empty.
	 * @param joinOn
	 * @return
	 * @see #join(String)
	 */
	public YadaSql join(boolean enabled, String joinOn) {
		if (enabled) {
			return join(joinOn);
		}
		return this;
	}
	
	/**
	 * Adds a join condition if not already added
	 * @param joinOn a join that could include a ON operand, like "left join User on e.uid = u.id". Can be null or empty.
	 * @return
	 */
	public YadaSql join(String joinOn) {
		if (StringUtils.isNotEmpty(joinOn)) {
			nowInHaving=false;
			if (joins.indexOf(joinOn)<0) {
				joins.append(joinOn).append(" ");
			}
		}
		return this;
	}
	
	/**
	 * Adds a where condition
	 * @param whereConditions a condition like "where a>0" or just "a>0", can be null or empty
	 * @return
	 */
	public YadaSql where(String whereConditions) {
		return appendWhere(whereConditions, "where ");
	}

	/**
	 * Adds a where condition
	 * @param enabled
	 * @param whereConditions a condition like "where a>0" or just "a>0"
	 * @return
	 */
	public YadaSql where(boolean enabled, String whereConditions) {
		lastSkipped = !enabled;
		if (enabled) {
			return where(whereConditions);
		}
		return this;
	}
	
	/**
	 * Adds a where condition only if the collection is not null and non empty
	 * @param enabled
	 * @param whereConditions a condition like "where a in :someCollection"
	 * @return
	 */
	public YadaSql whereNotEmpty(Collection collection, String whereConditions) {
		return where(collection!=null && !collection.isEmpty(), whereConditions);
	}
	
	/**
	 * Add a "where aaa in (x, y, z)" clause. Skipped if the collection is null or empty.
	 * The collection is converted to a comma-separated strings.
	 * <b>NOTE:</b> It is always better to use collections as a parameter, like "aaa in :someCollection"
	 * @param attributeName attribute or column name
	 * @param values a list of values (e.g. integers)
	 */
	public YadaSql whereIn(String attributeName, Collection values) {
		if (CollectionUtils.isNotEmpty(values)) {
			String valueListString = StringUtils.join(values, ',');
			where(attributeName + " in ("+valueListString+")");
		}
		return this;
	}
	
	/**
	 * Add a "where aaa in (select ...)" clause.
	 * @param attributeName attribute (can also be a collection) or column name
	 * @param subselect a subselect that returns any number of results compatibile with attributeName. Parameters set on the subquery are carried over.
	 * @return
	 */
	public YadaSql whereIn(String attributeName, YadaSql subselect) {
		where(attributeName + " in ("+subselect.sql()+")");
		this.parameters.putAll(subselect.parameters);
		return this;
	}
	
	/**
	 * Adds a having condition
	 * @param havingConditions, can be null or empty
	 * @return
	 */
	public YadaSql having(String havingConditions) {
		return appendHaving(havingConditions);
	}
	
	/**
	 * 
	 * @param enabled
	 * @param havingConditions a condition like "having a>0" or just "a>0"
	 * @return
	 */
	public YadaSql having(boolean enabled, String havingConditions) {
		lastSkipped = !enabled;
		if (enabled) {
			return having(havingConditions);
		}
		return this;
	}
	
	public YadaSql and(boolean enabled) {
		if (enabled) {
			return and();
		}
		return this;
	}
	
	public YadaSql and() {
		if (lastSkipped) {
			lastSkipped=false;
			return this;
		}
		if (!nowInHaving) {
			pendingWhereOperand = "and";
		} else {
			pendingHavingOperand = "and";
		}
		return this;
	}
	
	public YadaSql or(boolean enabled) {
		if (enabled) {
			return or();
		}
		return this;
	}
	
	public YadaSql or() {
		if (lastSkipped) {
			lastSkipped=false;
			return this;
		}
		if (!nowInHaving) {
			pendingWhereOperand = "or";
		} else {
			pendingHavingOperand = "or";
		}
		return this;
	}
	
	public YadaSql xor(boolean enabled) {
		if (enabled) {
			return xor();
		}
		return this;
	}
	
	public YadaSql xor() {
		if (lastSkipped) {
			lastSkipped=false;
			return this;
		}
		if (!nowInHaving) {
			pendingWhereOperand = "xor";
		} else {
			pendingHavingOperand = "xor";
		}
		return this;
	}
	
	/**
	 * Starts a subexpression. Be careful that the returned YadaSql object is different from the original one, so don't use the original one for ending the subexpression.
	 * @return
	 */
	public YadaSql startSubexpression() {
		return startSubexpression(true);
	}
	
	/**
	 * Starts a subexpression. Be careful that the returned YadaSql object is different from the original one, so don't use the original one for ending the subexpression.
	 * @param enabled when false, the whole content of the subexpression will be ignored
	 * @return
	 */
	public YadaSql startSubexpression(boolean enabled) {
		return new YadaSql(this, enabled);
//			if (!nowInHaving) {
//				return appendWhere("(");
//			} else {
//				return appendHaving("(");
//			}
	}

	/**
	 * Ends a where/having subexpression. To be called on the object returned by the {@link #startSubexpression()} method (or any method called on that object)
	 * @return
	 */
	public YadaSql endSubexpression() {
		return endSubexpression(null);
	}
	
	/**
	 * Ending a subexpression is always safe even if the startSubexpression had an "enabled" condition
	 * @param enabled is ignored
	 * @return
	 */
	@Deprecated
	public YadaSql endSubexpression(boolean enabled) {
		return endSubexpression();
	}
	
	/**
	 * Ends a where/having subexpression. To be called on the object returned by the {@link #startSubexpression()} method (or any method called on that object)
	 * @param alias the table alias, like in "select alias.a+alias.c from ( select 2*b as a ...) alias"
	 * @return
	 */
	public YadaSql endSubexpression(String alias) {
		if (this.enabled) {
			String sql = this.sql();
			if (!sql.isEmpty()) {
				sql = "(" + sql + ")";
				if (alias!=null) {
					// Must be a "from" subexpression like "select a from ( select ...) myTable"
					parent.appendQuery(sql + " " +alias);
					return parent;
				}
				if (!nowInHaving) {
					parent.where(sql);
				} else {
					parent.having(sql);
				}
			}
		}
		return parent;
	}
	
	public YadaSql groupBy(boolean enabled, String groupBy) {
		if (enabled) {
			return groupBy(groupBy);
		}
		return this;
	}
	
	/**
	 * 
	 * @param groupBy can be empty or null
	 * @return
	 */
	public YadaSql groupBy(String groupBy) {
		if (StringUtils.isNotEmpty(groupBy)) {
	 		nowInHaving=false;
			boolean firstGroupBy = this.groupBy.equals("");
			if (firstGroupBy && !groupBy.toLowerCase().startsWith("group by ")) {
				groupBy = "group by " + groupBy;
			}
			if (!firstGroupBy) {
				groupBy = ", " + groupBy;
			}
			this.groupBy += groupBy;
		}
		return this;
	}
	
	/**
	 * 
	 * @param orderBy can be empty or null
	 * @return
	 */
	public YadaSql orderBy(String orderBy) {
		if (StringUtils.isNotEmpty(orderBy)) {
			nowInHaving=false;
			boolean firstOrderBy = this.orderBy.equals("");
			if (firstOrderBy && !orderBy.toLowerCase().startsWith("order by ")) {
				orderBy = "order by " + orderBy;
			}
			if (!firstOrderBy) {
				orderBy = ", " + orderBy;
			}
			this.orderBy += orderBy;
		}
		return this;
	}
	
	
	/**
	 * Add sorting from a Spring Data Pageable
	 * @param pageable
	 * @return
	 */
	public YadaSql orderBy(Pageable pageable) {
		Iterator<Sort.Order> orders = pageable.getSort().iterator();
		while (orders.hasNext()) {
			Sort.Order order = orders.next();
			orderBy(order.getProperty() + " " + order.getDirection());
		}
		return this;
	}
	
	/**
	 * Set a limit
	 * @param limit the limit, or null for not doing anything
	 * @return
	 */
	public YadaSql limit(Integer limit) {
		nowInHaving=false;
		this.limit = limit;
		return this;
	}
	
	public YadaSql clearWhere() {
		whereConditions = new StringBuilder();
		return this;
	}
	
	/**
	 * Transforms a "select ... from" to a "select count(*) from"
	 * @return
	 */
	public YadaSql toCount() {
		return toCount(null);
	}
	
	/*
	 * Transforms a "select ... from" to a "select count(*) from"
	 * @param sql null for "select count(*)", or something like "select count(distinct e)"
	 */
	public YadaSql toCount(String sql) {
		int pos = queryBuffer.indexOf("from");
		queryBuffer.delete(0, pos);
		if (sql!=null) {
			queryBuffer.insert(0, sql + " ");
		} else {
			queryBuffer.insert(0, "select count(*) ");
		}
	    this.orderBy = "";
	    this.limit = null;
	    this.groupBy = "";
	    return this;
	}
	
	private void fixQuery(Query query) {
		queryDone = true;
		setAllParameters(query);
		if (limit!=null) {
			query.setMaxResults(limit);
		}
	}
	
	public Query nativeQuery(EntityManager em) {
		nativeQuery = true;
		Query query = em.createNativeQuery(sql());
		fixQuery(query);
		return query;
	}
	
	/**
	 * Create a native query that returns object instances
	 * @param em
	 * @param targetClass the result class
	 * @return
	 */
	public Query nativeQuery(EntityManager em, Class targetClass) {
		nativeQuery = true;
		Query query = em.createNativeQuery(sql(), targetClass);
		fixQuery(query);
		return query;
	}
	
	/**
	 * Create an instance of Query for executing a Java Persistence query language statement.
	 * @param em
	 * @return
	 * @see EntityManager#createQuery(String)
	 */
	public Query query(EntityManager em) {
		nativeQuery = false;
		Query query = em.createQuery(sql());
		fixQuery(query);
		return query;
	}
	
	/**
	 * Create an instance of TypedQuery for executing a Java Persistence query language statement. The select list of the query must contain only a single item, which must be assignable to the type specified by the resultClass argument.
	 * @param em
	 * @param targetClass
	 * @return
	 * @see EntityManager#createQuery(String, Class)
	 */
	public <T> TypedQuery<T> query(EntityManager em, Class<T> targetClass) {
		nativeQuery = false;
		TypedQuery<T> query = em.createQuery(sql(), targetClass);
		fixQuery(query);
		return query;
	}
	
	private void setAllParameters(Query query) {
		for (String name : parameters.keySet()) {
			if (hasParameter(name)) {
				query.setParameter(name, parameters.get(name));
			}
		}
		for (YadaSql unioned : unions) {
			unioned.setAllParameters(query);
		}
	}
	
	private boolean hasParameter(String parameter) {
		String regexp = ".*:"+parameter+"\\b.*";
		if (whereConditions.toString().matches(regexp)) {
			return true;
		}
		if (havingConditions.toString().matches(regexp)) {
			return true;
		}
		if (queryBuffer.toString().matches(regexp)) {
			return true; // for cases like "update UserProfile set company_id=:companyId"
		}
		return false;
	}

	/**
	 * To be used before calling query() or nativeQuery()
	 * @param name the parameter name, without the initial :
	 * @param value
	 * @return
	 */
	public YadaSql setParameter(String name, Object value) {
		if (queryDone) {
			throw new YadaInternalException("Parameters should be set before calling query()");
		}
		parameters.put(name, value);
		return this;
	}
	
	/**
	 * Add all the joins, where, having, group, order statements and parameters of another YadaSql object
	 * @param yadaSql the object to get items from, can be null to no nothing
	 * @return
	 */
	public YadaSql add(YadaSql yadaSql) {
		if (yadaSql!=null) {
			this.join(yadaSql.joins.toString());
			this.where(yadaSql.getWhere());
			this.pendingWhereOperand = yadaSql.pendingWhereOperand;
			this.having(yadaSql.havingConditions.toString());
			this.pendingHavingOperand = yadaSql.pendingHavingOperand;
			this.groupBy(yadaSql.groupBy);
			this.orderBy(yadaSql.orderBy);
			this.parameters.putAll(yadaSql.parameters);
		}
		return this;
	}
	
	public YadaSql setParameter(String name, String[] values) {
		return setParameter(name, Arrays.asList(values));
	}
	
	public YadaSql setParameter(String name, Long[] values) {
		return setParameter(name, Arrays.asList(values));
	}
	
	public YadaSql setParameterNotNull(String name, Object value) {
		if (queryDone) {
			throw new YadaInternalException("Parameters should be set before calling query()");
		}
		if (value!=null) {
			parameters.put(name, value);
		}
		return this;
	}
	
	/**
	 * Returns the resulting sql
	 * @return
	 */
	public String sql() {
		return sql(null, null);
	}
	
	/**
	 * Sets the whole query. Useful for debugging.
	 * @param query
	 */
	public void overwriteQuery(String query) {
		queryBuffer = new StringBuilder(query);
		joins = new StringBuilder();
		whereConditions = new StringBuilder();
		havingConditions = new StringBuilder();
		groupBy = "";
		orderBy = "";
		limit = null;
	}
	
	/**
	 * Returns the resulting sql after replacing aliases. It can be used when reusing an existing query with a new alias, to get something like 
	 * "select u from User u where u.type=1 and u.age = (select min(u2.age) from User u2 where u2.type=1)". In this case, create a query "where XX.type=1" and use it
	 * twice, first replacing "XX" with "u", then replacing "XX" with "u2".
	 * @param oldAliasName existing alias name (usually a placeholder) like "XX" in "from User XX where XX.name=...". Be sure to use unique character sequences.
	 * @param newAliasName replacement alias name, like "user" to transform the previous example in "from User user where user.name=..."
	 * @param oldToNew additional couples of alias substitutions, like "YY", "part", "ZZ", "cost"
	 * @return
	 */
	public String sql(String oldAliasName, String newAliasName, String... oldToNew) {
		StringBuilder builder = new StringBuilder(queryBuffer);
//		if (!insertValues.isEmpty()) {
//			processInsert(builder);
//		} else {
			builder.append(joins);
			builder.append(whereConditions);
			builder.append(havingConditions);
			builder.append(groupBy);
			builder.append(orderBy);
			if (parent == null && log.isDebugEnabled()) {
				log.debug(builder.toString());
			}
//		}
		for (YadaSql unioned : unions) {
			builder.append(" union ");
			builder.append(unioned.sql(oldAliasName, newAliasName, oldToNew));
		}
		String result = builder.toString().trim();
		if (oldAliasName!=null && newAliasName!=null) {
			result = result.replaceAll(oldAliasName, newAliasName);
			for (int i=0; i<oldToNew.length; i+=2) {
				result = result.replaceAll(oldToNew[i], oldToNew[i+1]);
			}
		}
		return result;
	}
	
//	private void processInsert(StringBuilder builder) {
//		builder.append("(");
//		for (String column : insertValues.keySet()) {
//			builder.append(column + ",");
//		}
//		builder.deleteCharAt(builder.length()-1);
//		builder.append(") values (");
//		for (String column : insertValues.keySet()) {
//			Object value = insertValues.get(column);
//			if (value instanceof String) {
//				builder.append("'");
//			}
//			builder.append(value);
//			if (value instanceof String) {
//				builder.append("'");
//			}
//			builder.append(",");
//		}
//		builder.deleteCharAt(builder.length()-1);
//		builder.append(") ");
//	}

	@Override
	public Field[] getExcludedFields() {
		return null;
	}

	@Override
	public String toString() {
		return sql();
	}

//	/**
//	 * Sets an alias used in the query. Needed to reuse the same
//	 * @param aliasPlaceholder the alias placeholder, like "ENTITY"
//	 * @param aliasName the alias value, like "u"
//	 */
//	public void addAlias(String aliasPlaceholder, String aliasName) {
//		aliasMap.put(aliasPlaceholder, aliasName);
//	}
	
	/**
	 * Returns the "where" section of the query, not starting with "where"
	 * @return
	 */
	public String getWhere() {
		return StringUtils.removeStart(whereConditions.toString(), "where ");
	}

}
