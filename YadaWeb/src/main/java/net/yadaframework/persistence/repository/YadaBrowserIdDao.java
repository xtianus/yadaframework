package net.yadaframework.persistence.repository;


import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired YadaBrowserIdRepository yadaBrowserIdRepository;
    private YadaHttpUtil yadaHttpUtil = new YadaHttpUtil();

	
	/**
	 * Get or create a YadaBrowserId using a cookie.
	 * The cookie will be set in case of creation.
	 * @param cookieName the name of the cookie that identifies the user
	 * @param expirationSeconds the expiration time for the cookie
	 * @param request
	 * @param response
	 * @return
	 */
	public YadaBrowserId ensureYadaBrowserId(String cookieName, int expirationSeconds, HttpServletRequest request, HttpServletResponse response) {
		// Check if cookie present
		String uuidString = yadaHttpUtil.getOneCookieValue(request, cookieName);
		if (uuidString!=null) {
			try {
				UUID uuid = UUID.fromString(uuidString);
				YadaBrowserId yadaBrowserId = yadaBrowserIdRepository.findByMostSigBitsAndLeastSigBits(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
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
		yadaBrowserId.setMostSigBits(uuid.getMostSignificantBits());
		yadaBrowserId.setLeastSigBits(uuid.getLeastSignificantBits());
		yadaBrowserId = yadaBrowserIdRepository.save(yadaBrowserId);
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
    	YadaBrowserId yadaBrowserId = yadaBrowserIdRepository.findByMostSigBitsAndLeastSigBits(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    	if (yadaBrowserId==null) {
    		yadaBrowserId = new YadaBrowserId();
    		yadaBrowserId.setUUID(uuid);
    		yadaBrowserId = yadaBrowserIdRepository.save(yadaBrowserId);
    	}
    	return yadaBrowserId;
    }
	
}
