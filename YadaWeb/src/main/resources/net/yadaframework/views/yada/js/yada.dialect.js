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
		const suggestionUrl = $input.attr("data-yadaSuggestionListUrl");
		const requestIdNameOverride = $input.attr("data-yadaSuggestionRequestIdNameOverride"); // Optional with precedence

		if ($dropdown.length==0) {
			console.error("Missing dropdown element around input tag - aborting suggestions");
			return;
		}
		
		// Arrow up/down moves to the suggestion list
		const key = event.key;
		if (key=="ArrowDown" || key=="ArrowUp") {
			// Move the focus to the suggestion list if any
			const $suggestionList = $(input).closest(".dropdown").find(".jsYadaSuggestionList:visible");
			const tot = $("a", $suggestionList).length;
			if (tot>0) {
				var toFocus = (key=="ArrowDown")? 0 : (tot-1);
				$("a", $suggestionList).get(toFocus).focus();
			}
			return;
		}
		
		if (yada.isAjaxTriggerKey(event)) {
			// Ajax call triggered, so no suggestion list must be returned
			return;
		}
		
		// Otherwise, show the suggestion list
		const data = {
			prefix: inputValue
		} 
		
		yada.dequeueFunctionCall(input, function() {
			yada.ajax(suggestionUrl, data, function(responseText, responseHtml){
				const $newSuggestionList = responseHtml.find(".jsYadaSuggestionList");
				$dropdown.find(".jsYadaSuggestionList").replaceWith($newSuggestionList); 
				$("a", $newSuggestionList).click(function() {
					// Insert into the input field the suggested text when clicked
					const clickedValue = $(this).text();
					$input.val(clickedValue);
					// Also set the suggestionId and suggestionIdName attributes on the input for use on submission
					// See /YadaWeb/src/main/resources/net/yadaframework/views/yada/formfields/inputSuggestionFragment.html
					const yadaRequestData = input.yadaRequestData || {};
					const suggestionIdValue = $(this).attr("data-id");
					if (suggestionIdValue) {
						const suggestionIdname = requestIdNameOverride || $(this).attr("data-idname") || "id";
						yadaRequestData[suggestionIdname] = suggestionIdValue;
						input.yadaRequestData = yadaRequestData;
					}
					// Trigger a keyup event so that any char counter is changed and any ajax call is made
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
		}); // dequeueFunctionCall
	}

	yada.updateInputCounter = function($inputTag, $counterDiv) {
		const maxlength = $inputTag.attr("maxlength");
		var currentLength = $inputTag.val().length;
		if (maxlength==null) {
			console.error("Missing maxlength attribute on input tag with data-yadaTagId=" + $inputTag.attr("data-yadaTagId"));
			return;
		}
		$("span:first-child", $counterDiv).text(currentLength);
		$("span:last-child", $counterDiv).text(maxlength);
		$inputTag.on("input", function() {
			currentLength = this.value.length;
			$("span:first-child", $counterDiv).text(currentLength);
		});
	}

	yada.initYadaDialect = function() {
		// Change the numeric field using plus/minus buttons
		function changeNumericField($inputTag, valueToAdd) {
			const min = Number($inputTag.attr("min")||Number.MIN_SAFE_INTEGER);
			const max = Number($inputTag.attr("max")||Number.MAX_SAFE_INTEGER);
			var mousePressed = true;
			$(this).on("mouseup mouseleave mousedrag", function() {
				$(this).off("mouseup mouseleave mousedrag");
				mousePressed=false;
				const e = jQuery.Event("keyup");
				e.key = "Enter"
				$inputTag.trigger(e); 
			});
			function changeValue() {
				var value = Number($inputTag.val());
				if (valueToAdd>0 && value>max-valueToAdd) {
					return;
				}
				if (valueToAdd<0 && value<min-valueToAdd) {
					return;
				}
				$inputTag.val(value+valueToAdd);
			}
			function reschedule() {
				if (mousePressed) {
					changeValue();
					setTimeout(reschedule, 100);
				}
			}
			changeValue();
			setTimeout(function(){
				reschedule()
			}, 500);
		}
		
		$(document).on("mousedown", ".yadaInputNumericIncrement", function() {
			const $inputTag = $(this).siblings("input.yadaInputNumber");
			const step = Number($inputTag.attr("step")||1);
			changeNumericField.bind(this)($inputTag, step);
		});
		$(document).on("mousedown", ".yadaInputNumericDecrement", function() {
			const $inputTag = $(this).siblings("input.yadaInputNumber");
			const step = Number($inputTag.attr("step")||1);
			changeNumericField.bind(this)($inputTag, -step);
		});
		// Constrain the numeric value to min/max
		$(document).on("input", "input.yadaInputNumber", function() {
			const $inputTag = $(this);
			const minText = $inputTag.attr("min");
			const maxText = $inputTag.attr("max");
			const value = Number($inputTag.val());
			if (minText!=null) {
				const min = Number(minText);
				if (value<min) {
					$inputTag.val(min);
					// Don't do this because we don't swallow the event: $inputTag.trigger('input');
				}
			}
			if (maxText!=null) {
				const max = Number(maxText);
				if (value>max) {
					$inputTag.val(max);
					// Don't do this because we don't swallow the event: $inputTag.trigger('input');
				}
			}
		});
		
	}	
	
}( window.yada = window.yada || {} ));
