package org.springframework.web.multipart.commons;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * Multipart request wrapper used when multipart parsing has already failed.
 * It exposes query-string parameters only and resolves uploaded files as absent.
 */
class YadaFailedMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {
	private final MultiValueMap<String, String> queryParameters;

	YadaFailedMultipartHttpServletRequest(HttpServletRequest request) {
		super(request);
		queryParameters = extractQueryParameters(request);
	}

	private MultiValueMap<String, String> extractQueryParameters(HttpServletRequest request) {
		String queryString = request.getQueryString();
		if (queryString == null || queryString.isBlank()) {
			return new LinkedMultiValueMap<>();
		}
		return new LinkedMultiValueMap<>(UriComponentsBuilder.newInstance().query(queryString).build().getQueryParams());
	}

	@Override
	protected void initializeMultipart() {
		setMultipartFiles(new LinkedMultiValueMap<>());
	}

	@Override
	@Nullable
	public String getParameter(String name) {
		return queryParameters.getFirst(name);
	}

	@Override
	@Nullable
	public String[] getParameterValues(String name) {
		List<String> values = queryParameters.get(name);
		if (values == null) {
			return null;
		}
		return values.toArray(new String[0]);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(queryParameters.keySet());
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		Map<String, String[]> result = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
			result.put(entry.getKey(), entry.getValue().toArray(new String[0]));
		}
		return result;
	}

	@Override
	public Part getPart(String name) {
		return null;
	}

	@Override
	public java.util.Collection<Part> getParts() {
		return Collections.emptyList();
	}

	@Override
	@Nullable
	public String getMultipartContentType(String paramOrFileName) {
		return null;
	}

	@Override
	@Nullable
	public HttpHeaders getMultipartHeaders(String paramOrFileName) {
		return null;
	}
}
