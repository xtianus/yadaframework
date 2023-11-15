package net.yadaframework.persistence.repository;


import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.entity.YadaPersistentEnum;

/**
 *
 */

// TODO fix the generics stuff

@Repository
@Transactional(readOnly = true)
public class YadaPersistentEnumDao {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext
	EntityManager em;

    @Autowired MessageSource messageSource;

    /**
     * Returns the persistent entity representing the given YadaLocalEnum implementation.
     * @param yadaLocalEnum
     * @return
     * @throws YadaInvalidUsageException if the enum has not been previously saved to the database (with initDatabase())
     * @see initDatabase
     */
    public <T extends Enum<T>> YadaPersistentEnum<T> find(YadaLocalEnum<T> yadaLocalEnum) throws YadaInvalidUsageException {
    	String enumClassName = yadaLocalEnum.getClass().getName();
    	int enumOrdinal = yadaLocalEnum.ordinal();
    	try {
			String sql = "from YadaPersistentEnum where enumClassName=:enumClassName and enumOrdinal=:enumOrdinal";
			@SuppressWarnings("unchecked")
			YadaPersistentEnum<T> result =
				(YadaPersistentEnum<T>) em.createQuery(sql)
				.setParameter("enumClassName", enumClassName)
				.setParameter("enumOrdinal", enumOrdinal)
				.getSingleResult();
			return result;
		} catch (NoResultException | NonUniqueResultException e) {
			log.error("YadaPersistenteEnum {}-{} misconfigured", enumClassName, enumOrdinal, e);
			throw new YadaInvalidUsageException(enumClassName + " misconfigured");
		}
    }

	/**
	 * Fills the database with enum localized values, when missing. Can be used at app startup.
	 * Can add new enum values for a given enum class, but not remove them (must be done manually).
	 * Any added/deleted language will be taken care of properly. You can even change from a "en" to a "en_US" locale.
	 * A change in the localized text is handled properly.
	 */
    @Transactional(readOnly = false)
	public void initDatabase(List<Class<? extends YadaLocalEnum<?>>> enumClasses, Collection<Locale> locales) {
		for (Class<? extends YadaLocalEnum<?>> enumClass : enumClasses) {
			String enumClassName = enumClass.getName();
			YadaLocalEnum<?>[] enumElements = enumClass.getEnumConstants(); // RUNNING, STOPPED
			// Check if each row is in the database
			for (YadaLocalEnum<?> enumElement : enumElements) {
				YadaPersistentEnum yadaPersistentEnum; // TODO fix generics
				String sql = "from YadaPersistentEnum where enumClassName = :enumClassName and enumName = :enumName";
				List<YadaPersistentEnum<? extends YadaLocalEnum<?>>> theEnumList = em.createQuery(sql)
					.setParameter("enumClassName", enumClassName)
					.setParameter("enumName", enumElement.name())
					.getResultList();
				if (theEnumList.size()==0) { // To be created new
					yadaPersistentEnum = new YadaPersistentEnum<>();
					yadaPersistentEnum.setEnumClassName(enumClassName);
					yadaPersistentEnum.setEnumOrdinal(enumElement.ordinal());
					yadaPersistentEnum.setEnumName(enumElement.name());
					em.persist(yadaPersistentEnum);
				} else { // Already in the database
					yadaPersistentEnum = theEnumList.get(0);
				}
				// Store reference in the normal enum
				enumElement.setYadaPersistentEnum(yadaPersistentEnum);
				// Always refresh the localized strings
				Map<String, String> langToText = yadaPersistentEnum.getLangToText();
				langToText.clear();
				for (Locale locale : locales) {
					String localeCode = locale.toString();
					String text = ((YadaLocalEnum) enumElement).toString(messageSource, locale);
					langToText.put(localeCode, text);
				}
			}
		}
	}

	public <T extends Enum<T>> YadaPersistentEnum<T> findOne(Long id) {
		return em.find(YadaPersistentEnum.class, id);
	}
}
