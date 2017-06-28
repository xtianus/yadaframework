package net.yadaframework.persistence.repository;


import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.core.YadaEnum;
import net.yadaframework.persistence.entity.YadaPersistentEnum;

/**
 * 
 */
@Repository
@Transactional(readOnly = true) 
public class YadaPersistentEnumDao {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext EntityManager em;
    
    @Autowired MessageSource messageSource;
	
	/**
	 * Fills the database with enum localized values, when missing. Can be used at app startup.
	 * Any added/deleted language will be taken care of properly. You can even pass from a "en" to a "en_US" locale.
	 */
    @Transactional(readOnly = false)
	public void initDatabase(List<Class<? extends Enum<?>>> enumClasses, List<Locale> locales) {
		for (Class<? extends Enum<?>> enumClass : enumClasses) {
			String enumClassName = enumClass.getName();
			Enum[] enumElements = enumClass.getEnumConstants(); // RUNNING, STOPPED
			// Check if each row is in the database
			for (Enum enumElement : enumElements) {
				YadaPersistentEnum yadaPersistentEnum;
				String sql = "from YadaPersistentEnum where enumClassName = :enumClassName and enumName = :enumName";
				List<YadaPersistentEnum> theEnumList = em.createQuery(sql)
					.setParameter("enumClassName", enumClassName)
					.setParameter("enumName", enumElement.name())
					.getResultList();
				if (theEnumList.size()==0) { // To be created new
					yadaPersistentEnum = new YadaPersistentEnum();
					yadaPersistentEnum.setEnumClassName(enumClassName);
					yadaPersistentEnum.setEnumOrdinal(enumElement.ordinal());
					yadaPersistentEnum.setEnumName(enumElement.name());
					em.persist(yadaPersistentEnum);
				} else { // Already in the database
					yadaPersistentEnum = theEnumList.get(0);
				}
				// Always refresh the localized strings
				Map<String, String> langToText = yadaPersistentEnum.getLangToText();
				langToText.clear();
				for (Locale locale : locales) {
					String localeCode = locale.toString();
					String text = ((YadaEnum) enumElement).toString(messageSource, locale);
					langToText.put(localeCode, text);
				}
			}
		}
	}
}
