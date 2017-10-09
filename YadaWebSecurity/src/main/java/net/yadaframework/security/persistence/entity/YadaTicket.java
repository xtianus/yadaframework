package net.yadaframework.security.persistence.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.persistence.repository.YadaLocaleDao;
import net.yadaframework.web.YadaJsonView;

/**
 *
 */
@Entity
public class YadaTicket implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// For synchronization with external databases
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Column(columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified;
	
	// For optimistic locking
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Version
	protected long version;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	protected Date creationDate;
	
	@OneToOne(fetch = FetchType.EAGER)
	protected YadaPersistentEnum<YadaTicketType> type;

	@OneToOne(fetch = FetchType.EAGER)
	protected YadaPersistentEnum<YadaTicketStatus> status;

	@Column(length=80)
	protected String subject;
	
	@Column(length=8192)
	protected String message;
	
	@ManyToOne(optional = false)
	protected YadaUserProfile owner;
	
	
// TODO allegare uno o più file	
//	/**
//	 * The main image to show in lists etc.
//	 */
//	@JsonView(YadaJsonView.WithEagerAttributes.class)
//	@OneToOne(cascade=CascadeType.REMOVE, orphanRemoval=true)
//	protected YadaAttachedFile attachment;
	
	/***********************************************************************/
	/* Id for DataTables                                                   */
	
	@Transient
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@JsonProperty("DT_RowId")
	public String getDT_RowId() {
		return this.getClass().getSimpleName()+"#"+this.id; // YadaProduct#142
	}
	
	/***********************************************************************/
	/* Plain getter / setter                                               */
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	
}
