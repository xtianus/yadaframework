package net.yadaframework.web.social;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

/**
 * This class holds an operation that has to be done on Facebook using API version 9.0.
 * Use one of the static methods to create an instance. 
 * @see YadaSocial#execute(YadaFacebookRequest)
 */
public class YadaFacebookRequestV9 extends YadaFacebookRequest {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	// When there is a new Facebook API, create another subclass of YadaFacebookRequest, for example YadaFacebookRequestV10
	private final static String API_VERSION="v9.0";
	private final static String BASE_URL = "https://graph.facebook.com";

	/**
	 * Factory method to create a "post link to page" request.
	 * See the <a href="https://developers.facebook.com/docs/pages/publishing/">Facebook docs</a>. 
	 * @param pageId
	 * @param link the link to post
	 * @param pageAccessToken
	 * @return
	 */
	public static YadaFacebookRequestV9 postLinkToPage(String pageId, String link, String pageAccessToken) {
		// Request:
		//	curl -i -X "POST https://graph.facebook.com/v9.0/{page-id}/feed
		//	  ?link=https://portal.facebook.com/products/
		//	  &access_token={page-access-token}"
		// Result:
		//	{
		//		  "id": "{page-post-id}"
		//	}		
		YadaFacebookRequestV9 result = new YadaFacebookRequestV9(HttpMethod.POST);
		result.resource = "/" + pageId + "/feed";
		result.requestParameters.put("link", link);
		result.requestParameters.put("access_token", pageAccessToken);
		result.resultValidPattern = "\\{\"id\":\"[^\"]*\"\\}";
		return result;
	}
	
	YadaFacebookRequestV9(HttpMethod httpMethod) {
		super(httpMethod, BASE_URL, API_VERSION);
	}
	
}
