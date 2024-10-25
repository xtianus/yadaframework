package net.yadaframework.web.datatables.config;

import org.apache.commons.lang3.StringUtils;

import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.datatables.options.YadaDTColumns;

public class YadaDataTableColumn extends YadaFluentBase<YadaDataTableHTML> {
	private String headerText;
	private String name; // ID for this column
	private YadaDTColumns yadaDTColumns;
	
	// Package visibility
	int positionInTable;
	Boolean orderAsc; // false for desc

	public YadaDataTableColumn(String headerText, YadaDataTableHTML parent) {
		super(parent);
		this.headerText = StringUtils.trimToEmpty(headerText);
		yadaDTColumns = parent.options.dtColumnsObj();
		yadaDTColumns.dtOrderable(true);
		yadaDTColumns.dtSearchable(true);
	}
	
	/**
	 * Makes the column not orderable
	 * @return this instance for method chaining
	 */
	public YadaDataTableColumn dtNotOrderable() {
		yadaDTColumns.dtOrderable(false);
		return this;
	}
	
	/**
	 * Makes the column not searchable
	 * @return this instance for method chaining
	 */
	public YadaDataTableColumn dtNotSearchable() {
		yadaDTColumns.dtSearchable(false);
		return this;
	}
	
	/**
	 * Set a name on this column for cross reference.
	 * @param name the name of the column, can be null, must be unique otherwise
	 * @return
	 */
	public YadaDataTableColumn dtName(String name) {
		this.name = name;
		yadaDTColumns.dtName(name);
		return this;
	}

	public String getHeaderText() {
		return headerText;
	}

	// Package visibility
	String getName() {
		return name;
	}

	@Override
	protected void validate() {
		if (this.name!=null) {
			boolean duplicateName = parent.columns.stream().anyMatch(column -> column!=this && name.equals(column.getName()));
			if (duplicateName) {
				throw new YadaInvalidUsageException("Duplicate column name: '{}'", name);
			}
		}
	}

	/**
	 * Choose this column for the initial sorting, ascending
	 * @return this instance for method chaining
	 * @see YadaDataTableColumn#dtOrderAsc(int) for multiple column sorting
	 */
	public YadaDataTableColumn dtOrderAsc() {
		orderAsc = true;
		addOrder(0); 
		return this;
	}
	
	/**
	 * Choose this column for the initial sorting, ascending
	 * @param precedence for multiple column sorting, a lower number means higher precedence
	 * @return this instance for method chaining
	 */
	public YadaDataTableColumn dtOrderAsc(int precedence) {
		orderAsc = true;
		addOrder(precedence); 
		return this;
	}
	
	/**
	 * Choose this column for the initial sorting, descending
	 * @return this instance for method chaining
	 * @see YadaDataTableColumn#dtOrderDesc(int) for multiple column sorting
	 */
	public YadaDataTableColumn dtOrderDesc() {
		orderAsc = false;
		addOrder(0); 
		return this;
	}
	
	/**
	 * Choose this column for the initial sorting, descending
	 * @param precedence for multiple column sorting, a lower number means higher precedence
	 * @return this instance for method chaining
	 */
	public YadaDataTableColumn dtOrderDesc(int precedence) {
		orderAsc = false;
		addOrder(precedence); 
		return this;
	}
	
	private void addOrder(int precedence) {
		if (parent.orderingMap.containsKey(precedence)) {
			throw new YadaInvalidUsageException("Duplicate order precedence '{}' in column '{}'", precedence, headerText);
		}
		parent.orderingMap.put(precedence, this); 
	}

	// Package visibility
	void setPositionInTable(int position) {
		this.positionInTable = position;
	}
}
