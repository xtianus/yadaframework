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
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.YadaMoneyConverter;
import net.yadaframework.persistence.entity.YadaPersistentEnum;
import net.yadaframework.security.persistence.entity.YadaUserProfile;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaOrder implements Serializable {
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

	@ManyToOne
	protected YadaUserProfile owner;

	@OneToOne(fetch = FetchType.EAGER)
	protected YadaPersistentEnum<YadaOrderStatus> orderStatus;

	@Temporal(TemporalType.TIMESTAMP)
	protected Date creationTimestamp = new Date();

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

	@Column(length=4)
	protected String currency; // "EUR"

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

	public long getVersion() {
		return version;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

}
