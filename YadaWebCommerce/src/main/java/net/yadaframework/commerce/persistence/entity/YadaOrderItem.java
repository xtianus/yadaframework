package net.yadaframework.commerce.persistence.entity;

import java.io.Serializable;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Entity
public class YadaOrderItem implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	protected int quantity;
	
	@Convert(converter = YadaMoneyConverter.class)
	protected YadaMoney unitPrice;
	
	@OneToOne(optional=false)
	protected YadaArticle article;
	
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

	public YadaArticle getArticle() {
		return article;
	}

	public void setArticle(YadaArticle article) {
		this.article = article;
	}

	public YadaOrder getOrder() {
		return order;
	}

	public void setOrder(YadaOrder order) {
		this.order = order;
	}

}
