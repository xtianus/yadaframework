package net.yadaframework.persistence.entity;

import java.util.Locale;

import org.springframework.context.MessageSource;

import net.yadaframework.core.YadaLocalEnum;

/**
 * The localized state of a YadaJob:
 * <ul>
 * <li>TECHNICAL_SUPPORT: the type is technical support</li>
 * <li>BILLING: the type is billing</li>
 * <li>OTHER: all other types</li>
 * </ul>
 *
 */
public enum YadaTicketType implements YadaLocalEnum<YadaTicketType> {
	// In messages.properties:
	// yada.tickettype.technicalsupport = Technical Support
	// yada.tickettype.billing = Billing
	// yada.tickettype.other = Other

	TECHNICAL_SUPPORT("yada.tickettype.technicalsupport"),
	BILLING("yada.tickettype.billing"),
	OTHER("yada.tickettype.other");
	
	private String messageKey;
	private YadaPersistentEnum<YadaTicketType> yadaPersistentEnum;
	
	private YadaTicketType(String messageKey) {
		this.messageKey = messageKey;
	}
	
	public YadaPersistentEnum<YadaTicketType> toYadaPersistentEnum() {
		return yadaPersistentEnum;
	}
	
	// TODO fix generics
	public void setYadaPersistentEnum(YadaPersistentEnum yadaPersistentEnum) {
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
