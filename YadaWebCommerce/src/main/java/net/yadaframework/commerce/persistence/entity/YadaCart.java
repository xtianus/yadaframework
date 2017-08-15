package net.yadaframework.commerce.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
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

import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.security.persistence.entity.YadaUserProfile;

@Entity
public class YadaCart implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	@ManyToOne
	protected YadaUserProfile owner;
	
	@Temporal(TemporalType.TIMESTAMP)
	protected Date creationTimestamp;
	
	@OneToMany(mappedBy="cart", cascade=CascadeType.ALL, orphanRemoval=true)
	List<YadaCartItem> cartItems;
	
	public YadaUserProfile getOwner() {
		return owner;
	}

	public void setOwner(YadaUserProfile owner) {
		this.owner = owner;
	}

}
