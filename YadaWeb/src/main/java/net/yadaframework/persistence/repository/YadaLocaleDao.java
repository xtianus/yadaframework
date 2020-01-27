package net.yadaframework.persistence.repository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaInvalidValueException;
import net.yadaframework.persistence.YadaSql;

/**
 * Locale-related operations on entities
 */
@Repository
@Transactional(readOnly = true)
public class YadaLocaleDao {

    @Autowired private YadaConfiguration config;
    @Autowired private JdbcTemplate jdbcTemplate;

    @PersistenceContext private EntityManager em;

    /**
     * Finds all entities of the given type, then initializes all localized string attributes defined as Map&lt;Locale, String>
     * @param entityClass
     * @return
     */
    public <entityClass> List<entityClass> findAllWithLocalValues(Class<?> entityClass) {
    	@SuppressWarnings("unchecked")
		List<entityClass> result = (List<entityClass>) YadaSql.instance().selectFrom("from " + entityClass.getSimpleName())
    		.query(em, entityClass).getResultList();

    	YadaUtil.prefetchLocalizedStringList(result, entityClass);
    	return result;
    }

    /**
     * Finds an object of the given id, then initializes all localized string attributes defined as Map&lt;Locale, String>
     * @param entityId
     * @param entityClass
     * @return
     */
    public <entityClass> entityClass findOneWithLocalValues(Long entityId, Class<?> entityClass) {
    	@SuppressWarnings("unchecked")
    	entityClass entity = (entityClass) em.find(entityClass, entityId);
    	if (entity!=null) {
    		YadaUtil.prefetchLocalizedStrings(entity, entityClass);
    	}
    	return entity;
    }

    /**
     * Finds an object of the given id, then initializes all localized string attributes defined as Map&lt;Locale, String>
     * with recursion on the fields.
     * @param entityId
     * @param entityClass
     * @return
     */
   public <entityClass> entityClass findOneWithLocalValuesRecursive(Long entityId, Class<?> entityClass) {
    	@SuppressWarnings("unchecked")
    	entityClass entity = (entityClass) em.find(entityClass, entityId);
    	if (entity!=null) {
    		YadaUtil.prefetchLocalizedStringsRecursive(entity, entityClass);
    	}
    	return entity;
    }

    /**
     * Get the localized value of an entity's attribute
     * @param entity the entity instance
     * @param attributeName the name of the attribute that must be defined as Map<Locale, String>
     * @return the localized value in the current locale, or in the default configured locale if the value is not set for that locale,
     * or an empty string if no value is found.
     */
   public String getLocalValue(Object entity, String attributeName) {
    	return getLocalValue(entity, attributeName, null);
    }

    /**
     * Get the localized value of an entity's attribute
     * @param entity the entity instance
     * @param attributeName the name of the attribute that must be defined as Map<Locale, String>
     * @param locale the locale for the value, use null for the current request locale
     * @param returnNull (optional) true if null must be returned when no value is present instead of the empty string
     * @return the localized value in the specified locale, or in the default configured locale if the value is not set for that locale,
     * or an empty string if no value is found or null if no value is found and returnNull is true.
     */
    public String getLocalValue(Object entity, String attributeName, Locale locale, Boolean... returnNull) {
    	try {
			Class<? extends Object> entityClass = entity.getClass();
			Method getId = entityClass.getMethod("getId");
			Long entityId = (Long) getId.invoke(entity);
			return getLocalValue(entityId, entityClass, attributeName, locale, returnNull);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new YadaInternalException("Can't get local value for object " + entity + " attribute " + attributeName, e);
		}
    }

    /**
     * Get the localized value of an entity's attribute
     * @param entityId the id of the entity
     * @param entityClass the class of the entity
     * @param attributeName the name of the attribute that must be defined as Map<Locale, String>
     * @param locale the locale for the value, use null for the current request locale
     * @param returnNull (optional) true if null must be returned when no value is present instead of the empty string
     * @return the localized value in the specified locale, or in the default configured locale if the value is not set for that locale,
     * or an empty string if no value is found or null if no value is found and returnNull is true.
     */
    public String getLocalValue(long entityId, Class<?> entityClass, String attributeName, Locale locale, Boolean... returnNull) {
    	// select color from YadaArticle_color where YadaArticle_id=123 and locale="en_US"
		if (locale==null) {
			locale = LocaleContextHolder.getLocale();
		}
		String className = findClass(entityClass, attributeName);
		if (className==null) {
			throw new YadaInvalidValueException("The requested attirbute '{}' does not exist on '{}'", attributeName, entityClass);
		}
    	String tableName = className + "_" + attributeName;
    	String idColumn = className + "_id";
    	YadaSql yadaSql = YadaSql.instance().selectFrom("select " + attributeName + " from " + tableName)
    		.where(idColumn + " = :entityId").and()
    		.where("locale = :localeString").and()
    		.setParameter("entityId", entityId)
    		.setParameter("localeString", locale.toString());
    	List<?> result = yadaSql.nativeQuery(em).getResultList();

		if (result.isEmpty()) {
    		// Try with the default locale
    		Locale defaultLocale = config.getDefaultLocale();
    		if (defaultLocale!=null && !defaultLocale.equals(locale)) {
    			yadaSql.setParameter("localeString", defaultLocale.toString());
    			result = yadaSql.nativeQuery(em).getResultList();
    		}
    	}
    	if (result.isEmpty()) {
    		if (returnNull!=null && returnNull.length>0 && Boolean.TRUE.equals(returnNull[0])) {
    			return null;
    		}
    		return "";
    	}
    	return (String) result.get(0);
    }

    private boolean tableExists(String tableName) {
    	try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
	    	DatabaseMetaData meta = connection.getMetaData();
	    	ResultSet res = meta.getTables(null, null, tableName, new String[] {"TABLE"});
	    	boolean result = res.first();
	    	res.close();
	    	return result;
    	} catch(Exception e) {
    		return false;
    	}
    }

    private String findClass(Class<?> entityClass, String attributeName) {
    	String className = entityClass.getSimpleName();
    	String tableName = className + "_" + attributeName;
    	boolean exists = tableExists(tableName);
    	if (!exists) {
			// Try with the superclass
			entityClass = entityClass.getSuperclass();
			if (entityClass.equals(Object.class)) {
				return null; // Bottom of the hierarchy
			}
			return findClass(entityClass, attributeName);
    	}
    	return className;
    }

}
