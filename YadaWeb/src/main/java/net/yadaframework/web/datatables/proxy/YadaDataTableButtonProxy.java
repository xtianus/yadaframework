package net.yadaframework.web.datatables.proxy;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import net.yadaframework.web.datatables.config.YadaDataTableButton;
import net.yadaframework.web.datatables.config.YadaDataTableConfirmDialog;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;

/**
 * This class implements the methods needed for <b>internal use</b> 
 * so that they don't pollute the fluent interface.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDataTableButtonProxy extends YadaDataTableButton {

	public YadaDataTableButtonProxy(String text, YadaDataTableHTML parent) {
		super(text, parent);
	}
	
	public String getElementLoader() {
		return elementLoader;
	}
	
	public String getType() {
		return type;
	}
	
	public boolean isGlobal() {
		return global;
	}

	public String getUrl() {
		return url;
	}

	public String getUrlProvider() {
		return urlProvider;
	}

	public String getText() {
		return text;
	}

	public String getIcon() {
		return icon;
	}

	public String getToolbarCssClass() {
		return toolbarCssClass;
	}

	public String getIdName() {
		return idName;
	}

	public boolean isAjax() {
		return ajax;
	}

	public Boolean isHidePageLoader() {
		return hidePageLoader;
	}

	public String getWindowTarget() {
		return windowTarget;
	}

	public String getWindowFeatures() {
		return windowFeatures;
	}

	public String getShowCommand() {
		return showCommandIcon;
	}

	public Set<Integer> getRoles() {
		return roles;
	}

	public YadaDataTableConfirmDialog getConfirmDialog() {
		return yadaDataTableConfirmDialog;
	}

}

