package net.yadaframework.security.persistence.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;

/**
 * A message inside a YadaTicket.
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaTicketMessage extends YadaUserMessage<YadaUserMessageType>  implements Serializable {
	private static final long serialVersionUID = 1L;

	@ManyToOne(optional = false)
	protected YadaTicket yadaTicket;

	public YadaTicketMessage() {
		// All YadaTicketMessage instances are a YadaUserMessage with this specific type
		super.setType(YadaUserMessageType.TICKET);
	}
	
	public YadaTicket getYadaTicket() {
		return yadaTicket;
	}

	public void setYadaTicket(YadaTicket yadaTicket) {
		this.yadaTicket = yadaTicket;
	}

}
