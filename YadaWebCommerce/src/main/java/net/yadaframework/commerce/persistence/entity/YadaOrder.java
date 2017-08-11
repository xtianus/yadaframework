package net.yadaframework.commerce.persistence.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import net.yadaframework.persistence.entity.YadaPersistentEnum;

@Entity
public class YadaOrder implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@OneToOne(fetch = FetchType.EAGER)
	protected YadaPersistentEnum<YadaOrderStatus> orderStatus;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date creationTimestamp;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date stateChangeTimestamp;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date shippingTimestamp;
	
	@Column(length=512)
	private String trackingData;
	
	@Column(length=2048)
	private String notes;
	
	@Convert(converter = YadaMoneyConverter.class)
	private YadaMoney totalAmount;

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

	public YadaMoney getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(YadaMoney totalAmount) {
		this.totalAmount = totalAmount;
	}
	
	
	
	
	


}
