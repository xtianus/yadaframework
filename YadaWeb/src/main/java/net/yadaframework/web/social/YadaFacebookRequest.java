package net.yadaframework.web.social;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import net.yadaframework.exceptions.InternalException;
import net.yadaframework.exceptions.YadaSocialException;

public class YadaFacebookRequest {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Fetch profile
	 */
	public final static String API_PROFILE="/v2.12/me";
	// Docs: https://developers.facebook.com/docs/graph-api/reference/v2.12/page/feed#publish
	// You can use "me" instead of the page access token
	public final static String API_PAGE_POST_AS_ADMIN="/v2.12/me/feed";
	
	private String BASE_URL = "https://graph.facebook.com";

	private String api;
	private HttpMethod httpMethod; // GET o POST
	private Map<String, String> parameters = new HashMap<String, String>();
	
	public YadaFacebookRequest(String accessToken) {
		parameters.put("access_token", accessToken);
	}
	
	/**
	 * Generic request
	 * @param api
	 * @param accessToken
	 * @param httpMethod
	 */
	public YadaFacebookRequest(String api, String accessToken, HttpMethod httpMethod) {
		this.api = api;
		this.httpMethod = httpMethod;
		parameters.put("access_token", accessToken);
	}
	
	void checkForErrors(String body) throws YadaSocialException {
		if (API_PAGE_POST_AS_ADMIN == api) {
			if (body.indexOf("\"id\":")<0) {
				throw new YadaSocialException("Invalid response from Facebook when calling {}: {}", api, body);
			}
		}
	}
	
	/**
	 * Create a request for a Post as the page admin (not as the user)
	 * @param linkToPost the link to post
	 */
	public void facebookPagePostAsAdmin(String linkToPost) {
		this.api = API_PAGE_POST_AS_ADMIN;
		this.setParameter("link", linkToPost);
		this.httpMethod = HttpMethod.POST;
	}
	
	public void facebookCurrentProfile() {
		this.api = API_PROFILE;
		this.httpMethod = HttpMethod.GET;
	}
	
	/**
	 * 
	 * @return he url like "https://graph.facebook.com/v2.3/me?access_token=uefbise&id=23423"
	 */
	String getUrl() {
		if (api==null || httpMethod==null) {
			throw new InternalException("Missing api/httpMethod");
		}
		StringBuffer result = new StringBuffer();
		result.append(BASE_URL).append(api).append("?");
//		result.append("?access_token=").append(accessToken);
		for (String key : parameters.keySet()) {
			String value = parameters.get(key);
			result.append("&").append(key).append("=").append(value);
		}
		return result.toString();
	}

	public void setParameter(String name, String value) {
		parameters.put(name, value);
	}

	HttpMethod getHttpMethod() {
		return httpMethod;
	}

	Map<String, String> getParameters() {
		return parameters;
	}

	
}
