//////////////////
/// DataTables ///
//////////////////

(function( yada ) {
	"use strict";
	
	// Namespace trick explained here: http://stackoverflow.com/a/5947280/587641
	// For a public property or function, use "yada.xxx = ..."
	// For a private property use "var xxx = "
	// For a private function use "function xxx(..."

	/**
	 * Create a new DataTable with the given options:
	 * @param $table the jquery table, e.g. $('#myTable'). It must have an id.
	 * @param dataUrl the ajax url to retrieve data in json format, e.g. [[@{/gestione/ajaxCompanyTablePage}]]
	 * @param dataAttributes names of the json attributes for data of each column, e.g. ['name', 'surname', 'address']
	 *        The array can also contain full DataTable objects with data/name/orderable/searchable fields, e.g. ['name', 'surname', {data:'address',orderable:false}]
	 * @param editDef object containing url and text (can be null): 
	 * 	- url that returns the edit form e.g. [[@{/gestione/ajaxRenameTagForm}]]
	 * 	- title to show in the tooltip
	 *  - idName the name of the id request parameter (optional - defaults to "id")
	 *  - noLoader true to hide the ajax loader
	 *  - noAjax true to make a normal request and load a new page
	 * @param deleteDef object containing url and text (can be null):
	 * 	- url to delete the row e.g. [[@{/gestione/ajaxDeleteTag}]]
	 * 	- title to show in the tooltip
	 *  - confirmTitle (optional)
	 * 	- confirmOneMessage (optional)
	 * 	- confirmManyMessage (optional)
	 *  - confirmButtonText (optional)
	 *  - abortButtonText (optional)
	 *  - idName the name of the id request parameter (optional - defaults to "id")
	 *  - nameColumn the index of the column holding the text to show in the dialog when deleting one element (checkboxes = 1)
	 * @param order array of column ordering, e.g. [ [1, 'asc'] ] or [ [ 0, 'asc' ], [ 1, 'asc' ] ]
	 *        - be careful that in thymeleaf you need a space between two open squares: [ [ not [[
	 * NOTE: index 1 is the checkbox column
	 * @param pageLength the number of rows per page
	 * @param (optional) languageUrl url to the language file, like http://server.example/path/18n/italian.lang
	 * @param (optional) extraButtons additional command button definitions in an array of objects:
	 * - url to call when the button is clicked 
	 * - title to show in the tooltip
	 * - icon for the button, like '<i class="fa fa-power-off" aria-hidden="true"></i>'
	 * - idName the name of the id request parameter (optional - defaults to "id")
	 * - ajax (optional) false to make a normal page transition to the target link
	 * @returns the DataTable object
	 */
	yada.dataTableCrud = function($table, dataUrl, dataAttributes, editDef, deleteDef, order, pageLength, languageUrl, extraButtons, removeCheckbox) {
		// Method argument validation
		if ($table == null || typeof $table != "object" || $table.length!=1 || typeof $table[0] != "object") {
			return;
		}
		if (dataUrl==null || typeof dataUrl != "string") {
			console.error("yada.datatables: dataUrl must be a string");
			return;
		}
		if (!Array.isArray(dataAttributes) || dataAttributes.length == 0) {
			console.error("yada.datatables: dataAttributes must be a non-empty array");
			return;
		}
		if (editDef!=null && (typeof editDef != "object" || Array.isArray(editDef))) {
			console.error("yada.datatables: editDef must be an object or null");
			return;
		}
		if (deleteDef!=null && (typeof deleteDef != "object" || Array.isArray(deleteDef))) {
			console.error("yada.datatables: deleteDef must be an object or null");
			return;
		}
		if (!Array.isArray(order) || order.length == 0 ) {
			console.error("yada.datatables: order must be a non-empty array");
			return;
		}
		if (typeof pageLength != "number" ) {
			console.error("yada.datatables: pageLength must be an integer");
			return;
		}
		if (languageUrl!=null && typeof languageUrl != "string") {
			console.error("yada.datatables: languageUrl must be a string or null");
			return;
		}
		if (extraButtons!=null && (!Array.isArray(extraButtons) || extraButtons.length == 0 || typeof extraButtons[0] != "object" || Array.isArray(extraButtons[0]) ) ) {
			console.error("yada.datatables: extraButtons must be a non-empty array of objects or null");
			return;
		}
		if (removeCheckbox!=null && typeof removeCheckbox != "boolean") {
			console.error("yada.datatables: removeCheckbox must be a boolean or null");
			return;
		}
		
		//
		var tableId = $table.attr('id');
		if (tableId==undefined) {
			console.error("yada.datatables: $table must have a valid id attribute");
			return;
		}
		
		var totColumns = $('th', $table).length;
		var neededColumns = dataAttributes.length + 2 + (removeCheckbox?0:1);
		if (totColumns!=neededColumns) {
			yada.showErrorModal("Internal Error", "Table '" + $table[0].id + "' has " + totColumns + " columns but " + neededColumns + " where expected - (ignored)");
		}
		var columnDef = [
		    {
		    	data: null,
		    	defaultContent:'',
		    	className: 'control',
		    	orderable: false,
				searchable: false
		    }
		];
		if (!removeCheckbox) {
			columnDef.push(
				{	// Colonna dei checkbox
					data: null,
					name: 'seleziona', // Non usato
					orderable: false,
					searchable: false,
					render: function ( data, type, row ) {
						if ( type === 'display' ) {
							return '<input type="checkbox" class="yadaCheckInCell s_rowSelector"/>';
						}
						return data;
					},
					width: "50px",
					className: "yadaCheckInCell"
				}	                   
			);
		}
		// Add field columns
		for (var i = 0; i < dataAttributes.length; i++) {
			var field = dataAttributes[i];
			// Default column definition:
			var fieldDef = {
				data: field, // Supposed to be a string
				defaultContent: '---',
				name: field,
				orderable: true,
				searchable: true
			}
			if (typeof field == "object") {
				fieldDef = field;
				if (fieldDef.name == undefined && typeof fieldDef.data == "string" ) {
					// If not specified, the name is the same as the data
					fieldDef.name = fieldDef.data; 
				}
			}
//			var fieldData=field, fieldName=field, fieldSearchable=true, fieldOrderable=true; // Defaults
//			if (field.data != undefined) {
//				fieldData = field.data;
////				if (typeof field.data == "string") {
//				fieldName = field.data.toString(); // Default is same as data
////				}
//			}
//			if (field.name != undefined) {
//				fieldName = field.name;
//			}
//			if (field.searchable != undefined) {
//				fieldSearchable = field.searchable;
//			}
//			if (field.orderable != undefined) {
//				fieldOrderable = field.orderable;
//			}
			
			columnDef.push(
				fieldDef
//				{
//				data: fieldData, // nome dell'attributo or function
//				defaultContent: '---', // Value when null or undefined
//				name: fieldName, // nome interno pari al nome dell'attributo
//				orderable: fieldOrderable,
//				searchable: fieldSearchable
//				}
			);
		}
		// Colonna dei comandi
		columnDef.push({ 
			data: null, 
			className: 'yadaCommandButtonCell',
			name: 'comandi',
			orderable: false,
			searchable: false,
			width: '50px',
		    render: function ( data, type, row ) {
		    	var rowId = yada.getHashValue(data.DT_RowId);
		        if ( type === 'display' ) {
		        	var buttons = '';
		        	for (var i=0; extraButtons!=null && i<extraButtons.length; i++) {
		        		var displayIconOnRow = extraButtons[i].showRowIcon;
		        		if (displayIconOnRow==null) {
		        			displayIconOnRow = extraButtons[i].noRowIcon; // Fallback to deprecated attribute
		        		}
		        		if (typeof displayIconOnRow == "function") {
		        			displayIconOnRow = displayIconOnRow(data, row);
		        		}
		        		if (displayIconOnRow==null) {
		        			displayIconOnRow = true;
		        		}
		        		if (displayIconOnRow!=null && extraButtons[i].noRowIcon != null && extraButtons[i].showRowIcon == null) {
		        			displayIconOnRow = !displayIconOnRow; // Invert the logic
		        		}
		        		if (displayIconOnRow) {
			        		buttons +=
			        			'<a class="yadaTableExtraButton' + i + ' yadaRowCommandButton" href="#' +
			        			rowId + '" title="' + extraButtons[i].title + '">' + extraButtons[i].icon + '</a>';
		        		} else if (displayIconOnRow=="disabled") {
		        			buttons += '<span class="yadaTableExtraButton' + i + ' yadaRowCommandButton disabled" ' + 'title="' + extraButtons[i].title + '"' + '>' + extraButtons[i].icon + '</span>';
		        		} else {
		        			// No button
		        		}
		        	}
		        	if (editDef!=null) {
		        		buttons +=
		        			'<a class="s_editRow yadaRowCommandButton" href="#'+rowId+'" title="'+editDef.title+'"><i class="fa fa-pencil-square-o"></i></a>';
		        	}
		        	if (deleteDef!=null) {
		        		buttons +=
		        			'<a class="s_deleteRow yadaRowCommandButton" href="#'+rowId+'" title="'+deleteDef.title+'"><i class="fa fa-trash-o"></i></a>';
		        	}
		        	return buttons;
		        }
		        return data;
		    }
	    });
		var dataTable = $table.DataTable( {
	        responsive: true,
	        pageLength: pageLength,
			orderMulti: order.length>1,
			order: order,
			columns: columnDef,					
		    serverSide: true,
		    ajax: function(data, callback, settings) {
		    	// Need to add any extra parameter if a form is present
		    	var addedData = $("form.yada_dataTables_"+tableId).serializeArray();
		    	var extraParam = data['extraParam']={};
		    	var i=0;
		    	for (i=0; i<addedData.length; i++) {
		    		var paramObj = addedData[i];
		    		var paramName = paramObj.name;
		    		var paramValue = paramObj.value;
		    		extraParam[paramName] = paramValue;
		    	}
		    	var noLoader = $table.hasClass('noLoader');
		    	yada.ajax(dataUrl, jQuery.param(data), callback, 'POST', null, noLoader);
		    },
		    language: {
		    	url: languageUrl
		    }
	//	    ajax: {
	//	    	url: dataUrl,
	//	    	type: 'POST',
	//	    	beforeSend: function() { yada.loaderOn(); },
	//	    	complete: function() { yada.loaderOff(); }
	//	    }
		});
		
		/*
		if (languageUrl!=null) {
			dataTable.language = {
				url: languageUrl	
			}
		}
		*/
		
		dataTable.on('draw.dt', function () {
			var thisTable = this; // DOM table
			var thisDataTable = $(this).DataTable(); // DataTable API
			//
			// Eseguiti a table caricata
			//
			// Pulsante di cancellazione singola riga
			$('a.s_deleteRow', thisTable).click(function(e) {
				e.preventDefault();
				var id = yada.getHashValue($(this).attr('href'));
				var idName = deleteDef.idName || "id";
				var nameColumn = deleteDef.nameColumn || 3;
				var requestData = {};
				var noLoader = deleteDef.noLoader || false;
				requestData[idName] = id;
				var $row = $(this).parents('tr');
				// var dtrow = dataTable.row($row);
				var confirmTitle = deleteDef.confirmTitle || null;
				var confirmButtonText = deleteDef.confirmButtonText || "Delete";
				var abortButtonText = deleteDef.abortButtonText || "Cancel";
				var confirmMessage = deleteDef.confirmOneMessage || "Do you want to delete {0}?";
				var rowName = thisDataTable.cell($row, nameColumn).data(); // We assume that column 1 is the select, column 2 is the id and column 3 is the name or similar 
				confirmMessage = confirmMessage.replace("{0}", rowName);
				yada.confirm(confirmTitle, confirmMessage, function(result) {
					if (result==true) {
						yada.ajax(deleteDef.url, requestData, function() {
							thisDataTable.draw(false);
						}, null, null, noLoader);
					}
				}, confirmButtonText + ' "' + rowName + '"', abortButtonText);
			});
			
			// Pulsante di edit riga
			$('a.s_editRow', thisTable).click(function(e) {
				e.preventDefault();
				var id = yada.getHashValue($(this).attr('href'));
				var idName = editDef.idName || "id";
				if (editDef.noAjax==true) {
					const targetUrl = yada.addOrUpdateUrlParameter(editDef.url, idName, id);
					window.location.href = targetUrl;
					return;
				}
				var requestData = {};
				var noLoader = editDef.noLoader || false;
				requestData[idName] = id;
				// Devo abilitare ajax ricorsivamente per quando il form ritorna con un errore di validazione
				var handler = function(responseText, responseHtml) {
					yada.datatableDrawOnModalClose(thisDataTable);
					recursiveEnableAjaxForm(responseText, responseHtml);
				};
				yada.ajax(editDef.url, requestData, handler, null, null, noLoader);
			});
			
			// Handler per gli extra buttons
			for (var i=0; extraButtons!=null && i<extraButtons.length; i++) {
				makeExtraButtonHandler(extraButtons[i], $('a.yadaTableExtraButton' + i, thisTable), thisDataTable, $(thisTable));
				// Legacy
				// makeExtraButtonHandler(extraButtons[i], $('a.s_extraButton' + i, thisTable), thisDataTable, $(thisTable));
			}
			
			// Seleziona/deseleziona tutto
			$('.s_columnSelector', thisTable).change(function() {
				var checked = $(this).prop('checked');
				$(this).parents('table').find("td input.yadaCheckInCell[type='checkbox']").prop('checked', checked).change();
			});
			
			// Checkbox riga
			$('.s_rowSelector', thisTable).change(function() {
				// Controllo se almeno uno è checked
				var totChecked = 0;
				$('.s_rowSelector').each(function( index ) {
					var checked = $(this).prop('checked');
					totChecked += checked?1:0;
				});
				// Abilito i bottoni della toolbar
				if (totChecked == 1) {
					$(this).parents('div.yadaTableBlock').find('div.yadaTableToolbar .yadaTableSinglerowButton').removeClass('disabled');
					$(this).parents('div.yadaTableBlock').find('div.yadaTableToolbar .yadaTableMultirowButton:not(.yadaTableSinglerowButton)').addClass('disabled');
					// Legacy:
					$(this).parents('div.yadaTableBlock').find('div.yadaTableToolbar .s_singlerowButton').removeClass('disabled');
					$(this).parents('div.yadaTableBlock').find('div.yadaTableToolbar .s_multirowButton:not(.s_singlerowButton)').addClass('disabled');
				} else if (totChecked > 1) {
					$(this).parents('div.yadaTableBlock').find('div.yadaTableToolbar .yadaTableMultirowButton').removeClass('disabled');
					$(this).parents('div.yadaTableBlock').find('div.yadaTableToolbar .yadaTableSinglerowButton:not(.yadaTableMultirowButton)').addClass('disabled');
					// Legacy:
					$(this).parents('div.yadaTableBlock').find('div.yadaTableToolbar .s_multirowButton').removeClass('disabled');
					$(this).parents('div.yadaTableBlock').find('div.yadaTableToolbar .s_singlerowButton:not(.s_multirowButton)').addClass('disabled');
				} else {
					var toolbarButtons = $(this).parents('div.yadaTableBlock').find('div.yadaTableToolbar .btn:not(.s_addButton):not(.yadaTableAlwaysButton)');
					toolbarButtons.addClass('disabled');
				}
			});
		} );
		
		// yadaTableToolbar "add button"
		if (editDef!=null) {
			var addButton = $table.parents('.yadaTableBlock').find('.yadaTableToolbar a.s_addButton');
			addButton.click(function(e) {
				e.preventDefault();
				// The handlert enables ajax forms on the loaded response, and adds a handler to redraw the table on modal close
				// Devo abilitare ajax ricorsivamente per quando il form ritorna con un errore di validazione
				var handler = function(responseText, responseHtml) {
					yada.datatableDrawOnModalClose(dataTable);
					recursiveEnableAjaxForm(responseText, responseHtml);
				};
				var noLoader = editDef.noLoader || false;
				yada.ajax(editDef.url, null, handler, null, null, noLoader);
			});
	
			// yadaTableToolbar "edit button"
			var editButton = $table.parents('.yadaTableBlock').find('.yadaTableToolbar a.s_editButton');
			editButton.click(function(e) {
				e.preventDefault();
				// The handlert enables ajax forms on the loaded response, and adds a handler to redraw the table on modal close
				var handler = function(responseText, responseHtml) {
					yada.datatableDrawOnModalClose(dataTable);
					recursiveEnableAjaxForm(responseText, responseHtml);
				};
				var id = yada.getHashValue($table.find("tbody [type='checkbox']:checked").parents('tr').attr('id'));
				var idName = editDef.idName || "id";
				var requestData = {};
				requestData[idName] = id;
				var noLoader = editDef.noLoader || false;
				yada.ajax(editDef.url, requestData, handler, null, null, noLoader);
			});
		}
		
		// yadaTableToolbar "delete button"
		if (deleteDef!=null) {
			var deleteButton = $table.parents('.yadaTableBlock').find('.yadaTableToolbar a.s_deleteButton');
			deleteButton.click(function(e) {
				e.preventDefault();
				var $checks = $table.find("tbody [type='checkbox']:checked");
				var totElements = $checks.length;
				var ids = $checks.map(function() {
					var id = $(this).parents('tr').attr('id');
					if (id==null) {
						alert('Internal Error-ID missing in row: did you forget "DT_RowId" in the Model?');
					}
					return yada.getHashValue(id);
				}).get();
		
				var confirmTitle = deleteDef.confirmTitle || null;
				var confirmMessage = deleteDef.confirmManyMessage || "Do you want to delete {0} elements?";
				confirmMessage = confirmMessage.replace("{0}", totElements);
				var confirmButtonText = deleteDef.confirmButtonText || "Delete";
				var abortButtonText = deleteDef.abortButtonText || "Cancel";
				var noLoader = deleteDef.noLoader || false;
				yada.confirm(confirmTitle, confirmMessage, function(result) {
					if (result==true) {
						var idName = deleteDef.idName || "id";
						var requestData = {};
						requestData[idName] = ids;
						var handler = function() {
							dataTable.draw(false);
						};
						handler.executeAnyway=true;
						yada.ajax(deleteDef.url, requestData, handler, null, null, noLoader);
					}
				}, confirmButtonText + ' ' + totElements, abortButtonText);
			});
		}
		
		// yadaTableToolbar: aggiunta extraButtons
		if (extraButtons!=null) {
			makeToolbarExtraButtons(extraButtons, dataTable, $table);
		}
			  
		return dataTable;
	};
	
	yada.datatableDrawOnModalClose = function(dataTable) {
		$('#ajaxModal').on('hide.bs.modal', function (e) {
			dataTable.draw(false);
			$('#ajaxModal').unbind('hide.bs.modal');
		});
	}
	
	function makeToolbarExtraButtons(extraButtons, dataTable, $table) {
		var sortedExtraButtons = extraButtons.sort(function (a, b) { return Math.sign(a.toolbarPosition-b.toolbarPosition) });
		for (var i=0; i<sortedExtraButtons.length; i++) {
			var btndef = sortedExtraButtons[i];
			if (btndef.toolbarClass != null) {
				var disabled = (btndef.toolbarClass != "yadaTableAlwaysButton") ? "disabled" : "";
				var buttonHtml = '<a class="btn btn-primary '+disabled+' '+btndef.toolbarClass+' s_toolbarExtraButton' + i + '"' +
				' href="#" title="'+ btndef.title +'">' +
				btndef.icon.replace('fa ', 'fa fa-lg ') + ' <span>'+btndef.toolbarText+'</span>' +
				'</a> ';
				var pos = btndef.toolbarPosition;
				var $yadaTableToolbar = $table.parents('.yadaTableBlock').find('.yadaTableToolbar');
				var $existing = $('a.btn', $yadaTableToolbar);
				if (pos==null || $existing.length<=pos) {
					$yadaTableToolbar.append(buttonHtml);
				} else {
					$existing.eq(pos).before(buttonHtml);
				}
				makeExtraButtonHandler(btndef, $('.s_toolbarExtraButton' + i, $yadaTableToolbar), dataTable, $table);
			}
		}
	}
	
	function makeExtraButtonHandler(extraButtonDef, $button, dataTable, $table) {
		$button.click(function(e) {
			e.preventDefault();
			var isRowIcon = $(this).hasClass("yadaRowCommandButton");
			var buttonUrl = extraButtonDef.url;
			var ids = [];
			var id = yada.getHashValue($(this).attr('href'));
			if (!isRowIcon) {
				// Toolbar button
				var $checks = $table.find("tbody [type='checkbox']:checked");
				var totElements = $checks.length;
				ids = $checks.map(function() {
					var id = $(this).parents('tr').attr('id');
					if (id==null) {
						alert('Internal Error-ID missing in row: did you forget "DT_RowId" in the Model?');
					}
					return yada.getHashValue(id);
				}).get();
			} else {
				ids = [id];
			}
			var noLoader = extraButtonDef.noLoader || false;
			if (typeof extraButtonDef.url == "function") {
				if (isRowIcon) {
					// Row button
					var rowData = dataTable.row(this.parentElement).data();
					buttonUrl = buttonUrl(rowData);
				} else {
					// Toolbar button
					// TODO to be tested
					var rowData = dataTable.rows();
					buttonUrl = buttonUrl(rowData, ids);
				}
			}
			var idName = extraButtonDef.idName==null ? "id" : extraButtonDef.idName;
			var param = (ids.length>1?ids:ids[0]); // Either send one id or all of them
			if (extraButtonDef.ajax === false) {
				if (typeof extraButtonDef.url != "function") {
					buttonUrl = yada.addOrUpdateUrlParameter(buttonUrl, idName, param);
				}
				if (extraButtonDef.windowName!=null) {
					window.open(buttonUrl, extraButtonDef.windowName, extraButtonDef.windowFeatures);
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
				dataTable.draw(false); // Always reload table content on return from ajax call (there could be no modal)
				yada.datatableDrawOnModalClose(dataTable);
				recursiveEnableAjaxForm(responseText, responseHtml);
			};
			yada.ajax(buttonUrl, requestData, handler, null, null, noLoader);
		});
	}
	
	function recursiveEnableAjaxForm(responseText, responseHtml) {
		yada.enableAjaxForm($('form.yadaAjaxForm', responseHtml), recursiveEnableAjaxForm);
		// Legacy
		yada.enableAjaxForm($('form.s_ajaxForm', responseHtml), recursiveEnableAjaxForm);
	};

	
}( window.yada = window.yada || {} ));


