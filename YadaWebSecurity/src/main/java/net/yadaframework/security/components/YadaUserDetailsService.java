package net.yadaframework.security.components;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
		UserDetails result = null;
		YadaUserCredentials yadaUserCredentials = null;
		int totFound=-1;
		boolean lockout=false;
		try {
			List<YadaUserCredentials> userCredentialsList = yadaUserCredentialsDao.findByUsername(username.toLowerCase());
			totFound = userCredentialsList.size();
			if (totFound==1) {
				yadaUserCredentials = userCredentialsList.get(0);
				// BFA prevention: dopo N tentativi sbagliati consecutivi, blocco l'accesso per tot minuti
				// Il modo in cui è fatto è ridicolo perchè resetta il conto solo se sbaglio dopo che è passato il timeout...!
				int maxFailed = yadaConfiguration.getMaxPasswordFailedAttempts();
				int lockMillis = yadaConfiguration.getPasswordFailedAttemptsLockoutMinutes()*60000;
				Date lastFailedTimestamp = yadaUserCredentials.getLastFailedAttempt();
				if (yadaUserCredentials.getFailedAttempts()>=maxFailed && lastFailedTimestamp!=null) {
					if (System.currentTimeMillis()-lastFailedTimestamp.getTime()<lockMillis) {
						lockout = true;
					} else {
						yadaUserCredentialsDao.resetFailedAttempts(username.toLowerCase());
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
		if (totFound>1) {
			log.error("Internal Error: more than one UserCredentials with username='{}' - rejecting login", username);
			throw new InternalAuthenticationException("Too many users with same username");
		}
		if (totFound<1) {
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
	 * Authenticate the user without setting the lastSuccessfulLogin timestamp
	 * @param userCredentials
	 */
	public Authentication authenticateAs(YadaUserCredentials userCredentials) {
		return authenticateAs(userCredentials, true);	
	}
	
	/**
	 * Authenticate the user
	 * @param userCredentials
	 * @param setTimestamp true to set the lastSuccessfulLogin timestamp
	 */
	public Authentication authenticateAs(YadaUserCredentials userCredentials, boolean setTimestamp) {
		UserDetails userDetails = createUserDetails(userCredentials);
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
		if (setTimestamp) {
			yadaUserCredentialsDao.updateLoginTimestamp(userCredentials.getUsername().toLowerCase());
			yadaUserCredentialsDao.resetFailedAttempts(userCredentials.getUsername().toLowerCase());
		}
		return auth;
	}
	
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
	 * Ritorna true se la password passata è valida per l'utente
	 * @param passwordTyped
	 * @param yadaUserCredentials
	 * @return
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
