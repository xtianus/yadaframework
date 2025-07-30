package net.yadaframework.security.components;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.TooManyFailedAttemptsException;
import net.yadaframework.security.exceptions.InternalAuthenticationException;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;

@Component
@DependsOn("passwordEncoder")
public class YadaUserDetailsService implements UserDetailsService {
	private transient final Logger log = LoggerFactory.getLogger(getClass());
	private transient final Logger logSec = LoggerFactory.getLogger("security");
	@Autowired PasswordEncoder encoder;
	@Autowired YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired YadaConfiguration yadaConfiguration;
	
	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository(); 

	/**
	 * Change the roles of the currently authenticated user, but not on the database
	 * @param authentication the current Authentication object
	 * @param roleIds the database ids of the needed roles
	 */
	public void changeCurrentRoles(Authentication authentication, int[] roleIds) {
	    Set<GrantedAuthority> authorities = new HashSet<>();
	    for (int roleId : roleIds) {
	    	authorities.add(new SimpleGrantedAuthority(yadaConfiguration.getRoleSpringName(roleId)));
		}
	    Authentication newAuth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), authorities);
	    SecurityContextHolder.getContext().setAuthentication(newAuth);
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, InternalAuthenticationException, TooManyFailedAttemptsException {
		username = username.trim().toLowerCase();
		UserDetails result = null;
		YadaUserCredentials yadaUserCredentials = null;
		boolean lockout=false;
		try {
			yadaUserCredentials = yadaUserCredentialsDao.findFirstByUsername(username);
			if (yadaUserCredentials!=null) {
				// BFA prevention: after some login failures, block login for a configured timeout
				int maxFailed = yadaConfiguration.getMaxPasswordFailedAttempts();
				int lockMillis = yadaConfiguration.getPasswordFailedAttemptsLockoutMinutes()*60000;
				Date lastFailedTimestamp = yadaUserCredentials.getLastFailedAttempt();
				if (yadaUserCredentials.getFailedAttempts()>maxFailed && lastFailedTimestamp!=null) {
					if (System.currentTimeMillis()-lastFailedTimestamp.getTime() < lockMillis) {
						lockout = true;
					} else {
						// After the timeout, login can be tried again
						yadaUserCredentialsDao.resetFailedAttempts(username);
						yadaUserCredentials.setFailedAttempts(0);
						yadaUserCredentials.setLastFailedAttempt(null);
					}
				}
				if (!lockout) {
					result = createUserDetails(yadaUserCredentials);
				}
			}
		} catch (Exception e) {
			log.error("Internal error while authenticating user", e);
			throw new InternalAuthenticationException("Internal Error", e);
		}
//		if (totFound>1) {
//			log.error("Internal Error: more than one UserCredentials with username='{}' - rejecting login", username);
//			throw new InternalAuthenticationException("Too many users with same username");
//		}
		if (yadaUserCredentials==null) {
			log.debug("Username '{}' not found", username);
			throw new UsernameNotFoundException("Username " + username + " not found");
		}
		if (lockout) {
			logSec.debug("Username '{}' too many failed attempts: locked out", username);
			throw new TooManyFailedAttemptsException();
		}
		return result;
	}

	private UserDetails createUserDetails(YadaUserCredentials userCredentials) {
		Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
		for (Integer roleId : userCredentials.getRoles()) {
			authorities.add(new SimpleGrantedAuthority(yadaConfiguration.getRoleSpringName(roleId)));
		}
		UserDetails userDetails = new org.springframework.security.core.userdetails.User(userCredentials.getUsername().toLowerCase(), userCredentials.getPassword(), userCredentials.isEnabled(), true, !userCredentials.isChangePassword(), true, authorities);
		return userDetails;
	}

	/**
	 * Manual authentication for Spring Security 6 without setting the login timestamp.
	 * @param userCredentials
	 * @param request
	 * @param response
	 */
	public Authentication authenticateAs(YadaUserCredentials userCredentials, HttpServletRequest request, HttpServletResponse response) {
		if (request==null || response==null) {
			log.warn("Using deprecated authentication method");
			return authenticateAs(userCredentials, false);
		}
		return authenticateAs(userCredentials, false, request, response);
	}
	
	/**
	 * Manual authentication for Spring Security 6. Also sets the login timestamp and clears the failed attempts counter.
	 * @param userCredentials
	 * @param request
	 * @param response
	 * @param setTimestamp true to set the lastSuccessfulLogin timestamp
	 */
	public Authentication authenticateAs(YadaUserCredentials userCredentials, boolean setTimestamp, HttpServletRequest request, HttpServletResponse response) {
		if (request==null || response==null) {
			log.warn("Using deprecated authentication method");
			return authenticateAs(userCredentials, setTimestamp);
		}
		UserDetails userDetails = createUserDetails(userCredentials);
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		// Docs: https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html#store-authentication-manually
		SecurityContext context = securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(auth); 
		securityContextHolderStrategy.setContext(context);
		securityContextRepository.saveContext(context, request, response); 		
        //
		if (setTimestamp) {
			yadaUserCredentialsDao.updateLoginTimestamp(userCredentials.getUsername());
			yadaUserCredentialsDao.resetFailedAttempts(userCredentials.getUsername());
		}
		return auth;
	}

	/**
	 * Authenticate the user without setting the lastSuccessfulLogin timestamp
	 * @param userCredentials
	 * @deprecated because for Spring 5
	 */
	@Deprecated
	public Authentication authenticateAs(YadaUserCredentials userCredentials) {
		return authenticateAs(userCredentials, true);
	}

	/**
	 * Authenticate the user
	 * @param userCredentials
	 * @param setTimestamp true to set the lastSuccessfulLogin timestamp
	 * @deprecated because for Spring 5
	 */
	@Deprecated
	public Authentication authenticateAs(YadaUserCredentials userCredentials, boolean setTimestamp) {
		UserDetails userDetails = createUserDetails(userCredentials);
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		SecurityContext context = SecurityContextHolder.getContext();
		context.setAuthentication(auth);
		// Fix for authentication being ignored in Spring Security 6.2.0 because of requireExplicitAuthenticationStrategy(true) by default
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest req = ((ServletRequestAttributes) requestAttributes).getRequest();
            HttpSession session = req.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        }
        //
		if (setTimestamp) {
			yadaUserCredentialsDao.updateLoginTimestamp(userCredentials.getUsername());
			yadaUserCredentialsDao.resetFailedAttempts(userCredentials.getUsername());
		}
		return auth;
	}

	/**
	 * Change the old password with the new password, but only if the old password is valid.
	 * If the change is successful, no exception is thrown.
	 * @param username user that needs to change password
	 * @param passwordTyped current password
	 * @param newPassword new password
	 * @throws UsernameNotFoundException if the username does not exist
	 * @throws BadCredentialsException if the supplied password is not valid for the user
	 * @throws InternalAuthenticationException in any other error occurs
	 */
	public void changePasswordIfAuthenticated(String username, String passwordTyped, String newPassword) throws UsernameNotFoundException, InternalAuthenticationException, BadCredentialsException {
		// Prima controllo che username e password siano validi, poi setto la nuova password
		try {
			username = username.toLowerCase();
			YadaUserCredentials userCredentials = yadaUserCredentialsDao.findFirstByUsername(username);
			if (userCredentials==null) {
				throw new UsernameNotFoundException("Username " + username + " not found");
			}
			boolean pwdMatch=passwordMatch(passwordTyped, userCredentials);
			if (pwdMatch) {
				userCredentials = yadaUserCredentialsDao.changePassword(userCredentials, newPassword);
			} else {
				log.debug("Invalid password: {}", passwordTyped);
				throw new BadCredentialsException("Password invalid");
			}
		} catch (UsernameNotFoundException e) {
			throw e;
		} catch (BadCredentialsException e) {
			throw e;
		} catch (Exception e) {
			throw new InternalAuthenticationException("Internal Error", e);
		}
	}

	/**
	 * Check if some string matches the user password
	 * @param passwordTyped the string to check
	 * @param yadaUserCredentials credentials of the user
	 * @return true if passwordTyped is the user password
	 */
	public boolean passwordMatch(String passwordTyped, YadaUserCredentials yadaUserCredentials) {
		boolean pwdMatch=false;
		if (encoder!=null && yadaUserCredentials!=null) {
			pwdMatch = encoder.matches(passwordTyped, yadaUserCredentials.getPassword());
		} else if (yadaUserCredentials!=null) {
			pwdMatch = yadaUserCredentials.getPassword().equals(passwordTyped);
		}
		return pwdMatch;
	}

	public boolean validatePasswordSyntax(String password, int minLen, int maxLen) {
		return !StringUtils.isEmpty(password) && password.length()>=minLen && password.length()<=maxLen;
	}

	public boolean validatePasswordSyntax(String password) {
		return validatePasswordSyntax(password, yadaConfiguration.getMinPasswordLength(), yadaConfiguration.getMaxPasswordLength());
	}



}
