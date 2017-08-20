package net.yadaframework.cms.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.cms.persistence.entity.YadaProduct;

@Transactional(readOnly = true) 
public interface YadaProductRepository<T extends YadaProduct> extends JpaRepository<T, Long> {
	
	// TODO metodo che fa il find con l'id e carica anche le stringhe localizzate
	// ammesso che si possa fare con il join fetch di attributi multipli
	// ma forse è meglio fare un dao generico YadaLocaleDao.java che con la reflection tira dentro gli attributi localizzati

}
