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
	protected YadaUserProfile payer;

	@ManyToOne(optional = true)
	protected YadaUserProfile payee;

	// Could be cents or thousandth depending on the application
	@Convert(converter = YadaMoneyConverter.class)
	@Column(nullable = false)
	protected YadaMoney amount;

	@Column(length = 8)
	protected String currencyCode;

	protected Date timestamp;

	@Column(length = 32)
	protected String status;
	protected String transactionId;
	protected String description;

	@Column(length = 8192)
	protected String data; // Any application-specific data

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
	public YadaUserProfile getPayer() {
		return payer;
	}
	public void setPayer(YadaUserProfile payer) {
		this.payer = payer;
	}
	public YadaUserProfile getPayee() {
		return payee;
	}
	public void setPayee(YadaUserProfile payee) {
		this.payee = payee;
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




}
