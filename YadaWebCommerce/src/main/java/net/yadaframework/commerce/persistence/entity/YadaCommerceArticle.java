package net.yadaframework.commerce.persistence.entity;

import jakarta.persistence.Entity;

import net.yadaframework.cms.persistence.entity.YadaArticle;

@Entity
public class YadaCommerceArticle extends YadaArticle {
	private static final long serialVersionUID = 1L;

	protected int availableQuantity; // Warehouse stock

	protected int daysBeforeAvailable; // Usually available in X days

	public int getAvailableQuantity() {
		return availableQuantity;
	}

	public void setAvailableQuantity(int availableQuantity) {
		this.availableQuantity = availableQuantity;
	}

	public int getDaysBeforeAvailable() {
		return daysBeforeAvailable;
	}

	public void setDaysBeforeAvailable(int daysBeforeAvailable) {
		this.daysBeforeAvailable = daysBeforeAvailable;
	}


}
