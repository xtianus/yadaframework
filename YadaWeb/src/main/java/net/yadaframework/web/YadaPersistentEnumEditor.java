package net.yadaframework.web;

import java.beans.PropertyEditorSupport;

import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.entity.YadaPersistentEnum;

/**
 * Used by thymeleaf to convert between string representation and YadaPersistentEnum.
 * In can convert any number of YadaLocalEnum implementations, provided that the enum names don't clash.
 * Must be set in a controller using an initBinder:
 * <pre>
 * @InitBinder
 * public void initBinder(WebDataBinder binder) {
 *    binder.registerCustomEditor(YadaPersistentEnum.class, new YadaPersistentEnumEditor(new Class[] {EnumCategory.class, EnumGender.class}));
 *
 */
public class YadaPersistentEnumEditor extends PropertyEditorSupport {
	private Class[] allEnums;
	
	/**
	 * Create an editor that can convert the specified enum classes.
	 * @param allEnums array of enum classes that implement YadaLocalEnum
	 */
	public YadaPersistentEnumEditor(Class[] allEnums) {
		this.allEnums = allEnums;
	}
	
	@Override
	public void setAsText(String enumName) throws IllegalArgumentException {
		if (enumName.equals("-1")) {
			return; // Select header
		}
		for (Class someEnum : allEnums) {
			try {
				YadaLocalEnum result = (YadaLocalEnum) Enum.valueOf(someEnum, enumName);
				this.setValue(result.toYadaPersistentEnum());
				return;
			} catch (Exception e) {
				// Ignore and keep going;
			}
		}
		throw new YadaInvalidUsageException("Name {} not found in enums {}", enumName, allEnums);
	}
	
	@Override
	public String getAsText() {
		YadaPersistentEnum toConvert = (YadaPersistentEnum) getValue();
		return toConvert!=null?toConvert.getLocalText():null;
	}
}

