package net.yadaframework.commerce.persistence.entity;

import java.util.Locale;

import org.springframework.context.MessageSource;

import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.persistence.entity.YadaPersistentEnum;

public enum YadaOrderStatus implements YadaLocalEnum<YadaOrderStatus> {
	UNPAID("yada.orderStatus.unpaid"),			// New order, waiting for payment
	PAID("yada.orderStatus.paid"),				// Payment ok, can be processed
	PROCESSING("yada.orderStatus.processing"),	// Someone is working on the order
	SHIPPED("yada.orderStatus.shipped"),		// Items shipped
	CANCELLED("yada.orderStatus.cancelled"),	// Order cancelled for any reason
	WAITING("yada.orderStatus.waiting"),		// Waiting on some condition, e.g. item availability
	CONFIRMED("yada.orderStatus.confirmed")		// Customer confirmed order reception
	;

	private String messageKey;
	private YadaPersistentEnum<YadaOrderStatus> yadaPersistentEnum;
	
	private YadaOrderStatus(String messageKey) {
		this.messageKey = messageKey;
	}

	@Override
	public YadaPersistentEnum<YadaOrderStatus> toYadaPersistentEnum() {
		return yadaPersistentEnum;
	}

	@Override
	public void setYadaPersistentEnum(YadaPersistentEnum yadaPersistentEnum) {
		this.yadaPersistentEnum = yadaPersistentEnum;
	}

	@Override
	public String toString(MessageSource messageSource, Locale locale) {
		return messageSource.getMessage(messageKey, null, locale);
	}

}
