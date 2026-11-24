package net.yadaframework.tests.web.audit;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import net.yadaframework.tests.support.ApplicationSession;
import net.yadaframework.tests.support.LogCapture;

/**
 * Sends requests to the test application and returns the MDC of the AuditFilter log line of each one, identified by a unique
 * query string, so that the "remoteIp" and "forwardedFor" fields can be checked. The MDC is copied on the logging thread:
 * a captured event would read the MDC of the test thread.
 */
final class AuditRequestProbe {
	private final ApplicationSession session;
	private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

	/**
	 * Copies the MDC of the audit lines on the thread that logs them
	 */
	private static final class MdcAppender extends AppenderBase<ILoggingEvent> {
		private final List<String[]> messages = new CopyOnWriteArrayList<>();
		private final List<Map<String, String>> mdcs = new CopyOnWriteArrayList<>();

		@Override
		protected void append(ILoggingEvent event) {
			if (event.getLoggerName().endsWith("AuditFilter") && event.getFormattedMessage().startsWith("queryString:")) {
				messages.add(new String[] { event.getFormattedMessage() });
				mdcs.add(Map.copyOf(event.getMDCPropertyMap()));
			}
		}

		Map<String, String> find(String marker) {
			for (int i = 0; i < messages.size() && i < mdcs.size(); i++) {
				if (messages.get(i)[0].contains(marker)) {
					return mdcs.get(i);
				}
			}
			return null;
		}
	}

	AuditRequestProbe(ApplicationSession session) {
		this.session = session;
	}

	private interface Sender {
		void send(String marker) throws Exception;
	}

	private Map<String, String> capture(boolean required, Sender sender) throws Exception {
		String marker = "probe" + UUID.randomUUID().toString().replace("-", "");
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		MdcAppender appender = new MdcAppender();
		appender.setContext(loggerContext);
		appender.start();
		try (LogCapture logs = LogCapture.start()) {
			loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).addAppender(appender);
			try {
				sender.send(marker);
				long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(required ? 5 : 1);
				Map<String, String> mdc = appender.find(marker);
				while (mdc == null && System.nanoTime() < deadline) {
					Thread.sleep(50);
					mdc = appender.find(marker);
				}
				if (mdc == null && required) {
					throw new AssertionError("No audit log event for " + marker + " in:\n" + logs.text());
				}
				return mdc;
			} finally {
				loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).detachAppender(appender);
				appender.stop();
			}
		}
	}

	/**
	 * Requests the login page with the given X-Forwarded-For values
	 * @return the MDC of the audit line of the request
	 */
	Map<String, String> get(String... forwardedFor) throws Exception {
		return capture(true, marker -> {
			HttpRequest.Builder builder = HttpRequest.newBuilder(session.application().resolve("/login?" + marker + "=1")).GET();
			for (String value : forwardedFor) {
				builder.header("X-Forwarded-For", value);
			}
			client.send(builder.build(), HttpResponse.BodyHandlers.discarding());
		});
	}

	/**
	 * Sends the request on a raw socket so that header bytes are not filtered by the HTTP client
	 * @return the MDC of the audit line, or null if the server rejected the request before the filter
	 */
	Map<String, String> getRaw(String rawHeaderValue) throws Exception {
		return capture(false, marker -> {
			URI uri = session.application().baseUri();
			try (Socket socket = new Socket(uri.getHost(), uri.getPort())) {
				socket.setSoTimeout(10000);
				String request = "GET " + uri.getPath().replaceAll("/$", "") + "/login?" + marker + "=1 HTTP/1.1\r\nHost: " + uri.getHost() + ":" + uri.getPort()
					+ "\r\nX-Forwarded-For: " + rawHeaderValue + "\r\nConnection: close\r\n\r\n";
				OutputStream out = socket.getOutputStream();
				out.write(request.getBytes(StandardCharsets.ISO_8859_1));
				out.flush();
				InputStream in = socket.getInputStream();
				in.readAllBytes();
			}
		});
	}
}
