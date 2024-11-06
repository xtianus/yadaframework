
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents a button configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/buttons">Button</a>
 */
public class YadaDTButton extends YadaFluentBase<YadaDTOptions> {
    private String text;
    private String action;

    YadaDTButton(YadaDTOptions parent) {
        super(parent);
    }

    /**
     * Set the text for the button.
     * 
     * @param text the button text
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/buttons.text">text</a>
     */
    public YadaDTButton dtText(String text) {
        this.text = text;
        return this;
    }

    /**
     * Get the text for the button.
     * 
     * @return the button text
     */
    public String getText() {
        return text;
    }

    /**
     * Set the action for the button.
     * 
     * @param action the action performed by the button
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/buttons.action">action</a>
     */
    public YadaDTButton dtAction(String action) {
        this.action = action;
        return this;
    }

    /**
     * Get the action for the button.
     * 
     * @return the action performed by the button
     */
    public String getAction() {
        return action;
    }

    /**
     * Get the parent DataTable options.
     * 
     * @return the parent DataTable options
     */
    public YadaDTOptions parent() {
        return super.parent;
    }
}
