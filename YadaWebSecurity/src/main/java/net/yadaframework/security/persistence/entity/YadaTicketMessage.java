package net.yadaframework.security.persistence.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonView;

import net.yadaframework.web.YadaJsonView;

@Entity
public class YadaTicketMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	// For synchronization with external databases
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Column(columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified;
	
	// For optimistic locking
	@Version
	protected long version;
	
	@Temporal(TemporalType.TIMESTAMP)
	protected Date dateSent = new Date();
	
	@ManyToOne(optional = false)
	protected YadaTicket yadaTicket;
	
	@Column(length=8192)
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	protected String message;
	
	@ManyToOne(optional = false)
	protected YadaUserProfile author;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDateSent() {
		return dateSent;
	}

	public void setDateSent(Date dateSent) {
		this.dateSent = dateSent;
	}

	public YadaTicket getYadaTicket() {
		return yadaTicket;
	}

	public void setYadaTicket(YadaTicket yadaTicket) {
		this.yadaTicket = yadaTicket;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public YadaUserProfile getAuthor() {
		return author;
	}

	public void setAuthor(YadaUserProfile author) {
		this.author = author;
	}
	
}
