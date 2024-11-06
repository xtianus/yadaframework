package net.yadaframework.web.datatables.proxy;

import net.yadaframework.web.datatables.config.YadaDataTableColumn;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;

/**
 * This class implements the methods needed for <b>internal use</b> 
 * so that they don't pollute the fluent interface.
 */
public class YadaDataTableColumnProxy extends YadaDataTableColumn {

	public YadaDataTableColumnProxy(String headerText, String data, YadaDataTableHTML parent) {
		super(headerText, data, parent);
	}
	
	public String getName() {
		return super.getName();
	}
	
	public String getHeaderText() {
		return headerText;
	}
	
	public void setPositionInTable(int position) {
		this.positionInTable = position;
	}
	

}
