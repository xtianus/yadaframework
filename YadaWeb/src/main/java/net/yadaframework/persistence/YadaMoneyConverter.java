package net.yadaframework.persistence;

import jakarta.persistence.AttributeConverter;

/**
 * JPA converter between Object representation "YadaMoney" and DB column "Long"
 *
 */
public class YadaMoneyConverter implements AttributeConverter<YadaMoney, Long> {

	@Override
	public Long convertToDatabaseColumn(YadaMoney attribute) {
		if (attribute==null) {
			return 0l;
		}
		return attribute.getInternalValue();
	}

	@Override
	public YadaMoney convertToEntityAttribute(Long dbData) {
		if (dbData==null) {
			dbData = 0l;
		}
		YadaMoney result = YadaMoney.fromDatabaseColumn(dbData);
		return result;
	}


}
