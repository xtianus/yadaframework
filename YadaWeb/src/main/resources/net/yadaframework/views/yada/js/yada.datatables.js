//////////////////
/// DataTables ///
//////////////////

(function( yada ) {
	// Namespace trick explained here: http://stackoverflow.com/a/5947280/587641
	// For a public property or function, use "yada.xxx = ..."
	// For a private property use "var xxx = "
	// For a private function use "function xxx(..."

	"use strict";
	yada.dataTable = function(dataTableJson, userLanguage) {
		const tableId = dataTableJson.id;

		const dataTable = $("#" + tableId).DataTable( {
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
		    	var noLoader = $table.hasClass('noLoader') || $table.hasClass('yadaNoLoader');
		    	yada.ajax(dataUrl, jQuery.param(data), callback, 'POST', null, noLoader);
		    },
		    language: {
		    	url: languageUrl
		    }
		});		
		
		
	}
	

	yada.dtMakeButtonHandler = function(buttonId, buttonData, dataTableId) {
		const $button = $("#"+buttonId);
		$button.click(function(e) {
			e.preventDefault();
			const dataTableObject = window[dataTableId];
			const $table = $("#" + dataTableId);
			var isRowIcon = $(this).hasClass("yadaRowCommandButton");
			var buttonUrl = buttonData.url;
			var ids = [];
			var id = yada.getHashValue($(this).attr('href')); // This has a value when isRowIcon
			var totElements = 1;
			if (!isRowIcon) {
				// Toolbar button
				var $checks = $table.find("tbody [type='checkbox']:checked");
				totElements = $checks.length;
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

	
}( window.yada = window.yada || {} ));


