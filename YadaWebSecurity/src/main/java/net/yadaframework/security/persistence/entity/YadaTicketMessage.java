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
//public class YadaTicketMessage implements Serializable {
public class YadaTicketMessage<T extends Enum<T>> extends YadaUserMessage<T>  implements Serializable {
	private static final long serialVersionUID = 1L;

	@ManyToOne(optional = false)
	protected YadaTicket yadaTicket;

	public YadaTicket getYadaTicket() {
		return yadaTicket;
	}

	public void setYadaTicket(YadaTicket yadaTicket) {
		this.yadaTicket = yadaTicket;
	}

}
