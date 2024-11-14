package net.yadaframework.web.datatables.config;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.YadaJsonRawStringSerializer;
import net.yadaframework.web.datatables.proxy.YadaDataTableConfirmDialogProxy;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDataTableButton extends YadaFluentBase<YadaDataTableHTML> {
	@JsonIgnore private YadaConfiguration config = (YadaConfiguration) YadaUtil.getBean("config");
	@JsonIgnore private YadaWebUtil yadaWebUtil = YadaWebUtil.INSTANCE;
	@JsonIgnore private YadaUtil yadaUtil = YadaUtil.INSTANCE;
	
	@JsonProperty protected String type; // unique identifier for the button in the table row
	
	protected String url; // URL to be called when the button is clicked, it can be a thymeleaf expression and will be inserted in a @{} when missing
	protected String urlProvider; // javascript function that can return a computed url
	protected String text; // Tooltip text for the command icon and text for the toolbar button
	protected String icon; // HTML content for the button's icon
	protected boolean global = false; // True for a button that is always enabled regardless of row selection, for example an Add button. Global buttons are not shown in the command column.
	protected boolean multiRow = false; // True for a toolbar button that is enabled also when multiple rows are selected
	protected String toolbarCssClass = "btn btn-default"; // CSS class to be applied to the button
	protected String idName = "id"; // Name of the ID request parameter (optional, default is "id")
	protected boolean ajax = true; // Boolean to indicate if the button should use an AJAX request
	protected Boolean hidePageLoader; // Boolean to control whether the page loader should be shown or not
	protected String elementLoader; // CSS selector for the element that will be hidden by the loader
	protected YadaDataTableConfirmDialog yadaDataTableConfirmDialog;
	protected String windowTarget; // Name of the window for opening the URL in a new window.
	protected String windowFeatures; // Features of the window when opened
	@JsonSerialize(using = YadaJsonRawStringSerializer.class)
	protected String showCommandIcon; // Function that determines whether to show the button icon for each row
	protected Set<Integer> roles; // Roles that can access the button
    
	protected YadaDataTableButton(String text, YadaDataTableHTML parent) {
		super(parent);
		this.text = text;
		this.type = "yada" + yadaUtil.getRandomString(8);
	}

    /**
     * Enable javascript-side confirmation dialog for button action. 
     * NOte: a backend call will always be able to trigger a confirmation dialog regardless of this setting.
     * @return this instance for method chaining
     */
    public YadaDataTableConfirmDialog dtConfirmDialogObj() {
    	if (yadaDataTableConfirmDialog != null) {
			throw new YadaInvalidUsageException("dtConfirmDialog can only be called once per button");
		}
    	this.yadaDataTableConfirmDialog = new YadaDataTableConfirmDialogProxy(this, parent);
        return yadaDataTableConfirmDialog;
    }
    
    /**
     * Enable the toolbar button when one or many rows are selected.
     * The default is for the toolbar button to be enabled only when one row is selected.
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtMultiRow() {
    	this.multiRow = true;
		return this;
    }
    
	/**
	 * CSS class to be applied to the button in the toolbar, for example 'btn-primary'.
	 * The 'btn' class is added automatically.
	 * @param cssClass
	 * @return this instance for method chaining
	 */
	public YadaDataTableButton dtToolbarCssClass(String cssClass) {
		this.toolbarCssClass = cssClass;
		return this;
	}
    
    /**
     * HTML content for the button's icon. The icon is used in the command column when enabled, and on the left of the toolbar button text.
     * @param iconHTML
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtIcon(String iconHTML) {
		this.icon = iconHTML;
		return this;
	}
    
    /**
     * javascript function to be called when the button is clicked in order to compute the target URL for the button action.
     * 
     * When the row icon is clicked, it will receive the row data as a parameter. 
     * When the toolbar button is clicked, it will receive the datatable api object and the array of selected ids as parameters.
     * 
     * The function should return the target URL or null to prevent any further action.
     * @param function the name of the js function that must be visible in page
     * @see <a href="https://datatables.net/reference/api/row().data()">DataTables row().data() API</a> 
     * @throws YadaInvalidUsageException when both url and urlProvider are set
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtUrlProvider(String function) {
    	if (this.url!=null) {
	    	throw new YadaInvalidUsageException("Cannot set url and urlProvider at the same time");
    	}
		this.urlProvider = function;
		return this;
	}
    
    /**
     * URL to be called when the button is clicked, it can be a thymeleaf expression and will be inserted in a @{} when missing
     * @param url
     * @throws YadaInvalidUsageException when both url and urlProvider are set
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtUrl(String url) {
    	if (this.urlProvider!=null) {
    		throw new YadaInvalidUsageException("Cannot set url and urlProvider at the same time");
    	}
    	this.url = yadaWebUtil.ensureThymeleafUrl(url);
    	return this;
    }
    
    /**
     * Set for a button that is always enabled regardless of row selection, for example an Add button.
     * Global buttons are not shown in the command column.
     * @return this instance for method chaining
     * @throws YadaInvalidUsageException when both showCommandIcon and global are set
     */
    public YadaDataTableButton dtGlobal() {
    	if (showCommandIcon!=null) {
	    	throw new YadaInvalidUsageException("Cannot set showCommandIcon and global at the same time");
    	}
        this.global = true;
        return this;
    }
    
    /**
     * Name of the ID request parameter, default is "id". Useless for global buttons.
     * @param idName
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtIdName(String idName) {
    	if (StringUtils.isBlank(idName)) {
    		throw new YadaInvalidUsageException("idName cannot be blank");
    	}
        this.idName = idName;
        return this;
    }
    
    /**
     * Indicate that the button should use a normal request
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtNoAjax() {
        this.ajax = false;
        return this;
    }
    
    /**
     * Do not show the page loader when the button is clicked
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtHidePageLoader() {
    	if (elementLoader!=null) {
	    	throw new YadaInvalidUsageException("Cannot set hidePageLoader and elementLoader at the same time");
    	}
        this.hidePageLoader = true;
        return this;
    }
    
    /**
     * Show the loader on the selected element
     * @param cssSelector a CSS selector for the element that will be hidden by the loader
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtElementLoader(String cssSelector) {
    	if (hidePageLoader!=null) {
	    	throw new YadaInvalidUsageException("Cannot set hidePageLoader and elementLoader at the same time");
    	}
    	this.elementLoader = cssSelector;
    	return this;
    }
    
    /**
     * Name of the window for opening the URL in a new window.
     * Special values are "_blank", "_self", "_parent", "_top"
     * @param windowTarget
     * @return this instance for method chaining
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Window/open">Window: open() method</a>
    */
    public YadaDataTableButton dtWindowTarget(String windowTarget) {
        this.windowTarget = windowTarget;
        return this;
    }
    
    /**
     * Features of the window when opened, such as its size, scrollbars, and whether it is resizable. 
     * @param windowFeatures
     * @return this instance for method chaining
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Window/open">Window: open() method</a>
     */
    public YadaDataTableButton dtWindowFeatures(String windowFeatures) {
        this.windowFeatures = windowFeatures;
        return this;
    }
    
    /**
     * Javascript function that determines whether to show the button icon for each row.
     * It receives the "data", "row" and "meta" parameters as for the render functions, and also dataTableJson and currentUserRoles. 
     * It must return true/false to show/hide the icon or "disabled" to show it disabled.
     * These buttons are not shown in the toolbar.
     * @see <a href="https://datatables.net/reference/api/row().data()">DataTables row().data() API</a> 
     * @throws YadaInvalidUsageException when both showCommandIcon and global are set
     * @param function some javascript function that returns true or false or "disabled".
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtShowCommandIcon(String function) {
    	if (global) {
	    	throw new YadaInvalidUsageException("Cannot set showCommandIcon and global at the same time");
    	}
        this.showCommandIcon = function;
        return this;
    }
    
    /**
     * Role that can access the button. Will be removed if the user does not have this role. 
     * Can be called multiple times.
     * @param roleKey the key of the role as configured in the application xml
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtRole(String roleKey) {
    	if (this.roles==null) {
    		this.roles = new HashSet<>();
    	}
    	Integer roleId = config.getRoleId(roleKey); // Already throws exception if not found
        this.roles.add(roleId);
        return this;
    }

	@Override
	public YadaDataTableHTML back() {
		// When the button is not global, there must be an icon
		if (!global && StringUtils.isBlank(icon)) {
			throw new YadaInvalidUsageException("Button '{}' needs an icon to be shown in the command column", text);
		}
		if (windowFeatures != null) {
			if (windowTarget == null) {
				throw new YadaInvalidUsageException("windowName must be set if windowFeatures is set");
			}
		}
		return super.back();
	}

}
