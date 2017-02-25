package net.yadaframework.persistence.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.core.YadaRegistrationType;
import net.yadaframework.persistence.entity.YadaRegistrationRequest;

@Transactional(readOnly = true) 
public interface YadaRegistrationRequestRepository extends JpaRepository<YadaRegistrationRequest, Long> {

	List<YadaRegistrationRequest> findByIdAndTokenOrderByTimestampDesc(long id, long token);

	// List<YadaRegistrationRequest> findByEmail(String email);
	List<YadaRegistrationRequest> findByEmailAndRegistrationType(String email, YadaRegistrationType registrationType);

	/**
	 * Trova tutti gli elementi con timestamp antecedente la data indicata
	 * @param upperLimit
	 * @return
	 */
	List<YadaRegistrationRequest> findByTimestampBefore(Date upperLimit);

}
