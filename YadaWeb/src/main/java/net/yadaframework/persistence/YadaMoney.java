package net.yadaframework.persistence;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An amount of money with a 1/10000 precision, stored as a long both in java and in the database.
 * The currency must be stored somewhere else.
 * "The rule of thumb for storage of fixed point decimal values is to store at least one more decimal
 * place than you actually require to allow for rounding."
 * https://stackoverflow.com/q/224462/587641
 * Some world currencies use 3 decimals: http://www.thefinancials.com/Default.aspx?SubSectionID=curformat
 *
 * This class can be stored in the database as a long when converted with YadaMoneyConverter:
 * <pre>
 * @Convert(converter = YadaMoneyConverter.class)
 * private YadaMoney balance;
 * </pre>
 */
public class YadaMoney {
	private long internalValue = 0; // Amount in 1/10000 of the currency
	private static final int multiplier = 10000;

	public YadaMoney() {
	}

	/**
	 *
	 * @param amount an amount of money with no decimals
	 */
	public YadaMoney(int amount) {
		this.internalValue = amount * multiplier;
	}

	/**
	 * Convert a string with a decimal value that uses the comma separator of the specified locale
	 * @param amount the decimal value like "12,87"
	 * @param locale the locale to parse the comma separator, like Locale.ITALY
	 * @throws ParseException if the amount contains invalid characters
	 */
	public YadaMoney(String amount, Locale locale) throws ParseException {
		// From https://stackoverflow.com/a/16879667/587641
		NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
		ParsePosition parsePosition = new ParsePosition(0);
		Number number = numberFormat.parse(amount, parsePosition);
		if (parsePosition.getIndex() != amount.length()){
			throw new ParseException("Invalid double input: '" + amount + "'", parsePosition.getIndex());
		}
		this.internalValue = (long)(number.doubleValue() * multiplier);
	}

	public YadaMoney(long amount) {
		this.internalValue = amount * multiplier;
	}

	public YadaMoney addCents(long cents) {
		this.internalValue += (cents * multiplier / 100);
		return this;
	}

	public YadaMoney add(YadaMoney toAdd) {
		this.internalValue += toAdd.internalValue;
		return this;
	}

	public YadaMoney subtract(YadaMoney toRemove) {
		this.internalValue -= toRemove.internalValue;
		return this;
	}

	/**
	 * Returns the value with N decimal places
	 * @param decimals the number of decimal places
	 * @return
	 */
	public double getRoundValue(int decimals) {
		double toKeep = Math.pow(10, decimals); // 100 for 2 decimals
		long rounded = Math.round(internalValue*toKeep/multiplier); // Round to the nearest. TODO see https://en.wikipedia.org/wiki/Rounding#Round_half_away_from_zero
		return rounded/toKeep;
	}

	/**
	 * Returns the value with 2 decimal places
	 * @return
	 */
	public double getRoundValue() {
		return getRoundValue(2);
	}

	/**
	 * Convert to a string with 2 decimal places
	 */
	@Override
	@JsonProperty("value")
	public String toString() {
		// Decimal formats are generally not synchronized
		Locale locale = LocaleContextHolder.getLocale();
		NumberFormat formatter = NumberFormat.getNumberInstance(locale);
		if (formatter instanceof DecimalFormat) {
			((DecimalFormat) formatter).applyPattern("#0.00");
		}
		return formatter.format(getRoundValue());
	}

	/**
	 * Convert to a string with 2 decimal places
	 */
	public String toString(Locale locale) {
		// Decimal formats are generally not synchronized
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getNumberInstance(locale);
		formatter.applyPattern("#0.##");
		return formatter.format(getRoundValue());
	}

	@Override
	public int hashCode() {
		return new Long(internalValue).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof YadaMoney && ((YadaMoney)obj).internalValue == internalValue;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		YadaMoney yadaMoney = new YadaMoney();
		yadaMoney.internalValue = this.internalValue;
		return yadaMoney;
	}

	Long getInternalValue() {
		return this.internalValue;
	}

	void setInternalValue(long internalValue) {
		this.internalValue = internalValue;
	}
}
