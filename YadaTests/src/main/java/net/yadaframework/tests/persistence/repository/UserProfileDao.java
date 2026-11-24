package net.yadaframework.tests.persistence.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.security.persistence.repository.YadaUserProfileDao;
import net.yadaframework.tests.persistence.entity.UserProfile;

@Repository
@Transactional(readOnly = true)
public class UserProfileDao extends YadaUserProfileDao<UserProfile> {
}
