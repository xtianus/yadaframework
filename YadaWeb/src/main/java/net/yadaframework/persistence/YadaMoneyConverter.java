package net.yadaframework.persistence;

import javax.persistence.AttributeConverter;

/**
 * JPA converter between Object representation "YadaMoney" and DB column "Long"
 *
 */
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
