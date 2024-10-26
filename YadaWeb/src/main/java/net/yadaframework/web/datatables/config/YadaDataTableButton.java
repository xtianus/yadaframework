package net.yadaframework.web.datatables.config;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class YadaDataTableButton extends YadaFluentBase<YadaDataTableHTML> {
	@JsonIgnore private YadaConfiguration config = (YadaConfiguration) YadaUtil.getBean("config");
	@JsonIgnore private YadaWebUtil yadaWebUtil = YadaWebUtil.INSTANCE;
	
	private String url; // URL to be called when the button is clicked, it can be a thymeleaf expression and will be inserted in a @{} when missing
    private String handler; // javascript function to be called when the button is clicked, will receive the row data as a parameter (see details)
    private String text; // Tooltip text for the command icon and text for the toolbar button
    private String icon; // HTML content for the button's icon
    private boolean global = false; // True for a button that is always enabled regardless of row selection, for example an Add button. Global buttons are not shown in the command column.
    private String toolbarCssClass = "btn btn-default"; // CSS class to be applied to the button
    private String idName = "id"; // Name of the ID request parameter (optional, default is "id")
    private boolean ajax = true; // Boolean to indicate if the button should use an AJAX request
    private boolean pageLoader = true; // Boolean to control whether the page loader should be shown or not
    private YadaDataTableConfirmDialog yadaDataTableConfirmDialog;
    private String windowTarget; // Name of the window for opening the URL in a new window.
    private String windowFeatures; // Features of the window when opened
    private String showCommand; // Function that determines whether to show the button icon for each row
    private Set<Integer> roles = new HashSet<>(); // Roles that can access the button
    
    public YadaDataTableButton(String text, YadaDataTableHTML parent) {
		super(parent);
		this.text = text;
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
    	this.yadaDataTableConfirmDialog = new YadaDataTableConfirmDialog(this, parent);
        return yadaDataTableConfirmDialog;
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
     * javascript function to be called when the button is clicked.
     * When the row icon is clicked, it will receive the row data as a parameter. 
     * When the toolbar button is clicked, it will receive an array of selected rows as a parameter.
     * @param url
     * @see <a href="https://datatables.net/reference/api/row().data()">DataTables row().data() API</a> 
     * @throws YadaInvalidUsageException when both url and function are set
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtHandler(String handler) {
    	if (this.url!=null) {
	    	throw new YadaInvalidUsageException("Cannot set url and function at the same time");
    	}
		this.handler = handler;
		return this;
	}
    
    /**
     * URL to be called when the button is clicked, it can be a thymeleaf expression and will be inserted in a @{} when missing
     * @param url
     * @throws YadaInvalidUsageException when both url and function are set
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtUrl(String url) {
    	if (this.handler!=null) {
    		throw new YadaInvalidUsageException("Cannot set url and function at the same time");
    	}
    	this.url = yadaWebUtil.ensureThymeleafUrl(url);
    	return this;
    }
    
    /**
     * Set for a button that is always enabled regardless of row selection, for example an Add button.
     * Global buttons are not shown in the command column.
     * @return this instance for method chaining
     * @throws YadaInvalidUsageException when both showCommand and global are set
     */
    public YadaDataTableButton dtGlobal() {
    	if (showCommand!=null) {
	    	throw new YadaInvalidUsageException("Cannot set showCommand and global at the same time");
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
    public YadaDataTableButton dtNoPageLoader() {
        this.pageLoader = false;
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
     * It receives the row data as a parameter. 
     * @see <a href="https://datatables.net/reference/api/row().data()">DataTables row().data() API</a> 
     * @throws YadaInvalidUsageException when both showCommand and global are set
     * @param showCommand
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtShowCommand(String showCommand) {
    	if (global) {
	    	throw new YadaInvalidUsageException("Cannot set showCommand and global at the same time");
    	}
        this.showCommand = showCommand;
        return this;
    }
    
    /**
     * Role that can access the button. Will be removed if the user does not have this role. 
     * Can be called multiple times.
     * @param roleKey the key of the role as configured in the application xml
     * @return this instance for method chaining
     */
    public YadaDataTableButton dtRole(String roleKey) {
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

	public boolean isGlobal() {
		return global;
	}

	public String getUrl() {
		return url;
	}

	public String getHandler() {
		return handler;
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

	public boolean isPageLoader() {
		return pageLoader;
	}

	public String getWindowTarget() {
		return windowTarget;
	}

	public String getWindowFeatures() {
		return windowFeatures;
	}

	public String getShowCommand() {
		return showCommand;
	}

	public Set<Integer> getRoles() {
		return roles;
	}

	public YadaDataTableConfirmDialog getConfirmDialog() {
		return yadaDataTableConfirmDialog;
	}
}
