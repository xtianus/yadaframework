package net.yadaframework.web.datatables.config;

import org.apache.commons.lang3.StringUtils;

import net.yadaframework.exceptions.YadaInvalidUsageException;

public class YadaDataTableColumn {
	private String headerText;
	private String name; // ID for this column
	
	public YadaDataTableColumn(String headerText, String name, YadaDataTableHTML parent) {
		// super(parent);
		this.headerText = StringUtils.trimToEmpty(headerText);
		if (name!=null) {
			boolean duplicateName = parent.columns.stream().anyMatch(column -> name.equals(column.getName()));
			if (duplicateName) {
				throw new YadaInvalidUsageException("Duplicate column name: '{}'", name);
			}
			parent.options.dtColumns().dtName(name);
			this.name = name;
		}
	}

	public String getHeaderText() {
		return headerText;
	}

	public String getName() {
		return name;
	}
	
}
