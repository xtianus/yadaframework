package net.yadaframework.tests.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

/**
 * The mail sender of the YadaTests application: messages built by YadaEmailService are kept in memory instead of
 * being sent, so that tests can read the links and texts that the application issued. Nothing ever connects to a mail
 * server. The mailbox is emptied by {@link #clear()}, which the database reset calls through
 * {@link TestDataService#seedDefaults()}.
 * <p>
 * Get it with {@code application.getBean(CapturingMailSender.class)}.
 */
public class CapturingMailSender extends JavaMailSenderImpl {
	private final List<CapturedMail> messages = new CopyOnWriteArrayList<>();

	@Override
	protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
		for (MimeMessage mimeMessage : mimeMessages) {
			try {
				mimeMessage.saveChanges();
				messages.add(new CapturedMail(addresses(mimeMessage.getFrom()), addresses(mimeMessage.getAllRecipients()), mimeMessage.getSubject(), body(mimeMessage)));
			} catch (MessagingException e) {
				throw new MailParseException("Can't read the captured message", e);
			} catch (IOException e) {
				throw new MailPreparationException("Can't read the body of the captured message", e);
			}
		}
	}

	private static List<String> addresses(Address[] addresses) {
		return addresses == null ? List.of() : Arrays.stream(addresses).map(Address::toString).toList();
	}

	/**
	 * @return the html part, or else the text part, of a possibly nested multipart message
	 */
	private static String body(Part part) throws MessagingException, IOException {
		String html = findPart(part, "text/html");
		return html != null ? html : findPart(part, "text/plain");
	}

	private static String findPart(Part part, String mimeType) throws MessagingException, IOException {
		if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
			return null;
		}
		if (part.isMimeType(mimeType)) {
			return (String) part.getContent();
		}
		if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent();
			for (int i = 0; i < multipart.getCount(); i++) {
				String found = findPart(multipart.getBodyPart(i), mimeType);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/**
	 * @return the captured messages, oldest first
	 */
	public List<CapturedMail> messages() {
		return new ArrayList<>(messages);
	}

	/**
	 * @param address a recipient address, e.g. "user@yts.test"
	 * @return the messages sent to that address, oldest first
	 */
	public List<CapturedMail> messagesTo(String address) {
		return messages(mail -> mail.to().stream().anyMatch(recipient -> recipient.equalsIgnoreCase(address)));
	}

	/**
	 * @param address a recipient address
	 * @return the latest message sent to that address
	 * @throws IllegalStateException when there is none
	 */
	public CapturedMail lastMessageTo(String address) {
		List<CapturedMail> found = messagesTo(address);
		if (found.isEmpty()) {
			throw new IllegalStateException("No email captured for " + address + " (" + messages.size() + " captured in total)");
		}
		return found.get(found.size() - 1);
	}

	/**
	 * @param filter the condition
	 * @return the captured messages that satisfy the condition, oldest first
	 */
	public List<CapturedMail> messages(Predicate<CapturedMail> filter) {
		return messages.stream().filter(filter).toList();
	}

	/**
	 * Empties the mailbox
	 */
	public void clear() {
		messages.clear();
	}
}
