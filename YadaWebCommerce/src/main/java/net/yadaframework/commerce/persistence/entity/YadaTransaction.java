package net.yadaframework.commerce.persistence.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

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
	@Temporal(TemporalType.TIMESTAMP)
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

	protected Date timestamp;

	@Column(length = 32)
	protected String status;
	protected String transactionId;
	protected String payerId1; // ID on the payment system, e.g. paypal "payer_id"
	protected String payerId2; // Another ID on the payment system, e.g. paypal "email_address"
	protected String description;

	@Column(length = 8192)
	protected String data; // Any application-specific data

	@OneToOne
	protected YadaOrder order;

	// True when this is the twin transaction in a double-ledger system
	protected Boolean inverse;

	///////////////////////////////////

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




}