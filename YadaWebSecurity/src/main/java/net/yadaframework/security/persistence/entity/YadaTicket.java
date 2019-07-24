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
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.web.YadaJsonView;

/**
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaTicket implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// For optimistic locking
	@Version
	protected long version;
	
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	@Column(length=80)
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected String title;

	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected int priority;
	
	protected Date creationDate = new Date();
	
	@OneToOne(fetch = FetchType.EAGER)
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected YadaPersistentEnum<?> type; // Can't set the generics type <?> because enum subclasses are not possible in Java and I wouldn't be able to set an application-defined enum

	@OneToOne(fetch = FetchType.EAGER)
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected YadaPersistentEnum<YadaTicketStatus> status;
	
	@Column
	//@JsonView(YadaJsonView.WithLazyAttributes.class)
	@OneToMany(mappedBy="yadaTicket", cascade=CascadeType.ALL, orphanRemoval=true)
	protected List<YadaTicketMessage> messages;
	
	
	@ManyToOne(optional = false)
	@OneToOne
	protected YadaUserProfile owner;
	
	@ManyToOne(optional = true)
	@OneToOne
	protected YadaUserProfile assigned;
	
	
// TODO allegare uno o più file	
//	/**
//	 * The main image to show in lists etc.
//	 */
//	//@JsonView(YadaJsonView.WithEagerAttributes.class)
//	@OneToOne(cascade=CascadeType.REMOVE, orphanRemoval=true)
//	protected YadaAttachedFile attachment;
	
	/***********************************************************************/
	/* Id for DataTables                                                   */
	
	@Transient
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	@JsonProperty("DT_RowId")
	public String getDT_RowId() {
		return this.getClass().getSimpleName()+"#"+this.id; // YadaProduct#142
	}
	
	@Transient
	@JsonProperty
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	public String getOwnerName() {
		return owner.getUserCredentials().getUsername();
	}
	
	@Transient
	@JsonProperty
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	public String getAssignedName() {
		return assigned!=null?assigned.getUserCredentials().getUsername():null;
	}
	
	@Transient
	public void setType(YadaLocalEnum<?> type) {
		this.type = type.toYadaPersistentEnum();
	}
	
	@Transient
	public boolean isOpen() {
		return status!=null && status.getEnum().equals(YadaTicketStatus.OPEN);
	}
	
	@Transient
	public boolean isClosed() {
		return status!=null && status.getEnum().equals(YadaTicketStatus.CLOSED);
	}
	
	@Transient
	public boolean isAnswered() {
		return status!=null && status.getEnum().equals(YadaTicketStatus.ANSWERED);
	}
	
	/* //new
	@JsonProperty
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	public String getSubject() {
		String subject="";
		if (messages.size()>0) {
			subject = messages.get(0).getTitle();
		}
		
		return subject;
	}
	*/
		
	/*
	@JsonProperty("message")
	//@JsonView(YadaJsonView.WithEagerAttributes.class)
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
	public YadaPersistentEnum<?> getType() {
		return type;
	}

	public void setType(YadaPersistentEnum<?> type) {
		this.type = type;
	}

	public YadaPersistentEnum<YadaTicketStatus> getStatus() {
		return status;
	}

	public void setStatus(YadaPersistentEnum<YadaTicketStatus> status) {
		this.status = status;
	}
	
	public void setStatus(YadaTicketStatus status) {
		this.status = status.toYadaPersistentEnum();
	}
	
	
	/*
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}*/

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

	public YadaUserProfile getAssigned() {
		return assigned;
	}

	public void setAssigned(YadaUserProfile assigned) {
		this.assigned = assigned;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}


	
}
