package net.yadaframework.components;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.entity.YadaClause;
import net.yadaframework.persistence.repository.YadaClauseRepository;

// Not a @Component
abstract public class YadaSetup {
	private transient Logger log = LoggerFactory.getLogger(YadaSetup.class);
	
	@Autowired private YadaConfiguration yadaConfiguration;
	@Autowired private YadaClauseRepository yadaClauseRepository;

	@PostConstruct
	public void init() throws Exception {
		log.info("Setup started");
		
		setupApplication();
		
		List<Map<String, Object>> userList = yadaConfiguration.getSetupUsers();
		setupUsers(userList);
		
		// Se nel database non ce ne sono, vengono create le clauses
		if (yadaClauseRepository.count()==0) {
			List<YadaClause> yadaClauses = yadaConfiguration.getSetupClauses();
			if (!yadaClauses.isEmpty()) {
				yadaClauseRepository.save(yadaClauses);
			}
		}
		
		log.info("Setup finished");
	}

	/**
	 * Store a list of configured users into the database.
	 * @param userList
	 */
	abstract protected void setupUsers(List<Map<String, Object>> userList) ;
	
	/**
	 * Setup of the database entities not user-related, or create other initial artifacts (folders, files, etc.)
	 */
	abstract protected void setupApplication() ;

}
