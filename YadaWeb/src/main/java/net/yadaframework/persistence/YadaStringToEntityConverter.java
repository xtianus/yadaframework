package net.yadaframework.persistence;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import net.yadaframework.persistence.repository.YadaGenericEntityDao;

// Volevo registrare questa classe come converter da id a Entity ma non viene presa
// TODO provare questo workaround: http://www.apprenticeshipnotes.org/2013/03/springframework-using-annotations-to_14.html
public class YadaStringToEntityConverter implements ConditionalGenericConverter {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired YadaGenericEntityDao yadaGenericEntityDao;
	
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source==null) {
			return null;
		}
		String souceString = (String) source;
		long id = Long.parseLong(souceString);
		return yadaGenericEntityDao.getEntity(id, targetType.getType());
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return null; // null means any is accepted
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return sourceType.getType() == String.class && targetType.hasAnnotation(javax.persistence.Entity.class);
	}
}
