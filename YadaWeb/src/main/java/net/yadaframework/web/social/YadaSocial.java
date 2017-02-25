package net.yadaframework.web.social;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import net.yadaframework.exceptions.YadaSocialException;

@Service
public class YadaSocial {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	public String execute(YadaFacebookRequest yadaFacebookRequest) throws YadaSocialException {
		try {
			RestTemplate restTemplate = new RestTemplate();
			String url = yadaFacebookRequest.getUrl();
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, yadaFacebookRequest.getHttpMethod(), null, String.class);
			String body = responseEntity.getBody();
			yadaFacebookRequest.checkForErrors(body);
			return body;
		} catch (Exception e) {
			throw new YadaSocialException(e);
		}
	}
	
}
