package net.yadaframework.commerce.persistence.entity;

import javax.persistence.AttributeConverter;

public class YadaMoneyConverter implements AttributeConverter<YadaMoney, Long> {

	@Override
	public Long convertToDatabaseColumn(YadaMoney attribute) {
		return attribute.getAmount();
	}

	@Override
	public YadaMoney convertToEntityAttribute(Long dbData) {
		return new YadaMoney(dbData);
	}


}
