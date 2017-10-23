package net.yadaframework.security.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.persistence.entity.YadaTicketStatus;
import net.yadaframework.persistence.entity.YadaTicketType;
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
	@Version
	protected long version;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected int priority;
	
	protected Date creationDate;
	
	@OneToOne(fetch = FetchType.EAGER)
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected YadaPersistentEnum<YadaTicketType> type;

	@OneToOne(fetch = FetchType.EAGER)
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected YadaPersistentEnum<YadaTicketStatus> status;

	@Column(length=80)
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected String subject;
	
	/*
	@Column(length=8192)
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected String message;
	*/
	
	@Column
	//@JsonView(YadaJsonView.WithLazyAttributes.class)
	@OneToMany(mappedBy="yadaTicket", cascade=CascadeType.ALL, orphanRemoval=true)
	protected List<YadaTicketMessage> messages;
	
	
	@ManyToOne(optional = false)
	// @JsonView(YadaJsonView.WithLazyAttributes.class)
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
	
	@JsonProperty
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	public String getUsername() {
		return owner.getUserCredentials().getUsername();
	}
	
	/*
	@JsonProperty("message")
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	public String getMessage() {
		String message = "";
		for(YadaTicketMessage  msg : messages) {
			message = msg.getMessage() ;
		}
		return message;
	}
	*/
	
	/***********************************************************************/
	/* Plain getter / setter                                               */
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	/////////////////////////
	public YadaPersistentEnum<YadaTicketType> getType() {
		return type;
	}

	public void setType(YadaPersistentEnum<YadaTicketType> type) {
		this.type = type;
	}

	public YadaPersistentEnum<YadaTicketStatus> getStatus() {
		return status;
	}

	public void setStatus(YadaPersistentEnum<YadaTicketStatus> status) {
		this.status = status;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	/*
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}*/

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}
	
	public YadaUserProfile getOwner() {
		return owner;
	}

	public void setOwner(YadaUserProfile owner) {
		this.owner = owner;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public List<YadaTicketMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<YadaTicketMessage> messages) {
		this.messages = messages;
	}


	
}
