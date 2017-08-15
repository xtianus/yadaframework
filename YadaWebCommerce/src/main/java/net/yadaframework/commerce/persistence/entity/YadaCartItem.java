package net.yadaframework.commerce.persistence.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class YadaCartItem implements Serializable {
	private static final long serialVersionUID = 1L;
	
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
	

}
