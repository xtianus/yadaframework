package net.yadaframework.commerce.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;

import net.yadaframework.commerce.exceptions.YadaCurrencyMismatchException;
import net.yadaframework.commerce.persistence.repository.YadaTransactionDao;
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

	@Column(columnDefinition="TIMESTAMP NULL")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date creationTimestamp = new Date();

	@Column(columnDefinition="TIMESTAMP NULL")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date stateChangeTimestamp;

	@Column(columnDefinition="TIMESTAMP NULL")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date shippingTimestamp;

	@Column(length=512)
	protected String trackingData;

	@Column(length=2048)
	protected String notes;

	/**
	 * The cost of this order. May not have been paid.
	 * @see #getTotalPayment(YadaTransactionDao)
	 */
	@Convert(converter = YadaMoneyConverter.class)
	protected YadaMoney totalPrice;

	@Column(length=4)
	protected String currency; // "EUR"

	@OneToMany(mappedBy="order", cascade=CascadeType.ALL, orphanRemoval=true)
	protected List<YadaOrderItem> orderItems;

	///////////////////////////////////////////

	/**
	 * Returns the sum of all transactions relative to this order created by the owner.
	 * The result can be positive, negative or zero (no transaction or payment+refund), but never null.
	 * @param yadaTransactionDao
	 * @return a positive value if all transactions sum to a negative value (payment has been made)
	 */
	public YadaMoney getTotalPayment(YadaTransactionDao yadaTransactionDao) {
		YadaMoney total = new YadaMoney(0);
		String currencyCode = null;
		List<YadaTransaction> yadaTransactions = yadaTransactionDao.find(this);
		for (YadaTransaction yadaTransaction : yadaTransactions) {
			String newCurrency = yadaTransaction.getCurrencyCode();
			if (currencyCode!=null && !currencyCode.equals(newCurrency)) {
				throw new YadaCurrencyMismatchException("Currency {} differs from {}", currencyCode, newCurrency);
			}
			currencyCode = newCurrency;
			total = total.getSum(yadaTransaction.getAmount());
		}
		return total.getNegated();
	}

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
