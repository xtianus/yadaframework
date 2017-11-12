package net.yadaframework.security.persistence.entity;

import java.util.Locale;

import org.springframework.context.MessageSource;

import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.persistence.entity.YadaPersistentEnum;

/**
 * The localized type of a YadaTicket. Can be used as-is, or an application-specific one can be created.
 *
 */
public enum YadaTicketType implements YadaLocalEnum<YadaTicketType> {
	// In messages.properties:
	// yada.tickettype.technicalsupport = Technical Support
	// yada.tickettype.billing = Billing
	// yada.tickettype.other = Other

	TECHNICAL_SUPPORT("yada.tickettype.technicalsupport"),
	COMMERCIAL_SUPPORT("yada.tickettype.commercialsupport"),
	BILLING("yada.tickettype.billing"),
	FEEDBACK("yada.tickettype.feedback"),
	OTHER("yada.tickettype.other");
	
	private String messageKey;
	private YadaPersistentEnum<YadaTicketType> yadaPersistentEnum;
	
	private YadaTicketType(String messageKey) {
		this.messageKey = messageKey;
	}
	
	public YadaPersistentEnum<YadaTicketType> toYadaPersistentEnum() {
		return yadaPersistentEnum;
	}
	
	@Override
	public void setYadaPersistentEnum(YadaPersistentEnum<YadaTicketType> yadaPersistentEnum) {
		this.yadaPersistentEnum = yadaPersistentEnum;
	}
	
	/**
	 * Return the localized text for this enum
	 * @param messageSource
	 * @param locale
	 * @return
	 */
	public String toString(MessageSource messageSource, Locale locale) {
		return messageSource.getMessage(messageKey, null, locale);
	}
}
