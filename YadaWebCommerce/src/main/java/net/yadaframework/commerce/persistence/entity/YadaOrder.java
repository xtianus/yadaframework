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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import net.yadaframework.persistence.entity.YadaPersistentEnum;

@Entity
public class YadaOrder implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	@OneToOne(fetch = FetchType.EAGER)
	protected YadaPersistentEnum<YadaOrderStatus> orderStatus;
	
	@Temporal(TemporalType.TIMESTAMP)
	protected Date creationTimestamp;
	
	@Temporal(TemporalType.TIMESTAMP)
	protected Date stateChangeTimestamp;
	
	@Temporal(TemporalType.TIMESTAMP)
	protected Date shippingTimestamp;
	
	@Column(length=512)
	protected String trackingData;
	
	@Column(length=2048)
	protected String notes;
	
	@Convert(converter = YadaMoneyConverter.class)
	protected YadaMoney totalPrice;
	
	@OneToMany(mappedBy="order", cascade=CascadeType.ALL, orphanRemoval=true)
	protected List<YadaOrderItem> orderItems;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public YadaPersistentEnum<YadaOrderStatus> getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(YadaPersistentEnum<YadaOrderStatus> orderStatus) {
		this.orderStatus = orderStatus;
	}

	public Date getCreationTimestamp() {
		return creationTimestamp;
	}

	public void setCreationTimestamp(Date creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	public Date getStateChangeTimestamp() {
		return stateChangeTimestamp;
	}

	public void setStateChangeTimestamp(Date stateChangeTimestamp) {
		this.stateChangeTimestamp = stateChangeTimestamp;
	}

	public Date getShippingTimestamp() {
		return shippingTimestamp;
	}

	public void setShippingTimestamp(Date shippingTimestamp) {
		this.shippingTimestamp = shippingTimestamp;
	}

	public String getTrackingData() {
		return trackingData;
	}

	public void setTrackingData(String trackingData) {
		this.trackingData = trackingData;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public YadaMoney getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(YadaMoney totalPrice) {
		this.totalPrice = totalPrice;
	}

	public List<YadaOrderItem> getOrderItems() {
		return orderItems;
	}

	public void setOrderItems(List<YadaOrderItem> orderItems) {
		this.orderItems = orderItems;
	}

}
