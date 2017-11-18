package net.yadaframework.persistence.repository;


import java.sql.Timestamp;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.persistence.entity.YadaJobState;

/**
 */
@Repository
@Transactional(readOnly = true) 
public class YadaJobDao {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext private EntityManager em;
    
    /**
     * Deletes a job
     * @param yadaJob
     */
    @Transactional(readOnly = false)
    public void delete(YadaJob yadaJob) {
    	// Natively delete job associations (quicker than JPA)
       	YadaSql.instance()
       		.selectFrom("delete from YadaJob_BeActive")
    		.where("where YadaJob_id = :id or jobsMustBeActive_id = :id")
    		.setParameter("id", yadaJob.getId())
    		.nativeQuery(em)
    		.executeUpdate();
       	YadaSql.instance()
	       	.selectFrom("delete from YadaJob_BeCompleted")
	       	.where("where YadaJob_id = :id or jobsMustComplete_id = :id")
	       	.setParameter("id", yadaJob.getId())
	       	.nativeQuery(em)
	       	.executeUpdate();
       	YadaSql.instance()
	       	.selectFrom("delete from YadaJob_BeInactive")
	       	.where("where YadaJob_id = :id or jobsMustBeInactive_id = :id")
	       	.setParameter("id", yadaJob.getId())
	       	.nativeQuery(em)
	       	.executeUpdate();
       	// JPA-delete so that any joined application subclasses (which are obviously unknown when writing this) are deleted too
       	// Better to reload the entity because the argument might come from another entitymanager (?!)
       	YadaJob deletable = em.find(yadaJob.getClass(), yadaJob.getId());
       	em.remove(deletable);
    }
    
//    /**
//     * If the job is RUNNING, set the state to ACTIVE
//     * @param yadaJob
//     */
//    @Transactional(readOnly = false)
//    public void setActiveWhenRunning(YadaJob yadaJob) {
//    	// Wrong:
//    	YadaSql.instance().updateSet("update YadaJob yg join yg. set yg.jobStateObject = :active")
//		.where("where yg = :job and yg.jobStateObject = :running")
//		.setParameter("active", YadaJobState.ACTIVE.toYadaPersistentEnum())
//		.setParameter("job", yadaJob)
//		.setParameter("running", YadaJobState.RUNNING.toYadaPersistentEnum())
//		.query(em).executeUpdate();
//    }
    
    /**
     * Return the list of jobs that can be recovered after a crash.
     * @return
     */
    @SuppressWarnings("unchecked")
	public List<YadaJob> getRecoverableJobs() {
    	return YadaSql.instance().selectFrom("select yg from YadaJob yg left join yg.jobStateObject")
    		.where("where yg.jobGroupPaused = false and yg.jobRecoverable = true and yg.jobStateObject = :running")
    		.setParameter("running", YadaJobState.RUNNING.toYadaPersistentEnum())
    		.query(em).getResultList();
    }
    
	/**
	 * Disable all RUNNING jobs that are not recoverable and not group-paused. Called at server startup.
	 * @param stateObject
	 */
    @Transactional(readOnly = false)
	public void setUnrecoverableJobState() {
		YadaSql.instance().updateSet("update YadaJob yg set yg.jobStateObject = :disabled")
		.where("where yg.jobGroupPaused = false and yg.jobRecoverable = false and yg.jobStateObject = :running")
		.setParameter("disabled", YadaJobState.DISABLED.toYadaPersistentEnum())
		.setParameter("running", YadaJobState.RUNNING.toYadaPersistentEnum())
		.query(em).executeUpdate();
	}

    /**
     * Change the state of all jobs in the group
     * @param jobGroup
     * @param fromState
     * @param toState
     */
	public void changeAllStates(String jobGroup, YadaJobState fromState, YadaJobState toState) {
		YadaSql.instance().set("set "); //////////////// TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		
		
	}
	
}
