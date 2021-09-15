package net.yadaframework.commerce.persistence.entity;

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

@Entity
public class YadaCartItem implements Serializable {
	private static final long serialVersionUID = 1L;

	// For synchronization with external databases
	@Column(insertable = false, updatable = false, columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified;

	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;

	protected int quantity;

	@ManyToOne
	protected YadaCommerceArticle article;

	@ManyToOne
	protected YadaCart cart;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public YadaCommerceArticle getArticle() {
		return article;
	}

	public void setArticle(YadaCommerceArticle article) {
		this.article = article;
	}

	public YadaCart getCart() {
		return cart;
	}

	public void setCart(YadaCart cart) {
		this.cart = cart;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public long getVersion() {
		return version;
	}


}
