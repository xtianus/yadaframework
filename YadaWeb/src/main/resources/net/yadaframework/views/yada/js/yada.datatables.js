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
	 * Initialize the datatable (internal use)
	 */
	yada.dataTable = function(dataTableJson, ajaxUrl, commandColumnName, preprocessorName, postprocessorName) {
		const dataTableId = dataTableJson.id;
		const dataTableHtml = dataTableJson.html;
		const dataTableOptions = dataTableJson.options;
		// Set our ajax method
		dataTableOptions.ajax = function(data, callback, settings) {
			ajaxCaller(data, callback, settings, dataTableId, ajaxUrl);
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
		
		const dataTableApi = $("#" + dataTableId).DataTable(dataTableOptions);	
		
		// Postprocessor can operate on the created table
		const postprocessor = window[postprocessorName];
		if (typeof postprocessor === "function") {
		    postprocessor(dataTableApi, dataTableOptions);
		}
			
		return dataTableApi;
	}
	

	// TODO chiamare da yada.dataTable non da fuori se si riesce
	yada.dtMakeButtonHandler = function(buttonId, buttonData, dataTableId) {
		const $button = $("#"+buttonId);
		$button.click(function(e) {
			e.preventDefault();
			const dataTableObject = window[dataTableId];
			const $table = $("#" + dataTableId);
			var isRowIcon = $(this).hasClass(CLASS_COMMANDBUTTON);
			var buttonUrl = buttonData.url;
			var ids = [];
			var id = yada.getHashValue($(this).attr('href')); // This has a value when isRowIcon
			var totElements = 1;
			if (!isRowIcon) {
				// Toolbar button
				var $checks = $table.find("tbody [type='checkbox']:checked");
				totElements = $checks.length;
				ids = $checks.map(function() {
					const rowId = $(this).parents('tr').attr('id'); // "UserTable_UserProfile_22"
					if (rowId==null) {
						alert('Internal Error: ID missing in row. "DT_RowId" might not have been set on the backend');
					}
					return dtRowIdToEntityId(rowId);
				}).get();
			} else {
				ids = [id];
			}
			var noLoader = buttonData.noLoader || false;
			if (typeof buttonData.url == "function") {
				if (isRowIcon) {
					// Row button
					var rowData = dataTableObject.row(this.parentElement).data();
					buttonUrl = buttonUrl(rowData);
				} else {
					// Toolbar button
					// TODO to be tested
					var rowData = dataTableObject.rows();
					buttonUrl = buttonUrl(rowData, ids);
				}
			}
			var idName = buttonData.idName==null ? "id" : buttonData.idName;
			var param = (ids.length>1?ids:ids[0]); // Either send one id or all of them
			if (buttonData.ajax === false) {
				if (typeof buttonData.url != "function") {
					buttonUrl = yada.addOrUpdateUrlParameter(buttonUrl, idName, param);
				}
				if (buttonData.windowName!=null) {
					window.open(buttonUrl, buttonData.windowName, buttonData.windowFeatures);
				} else {
					window.location.replace(buttonUrl);
				}
				return;
			}
			var requestData = {};
			if (idName!="") {
				requestData[idName] = param;
			}
			var handler = function(responseText, responseHtml) {
				dataTableObject.draw(false); // Always reload table content on return from ajax call (there could be no modal)
				yada.datatableDrawOnModalClose(dataTableObject);
				recursiveEnableAjaxForm(responseText, responseHtml);
			};
			if (buttonData.confirm!=true) {
				yada.ajax(buttonUrl, requestData, handler, null, null, noLoader);
			} else {
				// Confirm modal
				const confirmTitle = buttonData.confirmTitle || null;
				var confirmMessage = null;
				if (totElements<2) {
					var rowIndex = dataTableObject.row(this.parentElement).index();
					if (!isRowIcon) {
						rowIndex = dataTableObject.row($table.find("tbody [type='checkbox']:checked").parent()).index();
					}
					var nameColumn = buttonData.confirmNameColumn || 3; // We assume that column 1 is the select, column 2 is the id and column 3 is the name or similar
					const rowName = dataTableObject.cell(rowIndex, nameColumn).data(); 
					confirmMessage = buttonData.confirmOneMessage || "Do you want to delete {0}?";
					confirmMessage = confirmMessage.replace("{0}", rowName);
				} else {
					confirmMessage = buttonData.confirmManyMessage || `Do you want to delete ${totElements} elements?`;
				}
				yada.confirm(confirmTitle, confirmMessage, function(result) {
						if (result==true) {
							yada.ajax(buttonUrl, requestData, handler, null, null, noLoader);
						}
					}, buttonData.confirmButtonText, buttonData.abortButtonText
				);
				
			}			
		});
	}

	/**
	 * Rendere the commands column (internal use)
	 */
	yada.dtCommandRender =  function(data, type, row, meta, dataTableHtmlJson) {
        if ( type === 'display' ) {
	    	const entityId = dtRowIdToEntityId(data.DT_RowId); // 22
			const yadaButtons = dataTableHtmlJson.buttons;
			let displayIconOnRow = true;
        	let buttonsHtml = '';
			yadaButtons.filter(button => !button.global).forEach(button => {
				const showCommandIconFunction = button.showCommandIcon;
        		if (typeof showCommandIconFunction == "function") {
        			displayIconOnRow = showCommandIconFunction(data, row);
        		}
        		if (displayIconOnRow==true) {
	        		buttonsHtml += `<a class="${CLASS_COMMANDBUTTON}" href="#${entityId}" title="${button.text}">${button.icon}</a>`;
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
	function dtRowIdToEntityId(dtRowId) {
		return dtRowId.match(/_(\d+)$/)[1];
	}
					
}( window.yada = window.yada || {} ));


