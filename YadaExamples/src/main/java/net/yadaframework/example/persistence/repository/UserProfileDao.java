package net.yadaframework.example.persistence.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.example.persistence.entity.UserProfile;
import net.yadaframework.security.persistence.repository.YadaUserProfileDao;

@Repository
@Transactional(readOnly = true)
public class UserProfileDao extends YadaUserProfileDao<UserProfile> {
    @PersistenceContext private EntityManager em;

    /**
     * Find a userProfile from its id
     * @param userProfileId
     * @return UserProfile instance or null
     */
    public UserProfile find(long userProfileId) {
    	UserProfile userProfile = em.find(UserProfile.class, userProfileId);
    	return userProfile;
    }
}
