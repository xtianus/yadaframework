// yada.dialect.js
// Yada Thymeleaf dialect support
// Depends on yada.ajax.js

(function( yada ) {
	"use strict";
	
	// Namespace trick explained here: http://stackoverflow.com/a/5947280/587641
	// For a public property or function, use "yada.xxx = ..."
	// For a private property use "const xxx = "
	// For a private function use "function xxx(..."
	
	/**
	 * Used by <yada:input> to implement the suggestion list
	 * @param event the onkeyup KeyboardEvent
	 */		
	yada.suggestionList = function(event) {
		const input = event.target;
		const $input = $(input);
		const $dropdown = $input.closest(".dropdown");
		const inputValue = $.trim(input.value);
		const addElementUrl = $input.attr("data-yadaSuggestionAddUrl"); // Optional
		const suggestionUrl = $input.attr("data-yadaSuggestionListUrl");
		const suggestionReplace = $input.attr("data-yadaUpdateOnSuccess"); // Optional
		const addIdRequestNameOverride = $input.attr("data-yadaSuggestionAddIdRequestNameOverride"); // Optional with precedence
		const key = event.key; // https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/key/Key_Values

		const addSuggestedElement = function(responseText, $responseHtml) {
			if (suggestionReplace!=null) {
				yada.extendedSelect($input, suggestionReplace).replaceWith($responseHtml.children()); 
			}
			$responseHtml.find("input").focus(); // This may not always be correct
		}

		if ($dropdown.length==0) {
			console.error("Missing dropdown element around input tag - aborting suggestions");
			return;
		}
		
		// Send input field value to server
		// When newline, space, comma, cursor up/down/right are pressed, make the call to create a new element
		if (addElementUrl!=null && inputValue!="" && inputValue!="#" && (key=="Enter" || key==" " || key=="," || key=="ArrowRight" || key=="ArrowUp")) {
			const suggestionId = input.suggestionId; // set when clicking on the suggestion
			const suggestionIdname = addIdRequestNameOverride || input.suggestionIdname || "id"; // set when clicking on the suggestion
			const data = {};
			const inputName = input.name || 'value';
			data[inputName] = inputValue;
			if (suggestionId) {
				data[suggestionIdname] = suggestionId;
			}
			// const remoteUrl = addElementUrl + '&value=' + encodeURIComponent(inputValue); 
			yada.ajax(addElementUrl, data, addSuggestedElement, null, null, false);
			input.value="";
			input.dispatchEvent(new Event('input')); // Resets the counter
			event.preventDefault();
			return false;
		}
		
		// Arrow down moves to the suggestion list
		if (key=="ArrowDown") {
			// Move the focus to the suggestion list if any
			const $suggestionList = $(input).closest(".dropdown").find(".jsYadaSuggestionList:visible");
			if ($("a", $suggestionList).length>0) {
				$("a", $suggestionList).get(0).focus();
			}
			return;
		}
		
		// Otherwise, show the suggestion list
		const data = {
			prefix: inputValue
		} 
		yada.ajax(suggestionUrl, data, function(responseText, responseHtml){
			const $newSuggestionList = responseHtml.find(".jsYadaSuggestionList");
			$dropdown.find(".jsYadaSuggestionList").replaceWith($newSuggestionList); 
			$("a", $newSuggestionList).click(function() {
				// Insert into the input field the suggested text when clicked
				const clickedValue = $(this).text();
				$input.val(clickedValue);
				// Also set the suggestionId and suggestionIdName attributes on the input for use on submission
				// See /YadaWeb/src/main/resources/net/yadaframework/views/yada/formfields/inputSuggestionFragment.html
				input.suggestionId = $(this).attr("data-id"); 
				input.suggestionIdname = $(this).attr("data-idname");
				// Trigger a keyup event so that any char counter is changed
				var e = jQuery.Event("keyup");
				e.key = "Enter"
				$input.trigger(e); 
			});
			// Show or hide the suggestions if there are some or none
			const $toggler = $dropdown.find("div[data-bs-toggle=dropdown]");
			const dropdownApi = new bootstrap.Dropdown($toggler[0]);
			if ($("a", $newSuggestionList).length>0) {
				dropdownApi.show();
			} else {
				dropdownApi.hide();
			}
		}, null, null, true);
	}

}( window.yada = window.yada || {} ));
