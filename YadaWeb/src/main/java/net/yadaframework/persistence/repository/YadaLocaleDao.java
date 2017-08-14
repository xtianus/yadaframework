package net.yadaframework.persistence.repository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.persistence.YadaSql;

/**
 * Locale-related operations on entities 
 */
@Repository
@Transactional(readOnly = true) 
public class YadaLocaleDao {
	
    @Autowired private YadaConfiguration config;

    @PersistenceContext private EntityManager em;
    
    
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
     * @param locale the locale for the value, use null for the current locale
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
     * @param locale the locale for the value, use null for the current locale
     * @param returnNull (optional) true if null must be returned when no value is present instead of the empty string
     * @return the localized value in the specified locale, or in the default configured locale if the value is not set for that locale, 
     * or an empty string if no value is found or null if no value is found and returnNull is true.
     */
    public String getLocalValue(long entityId, Class<?> entityClass, String attributeName, Locale locale, Boolean... returnNull) {
    	// select color from YadaArticle_color where YadaArticle_id=123 and locale="en_US"
		if (locale==null) {
			locale = LocaleContextHolder.getLocale();
		}
    	String className = entityClass.getSimpleName();
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

}
