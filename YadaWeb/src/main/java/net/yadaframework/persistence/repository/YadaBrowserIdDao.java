package net.yadaframework.persistence.repository;


import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.persistence.entity.YadaBrowserId;
import net.yadaframework.raw.YadaHttpUtil;

/**
 * 
 */
@Repository
@Transactional(readOnly = true) 
public class YadaBrowserIdDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext EntityManager em;
    
    private YadaHttpUtil yadaHttpUtil = new YadaHttpUtil();

	/**
	 * Find the instance with the given value
	 * @param mostSigBits
	 * @param leastSigBits
	 * @return
	 */
    public YadaBrowserId findByMostSigBitsAndLeastSigBits(long mostSigBits, long leastSigBits) {
    	String sql = "select e from YadaBrowserId e where e.mostSigBits = :mostSigBits and e.leastSigBits = :leastSigBits";
		List<YadaBrowserId> resultList = em.createQuery(sql, YadaBrowserId.class)
			.setMaxResults(1)
			.setParameter("mostSigBits", mostSigBits)
			.setParameter("leastSigBits", leastSigBits)
			.getResultList();
		return normaliseSingleResult(resultList);
    }

	
	/**
	 * Get or create a YadaBrowserId using a cookie.
	 * The cookie will be set in case of creation.
	 * @param cookieName the name of the cookie that identifies the user
	 * @param expirationSeconds the expiration time for the cookie
	 * @param request
	 * @param response
	 * @return
	 */
    @Transactional(readOnly = false)
	public YadaBrowserId ensureYadaBrowserId(String cookieName, int expirationSeconds, HttpServletRequest request, HttpServletResponse response) {
		// Check if cookie present
		String uuidString = yadaHttpUtil.getOneCookieValue(request, cookieName);
		if (uuidString!=null) {
			try {
				UUID uuid = UUID.fromString(uuidString);
				YadaBrowserId yadaBrowserId = findByMostSigBitsAndLeastSigBits(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
				if (yadaBrowserId!=null) {
					return yadaBrowserId;
				}
				// If not found in the database, keep going and create a new one
			} catch (IllegalArgumentException e) {
				log.warn("Invalid UUID cookie value: {}", uuidString);
			}
		}
		// Create and set
		UUID uuid = UUID.randomUUID();
		YadaBrowserId yadaBrowserId = new YadaBrowserId();
		em.persist(yadaBrowserId);
		yadaBrowserId.setMostSigBits(uuid.getMostSignificantBits());
		yadaBrowserId.setLeastSigBits(uuid.getLeastSignificantBits());
		Cookie cookie = new Cookie(cookieName, uuid.toString());
		cookie.setMaxAge(expirationSeconds);
		cookie.setPath("/");
		response.addCookie(cookie);
		return yadaBrowserId;
	}

    /**
     * Find or create the instance with the given UUID
     * @param uuid
     * @return
     */
    @Transactional(readOnly = false) 
    public YadaBrowserId findOrCreate(UUID uuid) {
    	YadaBrowserId yadaBrowserId = findByMostSigBitsAndLeastSigBits(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    	if (yadaBrowserId==null) {
    		yadaBrowserId = new YadaBrowserId();
    		em.persist(yadaBrowserId);
    		yadaBrowserId.setUUID(uuid);
    	}
    	return yadaBrowserId;
    }
    
    /**
     * For backwards compatibility, returns null when no result is found
     * @param resultList
     * @return
     */
    private YadaBrowserId normaliseSingleResult(List<YadaBrowserId> resultList) {
		// Need to keep the contract of the Spring Data Repository, so we return null when no value found.
		if (resultList.isEmpty()) {
			return null;
		} else {
			return resultList.get(0);
		}
    }

	
}
