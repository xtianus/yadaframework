package net.yadaframework.components;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.yadaframework.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.persistence.entity.YadaRegistrationRequest;

@Component
public class YadaTokenHandler {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Crea un token-link unendo le due componenti passate.
	 * @param id
	 * @param token
	 * @param linkParameters name-value paris of url parameters to add at the end - can be null or empty
	 * @return una stringa <ID>-<token>
	 */
	public String makeLink(long id, long token, Map<String, String> linkParameters) {
		StringBuilder result = new StringBuilder(id + "-" + token).append("?");
		if (linkParameters!=null) {
			try {
				for (String key : linkParameters.keySet()) {
					String name = URLEncoder.encode(key, "UTF-8");
					String value = URLEncoder.encode(linkParameters.get(key), "UTF-8");
					result.append(name).append("=").append(value).append("&");
				}
			} catch (UnsupportedEncodingException e) {
				log.error("Impossible error occurred", e);
			}
		}
		String link = result.toString();
		link = StringUtils.removeEnd(link, "&");
		link = StringUtils.removeEnd(link, "?");
		return link;
	}

	/**
	 * Crea un token-link
	 * @param yadaAutoLoginToken
	 * @param linkParameters name-value paris of url parameters to add at the end - can be null or empty
	 * @return una stringa <ID>-<token>
	 */
	public String makeLink(YadaAutoLoginToken yadaAutoLoginToken, Map<String, String> linkParameters) {
		return makeLink(yadaAutoLoginToken.getId(), yadaAutoLoginToken.getToken(), null);
	}
	
	/**
	 * Crea un token-link
	 * @param yadaRegistrationRequest
	 * @param linkParameters name-value paris of url parameters to add at the end - can be null or empty
	 * @return una stringa <ID>-<token>
	 */
	public String makeLink(YadaRegistrationRequest yadaRegistrationRequest, Map<String, String> linkParameters) {
		return makeLink(yadaRegistrationRequest.getId(), yadaRegistrationRequest.getToken(), linkParameters);
	}

	/**
	 * Splitta un token-link nelle sue due componenti id e token.
	 * @param linkId nel formato <ID>-<token>
	 * @return un array {<ID>, <token>}
	 */
	public long[] parseLink(String linkId) {
		try {
			String[] parts = linkId.split("-");
			long[] result = new long[2];
			result[0] = Long.parseLong(parts[0]);
			result[1] = Long.parseLong(parts[1]);
			return result;
		} catch (Exception e) {
			log.debug("Invalid linkId '{}'", linkId);
			return null;
		}
	}

}
