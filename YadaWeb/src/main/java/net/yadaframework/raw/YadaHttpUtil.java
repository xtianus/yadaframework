package net.yadaframework.raw;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;

/**
 * Miscellaneous HTTP functions
 *
 */
// non usare @Component perché questa classe viene usata anche senza Spring
// TODO (ma allora Base64Utils non deve essere usata!)
public class YadaHttpUtil {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	public final static int CONTENT_DOCUMENT=0;
	public final static int CONTENT_JAVASCRIPT=1;
	public final static int CONTENT_CSS=2;
	public final static int CONTENT_OTHER=3;
	public final static int CONTENT_UNKNOWN=4;
	public final static int CONTENT_XML=5;
	public final static int CONTENT_IMAGE=6;

	/**
	 * Validate a proxy over a given http page, with no proxy authentication
	 * @param proxyAddress
	 * @param proxyPort
	 * @param testUrl
	 * @param timeoutMillis
	 * @return
	 */
	public long validateProxy(String proxyAddress, int proxyPort, URL testUrl, int timeoutMillis) {
		return validateProxy(proxyAddress, proxyPort, testUrl, timeoutMillis, null, null, null);
	}
		
	/**
	 * Validate a proxy over a given http page
	 * @param proxyAddress
	 * @param proxyPort
	 * @param testUrl the page to fetch (must include the third slash if just the host, like "http://somehost.com/"
	 * @param timeoutMillis
	 * @param username proxy basic authentication, or null when not needed
	 * @param password proxy basic authentication, or null when not needed
	 * @param userAgent to use when connecting (can be null for the default). E.g. "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:47.0) Gecko/20100101 Firefox/47.0"
	 * @return the milliseconds taken to fetch the page, or -1 in case of error/timeout
	 */
	public long validateProxy(String proxyAddress, int proxyPort, URL testUrl, int timeoutMillis, String username, String password, String userAgent) {
		long start = System.currentTimeMillis();
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, proxyPort));
		try {
			URLConnection connection = testUrl.openConnection(proxy);
			connection.setReadTimeout(timeoutMillis);
			connection.setConnectTimeout(timeoutMillis);
			connection.setUseCaches(false);
			connection.getRequestProperty(password);
			if (userAgent!=null) {
				connection.setRequestProperty("User-Agent", userAgent);
			}
			if (username!=null && password !=null) {
				String auth = "Basic " + Base64Utils.encodeToString((username + ":" + password).getBytes());
				connection.setRequestProperty("Proxy-Connection", "Keep-Alive");
				connection.setRequestProperty("Proxy-Authorization", auth);
			}
			connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine = null;
			while ((inputLine = in.readLine()) != null); 
			in.close();
			return System.currentTimeMillis()-start;
		} catch (IOException e) {
			log.debug("Failed to validate proxy {}", proxyAddress, e);
		}
		return -1;
//		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.10.100.100", 80));
//		HttpURLConnection connection =(HttpURLConnection)new URL("http://abc.abcd.com").openConnection(proxy);
//		connection.setDoOutput(true);
//		connection.setDoInput(true);
//		connection.setRequestProperty("Content-type", "text/xml");
//		connection.setRequestProperty("Accept", "text/xml, application/xml");
//		connection.setRequestMethod("POST");
	}
	
	/**
	 * Aggiunge un cookie alla response, senza scadenza e per tutte le pagine
	 * @param name
	 * @param value
	 * @param response
	 */
	public void setCookie(String name, String value, HttpServletResponse response) {
		Cookie cookie = new Cookie(name, value);
		response.addCookie(cookie);
	}
	
	/**
	 * Ritorna true se il cookie specificato è presente
	 * @param name cookie name ignorecase
	 * @param request
	 * @return
	 */
	public boolean hasCookie(String name, HttpServletRequest request) {
		if (request!=null && name!=null) {
			Cookie[] cookies = request.getCookies();
			if (cookies!=null) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equalsIgnoreCase(name)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Ritorna tutti i cookie mandati con la request
	 * @param request
	 * @return
	 */
	public Map<String, String> getCookies(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		Map<String, String> result = new HashMap<String, String>();
		if (cookies!=null) {
			for(Cookie cookie : cookies){
				result.put(cookie.getName(), cookie.getValue());
			}
		}
		return result;
	}
	
	/**
	 * Return the value of the first cookie found with the given name.
	 * @param request
	 * @param name
	 * @return the cookie value or null if none is found
	 */
	public String getOneCookieValue(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies!=null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(name)) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Tests if a request is an Ajax request
	 * @param request
	 * @return
	 */
	public boolean isAjax(HttpServletRequest request) {
		String requestedWith = request.getHeader("x-requested-with");
		return requestedWith!=null && requestedWith.toLowerCase().contains("xmlhttprequest"); // contains, because there could be more than one "XMLHttpRequest" value in the header if added twice
	}
	
	/**
     * Uncompress an InputStream
     * @param compressedContent
     * @param encodingName can be "gzip" or "deflate"
     * @return the uncompressed input when uncompressed successfully, null when failed or the contentEncoding is not recognized
     */
	public byte[] uncompress(byte[] compressedContent, String encodingName) {
		byte[] result = null;
		InputStream inputStream = null;
		if (compressedContent.length==0) {
			return new byte[0]; // Empty uncompresses to empty
		}
		try {
			if ("gzip".equalsIgnoreCase(encodingName)) {
				inputStream = new GZIPInputStream(new ByteArrayInputStream(compressedContent));
			} else if ("deflate".equalsIgnoreCase(encodingName)) {
				inputStream = new DeflaterInputStream(new ByteArrayInputStream(compressedContent));
			} else {
				log.debug("Can't uncompress due to invalid encoding: {} (ignored)", encodingName);
				return null; // Not a valid encoding
			}
			ByteArrayOutputStream bout = new ByteArrayOutputStream((int) (compressedContent.length*1.2)); // *1.2 (20% increase) is just a guess
			byte[] buffer = new byte[8192>compressedContent.length/2?compressedContent.length/2+10:8192];
			int len;
			while ((len = inputStream.read(buffer)) != -1) {
				bout.write(buffer, 0, len);
			}
			inputStream.close();
			result = bout.toByteArray();
			bout.close();
		} catch (OutOfMemoryError e) {
			log.error("Can't uncompress due to lack of memory (ignored)", e);
		} catch (IOException e) {
			log.error("Can't uncompress due to IOException (ignored)", e);
		} catch (Throwable t) {
			log.error("Can't uncompress due to Throwable (ignored)", t);
		}
		return result;
	}

	/**
	 * Return true if the contentType is of text type: html, css, javascript...
	 * @param contentType
	 * @return
	 */
	public static boolean isContentTypeText(String contentType) {
		if (contentType==null) {
			return false;
		}
		return 
			contentType.startsWith("text") || // "text/html", "text/css"
			contentType.contains("javascript") ||
			contentType.contains("xhtml+xml")
			// TODO more?
			;
	}
	
	/**
	 * Return the document type by checking the request content type and the extension of the request path
	 * @param request
	 * @return
	 */
	public int getRequestDocumentType(HttpServletRequest request) {
		String contentType = request.getContentType();
		int result = getDocumentType(contentType);
		if (result!=CONTENT_UNKNOWN) {
			return result;
		}
		String pathInfo = request.getPathInfo();
		if (pathInfo==null) {
			return CONTENT_DOCUMENT;
		}
		if (pathInfo.endsWith(".css")) {
			return CONTENT_CSS;
		}
		if (pathInfo.endsWith(".js")) {
			return CONTENT_JAVASCRIPT;
		}
		if (pathInfo.endsWith(".gif") 
			|| pathInfo.endsWith(".jpeg") 
			|| pathInfo.endsWith(".jpg") 
			|| pathInfo.endsWith(".png") 
			|| pathInfo.endsWith(".tiff")) {
			return CONTENT_IMAGE;
		}
		return CONTENT_DOCUMENT;
	}

	/**
	 * Returms the content type of a response given the content-type
	 * @param fullContentType the content-type (from request.getContentType()), also with charset or other attributes like "text/html; charset=UTF-8"
	 * @return an integer for the type of the content
	 */
	public int getDocumentType(String fullContentType) {
		if (StringUtils.trimToNull(fullContentType)==null) {
			return CONTENT_UNKNOWN;
		}
		String[] parts = fullContentType.toLowerCase().split("[; ]", 2);
		String contentType = parts[0];
		if ("text/html".equals(contentType) || "application/xhtml+xml".equals(contentType)) {
			return CONTENT_DOCUMENT;
		}
		if ("text/xml".equals(contentType)) {
			return CONTENT_XML;
		}
		if ("application/javascript".equals(contentType) || "application/x-javascript".equals(contentType) || "text/javascript".equals(contentType)) {
			return CONTENT_JAVASCRIPT;
		}
		if ("text/css".equals(contentType)) {
			return CONTENT_CSS;
		}
		if (contentType.startsWith("image/")) {
			return CONTENT_IMAGE;
		}
		return CONTENT_OTHER;
	}
	
	/**
	 * Removes the port from a host address, which could also be ipv4 or ipv6
	 * @param hostAddress like "www.myserver.net", "www.myserver.net:8080", "123.12.13.125:8080", "[1fff:0:a88:85a3::ac1f]:8001"
	 * @return the host with no port like www.myserver.net or "123.12.13.125" or the original string on error
	 */
	public String removePort(String hostAddress) {
		try {
			int pos = hostAddress.lastIndexOf(']');
			if (pos>-1) {
				return hostAddress.substring(0, pos+1); // ipv6
			}
			pos = hostAddress.lastIndexOf(':');
			if (pos>-1) {
				return hostAddress.substring(0, pos);
			}
			return hostAddress;
		} catch (Exception e) {
			log.error("Can't extract host from {}", hostAddress);
		}
		return hostAddress;
	}
	
	/**
	 * Extract the address from a url, without schema and path but with port
	 * @param url an url like http://www.myserver.net:8080/context/path or //www.myserver.net:8080/context/path
	 * @return the host[:port] like www.myserver.net:8080, or ""
	 */
	public String extractAddress(String url) {
		if (StringUtils.trimToNull(url)!=null) {
			try {
				int pos = url.indexOf("//");
				pos = (pos<0?0:pos+2);
				int pos2 = url.indexOf("/", pos);
				pos2 = (pos2<0?url.length():pos2);
				return url.substring(pos, pos2);
			} catch (Exception e) {
				log.error("Can't extract address from {}", url);
			}
		}
		return "";
	}

	/**
	 * Extract the path from a url (the servlet context will be included)
	 * @param url an url like http://www.myserver.net:8080/xxx/yyy/zzz or //www.myserver.net:8080/xxx/yyy/zzz or www.myserver.net:8080/xxx/yyy/zzz
	 * @return the path like "/xxx/yyy/zzz" or "/" at the least
	 */
	public String extractPath(String url) {
		if (url!=null) {
			try {
				int pos = url.indexOf("//");
				pos = (pos<0?0:pos+2);
				int pos2 = url.indexOf("/", pos);
				pos2 = (pos2<0?url.length():pos2);
				String result = url.substring(pos2);
				if (result.length()>0) {
					return result;
				}
			} catch (Exception e) {
				log.error("Can't extract path from {}", url);
			}
		}
		return "/";
	}
	
	/**
	 * Given the current full address and a relative address, computes the new full address.
	 * If the relative address starts with / then it is considered relative to the server, not to the servlet context.
	 * The relative address can be a url parameter like ?xx=yy or an anchor like #aaa
	 * @param currentFullAddress like http://some.server.com/aaa/bbb.go
	 * @param newRelativeAddress like ccc.go or /xxx/yyy.go
	 * @return
	 */
	public String relativeToAbsolute(String currentFullAddress, String newRelativeAddress) {
		String prefix = currentFullAddress;
		char start = newRelativeAddress.charAt(0);
		if (start!='?' && start!='#') {
			if (start=='/') {
				int from = currentFullAddress.indexOf("//");
				int cutPos = currentFullAddress.indexOf("/", from+2);
				prefix = currentFullAddress.substring(0, cutPos);
			} else {
				prefix = currentFullAddress.substring(0, currentFullAddress.lastIndexOf('/')+1);
			}
		}
		return prefix + newRelativeAddress;
	}

//	/**
//	 * Extract the first level domain from a given address.
//	 * @param address the address in any form: www.facebook.com or https://www.google.com:8080/doit
//	 * @return the first level domain: facebook.com or google.com in the above examples
//	 */
//	public String extractDomain(String address) {
//		// Remove any schema
//		if (address.startsWith("http") || address.startsWith("//")) {
//			int pos = address.indexOf("//"); // http://www..., https://www...
//			if (pos>-1 && pos<7) {
//				address = address.substring(pos+2);
//			}
//		}
//		// Remove any path
//		int pos = address.indexOf("/"); // www.aaa.com/asd
//		if (pos>-1) {
//			address = address.substring(0, pos); // www.aaa.com
//		}
//		// Remove any port
//		pos = address.indexOf(":"); // www.aaa.com:8080
//		if (pos>-1) {
//			address = address.substring(0, pos); // www.aaa.com
//		}
//		// Cut to second dot from end
//		pos = address.lastIndexOf('.');
//		pos = address.lastIndexOf('.', pos-1);
//		if (pos>-1) {
//			address = address.substring(pos+1, address.length());
//		}
//		return address;
//	}
	
	/**
	 * Returns the current webapp address, e.g. http://www.myserver.net:8080/context/ with a trailing slash
	 * @param request
	 * @return
	 */
	public String getWebappFullAddress(HttpServletRequest request) {
		String trailing="/";
		String requestUrl = request.getRequestURL().toString(); // http://www.myserver.net:8080/app/test/ab;jsessionid=23536
		String contextPath = request.getContextPath(); // /context
		if (contextPath.length()==0) {
			contextPath="/";
			trailing="";
		}
		int doubleSlashPos = requestUrl.indexOf("//");
		int contextPathPos = requestUrl.indexOf(contextPath, doubleSlashPos+2);
		return requestUrl.substring(0, contextPathPos + contextPath.length()) + trailing;
	}

	public void redirectPermanent(String newUrl, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
	    response.setHeader("Location", newUrl);
	    response.setContentType("text/html");
	}

	public void redirectTemporary(String newUrl, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
		response.setHeader("Location", newUrl);
		response.setContentType("text/html");
	}

	public void setAttribute(HttpServletRequest request, String name, Object value) {
		request.setAttribute(name, value);
	}
	
	public void forward(String page, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			request.getRequestDispatcher(page).forward(request, response);
		} catch (Exception e) {
			// http://tomcat.10.x6.nabble.com/Tomcat-8-Listener-Web-RequestDispatcher-td5030350.html
			log.error("Can't forward to page '{}', probably not started yet (ignored): " + e.getMessage(), page);
		}
	}

}
