package net.yadaframework.persistence;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import net.yadaframework.web.YadaJsonView;

/**
 * An amount of money with a 1/10000 precision, stored as a long both in java and in the database.
 * "The rule of thumb for storage of fixed point decimal values is to store at least one more decimal 
 * place than you actually require to allow for rounding."
 * https://stackoverflow.com/q/224462/587641
 * Some world currencies use 3 decimals: http://www.thefinancials.com/Default.aspx?SubSectionID=curformat
 */
public class YadaMoney {
	private long amount = 0; // Amount in 1/10000 of the default currency
	private static final int multiplier = 10000;
	
	public YadaMoney() {
	}
	
	/**
	 * Set the given amount expressed as an integer multiple of the base unit. For example
	 * dollars or euro.
	 * @param integralCurrency an amount of money with no fractional part, like $9
	 */
	public void setCurrency(long integralCurrency) {
		this.amount = integralCurrency * multiplier;
	}
	
	/**
	 * Gets the amount with no decimal part. So 9.23 becomes 9, expressed as a multiple of the base unit, not as a fraction.
	 * @return
	 */
	public long getCurrency() {
		return amount / multiplier;
	}
	
	/**
	 * 
	 * @param amount value expressed in 1/10000 of the base unit
	 */
	public YadaMoney(Long amount) {
		if (amount!=null) {
			this.amount = amount.longValue();
		}	
	}

	public YadaMoney add(YadaMoney toAdd) {
		this.amount += toAdd.amount;
		return this;
	}
	
	public YadaMoney subtract(YadaMoney toAdd) {
		this.amount -= toAdd.amount;
		return this;
	}
	
	/**
	 * Returns the value with N decimal places
	 * @param decimals the number of decimal places
	 * @return
	 */
	public double toCurrency(int decimals) {
		double toKeep = Math.pow(10, decimals); // 100 for 2 decimals
		long rounded = Math.round(amount*toKeep/multiplier); // Round to the nearest. TODO see https://en.wikipedia.org/wiki/Rounding#Round_half_away_from_zero
		return rounded/toKeep;
	}
	
	/**
	 * Returns the value with 2 decimal places
	 * @return
	 */
	public double toCurrency() {
		return toCurrency(2);
	}
	
	/**
	 * Convert to a string with 2 decimal places
	 */
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@JsonProperty("value")
	public String toString() {
		// Decimal formats are generally not synchronized
		NumberFormat formatter = new DecimalFormat("#0.##");
		return formatter.format(toCurrency());
	}
	
	/**
	 * Convert to a string with 2 decimal places
	 */
	public String toString(Locale locale) {
		// Decimal formats are generally not synchronized
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getNumberInstance(locale);
		formatter.applyPattern("#0.##");
		return formatter.format(toCurrency());
	}

	/**
	 * The amount in 1/10000th
	 * @return
	 */
	public long getAmount() {
		return amount;
	}

	/**
	 * 
	 * @param amount the amount in 1/10000th
	 */
	public void setAmount(long amount) {
		this.amount = amount;
	}

	@Override
	public int hashCode() {
		return new Long(amount).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof YadaMoney && ((YadaMoney)obj).amount == amount;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new YadaMoney(amount);
	}
}
