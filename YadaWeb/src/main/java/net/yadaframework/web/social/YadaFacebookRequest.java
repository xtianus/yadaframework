package net.yadaframework.web.social;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaSocialException;

/**
 * Abstract superclass for Facebook operations. 
 * @see YadaFacebookRequestV9
 *
 */
public abstract class YadaFacebookRequest {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	private YadaWebUtil yadaWebUtil = (YadaWebUtil) YadaUtil.getBean("yadaWebUtil");
	
	protected String userAccessToken;

	protected String baseUrl;
	protected String apiVersion;
	protected HttpMethod httpMethod; // GET / POST
	protected String resource; // This is the URL path that follows the api version, i.e. "/feed"
	protected String resultValidPattern; // null for don't check
	protected String resultErrorPattern; // null for don't check
	
	protected Map<String, String> requestParameters = new HashMap<>();
	protected Map<String, Object> requestJson = new HashMap<>();

	protected YadaFacebookRequest(HttpMethod httpMethod, String baseUrl, String apiVersion) {
		this.httpMethod = httpMethod;
		this.baseUrl = baseUrl;
		this.apiVersion = apiVersion;
	}
	
	/**
	 * @return the url like "https://graph.facebook.com/v2.3/me?access_token=uefbise&id=23423"
	 */
	String makeUrl() {
		if (baseUrl==null || apiVersion==null || httpMethod==null || resource==null) {
			log.error("Some parameter is missing: baseUrl={}, apiVersion={}, httpMethod={}, resource={}", baseUrl, apiVersion, httpMethod, resource);
			throw new YadaInternalException("Missing endpoint parameter");
		}
		return yadaWebUtil.makeUrl(new String[] {baseUrl, apiVersion, resource}, requestParameters, Boolean.FALSE);
	}

	void checkForErrors(String body) throws YadaSocialException {
		if (resultValidPattern!=null && !Pattern.matches(resultValidPattern, body)) {
			throw new YadaSocialException("Unexpected response from Facebook when calling {}: {}", resource, body);
		}
		if (resultErrorPattern!=null && Pattern.matches(resultErrorPattern, body)) {
			throw new YadaSocialException("Error response from Facebook when calling {}: {}", resource, body);
		}
	}

	HttpMethod getHttpMethod() {
		return httpMethod;
	}

}
