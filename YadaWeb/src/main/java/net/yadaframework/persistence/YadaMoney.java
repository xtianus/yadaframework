package net.yadaframework.persistence;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

import com.fasterxml.jackson.annotation.JsonProperty;

import net.yadaframework.components.YadaUtil;

/**
 * An amount of money with a 1/10000 precision, stored as a long both in java and in the database.
 * The currency must be stored somewhere else.
 * Immutable value object: math operations create new instances.
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
public class YadaMoney implements Comparable<YadaMoney> {
	private final long internalValue; // Amount in 1/10000 of the currency
	private static final int multiplier = 10000;

	private YadaUtil yadaUtil = new YadaUtil();

	public YadaMoney() {
		this.internalValue = 0;
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
		double value = yadaUtil.stringToDouble(amount, locale);
		this.internalValue = (long)(value * multiplier);
	}

	/**
	 *
	 * @param integer amount
	 */
	public YadaMoney(long amount) {
		this.internalValue = amount * multiplier;
	}

	/**
	 *
	 * @param doubleValue
	 */
	public YadaMoney(double doubleValue) {
		this.internalValue = (long) (doubleValue * multiplier);
	}

	/**
	 * Creates a YadaMoney by setting the internal value directly
	 * @param internalValue
	 */
	// Package accessibility because it is implementation-dependent.
	// Need to pass a BigDecimal so that it can be different from the public long version.
	YadaMoney(BigDecimal internalValue) {
		this.internalValue = internalValue.longValue();
	}

	/**
	 * Returns true if the current value is equal to the amount specified or higher
	 * @param amount a double with an optional decimal part
	 * @param locale used for the decimal separator
	 * @return
	 * @throws ParseException
	 */
	public boolean isAtLeast(String amount, Locale locale) throws ParseException {
		double value = yadaUtil.stringToDouble(amount, locale);
		return this.internalValue >= value * multiplier;
	}

	/**
	 * Returns true if the current value is equal to the amount specified or higher
	 * @param amount
	 * @return
	 */
	public boolean isAtLeast(int amount) {
		return this.internalValue >= amount * multiplier;
	}

	/**
	 * Returns a new instance with the positive value of the current value
	 * @return a positive value
	 */
	public YadaMoney getAbsolute() {
		YadaMoney result = new YadaMoney(Math.abs(this.internalValue));
		return result;
	}

	/**
	 * Returns a new instance with the positive value of the current value.
	 * Same as getAbsolute()
	 * @return a positive value
	 * @see YadaMoney#getAbsolute()
	 */
	public YadaMoney getPositive() {
		return getAbsolute();
	}

	/**
	 * Returns a new instance with the negative value of the current value.
	 * If the current value was already negative, it stays negative.
	 * @return a negative value
	 */
	public YadaMoney getNegative() {
		YadaMoney result = new YadaMoney(- Math.abs(this.internalValue));
		return result;
	}

	/**
	 * Returns a new instance with the negated (inverted) value of the current value.
	 * It will be positive if the current value is negative, it will be negative if the current value is positive.
	 * @return a positive value when this was negative, a negative value when this was positive
	 */
	public YadaMoney getNegated() {
		YadaMoney result = new YadaMoney(- this.internalValue);
		return result;
	}

	/**
	 * Returns true if the value is zero
	 * @return
	 */
	public boolean isZero() {
		return this.internalValue == 0;
	}

	/**
	 * Returns true if the value is lower than zero
	 * @return
	 */
	public boolean isNegative() {
		return this.internalValue<0;
	}

	/**
	 * Returns true if the value is greater than zero
	 * @return
	 */
	public boolean isPositive() {
		return this.internalValue>0;
	}

	public YadaMoney addCents(long cents) {
		long newValue = this.internalValue + (cents * multiplier / 100);
		YadaMoney result = new YadaMoney(newValue);
		return result;
	}

	public YadaMoney add(YadaMoney toAdd) {
		long newValue = this.internalValue + toAdd.internalValue;
		YadaMoney result = new YadaMoney(newValue);
		return result;
	}

	public YadaMoney subtract(YadaMoney toRemove) {
		long newValue = this.internalValue - toRemove.internalValue;
		YadaMoney result = new YadaMoney(newValue);
		return result;
	}

	public YadaMoney divideBy(double factor) {
		long newValue = (long) (this.internalValue / factor);
		YadaMoney result = new YadaMoney(newValue);
		return result;
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
	 * Convert to a string with no decimal places (truncated)
	 */
	public String toIntString() {
		return Long.toString(internalValue/multiplier);
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
		YadaMoney yadaMoney = new YadaMoney(this.internalValue);
		return yadaMoney;
	}

	Long getInternalValue() {
		return this.internalValue;
	}

	@Override
	public int compareTo(YadaMoney other) {
		return (this.internalValue < other.internalValue) ? -1 : ((this.internalValue == other.internalValue) ? 0 : 1);
	}
}
