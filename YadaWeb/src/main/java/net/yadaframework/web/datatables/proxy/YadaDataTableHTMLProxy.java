package net.yadaframework.web.datatables.proxy;

import java.util.List;

import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.config.YadaDataTableButton;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;

/**
 * This class implements the methods needed for <b>internal use</b> 
 * so that they don't pollute the fluent interface.
 */
public class YadaDataTableHTMLProxy extends YadaDataTableHTML {

	public YadaDataTableHTMLProxy(YadaDataTable parent, YadaDTOptionsProxy options) {
		super(parent, options);
	}
	
	public void prepareConfiguration() {
		// The user might have skipped calling back() but we need it to prepare the configuration options
		if (!backCalled) {
			back();
		}
	}

	public boolean isCommandsTitle() {
		return commandsTitle!=null;
	}
	
	public boolean isSelectCheckbox() {
		return selectCheckboxTitle!=null;
	}
	
	public String getSelectCheckboxTitle() {
		return selectCheckboxTitle;
	}

	public List<YadaDataTableColumnProxy> getColumns() {
		return columns;
	}

	public String getCssClasses() {
		return cssClasses;
	}

	public String getCommandsTitle() {
		return commandsTitle;
	}
	
	public Boolean isShowFooter() {
		return showFooter;
	}

	public List<YadaDataTableButtonProxy> getButtons() {
		return buttons;
	}

//	public Map<Integer, YadaDataTableColumn> getOrderingMap() {
//		return orderingMap;
//	}

//	public YadaDTOptionsProxy getOptions() {
//		return options;
//	}

}
