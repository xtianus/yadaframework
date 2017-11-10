package net.yadaframework.security.persistence.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
//public class YadaTicketMessage implements Serializable {
public class YadaTicketMessage<T extends Enum<T>> extends YadaUserMessage  implements Serializable {
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
