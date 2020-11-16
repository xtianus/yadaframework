package net.yadaframework.web.social;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import net.yadaframework.exceptions.YadaSocialException;

@Service
public class YadaSocial {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Execute a Facebook request
	 * @param yadaFacebookRequest
	 * @return the response json
	 * @throws YadaSocialException
	 */
	public String execute(YadaFacebookRequest yadaFacebookRequest) throws YadaSocialException {
		try {
			RestTemplate restTemplate = new RestTemplate();
			String url = yadaFacebookRequest.makeUrl();
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, yadaFacebookRequest.getHttpMethod(), null, String.class);
			String body = responseEntity.getBody();
			yadaFacebookRequest.checkForErrors(body);
			return body;
		} catch (HttpClientErrorException e) {
			String responseBody = e.getResponseBodyAsString();
			throw new YadaSocialException(responseBody, e);
		}
	}
	
}
