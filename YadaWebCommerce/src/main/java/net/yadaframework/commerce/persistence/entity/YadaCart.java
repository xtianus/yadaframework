package net.yadaframework.commerce.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import net.yadaframework.security.persistence.entity.YadaUserProfile;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaCart implements Serializable {
	private static final long serialVersionUID = 1L;

	// For synchronization with external databases
	@Column(insertable = false, updatable = false, columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	protected Date modified;

	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;

	@ManyToOne
	protected YadaUserProfile owner;

	@Column(columnDefinition="TIMESTAMP NULL")
	protected Date creationTimestamp;

	@OneToMany(mappedBy="cart", cascade=CascadeType.ALL, orphanRemoval=true)
	List<YadaCartItem> cartItems;

	public YadaUserProfile getOwner() {
		return owner;
	}

	public void setOwner(YadaUserProfile owner) {
		this.owner = owner;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreationTimestamp() {
		return creationTimestamp;
	}

	public void setCreationTimestamp(Date creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	public List<YadaCartItem> getCartItems() {
		return cartItems;
	}

	public void setCartItems(List<YadaCartItem> cartItems) {
		this.cartItems = cartItems;
	}

	public long getVersion() {
		return version;
	}

}
