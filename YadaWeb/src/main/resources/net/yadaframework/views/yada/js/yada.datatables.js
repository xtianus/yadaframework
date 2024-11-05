//////////////////
/// DataTables ///
//////////////////

(function( yada ) {
	"use strict";
	// Namespace trick explained here: http://stackoverflow.com/a/5947280/587641
	// For a public property or function, use "yada.xxx = ..."
	// For a private property use "var xxx = "
	// For a private function use "function xxx(..."
		
	const CLASS_COMMANDBUTTON = "yadaRowCommandButton";
	
	/**
	 * Formats a string replacing {0} placeholders with the value in the cell named after 
	 * the value found in columnNames at the corresponding position.
	 * @param message the string with placeholders, e.g. "Product {0} color {1}"
	 * @param columnNames an array with the names of the columns that hold the value to replace, e.g. ["product.name", "product.color"]
	 * @param row the current row 
	 * @param firstValue when specified, this is the value for {0} while all other placeholders take values from the columns
	 */
	function formatMessage(message, columnNames, row, firstValue) {
		if (message==null || message=="" || (columnNames==null && firstValue==null)) {
			return message;
		}
		if (firstValue) {
			message = message.replace("{0}", firstValue);
		}
		let offset = firstValue?1:0;
	    columnNames.forEach((colName, index) => {
	        const placeholder = `{${index+offset}}`;
	        const value = yada.getValue(row, colName) || "";
	        message = message.replace(placeholder, value);
	    });
	    return message;
	}


	/**
	 * Initialize the datatable (internal use)
	 */
	yada.dataTable = function(dataTableJson, ajaxUrl, languageUrl, commandColumnName, preprocessorName, postprocessorName) {
		const dataTableId = dataTableJson.id;
		const dataTableHtml = dataTableJson.html;
		const dataTableOptions = dataTableJson.options;
		// Set our ajax method
		dataTableOptions.ajax = function(data, callback, settings) {
			ajaxCaller(data, callback, settings, dataTableId, ajaxUrl);
		}
		// Set the language
		if (languageUrl!=null) {
			dataTableOptions.language = { url: languageUrl};
		}
		// Make a closure on the commands column to access buttons
		const commandColumn = dataTableOptions.columns.find(col => col.name === commandColumnName);
		if (commandColumn) {
			const originalRender = commandColumn.render;
			commandColumn.render = function(data, type, row, meta) {
				return originalRender(data, type, row, meta, dataTableHtml);
			}
		}

		// Preprocessor can override, add, delete configured options
		const preprocessor = window[preprocessorName];
		if (typeof preprocessor === "function") {
		    preprocessor(dataTableOptions);
		}
		
		const $table = $("#" + dataTableId);
		const dataTableApi = $table.DataTable(dataTableOptions);
		
		// After the table is drawn, define the command button handlers
		dataTableApi.on('draw', function () {
			makeAllButtonHandlers(dataTableJson, $table, dataTableApi);
		});		
		
		// Postprocessor can operate on the created table
		const postprocessor = window[postprocessorName];
		if (typeof postprocessor === "function") {
		    postprocessor(dataTableApi, dataTableOptions);
		}
		
		return dataTableApi;
	}
	
	/**
	 * Make the button event handlers once the table has been drawn, both command buttons and toolbar buttons.
	 */
	function makeAllButtonHandlers(dataTableJson, $table, dataTableApi) {
		// const thisTable = this; // DOM table
		const yadaButtons = dataTableJson.html.buttons;
		yadaButtons.forEach(button => {
			const buttonType = button.type;
			const $buttonsSameType = $(`[data-buttontype="${buttonType}"]`);
			makeButtonHandler($buttonsSameType, button, $table, dataTableApi)
		});
	}
	
	/**
	 * Create the click handlers for command buttons and toolbar buttons
	 * @param $buttons jquery list of buttons if the same type. Will contain both command and toolbar buttons.
	 * @param buttonConf the json configuration common to all the buttons of the same type
	 */
	function makeButtonHandler($buttons, buttonConf, $table, dataTableApi) {
		$buttons.click(function(e){
			e.preventDefault();
			const $anchor = $(e.currentTarget);
			const totRows = 1;
			const ajax = buttonConf.ajax; // true, false
			const hidePageLoader = buttonConf.hidePageLoader; // true, false
			const elementLoader = buttonConf.elementLoader; // #someCssSelector
			let actualLoader = elementLoader!=null?$(elementLoader):[];
			if (actualLoader.length==0) {
				actualLoader = hidePageLoader;
			}
			const roles = buttonConf.roles; // [1, 8]
			let url = buttonConf.url;
			const idName = $anchor.attr('data-idname') || "id"; // "id" is the default when data-idname is missing or empty
			const requestData = {};
			//
			let ids = [];
			const isRowIcon = $anchor.hasClass(CLASS_COMMANDBUTTON);
			if (isRowIcon) {
				// Command button specific code
				const id = rowIdToEntityId($anchor.parents('tr').attr('id'));
				ids = [id];
			} else {
				// Toolbar button specific code
				const $checks = $table.find("tbody [type='checkbox']:checked");
				totRows = $checks.length;
				ids = $checks.map(function() {
					const rowId = $(this).parents('tr').attr('id'); // "UserTable_UserProfile_22"
					if (rowId==null) {
						yada.log('Internal Error: ID missing in row. "DT_RowId" might not have been set on the backend. This happens if an Entity does not have an id field.');
					}
					return rowIdToEntityId(rowId);
				}).get();
			}
			
			// urlProvider
			if (buttonConf.urlProvider) {
				const urlProvider = window[buttonConf.urlProvider];
				if (typeof urlProvider == "function") {
					if (isRowIcon) {
						// Row button sends one row
						var rowData = dataTableApi.row($anchor.parentElement).data();
						url = urlProvider(rowData);
					} else {
						// Toolbar button sends all rows
						url = urlProvider(ids, dataTableApi);
					}
					if (url==null) {
						return; // The urlProvider chose to stop execution 
					}
				}
			}

			requestData[idName] = ids;
			
			// Confirmation modal
			const confirmDialog = buttonConf.confirmDialog;
			const columnNames = confirmDialog?.columnNames;
			const confirmTitle = formatMessage(confirmDialog?.confirmTitle, columnNames, row);
			const confirmOneMessage = formatMessage(confirmDialog?.confirmOneMessage, columnNames, row);
			const confirmManyMessage = formatMessage(confirmDialog?.confirmManyMessage, columnNames, row, totRows);
			const confirmButtonText = formatMessage(confirmDialog?.confirmButtonText, columnNames, row);
			const abortButtonText = formatMessage(confirmDialog?.abortButtonText, columnNames, row);

			// Make request			
			if (confirmDialog) {
				yada.confirm(confirmTitle, totRows==1?confirmOneMessage:confirmManyMessage, function(confirmed) {
					if (confirmed==true) {
						dtDoButtonCall(href, requestData, ajax, actualLoader, buttonConf, dataTableApi);
					}
				}, confirmButtonText, abortButtonText);
			} else {
				dtDoButtonCall(href, requestData, ajax, actualLoader, buttonConf, dataTableApi);
			}
		});
	}
	
	function dtDoButtonCall(url, requestData, ajax, loader, buttonConf, dataTableApi) {
		if (ajax) {
			yada.ajax(url, requestData, ()=>dataTableApi.draw(false), null, null, loader);
		} else {
			url = yada.addUrlParameters(url, requestData);
			if (buttonConf.windowTarget) {
				window.open(url, buttonData.windowName, buttonData.windowFeatures);
			} else {
				window.location.href = url;
			}
			return;
		}
	}

	/**
	 * Rendere the commands column (internal use)
	 */
	yada.dtCommandRender =  function(data, type, row, meta, dataTableHtmlJson) {
        if ( type === 'display' ) {
	    	// const entityId = rowIdToEntityId(data.DT_RowId); // 22
			const yadaButtons = dataTableHtmlJson.buttons;
			let displayIconOnRow = true;
        	let buttonsHtml = '';
			yadaButtons.filter(button => !button.global).forEach(button => {
				const showCommandIconFunction = button.showCommandIcon;
        		if (typeof showCommandIconFunction == "function") {
        			displayIconOnRow = showCommandIconFunction(data, row);
        		}
        		if (displayIconOnRow==true) {
	        		buttonsHtml += `<a class="${CLASS_COMMANDBUTTON}" href="javascript:void(0)" data-buttontype="${button.type}"
					data-idname="${button.idName}"
					title="${button.text}">${button.icon}</a>`;
        		} else if (displayIconOnRow=="disabled") {
	        		buttonsHtml += `<span class="${CLASS_COMMANDBUTTON} disabled" title="${button.text}">${button.icon}</span>`;
        		} else {
        			// No button
        		}
        	});
        	return buttonsHtml;
        }
        return data;
    }
	

	/**
	 * Render the checkbox column (internal use)
	 */
	yada.dtCheckboxRender = function( data, type, row ) {
		if ( type === 'display' ) {
			return '<input type="checkbox" class="yadaCheckInCell s_rowSelector"/>';
		}
		return data;
	}
	
	/**
	 * Call the backend via ajax (internal use)
	 */
	function ajaxCaller(data, callback, settings, dataTableId, ajaxUrl) {
		data['dataTableId'] = dataTableId;
    	// Add any extra parameter when a form is present
    	var addedData = $("form.yada_dataTables_"+dataTableId).serializeArray();
    	var extraParam = data['extraParam']={};
		addedData.forEach(paramObj => {
	        const paramName = paramObj.name;
	        const paramValue = paramObj.value;
	        if (!extraParam[paramName]) {
	            extraParam[paramName] = [];
	        }
	        extraParam[paramName].push(paramValue);
	    });
	    const noLoader = $("#"+dataTableId).hasClass('yadaNoLoader');
    	yada.ajax(ajaxUrl, jQuery.param(data), callback, 'POST', null, noLoader);
    }

	/**
	 * Convert the row id as assigned to DT_RowId into an entity id number.
	 * @param dtRowId the full value of DT_RowId e.g. "UserTable_UserProfile_22"
	 * @return the number following the last underscor, which is the entity id, e.g. 22
	 */	
	function rowIdToEntityId(dtRowId) {
		return dtRowId.match(/_(\d+)$/)[1];
	}
					
}( window.yada = window.yada || {} ));


