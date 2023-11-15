package net.yadaframework.persistence.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Keeps a timestamped log of events for rate-limiting purposes
 * NOT USED YET!!! Aggiungere a persistence.xml poi
 * TODO vedi quanto fatto per FeccMe_Proxy
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaRateLog implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;

	@Column(columnDefinition="TIMESTAMP NULL")
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
