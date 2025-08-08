package net.yadaframework.example.persistence.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.example.persistence.entity.UserProfile;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.security.persistence.repository.YadaUserProfileDao;

@Repository
@Transactional(readOnly = true)
public class UserProfileDao extends YadaUserProfileDao<UserProfile> {
    @PersistenceContext private EntityManager em;

    /**
     * Delete a UserProfile
     * @param userProfile
     */
    @Transactional(readOnly = false)
    public void delete(UserProfile userProfile) {
    	// Delete user roles
    	YadaSql.instance().deleteFrom("delete from YadaUserCredentials_roles where YadaUserCredentials_id in "
    			+ "(select yuc.id from YadaUserCredentials yuc right outer join YadaUserProfile up on up.userCredentials_id = yuc.id "
    			+ "where up.id=:userProfileId)")
    	.setParameter("userProfileId", userProfile.getId())
    	.nativeQuery(em).executeUpdate();

    	// Delete YadaAutoLoginToken
    	YadaSql.instance().deleteFrom("delete from YadaAutoLoginToken where YadaUserCredentials_id in "
    			+ "(select yuc.id from YadaUserCredentials yuc right outer join YadaUserProfile up on up.userCredentials_id = yuc.id "
    			+ "where up.id=:userProfileId )")
    	.setParameter("userProfileId", userProfile.getId())
    	.nativeQuery(em).executeUpdate();

    	// Delete YadaSocialCredentials
    	YadaSql.instance().deleteFrom("delete from YadaSocialCredentials where yadaUserCredentials_id in "
    			+ "(select yuc.id from YadaUserCredentials yuc right outer join YadaUserProfile up on up.userCredentials_id = yuc.id "
    			+ "where up.id=:userProfileId )")
    			.setParameter("userProfileId", userProfile.getId())
    		.nativeQuery(em).executeUpdate();

    	// Delete UserProfile, YadaUserCredentials, YadaAutoLoginToken
    	YadaSql.instance().deleteFrom("delete yuc, up from YadaUserCredentials yuc ")
    		.join("right outer join YadaUserProfile up on up.userCredentials_id = yuc.id")
    		.where("where up.id=:userProfileId")
    		.setParameter("userProfileId", userProfile.getId())
    		.nativeQuery(em).executeUpdate();
    }
    
    /**
     * Find a userProfile from its id
     * @param userProfileId
     * @return UserProfile instance or null
     */
    public UserProfile find(Long userProfileId) {
    	if (userProfileId==null) {
    		return null;
    	}
    	UserProfile userProfile = em.find(UserProfile.class, userProfileId);
    	return userProfile;
    }
}
