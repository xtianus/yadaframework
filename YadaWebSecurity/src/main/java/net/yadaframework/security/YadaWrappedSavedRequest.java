package net.yadaframework.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.springframework.security.web.savedrequest.SavedRequest;

import net.yadaframework.components.YadaWebUtil;

/**
 * A wrapper for the saved request that allows to add url parameters.
 * Used to detect logins triggered by ajax calls.
 */
public class YadaWrappedSavedRequest implements SavedRequest {
	private static final long serialVersionUID = 1L;

	private SavedRequest savedRequest;
	private YadaWebUtil yadaWebUtil;
	private String overriddenRedirectUrl;

	public YadaWrappedSavedRequest(SavedRequest savedRequest, YadaWebUtil yadaWebUtil) {
		this.savedRequest = savedRequest;
		this.yadaWebUtil = yadaWebUtil;
		this.overriddenRedirectUrl = savedRequest.getRedirectUrl();
	}

	/**
	 * Add a url parameter or change its value if present
	 * @param name the name of the parameter, not urlencoded
	 * @param value the value of the parameter, not urlencoded. Can be null to only have the name with no value in the url
	 */
	public void addOrUpdateUrlParameter(String name, String value) {
		this.overriddenRedirectUrl = yadaWebUtil.addOrUpdateUrlParameter(overriddenRedirectUrl, name, value);
	}

	@Override
	public String getRedirectUrl() {
		return overriddenRedirectUrl;
	}

	@Override
	public List<Cookie> getCookies() {
		return savedRequest.getCookies();
	}

	@Override
	public String getMethod() {
		return savedRequest.getMethod();
	}

	@Override
	public List<String> getHeaderValues(String name) {
		return savedRequest.getHeaderValues(name);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return savedRequest.getHeaderNames();
	}

	@Override
	public List<Locale> getLocales() {
		return savedRequest.getLocales();
	}

	@Override
	public String[] getParameterValues(String name) {
		return savedRequest.getParameterValues(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return savedRequest.getParameterMap();
	}


}
