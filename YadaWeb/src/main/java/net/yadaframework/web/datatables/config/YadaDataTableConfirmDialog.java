package net.yadaframework.web.datatables.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDataTableConfirmDialog extends YadaFluentBase<YadaDataTableButton> {
    @JsonProperty private String confirmTitle; // Title for the confirmation dialog
    @JsonProperty private String confirmOneMessage; // Message for confirming a single action
    @JsonProperty private String confirmManyMessage; // Message for confirming multiple actions
    @JsonProperty private String confirmButtonText; // Text for the confirmation button
    @JsonProperty private String abortButtonText; // Text for the cancel button
    @JsonProperty private List<String> columnNames; // Column names that refer to the columns that hold the value for the placeholders in all the texts
    private YadaDataTableHTML yadaDataTableHTML;
	
	public YadaDataTableConfirmDialog(YadaDataTableButton parent, YadaDataTableHTML yadaDataTableHTML) {
		super(parent);
		this.yadaDataTableHTML = yadaDataTableHTML;
	}
	
	/**
	 * Set the columns that hold the value for the placeholders
	 * @param columnName array of column names that refer to columns that hold the value for the placeholders in all the texts. Example: "id", "name"
	 * @return this instance for method chaining
     * @see YadaDataTableColumn#dtName(String)
	 */
	public YadaDataTableConfirmDialog dtPlaceholderColumnName(String ... columnName) {
		if (columnName.length > 0) {
			this.columnNames = new ArrayList<>(Arrays.asList(columnName));
			List<String> missingColumns = this.columnNames.stream().filter(
				name -> yadaDataTableHTML.columns.stream().noneMatch(column -> name.equals(column.getName())))
			    .collect(Collectors.toList());
			if (!missingColumns.isEmpty()) {
			    throw new YadaInvalidUsageException("Unknown column names in ConfirmDialog: {}", missingColumns);
			}
		}
		return this;
	}
	
	/**
	 * Set the title for the confirmation dialog
	 * @param confirmTitle the title, can be null or have many placeholders like {0}, {1}, {2} etc.
	 * @return this instance for method chaining
	 */
	public YadaDataTableConfirmDialog dtTitle(String confirmTitle) {
		this.confirmTitle = StringUtils.trimToEmpty(confirmTitle);
		return this;
	}
	
	/**
	 * Set the message for the confirmation dialog when one element is selected
	 * @param confirmOneMessage the message, can be null or have many placeholders like {0}, {1}, {2} etc.
	 * @return this instance for method chaining
	 */
	public YadaDataTableConfirmDialog dtMessageSingular(String confirmOneMessage) {
		this.confirmOneMessage = StringUtils.trimToEmpty(confirmOneMessage);
		return this;
	}
	
	/**
	 * Set the message for the confirmation dialog when multiple elements are selected.
	 * @param confirmManyMessage the message, can be null or have many placeholders like {0}, {1}, {2} etc.
	 * 		The {0} placeholder will be replaced with the number of selected rows while the others are shifted one place
	 * @return this instance for method chaining
	 */
	public YadaDataTableConfirmDialog dtMessagePlural(String confirmManyMessage) {
		this.confirmManyMessage = StringUtils.trimToEmpty(confirmManyMessage);
		return this;
	}
	
	/**
	 * Set the text for the confirmation button
	 * @param confirmButtonText the text, can be null or have many placeholders like {0}, {1}, {2} etc.
	 * @return this instance for method chaining
	 */
	public YadaDataTableConfirmDialog dtConfirmButton(String confirmButtonText) {
		this.confirmButtonText = StringUtils.trimToEmpty(confirmButtonText);
		return this;
	}
	
	/**
	 * Set the text for the cancel button
	 * @param abortButtonText the text, can be null or have many placeholders like {0}, {1}, {2} etc.
	 * @return this instance for method chaining
	 */
	public YadaDataTableConfirmDialog dtAbortButton(String abortButtonText) {
		this.abortButtonText = StringUtils.trimToEmpty(abortButtonText);
		return this;
	}

}
