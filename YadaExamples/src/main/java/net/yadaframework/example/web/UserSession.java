package net.yadaframework.example.web;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import net.yadaframework.example.persistence.entity.UserProfile;

import net.yadaframework.security.web.YadaSession;

/**
 * User session
 */
@Component
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS) 
public class UserSession extends YadaSession<UserProfile> {
	// Add any attributes that you want to keep for the duration of the user session

}
