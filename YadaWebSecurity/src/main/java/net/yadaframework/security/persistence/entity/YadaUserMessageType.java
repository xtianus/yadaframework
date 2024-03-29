package net.yadaframework.security.persistence.entity;

import java.util.Locale;

import org.springframework.context.MessageSource;

import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.persistence.entity.YadaPersistentEnum;

/**
 * The localized type of a YadaUserMessage - can be replaced by a different one in the application.
 *
 */
public enum YadaUserMessageType implements YadaLocalEnum<YadaUserMessageType> {
	// In messages.properties:
	// yada.messagetype.ticket = Ticket
	// yada.messagetype.comment = Comment
	// yada.messagetype.other = Other

	TICKET("yada.messagetype.ticket"),
	COMMENT("yada.messagetype.comment"),
	OTHER("yada.messagetype.other");
	// Add new values to the end because the existing ones may be in a database already

	private String messageKey;
	private YadaPersistentEnum<YadaUserMessageType> yadaPersistentEnum;

	private YadaUserMessageType(String messageKey) {
		this.messageKey = messageKey;
	}

	@Override
	public YadaPersistentEnum<YadaUserMessageType> toYadaPersistentEnum() {
		return yadaPersistentEnum;
	}

	@Override
	public void setYadaPersistentEnum(YadaPersistentEnum<YadaUserMessageType> yadaPersistentEnum) {
		this.yadaPersistentEnum = yadaPersistentEnum;

	}

	/**
	 * Return the localized text for this enum
	 * @param messageSource
	 * @param locale
	 * @return
	 */
	@Override
	public String toString(MessageSource messageSource, Locale locale) {
		return messageSource.getMessage(messageKey, null, locale);
	}
}
