package net.yadaframework.commerce.persistence.entity;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.YadaMoneyConverter;
import net.yadaframework.security.persistence.entity.YadaUserProfile;

/**
 * Any monetary transaction e.g. paypal payment
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaTransaction {

	// For synchronization with external databases
	@Column(insertable = false, updatable = false, columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	protected Date modified;

	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;

	@ManyToOne(optional = true)
	protected YadaUserProfile accountOwner; // Sends or receives money

	@ManyToOne(optional = true)
	protected YadaUserProfile otherParty; 	// Receives or sends money

	// Could be cents or thousandth depending on the application
	@Convert(converter = YadaMoneyConverter.class)
	@Column(nullable = false)
	protected YadaMoney amount; // The account movement: negative when sending money, positive when receiving

	@Column(length = 8)
	protected String currencyCode;

	@Column(columnDefinition="TIMESTAMP NULL")
	protected Date timestamp;

	@Column(length = 32)
	protected String status;
	protected String transactionId;
	protected String payerId1; // ID on the payment system, e.g. paypal "payer_id"
	protected String payerId2; // Another ID on the payment system, e.g. paypal "email_address"
	protected String description;
	@Column(length = 32)
	protected String paymentSystem; // i.e. "paypal" or "balance"

	@Column(length = 8192)
	protected String data; // Any application-specific data

	@OneToOne
	protected YadaOrder order;

	// True when this is the twin transaction in a double-ledger system
	protected Boolean inverse = false;

	protected Boolean suspended = false; // true when the transaction has not been performed yet

	protected Boolean external = false; // true when the transaction is on external systems, like a bank transfer

	///////////////////////////////////

	/**
	 * Returns the timestamp formatted as a relative time from now, in the account owner's timezone
	 * @param locale
	 * @return a relative time like "1 minute ago"
	 */
	public String getTimestampAsRelative(Locale locale) {
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(timestamp.toInstant(), accountOwner.getTimezone().toZoneId());
		return YadaUtil.INSTANCE.getTimestampAsRelative(zonedDateTime, locale, null);
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
	public void setVersion(long version) {
		this.version = version;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public YadaUserProfile getAccountOwner() {
		return accountOwner;
	}
	public void setAccountOwner(YadaUserProfile payer) {
		this.accountOwner = payer;
	}
	public YadaUserProfile getOtherParty() {
		return otherParty;
	}
	public void setOtherParty(YadaUserProfile payee) {
		this.otherParty = payee;
	}
	public String getCurrencyCode() {
		return currencyCode;
	}
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	public YadaMoney getAmount() {
		return amount;
	}
	public void setAmount(YadaMoney amount) {
		this.amount = amount;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public YadaOrder getOrder() {
		return order;
	}
	public void setOrder(YadaOrder order) {
		this.order = order;
	}
	public String getPayerId1() {
		return payerId1;
	}
	public void setPayerId1(String payerId1) {
		this.payerId1 = payerId1;
	}
	public String getPayerId2() {
		return payerId2;
	}
	public void setPayerId2(String payerId2) {
		this.payerId2 = payerId2;
	}
	public Boolean getInverse() {
		return inverse;
	}
	public void setInverse(Boolean inverse) {
		this.inverse = inverse;
	}
	public String getPaymentSystem() {
		return paymentSystem;
	}
	public void setPaymentSystem(String paymentSystem) {
		this.paymentSystem = paymentSystem;
	}
	public Boolean getSuspended() {
		return suspended;
	}
	public void setSuspended(Boolean suspended) {
		this.suspended = suspended;
	}

	public Boolean getExternal() {
		return external;
	}

	public void setExternal(Boolean external) {
		this.external = external;
	}




}
