package net.yadaframework.persistence.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Keeps a timestamped log of events for rate-limiting purposes 
 * NOT USED YET!!! Aggiungere a persistence.xml poi
 * TODO vedi quanto fatto per FeccMe_Proxy
 */
@Entity
public class YadaRateLog implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date at;
	private int type;
	private long data1;
	private long data2;

	public YadaRateLog() {
		at = new Date();
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	
	
}
