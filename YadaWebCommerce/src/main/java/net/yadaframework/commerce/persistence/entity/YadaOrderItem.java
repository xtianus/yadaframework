package net.yadaframework.commerce.persistence.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.YadaMoneyConverter;

@Entity
public class YadaOrderItem implements Serializable {
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

	@Convert(converter = YadaMoneyConverter.class)
	protected YadaMoney unitPrice;

	// Not a reference to YadaArticle so that we can delete old articles but still have the correct code in the order item
	protected String articleCode;

	@ManyToOne(optional=false)
	protected YadaOrder order;

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

	public YadaMoney getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(YadaMoney unitPrice) {
		this.unitPrice = unitPrice;
	}

	public YadaOrder getOrder() {
		return order;
	}

	public void setOrder(YadaOrder order) {
		this.order = order;
	}

	public String getArticleCode() {
		return articleCode;
	}

	public void setArticleCode(String articleCode) {
		this.articleCode = articleCode;
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
