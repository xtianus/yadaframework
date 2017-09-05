package net.yadaframework.persistence;

import javax.persistence.AttributeConverter;

/**
 * JPA converter between Object representation "YadaMoney" and DB column "Long"
 *
 */
public class YadaMoneyConverter implements AttributeConverter<YadaMoney, Long> {

	@Override
	public Long convertToDatabaseColumn(YadaMoney attribute) {
		if (attribute==null) {
			return null;
		}
		return attribute.getAmount();
	}

	@Override
	public YadaMoney convertToEntityAttribute(Long dbData) {
		if (dbData==null) {
			return null;
		}
		return new YadaMoney(dbData);
	}


}
