package net.yadaframework.example.support;

import java.util.List;

import org.jsoup.Jsoup;

/**
 * An email captured by {@link CapturingMailSender} instead of being sent.
 * @param from the sender addresses
 * @param to the recipient addresses (to, cc and bcc)
 * @param subject the subject, with the environment prefix that YadaEmailService adds outside production, e.g. "[DEV] "
 * @param body the html or text body
 */
public record CapturedMail(List<String> from, List<String> to, String subject, String body) {

	/**
	 * @return the href of every link in the body, in document order, as written in the email (absolute urls)
	 */
	public List<String> links() {
		return Jsoup.parse(body).select("a[href]").eachAttr("href");
	}

	/**
	 * @return the only link of the body
	 * @throws IllegalStateException when the body doesn't contain exactly one link
	 */
	public String link() {
		List<String> links = links();
		if (links.size() != 1) {
			throw new IllegalStateException("Expected one link in the email \"" + subject + "\", found " + links.size());
		}
		return links.get(0);
	}

	/**
	 * @return the visible text of the body
	 */
	public String text() {
		return Jsoup.parse(body).text();
	}
}
