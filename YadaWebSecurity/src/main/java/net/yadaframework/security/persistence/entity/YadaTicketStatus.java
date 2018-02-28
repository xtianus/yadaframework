package net.yadaframework.security.persistence.entity;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.persistence.entity.YadaPersistentEnum;

/**
 * The localized state of a YadaTicket
 */
public enum YadaTicketStatus implements YadaLocalEnum<YadaTicketStatus> {
	// In messages.properties:
	//	yada.ticketstatus.open = OPEN
	//	yada.ticketstatus.answered = ANSWERED
	//	yada.ticketstatus.closed = CLOSED

	OPEN("yada.ticketstatus.open"),
	ANSWERED("yada.ticketstatus.answered"),
	CLOSED("yada.ticketstatus.closed");
	
	private String messageKey;
	private YadaPersistentEnum<YadaTicketStatus> yadaPersistentEnum;
	
	private YadaTicketStatus(String messageKey) {
		this.messageKey = messageKey;
	}
	
	public YadaPersistentEnum<YadaTicketStatus> toYadaPersistentEnum() {
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
	
	@Override
	public String toString() {
		Locale locale = LocaleContextHolder.getLocale();
		MessageSource messageSource = (MessageSource) YadaUtil.getBean("messageSource");
		return messageSource.getMessage(messageKey, null, locale);
	}
}
