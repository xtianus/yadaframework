// yada.ajax.js
// Depends on yada.js

(function( yada ) {
	"use strict";
	
	// Namespace trick explained here: http://stackoverflow.com/a/5947280/587641
	// For a public property or function, use "yada.xxx = ..."
	// For a private property use "var xxx = "
	// For a private function use "function xxx(..."
	
	const markerAjaxButtonOnly = 'yadaAjaxButtonOnly';
	const markerAjaxModal = 'yadaAjaxModal';
	var clickedButton;
		
	var ajaxCounter = 0; // Counter for the ajax call parallelism

	// WARNING: if you change this, also change it in yada.js
	const markerClass = 'yadaAjaxed'; // To prevent double submission
	
	// Deprecated: these were once used when opening the login form via yada.openLoginModal and yada.openLoginModalAjax
	yada.postLoginHandler = null; // Handler to run after login, if any
	var postLoginUrl = null;
	var postLoginData = null;
	var postLoginType = null;
	
	const yadaAjaxResponseHtmlRoot = "<div class='yadaAjaxResponseHtml'>";
	
	yada.markerAjaxModal = markerAjaxModal; // For use in other scripts
	
	/**
	 * Init yada ajax handlers on the specified element
	 * @param $element the element, or null for the entire body
	 */
	yada.initAjaxHandlersOn = function($element) {
		yada.enableAjaxForms(null, $element);
		yada.enableAjaxLinks(null, $element);
		yada.enableAjaxSelects(null, $element);
		yada.enableAjaxCheckboxes(null, $element);
		initObservers($element)
		yada.enableAjaxInputs();
	}
	
	/**
	* All observers should be enabled in this function, that is also called after a returned ajax HTML is cloned.
	*/
	function initObservers($element) {
		yada.enableAjaxTriggerInViewport($element);
	}
	
	//////////////////////
	/// Pagination support
	/**
	 * Changes the current URL history adding pagination parameters: the back button will enforce the previous page parameters on the url.
	 * This is needed when loading a new page via ajax so that the browser back button will run the correct query again.
	 * Pagination parameters on the nextpage url/form must always be named "page" and "size", but they can be different on the history url in order to have
	 * more than one pageable section on the same page, for example "product.page" and "project.page". 
	 * @param $linkOrForm the jQuery object of the clicked anchor, or submitted form, with pagination parameters named "page" and "size"
	 * @param pageParam the optional name of the parameter that will contain the page number to load on back, for example "product.page"
	 * @param sizeParam the optional name of the parameter that will contain the page size to load on back, for example "product.size"
	 * @param loadPreviousParam the optional name of the parameter that will contain the loadPrevious flag, for example "product.loadPrevious". 
	 *        It just tells when the user pressed the back button, so that eventually all previous pages can be loaded, not just the last one.
	 * @see net.yadaframework.web.YadaPageRequest
	 */
	yada.fixPaginationLinkHistory = function($linkOrForm, pageParam, sizeParam, loadPreviousParam) {
		const NEXTPAGE_NAME="page";
		const NEXTSIZE_NAME="size";
		const CONTAINERID_NAME="yadaContainer";
		const CONTAINERSCROLL_NAME="yadaScroll";
		// Default param names
		pageParam = pageParam || "page";
		sizeParam = sizeParam || "size";
		loadPreviousParam = loadPreviousParam || "loadPrevious";
		// 
		// ".../en/search/loadMoreProducts?searchString=tolo&page=2&size=4"
		const nextPageUrl = $linkOrForm.attr("data-yadahref") || $linkOrForm.attr("href");
		var nextPage = yada.getUrlParameter(nextPageUrl, NEXTPAGE_NAME);
		var nextSize = yada.getUrlParameter(nextPageUrl, NEXTSIZE_NAME);
		if (nextPageUrl==null) {
			// Could be a form
			nextPage = $("input[name="+NEXTPAGE_NAME+"]", $linkOrForm).val() || 1;
			nextSize = $("input[name="+NEXTSIZE_NAME+"]", $linkOrForm).val() || 32;
		}
		const currentUrl = window.location.href;
		var newUrl = yada.addOrUpdateUrlParameter(currentUrl, pageParam, nextPage);
		newUrl = yada.addOrUpdateUrlParameter(newUrl, sizeParam, nextSize);
		newUrl = yada.addOrUpdateUrlParameter(newUrl, loadPreviousParam, true); // This is always true
		// Add the container id and the scroll position. 
		// We presume that the scrolling element is the parent of the update target
		const updateTargetSelector = $linkOrForm.attr("data-yadaUpdateOnSuccess");
		const $container = yada.extendedSelect($linkOrForm, updateTargetSelector).parent();
		var containerId = $container.attr("id");
		if (containerId!=null) {
			// If there is no id, there is no autoscroll (which is both easier to implement and a way to turn off the scroll behavior somehow)
			const scrollPos = $container.scrollTop();
			newUrl = yada.addOrUpdateUrlParameter(newUrl, CONTAINERID_NAME, containerId);
			newUrl = yada.addOrUpdateUrlParameter(newUrl, CONTAINERSCROLL_NAME, scrollPos);
		}
		
		history.pushState({}, "", newUrl);
	};
	
	/**
	 * If the "data-yadaPaginationHistory" attribute is present, set a new history entry.
	 * @return true if the attribute is present.
	 */
	function handlePaginationHistoryAttribute($elem, $linkOrForm) {
		var yadaPagination = $elem.attr("data-yadaPaginationHistory"); // ="pageParam, sizeParam, loadPreviousParam"
		if (yadaPagination==null) {
			return false;
		}
		if (yadaPagination=="") {
			yadaPagination=null;
		}
		const paginationParams = yada.listToArray(yadaPagination);
		yada.fixPaginationLinkHistory($linkOrForm, paginationParams[0], paginationParams[1], paginationParams[2]);
		return true;
	}

	////////////////////
	/// Modal
	
	/**
	 * Open a modal when the location.hash contains the needed value.
	 * Example: openModalOnHash('/mymodal', ['id', 'name'], '/', function(data){return !isNaN(data.id);}
	 * @param targetUrl the modal url to open via ajax; can have url parameters
	 * @param paramNames an array of request parameter names that are assigned to from the hash
	 * @param separator the values contained in the hash are separated by this character
	 * @param validator a function that returns true if the hash values are valid
	 */
	yada.openModalOnHash = function(targetUrl, paramNames, separator, validator) {
		var hashValue = document.location.hash;
		if (hashValue!=null && hashValue.length>1) {
			try {
				var data = yada.hashPathToMap(paramNames, hashValue, separator);
				if (typeof validator == "function" && validator(data)) {
					yada.ajax(targetUrl, data);
				}
			} catch(e) {
				console.error(e);
			}
		}
	}

	////////////////////
	/// Form
	
	// This is for coupled selects
	yada.enableAjaxSelectOptions = function() {
		$('.s_chain select').change(function() {
			var selectedValue = $('option:selected', this).val();
			var $triggerSelectContainer = $(this).parents('.s_chain');
			var triggerContainerId = $triggerSelectContainer.attr('id');
			if (triggerContainerId) {
				var $targetSelectContainer = $triggerSelectContainer.siblings('[data-trigger-id='+triggerContainerId+']');
				if ($targetSelectContainer) {
					var targetUrl = $targetSelectContainer.attr('data-url');
					var targetExclude = $targetSelectContainer.attr('data-exclude-id');
					var selectedOption = $targetSelectContainer.attr('data-selected-option');
					var data={triggerId : selectedValue, excludeId : targetExclude};
					yada.ajax(targetUrl, data, function(responseText) {
						$('select', $targetSelectContainer).children().remove();
						$('select', $targetSelectContainer).append(responseText);
						if (selectedOption) {
							$('select option[value="'+selectedOption+'"]', $targetSelectContainer).prop('selected', true);
						}
						$('select', $targetSelectContainer).prop('disabled', false);
					});
				}
			}
		});
	};
	
	// @Deprecated. Should use the generic modal instead of the login modal
	function openLoginModalIfPresent(responseHtml) {
		var loadedLoginModal=$(responseHtml).find("#loginModal");
		if (loadedLoginModal.length>0) {
			var currentLoginModal = $('#loginModal.in');
			if (currentLoginModal.length>0) {
				// The login modal is already open: just update the content
				var currentLoginDialog = $('#loginModalDialog', currentLoginModal);
				currentLoginDialog.replaceWith($('#loginModalDialog', loadedLoginModal));
				$('#username').focus();
			} else {
				// Open a new login modal
				const $existingModals = $(".modal.show");
				$existingModals.modal("hide"); // Remove the background too
				$existingModals.remove(); // Remove any existing modals, stick modals too because their content may need to change after login
				$("#loginModal").remove(); // Just in case
				$("body").append(loadedLoginModal);
				$("#loginModal").on('shown.bs.modal', function (e) {
					$('#username').focus();
				})
				$('#loginModal').modal('show');
			}
			yada.enableAjaxForm($('#loginForm'), null); // Login POST via Ajax
			return true;
		}
		return false;
	}
	
	/**
	 * Returns true if a node has been removed from the DOM
	 * @param $someNode a jquery HTML element
	 */
	function isRemoved($someNode) {
		return $someNode.closest("html").length==0;
	}

	// Al ritorno di un post di login, mostra eventuali notify ed esegue l'eventuale handler, oppure ricarica la pagina corrente se l'handler non c'è.
	// @Deprecated. Should use the generic modal instead of the login modal
	yada.handlePostLoginHandler = function(responseHtml, responseText) {
		var isError = yada.isNotifyError(responseHtml);
		yada.handleNotify(responseHtml);
		if (yada.postLoginHandler != null) {
			if (!isError) { // Esegue l'handler solo se non ho ricevuto una notifica di errore
				yada.postLoginHandler(responseText, responseHtml); 
			}
		} else {
			// Not good: reload or not reload is application specific
			console.error("YadaWarning: deprecated page reload after ajax login")
			// If you really need to reload the page, do it in the login form successHandler
			debugger; // Set a debugger point here otherwise the above message is lost on page reload
			yada.loaderOn();
			window.location.href=yada.removeHash(window.location.href); // Ricarico la pagina corrente (senza ripetere la post) se non ho un handler
		}
		yada.postLoginHandler = null;
	};
	
	// Apre il modal del login se è già presente in pagina.
	// handler viene chiamato quando il login va a buon fine.
	// return true se il modal è presente ed è stato aperto, false se il modal non c'è e non può essere aperto.
	// @Deprecated. Should use the generic modal instead of the login modal
	yada.openLoginModal = function(url, data, handler, type) {
		if ($('#loginModal').length>0) {
			// ?????????? A cosa servono questi postXXXX ??????????????????
			postLoginUrl = url;
			postLoginData = data;
			yada.postLoginHandler = handler;
			postLoginType = type;
			$("#loginModal").on('shown.bs.modal', function (e) {
				$('#username').focus();
			})
			$('#loginModal').modal('show');
			return true;
		}
		return false;
	}
	
	// Apre il modal del login caricandolo via ajax.
	// handler viene chiamato quando il login va a buon fine
	// @Deprecated. Should use the generic modal instead of the login modal
	yada.openLoginModalAjax = function(loginFormUrl, handler, errorTitle, errorText) {
		yada.postLoginHandler = handler;
		$.get(loginFormUrl, function(responseText, statusText) {
			var responseHtml=$(yadaAjaxResponseHtmlRoot).html(responseText);
			var loginReceived = openLoginModalIfPresent(responseHtml);
			if (!loginReceived) {
				yada.showErrorModal(errorTitle, errorText);
			}
		});
	}
	
	// Chiama la funzione javascript yadaCallback() se presente nell'html ricevuto dal server.
	// - responseHtml = l'html ricevuto dal server, creato con $("<div>").html(responseText)
	yada.callYadaCallbackIfPresent = function(responseHtml) {
		// Cerco se c'è una funzione js da eseguire chiamata "yadaCallback".
		var scriptNodes = $(responseHtml).find("script#yadaCallback");
		if (scriptNodes.length>0) {
			$('#callbackJavascript').append(scriptNodes);
			yadaCallback();
			return true;
		}
		return false;
	}	
	
	function hasNoLoader($element) {
		return $element.hasClass("noLoader") || $element.hasClass("noloader") || $element.hasClass("yadaNoLoader") || $element.hasClass("yadaNoloader") || $element.hasClass("yadanoloader");
	}

	const ajaxTriggerInViewportObserver = new IntersectionObserver(entries => {
		entries.forEach(entry => {
			if (entry.intersectionRatio > 0) {
				// console.log("firing " + $(entry.target).attr("data-yadahref"));
				ajaxTriggerInViewportObserver.unobserve(entry.target); // Fires once only
 				makeAjaxCall(null, $(entry.target));
			}
		})
	})
	
	/**
	 * Enables the triggering of ajax calls when the element is entering the viewport (or is already in the viewport).
	 * @param $element the dom section where to look for elements to enable, can be null for the entire body
	 */
	yada.enableAjaxTriggerInViewport = function($element) {
		if ($element==null || $element=="") {
			$element = $('body');
		}
		var $target = $element.parent();
		if ($target.length==0) {
			$target = $element;
		}

		$('[data-yadaTriggerInViewport]', $target).each(function() {
			var fetchUrl = $(this).attr("data-yadaHref") || $(this).attr("href");
			if (fetchUrl!=null) {
				ajaxTriggerInViewportObserver.observe(this);
				// console.log("Observing " + $(this).attr("data-yadahref"));
			}
		});
	};
	
	/**
	 * Transform links and non-submit buttons into ajax links: all anchors/buttons with a class of "yadaAjax" will be sent via ajax.
	 * @param handler a function to call upon successful link submission, can be null
	 * @param $element the element on which to enable ajax links, can be null for the entire body
	 */
	yada.enableAjaxLinks = function(handler, $element) {
		if ($element==null || $element=="") {
			$element = $('body');
		}
		var $target = $element.parent();
		if ($target.length==0) {
			$target = $element;
		}
		$('a.yadaAjax, button.yadaAjax:not([type="submit"])', $target).each(function() {
			$(this).removeClass('yadaAjax');
			yada.enableAjaxLink($(this), handler);
		});
		// Legacy
		$('.s_ajaxLink', $target).each(function() {
			$(this).removeClass('s_ajaxLink');
			yada.enableAjaxLink($(this), handler);
		});
	};
	
	/**
	 * Enables ajax on a checkbox change. Will either submit a parent form or make an ajax call directly.
	 */
	// Legacy version
	yada.enableAjaxCheckboxes = function(handler, $element) {
		if ($element==null || $element=="") {
			$element = $('body');
		}
		var $target = $element.parent();
		if ($target.length==0) {
			$target = $element;
		}
		$("input[type='checkbox'].yadaAjax", $target).each(function() {
			$(this).removeClass('yadaAjax');
			yada.enableAjaxCheckbox($(this), handler);
		});
	};
	// Legacy version
	yada.enableAjaxCheckbox = function($checkbox, handler) {
		// If array, recurse to unroll
		if ($checkbox.length>1) {
			$checkbox.each(function() {
				yada.enableAjaxCheckbox($(this), handler);
			});
			return;
		}
		// From here on the $checkbox is a single element, not an array
		$checkbox.not('.'+markerClass).change(function(e) {
			$checkbox = $(this); // Needed otherwise $checkbox could be stale (from a previous ajax replacement) 
			// If there is a parent form, submit it, otherwise make an ajax call defined on the checkbox
			var $form = $checkbox.parents("form.yadaAjaxed");
			if ($form.length>0) {
				$form.submit();
				return;
			}
			return makeAjaxCall(e, $checkbox, handler);
		})
		$checkbox.removeClass('yadaAjax');
		$checkbox.not('.'+markerClass).addClass(markerClass);
	};

	/**
	 * Enables ajax calls on select change.
	 * @param handler a function to call upon successful link submission, can be null
	 * @param $element the element on which to enable ajax, can be null for the entire body
	 */
	
	// TODO this may conflict with yada.enableAjaxInputs and should be replaced with that one if possible

	// Legacy version
	yada.enableAjaxSelects = function(handler, $element) {
		if ($element==null || $element=="") {
			$element = $('body');
		}
		var $target = $element.parent();
		if ($target.length==0) {
			$target = $element;
		}
		$('select.yadaAjax', $target).each(function() {
			$(this).removeClass('yadaAjax');
			yada.enableAjaxSelect($(this), handler);
		});
	};
	// Legacy version
	yada.enableAjaxSelect = function($select, handler) {
		// If array, recurse to unroll
		if ($select.length>1) {
			$select.each(function() {
				yada.enableAjaxSelect($(this), handler);
			});
			return;
		}
		// From here on the $select is a single element, not an array
		$select.not('.'+markerClass).change(function(e) {
			$select = $(this); // Needed otherwise $select could be stale (from a previous ajax replacement) 
			return makeAjaxCall(e, $select, handler);
		})
		$select.removeClass('yadaAjax');
		$select.not('.'+markerClass).addClass(markerClass);
	};
	
	/**
	 * Sends a link/button via ajax, it doesn't have to have class .yadaAjax.
	 * Buttons must have a yada-href attribute and not be submit buttons.
	 * Links with a "yadaLinkDisabled" class are disabled.
	 * @param $link the jquery anchor or button (could be an array), e.g. $('.niceLink')
	 * @param handler funzione chiamata in caso di successo e nessun yadaWebUtil.modalError()
	 */
	// Legacy version - see yada.enableAjaxLinks
	yada.enableAjaxLink = function($link, handler) {
		// If array, recurse to unroll
		if ($link.length>1) {
			$link.each(function() {
				yada.enableAjaxLink($(this), handler);
			});
			return;
		}
		// From here on the $link is a single anchor, not an array
		$link.not('.'+markerClass).click(function(e) {
			$link = $(this); // Needed otherwise $link could be stale (from a previous ajax replacement) 
			// Fix pagination parameters if any
			handlePaginationHistoryAttribute($link, $link);
			//
			return makeAjaxCall(e, $link, handler);
		})
		$link.removeClass('yadaAjax');
		$link.removeClass('s_ajaxLink'); // Legacy
		$link.not('.'+markerClass).addClass(markerClass);
	};
	
	/**
	 * Returns true if the current input key is listed in the data-yadaAjaxTriggerKeys attibute.
	 * Also returns true if the current event is not a key event (like the input event)
	 * Example: yada:ajaxTriggerKeys="Enter| |,"
	 * @param inputEvent the input event that has been triggered
	 * return true if the key that triggered the event is listed in 
	 * the "data-yadaAjaxTriggerKeys" when present or if that attribute is not present
	 */
	yada.isAjaxTriggerKey  = function(keyEvent) {
		const key = keyEvent.key; // https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/key/Key_Values
		if (key==null) {
			return true; // Not a key event so we can't check the key
		}
		const input = keyEvent.target;
		const $input = $(input);
		const ajaxTriggerKeys = $input.attr("data-yadaAjaxTriggerKeys");
		if (ajaxTriggerKeys==null) {
			return true; // Do ajax call on any key when no attribute present
		}
		const triggerKeys = ajaxTriggerKeys.split("|");
		for (var i=0; i<triggerKeys.length; i++) {
			if (key==triggerKeys[i]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Enables ajax calls on <input> fields that fire the "keyup" event.
	 * There is no need to pass any element because it is always registered even on dynamically added content.
	 */
	yada.enableAjaxInputs = function() {
		if (this.enableAjaxInputsDone) {
			// Prevent binding multiple event handlers because yada.enableAjaxInputs is called after each ajax call for legacy reasons
			return;
		}
		// All input fields that are either yadaAjax or data-yadaHref get handled, if they are not radio
		var selector = "input.yadaAjax:not([type=radio]), input[data-yadaHref]:not([type=radio])";
		$(document).on("keyup", selector, function(e) {
			// If "data-yadaAjaxTriggerKeys" is present, call ajax only when one of the keys is pressed.
			// If attribute not present, always call ajax
			if (yada.isAjaxTriggerKey(e)) {
				const $input = $(this);
				// Ajax calls are not executed immediately, but after a timeout that is reset each time a valid key is pressed
				yada.dequeueFunctionCall(this, function(){
					makeAjaxCall(e, $input, null, true);
				});
			}
		});
		// Prevent form submission on Enter otherwise the ajax call is not made.
		// Browsers simulate a click on submit buttons when the enter key is pressed in a form, so we check using the "yadaDoNotSubmitNow" flag.
		// This doesn't always work and may be necessary to replace submit buttons with normal buttons to prevent form submission on enter. 
		$(selector).each(function(){
			const $input = $(this);
			// Form submission by Enter keypress is allowed when the input element ajax call is not triggered by "Enter".
			// This only happens if yadaAjaxTriggerKeys is present and does not contain "Enter"
			const ajaxTriggerKeys = $input.attr("data-yadaAjaxTriggerKeys");
			if (ajaxTriggerKeys==null || yada.stringContains(ajaxTriggerKeys, "Enter")) {
				const $form = $input.closest("form").not(".yadaEnterNoSubmit");
				$form.addClass("yadaEnterNoSubmit");
				$form.on("submit", function(e){
					// The "yadaDoNotSubmitNow" flag is added when the Enter key is pressed in any input element
					// that does not call ajax when pressing Enter
					const preventSubmit = $form.data("yadaDoNotSubmitNow")==true;
					if (preventSubmit) {
						// e.stopImmediatePropagation();
						e.preventDefault(); // No submit, but exec other handlers
						$form.data("yadaDoNotSubmitNow", false);
						yada.log("Form submission prevented");
						if (ajaxCounter<1) {
							yada.loaderOff();
						}
						// return false;
					}
				});
				
				$form.on("keydown", function(keyEvent){
					if (keyEvent.key=="Enter") {
						const $target = $(keyEvent.target);
						// The target could be any control in the form, also non-ajax inputs
						if (!$target.hasClass("yadaAjax") && $target.attr("data-yadaHref")==null) {
							return; // Non-ajax element can trigger submit
						}
						// Prevent submission depending on value of yadaAjaxTriggerKeys, but only if there is a submit control
						const wouldSubmit = $("[type=submit]:enabled", $form).length>0; 
						if (!wouldSubmit) {
							// The enter key would not cause a submit, so keep going normally
							return;
						}
						const targetAjaxTriggerKeys = $target.attr("data-yadaAjaxTriggerKeys");
						if (targetAjaxTriggerKeys==null || yada.stringContains(targetAjaxTriggerKeys, "Enter")) {
							$form.data("yadaDoNotSubmitNow", true); // Let the ajax call on the input element run
						}
					}
				});
			}
		});
		// Radio buttons that do not use keyup
		selector = "input.yadaAjax[type=radio], input[data-yadaHref][type=radio]";
		$(document).on("input", selector, function(e) {
			const $input = $(this);
			makeAjaxCall(e, $input, null, true);
		});
		this.enableAjaxInputsDone = true;
		$(selector).addClass(markerClass); // Not really needed
	};
	
	/**
	 * Make an ajax call when a link is clicked, a select is chosen, a checkbox is selected etc.
	 * @param e the triggering event, can be null (for yadaTriggerInViewport)
	 * @param $element the jQuery element that triggered the ajax call
	 * @param optional additional handler to call on success
	 * @param allowDefault true to allow the default event action, if any
	 */
	function makeAjaxCall(e, $element, handler, allowDefault) {
		if (e && !allowDefault==true) {
			e.preventDefault();
		}
		if ($element.hasClass("yadaAjaxDisabled")) {
			return false;
		}
		// Call, in sequence, the handler specified in data-successHandler and the one passed to this function
		var joinedHandler = function(responseText, responseHtml) {
			showFeedbackIfNeeded($element);
			deleteOnSuccess($element);
			responseHtml = updateOnSuccess($element, responseHtml); // This removes the added root <div>
			// No: Put the responseHtml back into a div if it is not an array and not the original yadaAjaxResponseHtml
			//     Can't be done because the html is removed from the page on append()
			// if (!(responseHtml instanceof Array) && responseHtml.attr("class")!="yadaAjaxResponseHtml") {				
			// 	responseHtml = $(yadaAjaxResponseHtmlRoot).append(responseHtml);
			// }
			// responseHtml = appendOnSuccess($element, responseHtml);
			var handlerNames = $element.attr("data-yadaSuccessHandler");
			if (handlerNames===undefined) {
				handlerNames = $element.attr("data-successHandler"); // Legacy
			}
			if (handlerNames!=null) {
				// Can be a comma-separated list of handlers, which are called in sequence
				var handlerNameArray = yada.listToArray(handlerNames);
				for (var i = 0; i < handlerNameArray.length; i++) {
					yada.executeFunctionByName(handlerNameArray[i], $element, responseText, responseHtml, $element[0]);
				}
			}
			if (handler != null) {
				handler(responseText, responseHtml, $element[0]);
			}
		}
		var url = $element.attr('data-yadaHref');
		if (url==null || url=='') {
			url = $element.attr('href');
		}
		if (url==null || url=='') {
			yada.log("No url for ajax call");
			return false;
		}
		
		// Execute submit handlers if any
		if (!execSubmitHandlers($element)) {
			return false;
		}
		
		var confirmText = $element.attr("data-yadaConfirm") || $element.attr("data-confirm");
		// Create data for submission
		var data = [];
		var value = [];
		var multipart = false;
		var noLoader = hasNoLoader($element);	
		// In a select, set the data object to the selected option
		if ($element.is("select")) {
			$("option:selected", $element).each(function(){ // Could be a multiselect!
				value.push($(this).val()); // $(this) is correct here
			});
		} else if ($element.is("input")) {
			if ($element.prop('type')=="checkbox") {
				value.push($element.prop('checked')); // Always send the element value
			} else {
				value.push($element.val());
			}
		}
		// Add form data when specified with yadaFormGroup
		const yadaFormGroup = $element.attr('data-yadaFormGroup');
		if (yadaFormGroup!=null) {
			// Find all forms of the same group
			const $formGroup = $('form[data-yadaFormGroup='+yadaFormGroup+']');
			if ($formGroup.length>0) {
				multipart = $formGroup.filter("[enctype='multipart/form-data']").length > 0;
				data = multipart ? new FormData() : [];
				addAllFormsInGroup($formGroup, data);
			}
		}
		// Any yadaRequestData is also sent (see yada.dialect.js)
		const yadaRequestData = $element[0].yadaRequestData; // Object with name=value
		data = mergeData(data, yadaRequestData);
		// Add element value
		if (value.length>0) {
			const name = $element.attr("name") || "value"; // Parameter name fallback to "value" by default
			const toAdd = {};
			toAdd.name = name;
			toAdd.value = value;
			data = mergeData(data, toAdd);
		}
		if (!multipart) {
			data = $.param(data);
		}	
		//
		if (confirmText!=null && confirmText!="") {
			var title = $element.attr("data-yadaTitle");
			var okButton = $element.attr("data-yadaOkButton") || $element.attr("data-okButton") || yada.messages.confirmButtons.ok;
			var cancelButton = $element.attr("data-yadaCancelButton") || $element.attr("data-cancelButton") || yada.messages.confirmButtons.cancel;
			var okShowsPreviousModal = $element.attr("data-yadaOkShowsPrevious")==null || $element.attr("data-yadaOkShowsPrevious")=="true";
			yada.confirm(title, confirmText, function(result) {
				if (result==true) {
					yada.ajax(url, data, joinedHandler==null?joinedHandler:joinedHandler.bind($element), null, getTimeoutValue($element), noLoader);
				}
			}, okButton, cancelButton, okShowsPreviousModal);
		} else {
			yada.ajax(url, data, joinedHandler==null?joinedHandler:joinedHandler.bind($element), null, null, noLoader);
		}
		return true; // Run other listeners
	}
	
	function getTimeoutValue($element) {
		var timeout = $element.attr('data-yadaTimeout');
		if (timeout==null) {
			timeout = $element.attr('data-timeout'); // Legacy
		}
		return timeout;
	}
	
	/**
	 * 
	 * @param $element the link or the form
	 * @returns true if "data-yadaDeleteOnSuccess" was present
	 */
	function deleteOnSuccess($element) {
		// Delete a (parent) element
		// The target can be a parent when the css selector starts with parentSelector (currently "yadaParents:").
		// The selector can be multiple, separated by comma. 
		var deleteSelector = $element.attr("data-yadaDeleteOnSuccess");
		if (deleteSelector != null) {
			var selectors = deleteSelector.split(',');
			// If we delete the $element first, then any following selected elements may not match when relative to the $element.
			// We therefore first get all selected element then delete them.
			const toDelete = [];
			
			for (var count=0; count<selectors.length; count++) {
				var selector = selectors[count];
				toDelete.push(yada.extendedSelect($element, selector));
			}
			for (var count=0; count<toDelete.length; count++) {
				toDelete[count].remove();
			}
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param $element the link or the form
	 * @param responseHtml jquery object received from the ajax call
	 * @returns the jQuery HTML that has been added to the page, which will be a clone 
	 *		    of responseHtml or the original responseHtml when no update has been made.
	 *		    In case of multiple replacements, an array will be returned.
	 */
	function updateOnSuccess($element, responseHtml) {
		// If "yadaUpdateOnSuccess" is set, replace its target; if it's empty, replace the original link.
		// The target can be a parent when the css selector starts with parentSelector (currently "yadaParents:").
		// The selector can be multiple, separated by comma. The replacement can be multiple, identified by yadaFragment
		// return postprocessOnSuccess($element, responseHtml, "data-yadaUpdateOnSuccess", $.fn.replaceWith);
		return postprocessOnSuccess($element, responseHtml, "data-yadaUpdateOnSuccess", $.fn.replaceWith);
		/*
		var updateSelector = $element.attr("data-yadaUpdateOnSuccess");
		if (updateSelector == null) {
			return responseHtml;
		}
		// Clone so that the original responseHtml is not removed by replaceWith.
		// All handlers are also cloned.
		var $replacement = responseHtml.children().clone(true, true); // Uso .children() per skippare il primo div inserito da yada.ajax()
		var $return = $replacement;
		var selectors = updateSelector.split(',');
		var $replacementArray = null;
		if (selectors.length>1) {
			// yadaFragment is used only when there is more than one selector, otherwise the whole result is used for replacement
			$replacementArray = $(".yadaFragment", responseHtml);
			if ($replacementArray.length==0) {
				$replacementArray = $("._yadaReplacement_", responseHtml); // Legacy
			}
		}
		if ($replacementArray!=null && $replacementArray.length>1) {
			$return = [];
		}
		var fragmentCount = 0;
		var focused = false;
		for (var count=0; count<selectors.length; count++) {
			var selector = selectors[count];
			if ($replacementArray!=null && $replacementArray.length>0) {
				// Clone so that the original responseHtml is not removed by replaceWith.
				// All handlers are also cloned.
				$replacement = $replacementArray.eq(fragmentCount).clone(true, true);
				if (count==0 && $replacementArray.length==1) {
					$return = $replacement;
				} else {
					$return.push($replacement);
	}
				// When there are more selectors than fragments, fragments are cycled from the first one
				fragmentCount = (fragmentCount+1) % $replacementArray.length;
			}
			yada.extendedSelect($element, selector).replaceWith($replacement);
			if (!focused) {
				// Focus on the first result element with data-yadaAjaxResultFocus
				const $toFocus = $("[data-yadaAjaxResultFocus]:not([readonly]):not([disabled])", $replacement);
				if ($toFocus.length>0) {
					$toFocus.get(0).focus();
					focused=true;
				}
			}
		}
		return $return;
		*/
	}
	
	/**
	 * 
	 * @param $element the link or the form
	 * @param responseHtml jquery object received from the ajax call
	 * @returns the jQuery HTML that has been added to the page, which will be a clone 
	 *		    of responseHtml or the original responseHtml when no update has been made.
	 *		    In case of multiple appends, an array will be returned.
	 * @deprecated use $append() in the selector instead
	 */
	 /*
	function appendOnSuccess($element, responseHtml) {
		// If "yadaAppendOnSuccess" is set, append to its target; if it's empty, append to the original element.
		// The target can be a parent when the css selector starts with parentSelector (currently "yadaParents:").
		// The selector can be multiple, separated by comma. The appended HTML can be multiple, identified by yadaFragment
		return postprocessOnSuccess($element, responseHtml, "data-yadaAppendOnSuccess", $.fn.append);
	}
	*/

	/**
	 * This function performs either an update or an append (or more in the future) depending on the parameters.
	*/
	function postprocessOnSuccess($element, responseHtml, attributeName, jqueryFunction) {
		var selector = $element.attr(attributeName);
		if (selector == null) {
			return responseHtml;
		}
		// Clone so that the original responseHtml is not removed by appending.
		// All handlers are also cloned.
		var $replacement = responseHtml.children().clone(true, true); // Uso .children() per skippare il primo div inserito da yada.ajax()
		initObservers($replacement);
		var $return = $replacement;
		var selectors = selector.split(',');
		var $replacementArray = null;
		// Handle multiple selectors in the update/append attribute
		if (selectors.length>1) {
			// yadaFragment is used only when there is more than one selector, otherwise the whole result is used for replacement
			$replacementArray = $(".yadaFragment", responseHtml);
			if ($replacementArray.length==0) {
				$replacementArray = $("._yadaReplacement_", responseHtml); // Legacy
			}
		}
		if ($replacementArray!=null && $replacementArray.length>1) {
			$return = [];
		}
		//
		var fragmentCount = 0;
		var focused = false;
		for (var count=0; count<selectors.length; count++) {
			var selector = selectors[count].trim();
			// Handle multiple selectors in the update/append attribute
			if ($replacementArray!=null && $replacementArray.length>0) {
				// Clone so that the original responseHtml is not removed by replaceWith.
				// All handlers are also cloned.
				$replacement = $replacementArray.eq(fragmentCount).clone(true, true);
				initObservers($replacement);
				if (count==0 && $replacementArray.length==1) {
					$return = $replacement;
				} else {
					$return.push($replacement);
				}
				// When there are more selectors than fragments, fragments are cycled from the first one
				fragmentCount = (fragmentCount+1) % $replacementArray.length;
			}
			// Detect the jquery funcion used in the selector, if any
			var jqueryFunction = $.fn.replaceWith; // Default
			var isReplace = true;
			var jqueryFunctions = [
				{"jqfunction": $.fn.replaceWith, "prefix": "$replaceWith"},
				{"jqfunction": $.fn.replaceWith, "prefix": "$replace"},		// $replace() is an alias for $replaceWith()
				{"jqfunction": $.fn.append, "prefix": "$append"},
				{"jqfunction": $.fn.prepend, "prefix": "$prepend"}
				// More can be added
			]
			for (var i = 0; i < jqueryFunctions.length; i++) {
				const toCheck = jqueryFunctions[i];
				if (yada.startsWith(selector, toCheck.prefix + "(") && selector.indexOf(")") > toCheck.prefix.length) {
					jqueryFunction = toCheck.jqfunction;
					selector = yada.extract(selector, toCheck.prefix + "(", ")");
					if (!yada.startsWith(toCheck.prefix, "$replace")) {
						isReplace = false; // Not a replace function
					}
					break;
				}
			}
			// Call the jquery function
			jqueryFunction.call(yada.extendedSelect($element, selector), $replacement);
			if (isReplace && (selector == null || selector.trim()=="")) {
				// The original element has been replaced so we need to change it or following selectors won't work anymore
				$element = $replacement;
			}
			if (!focused) {
				// Focus on the first result element with data-yadaAjaxResultFocus
				const $toFocus = $("[data-yadaAjaxResultFocus]:not([readonly]):not([disabled])", $replacement);
				if ($toFocus.length>0) {
					$toFocus.get(0).focus();
					focused=true;
				}
			}
		}
		return $return;
	}	
	
	/**
	 * Show a checkmark fading in and out
	 * @param $element
	 * @returns
	 */
	function showFeedbackIfNeeded($element) {
		var showFeedback = $element.attr("data-yadaShowAjaxFeedback");
		if (showFeedback!=undefined) {
			// Check if the HTML is in page already, else insert it
			const $feedbackElement = $("#yadaAjaxFeedback");
			if ($feedbackElement.length==0) {
				$("body").append("<div id='yadaAjaxFeedback' class='yadaAjaxFeedbackOk'><span class='yadaIcon yadaIcon-ok'></span></div>");
			}
			$("#yadaAjaxFeedback").fadeIn(200, function() {
				$("#yadaAjaxFeedback").fadeOut(800);
			});
		}
	}

	/**
	 * Transform forms into ajax forms: all forms with a class of "yadaAjax" will be sent via ajax.
	 * @param handler a function to call upon successful form submission. It can also be specified as a data-successHandler attribute on the form
	 * @param $element the element on which to enable ajax forms, can be null for the entire body
	 */
	yada.enableAjaxForms = function(handler, $element) {
		if ($element==null || $element=="") {
			$element = $('body');
		}
		var $target = $element.parent();
		if ($target.length==0) {
			$target = $element;
		}
		// This needs to be done every time because the ajax result might have new submit buttons inside, without a form wrapping them
		yada.enableSubmitButtons($target);
		//
		$('form.yadaAjax', $target).each(function() {
			$(this).removeClass('yadaAjax');
			yada.enableAjaxForm($(this), handler);
		});
		// Legacy
		$('.s_ajaxForm', $target).each(function() {
			$(this).removeClass('s_ajaxForm');
			yada.enableAjaxForm($(this), handler);
		});
		
		
		// TODO questo aggiunge la posizione verticale del modal, ma non credo serva per un form ajax
		//			beforeSubmit: function(formDataArray) {
		//					// Aggiunge la posizione verticale del modal tra i parametri del form
		//					formDataArray.push({name: 'ajaxModalScrollTop', value: Math.round($("#ajaxModal").scrollTop())})
		//					ldm.loaderOn();
		//				},
	};
	
	yada.enableSubmitButtons = function($element) {
		$element.find("button[type='submit']").not('.yadaClickedButtonHandler').each(function() {
			$(this).click(function() {
				clickedButton = this;
			});
			$(this).addClass('yadaClickedButtonHandler');
		});
	}
	
	/**
	 * Disable the ajax submission of a form that was previously initialised as an ajax form
	 * @param $form the jquery form (could be an array), e.g. $('.niceForm')
	 */
	yada.disableAjaxForm = function($form) {
		$form.off("submit");
		$form.removeClass(markerClass);
	}
	
	/**
	 * Execute any comma-separated list of sumbit handlers. Each can also be an inline function (with or without function(){} declaration).
	 * Execution stops after the first handler that returns false.
	 */
	function execSubmitHandlers($element) {
		// Invoke any submit handlers either on form, submit button or any ajax-enabled element
		var submitHandlerNames = $element.attr("data-yadaSubmitHandler");
		var submitHandlerNameArray = yada.listToArray(submitHandlerNames);
		for (var z = 0; z < submitHandlerNameArray.length; z++) {
			const result = yada.executeFunctionByName(submitHandlerNameArray[z], $element);
			if (result==false) {
				return false; // Do not send the form
			}
		}
		return true;
	}
	
	/**
	 * Merge some new data into an existing object
	 * @param data the object that receives the new data, can be either Array of objects or FormData  
	 * @param mergeFrom the object that contains new data like {product: "shoe", quantity: 2}, or null
	 * @return the 'data' object with new data
	*/
	function mergeData(data, mergeFrom) {
		if (mergeFrom==null) {
			return data;
		}
		const multipart = data instanceof FormData;
		if (!multipart && !(data instanceof Array)) {
			console.error("YadaError: data should be Array or FormData in mergeData().")
			return data;
		}
		Object.keys(mergeFrom).forEach(function(name) {
  			const value = mergeFrom[name];
	    	if (multipart) {
		        data.set(name, value); // Add data with no duplicates, overwriting previous
			} else {
				// Add data with no duplicates, overwriting previous
				const obj = {};
				obj[name] = value;
				$.extend(true, data, [obj]);
			}
		});
		return data;
	}
	
	/**
	 * Adds to data all fields in all the forms in the group, optionally excluding one of them
	 * @param $formGroup an array of jquery forms from which input data should be gathered
	 * @param data FormData or an array of objects (created with $.serializeArray()) that may already hold some form data and will contain all the gathered data
	 * @param $form some jquery form to exclude (optional)
	 */
	function addAllFormsInGroup($formGroup, data, $formToExclude) {
		const multipart = data instanceof FormData;
		if (!multipart && !(data instanceof Array)) {
			console.error("YadaError: data should be Array or FormData in addAllFormsInGroup().")
			return;
		}
		$formGroup.each(function() {
			var $eachForm = $(this);
			if (!$eachForm.is($formToExclude)) {
				if (multipart) {
					var eachFormdata = new FormData(this);
					// Can't use for - of with the current minifyjs version, so trying with a while loop
					//	for (var pair of eachFormdata.entries()) {
					//		data.append(pair[0], pair[1]);
					//	}
					var iterator = eachFormdata.entries();
					var iterElem = iterator.next();
				    while ( ! iterElem.done ) {
				    	var pair = iterElem.value;
				    	// const newData = {};
				    	// newData[pair[0]] = pair[1];
				    	// data = mergeData(data, newData); // Add data with no duplicates, keeping first value
				        data.set(pair[0], pair[1]); // Add data with no duplicates, overwriting previous
				        iterElem = iterator.next();
				    }
				} else {
					// mergeData(data, $eachForm.serializeArray()); // Add data with no duplicates, keeping first value
					$.extend(true, data, $eachForm.serializeArray()); // Add data with no duplicates, overwriting previous
				}
			}
		});
	}

	/**
	 * Sends a form via ajax, it doesn't have to have class .yadaAjax.
	 * @param $form the jquery form (could be an array), e.g. $('.niceForm')
	 * @param successHandler funzione chiamata in caso di successo e nessun yadaWebUtil.modalError()
	 */
	yada.enableAjaxForm = function($form, handler) {
		// If array, recurse to unroll
		if ($form.length>1) {
			$form.each(function() {
				yada.enableAjaxForm($(this), handler);
			});
			return;
		}
		yada.enableSubmitButtons($form);
		// From here on the $form is a single anchor, not an array.
		// Can't use document.activeElement to find the clicked button because of the possible "confirm" dialog
		// http://stackoverflow.com/a/33882987/587641
//		$form.find("button[type='submit']").not('.yadaClickedButtonHandler').each(function() {
//			$(this).click(function() {
//				clickedButton = this;
//			});
//			$(this).addClass('yadaClickedButtonHandler');
//		});
		
		// Set the confirm handlers on the form itself if no button has it
		$form.filter('[data-yadaConfirm]').not('.'+markerClass).each(function() {
			var $thisForm = $(this);
			// Check if there is no submit button with a data-yadaConfirm attribute
			var $button = $thisForm.find('button[type="submit"][data-yadaConfirm]');
			if ($button.length==0) {
				$thisForm.submit(function(e){
					if ($thisForm[0]['yadaConfirmed']==true) {
						$thisForm[0]['yadaConfirmed']=false;
						return; // Continue submission
					}
					$thisForm = $(this); // just in case
					var confirmText = $thisForm.attr("data-yadaConfirm");
					if (confirmText!=null && confirmText!="") {
						e.preventDefault(); // Stop form submission
						var title = $button.attr("data-yadaTitle");
						var okButton = $button.attr("data-yadaOkButton") || $button.attr("data-okButton") || yada.messages.confirmButtons.ok;
						var cancelButton = $button.attr("data-yadaCancelButton") || $button.attr("data-cancelButton") || yada.messages.confirmButtons.cancel;
		    			yada.confirm(title, confirmText, function(result) {
		    				if (result==true) {
		    					$thisForm[0]['yadaConfirmed']=true;
		    					$thisForm.submit();
		    				}
		    			}, okButton, cancelButton);
		    			return false;
		    		};
				});
			}
		});

		$form.not('.'+markerClass).submit(function(e) {
			// Only ajax forms enter here
			var $form=$(this); // Needed to overwrite the outside variable with the current form, otherwise we may handle the wrong form (because of cloning)
			// Invoke any submit handlers
			if (execSubmitHandlers($form)==false) {
				return false; // Do not send the form
			}
			//
			if (e.isDefaultPrevented()) {
				return; // Do not send the form - probably a cancel has been made
			}
//			// Form alias: submit another form after merging the current form children
//			var formAliasSelector = $form.attr('data-yadaFormAlias');
//			if (formAliasSelector!=null) {
//				var $formAliasArray = $(formAliasSelector);
//				if ($formAliasArray!=null && $formAliasArray.length>0) {
//					var formAlias = $formAliasArray[0];
//					if (formAlias.nodeName.toLowerCase()=="form") {
//						// Replace the current form with a new form composed of all input elements
//						var $newform=$(formAlias).clone(true);
//						$form.children().appendTo($newform);
//						$form = $newform; // Work on the merged form
//					}
//				}
//			}
			var $formGroup = $form;
			// Check if this form belongs to a yadaFormGroup
			var yadaFormGroup = $form.attr('data-yadaFormGroup');
			if (yadaFormGroup!=null) {
				// Find all forms of the same group
				$formGroup = $('form[data-yadaFormGroup='+yadaFormGroup+']');
			}
			// If the form is marked as markerAjaxButtonOnly do not submit it via ajax unless the clicked button is marked with 'yadaAjax'
			if ($form.hasClass(markerAjaxButtonOnly)) {
				if (clickedButton==null || !$(clickedButton).hasClass('yadaAjax')) {
					yada.addFormGroupFields($form, $formGroup); // Non-ajax submit
					// In any case, let it continue with the submit
					return; // Do a normal submit
				}
			}
			e.preventDefault(); // From now on the form can only be ajax-submitted
			var noLoader = hasNoLoader($form);
			var action = $(this).attr('action');
			// Check if it must be a multipart formdata
			var multipart = $form.attr("enctype")=="multipart/form-data";
			// If CKEditor is being used, ensure all fields are valid (copies from ck-managed-fields to normal form fields)
			$(".ck-editor__editable").each(function(){
				this.ckeditorInstance.updateSourceElement();
			});
			// Using FormData to send files too: https://developer.mozilla.org/en-US/docs/Web/API/FormData/FormData
			var data = multipart ? new FormData(this) : $(this).serializeArray();
			// Add data from the form group if any
			if ($formGroup.length>1) {
				addAllFormsInGroup($formGroup, data, $form);
			}
			// Add data from any child form recursively, if any
			var $childForm = $form[0]['yadaChildForm'];
			while ($childForm != null) {
				if (multipart) {
					var childFormData = new FormData($childForm);
					// Can't use for - of with the current minifyjs version, so trying with a while loop
					//	for (var pair of eachFormdata.entries()) {
					//		data.append(pair[0], pair[1]);
					//	}
					var iterator = childFormData.entries();
					var iterElem = iterator.next();
				    while ( ! iterElem.done ) {
				    	var pair = iterElem.value;
				        data.append(pair[0], pair[1]);
				        iterElem = iterator.next();
				    }
				} else {
					$.merge(data, $childForm.serializeArray());
				}
				$childForm = $childForm[0]['yadaChildForm'];
			}
			
			var buttonName = null;
			var buttonValue = null;
			var buttonHistoryAttribute = false;
			if (clickedButton!=null) {
				// Invoke any submit handlers
				if (execSubmitHandlers($(clickedButton))==false) {
					return false; // Do not send the form
				}
				//
				buttonName = $(clickedButton).attr("name");
				buttonValue = $(clickedButton).attr("value") || "";
				if (multipart && buttonName!=null && !data.has(buttonName)) {
					data.append(buttonName, buttonValue);
				} else if (!multipart && buttonName!=null && data[buttonName]==null) {
					data.push({name: buttonName, value: buttonValue});
				}
				var buttonAction = $(clickedButton).attr("formaction");
				if (buttonAction!=null) {
					action = buttonAction;
				}
				// Either the form or the button can have a noLoader flag
				noLoader |= hasNoLoader($(clickedButton));
				// Pagination history
				buttonHistoryAttribute = handlePaginationHistoryAttribute($(clickedButton), $(clickedButton).closest("form"));
			}
			if (!multipart) {
				data = $.param(data);
			}
			// Call, in sequence, the handler specified in data-successHandler and the one passed to this function.
			// Extend the handler to include form and button parameters
			var localClickedButton = clickedButton; // Create a closure otherwise the clicked button is lost
			var joinedHandler = function(responseText, responseHtml) {
				showFeedbackIfNeeded($form);
				if ($(localClickedButton).attr("data-yadaUpdateOnSuccess")!=null) {
					responseHtml = updateOnSuccess($(localClickedButton), responseHtml);
				} else {
					responseHtml = updateOnSuccess($form, responseHtml);
				}
				/*
				if ($(localClickedButton).attr("data-yadaAppendOnSuccess")!=null) {
					responseHtml = appendOnSuccess($(localClickedButton), responseHtml);
				} else {
					responseHtml = appendOnSuccess($form, responseHtml);
				}
				*/
				var formHandlerNames = $form.attr("data-yadaSuccessHandler");
				if (formHandlerNames===undefined) {
					formHandlerNames = $form.attr("data-successHandler"); // Legacy
				}
				// var dataHandler = window[formHandlerName];
				var buttonHandlerNames = $(localClickedButton).attr("data-yadaSuccessHandler");
				// var buttonDataHandler = window[buttonHandlerName];
				// The button handler has precedence over the form handler, which is called only if the former returns true
				// from all handlers.
				var runFormHandler = true;
				if (buttonHandlerNames != null) {
					// Can be a comma-separated list of handlers, which are called in sequence
					var handlerNameArray = yada.listToArray(buttonHandlerNames);
					for (var i = 0; i < handlerNameArray.length; i++) {
						runFormHandler &= yada.executeFunctionByName(handlerNameArray[i], $form, responseText, responseHtml, this, localClickedButton);
					}
				}
				if (runFormHandler == true && formHandlerNames!=null) {
					// Can be a comma-separated list of handlers, which are called in sequence
					var handlerNameArray = yada.listToArray(formHandlerNames);
					for (var i = 0; i < handlerNameArray.length; i++) {
						yada.executeFunctionByName(handlerNameArray[i], $form, responseText, responseHtml, this, localClickedButton);
					}
				}
				if (handler != null) {
					handler(responseText, responseHtml, this, localClickedButton);
				}
				// Deletion must be done later or it could prevent other actions from working when the target is self
				var deleted = deleteOnSuccess($(localClickedButton));
				if (!deleted) {
					deleteOnSuccess($form);
				}
			};
			var method = $form.attr('method') || "POST";
			
			if (!buttonHistoryAttribute) {
				handlePaginationHistoryAttribute($form, $form);
			}
			
			yada.ajax(action, data, joinedHandler.bind(this), method, getTimeoutValue($form), noLoader);
			clickedButton = null;
			return false; // Important so that the form is not submitted by the browser too
		}) // submit()
		
		// Set the confirm handlers on form buttons
	    $form.not('.'+markerClass).find("button[type='submit']").each(function() {
	    	var $button = $(this);
	    	var confirmText = $button.attr("data-yadaConfirm") || $button.attr("data-confirm");
	    	if (confirmText!=null && confirmText!="") {
	    		var title = $button.attr("data-yadaTitle");
	    		var okButton = $button.attr("data-yadaOkButton") || $button.attr("data-okButton") || yada.messages.confirmButtons.ok;
	    		var cancelButton = $button.attr("data-yadaCancelButton") || $button.attr("data-cancelButton") || yada.messages.confirmButtons.cancel;
	    		$button.click(function() {
	    			$button = $(this); // Needed otherwise $button could be stale (from a previous ajax replacement) 
	    			yada.confirm(title, confirmText, function(result) {
	    				if (result==true) {
	    					$button.off("click");
	    					$button.click();
	    					// No $form.submit(); because of the button name that has to be preserved (see clickedButton above)
	    					// TODO/BUG if the submit button contains an <input>, that value will not be included in $(form).serializeArray() and will not be sent
	    					// This only happens when the form is sent with $button.click(): its value is sent correctly when no confirm dialog is used.
	    					// The workaround is to set the value of <input> on the submit button itself using name= and value= attributes
	    				}
	    			}, okButton, cancelButton);
	    			return false; // Stop form submission
	    		});
	    	}
	    });
		$form.not('.'+markerClass).addClass(markerClass);
	};
	
	function showFullPage(html) {
		document.open();
		document.write(html);
		document.close();
	}
	
	/**
	 * Download ajax-received data
	 */
	yada.downloadData = function(data, filename, mimeType) {
		/*if (filename==null || filename.trim()=="") {
			filename = ""
		}*/
		var blob = new Blob([data], {type : mimeType});
		yada.downloadBlob(blob, filename);
	}
	
	/**
	 * Download ajax-received data
	 */
	yada.downloadBlob = function(blob, filename) {
		var link = document.createElement("a");
		link.href = URL.createObjectURL(blob);
		link.download = filename;
		link.style="display: none;";
		document.body.appendChild(link);
		link.click();
	}
	
	/**
	 * Esegue una get/post ajax passando data (stringa od oggetto). Gestisce il caso che sia necessario il login.
	 * Il metodo chiamato lato java può ritornare un notify chiamando yadaWebUtil.modalOk() o anche yadaWebUtil.modalError() etc.
	 * In caso di notify di un errore, l'handler non viene chiamato.
	 * @param url target url
	 * @param data data to send, string or object. Can be null. Json objects are converted with JSON.stringify and given a specific content-type.
	 *        To send a multipart request, data must be a FormData.
	 * @param successHandler(responseText, responseHtml);) funzione chiamata in caso di successo e nessun yadaWebUtil.modalError(). Viene chiamata anche in caso di errore se il suo flag executeAnyway è true
	 * @param method "POST" per il post oppure null o "GET" per il get
	 * @param timeout milliseconds timeout, null for default (set by the browser)
	 * @param hideLoader true for not showing the loader
	 * @param asJson true to send a JSON.stringify(data) with "application/json;charset=UTF-8"
	 * @param responseType set the response type, for example "blob" for downloading binary data (e.g. a pdf file)
	 */
	yada.ajax = function(url, data, successHandler, method, timeout, hideLoader, asJson, responseType) {
		if (successHandler=="GET" || successHandler=="POST") {
			console.error("YadaError: you are forgetting the successHandler in the yada.ajax call; use null for no handler.")
			return;
		}
		if (method==null) {
			method="GET"
		}
		if (timeout==null) {
			timeout=0; // Default
		}
		var processData = !(data instanceof FormData);  // http://stackoverflow.com/a/8244082/587641
		var contentType = undefined;
		if (asJson==true) {
			processData = false;
			contentType = "application/json;charset=UTF-8";
			data = JSON.stringify(data);
		} else {
			contentType = data instanceof FormData ? false : contentType;
		}
		if (hideLoader==true) {
			yada.loaderOff();
		} else {
			yada.loaderOn();
		}
		var xhrFields = {};
		if (responseType!=null) {
			xhrFields.responseType = responseType;
		}
		// Call the server
		ajaxCounter++;
		$.ajax({
			type: method,
			url: url,
			data: data,
			processData: processData,
			contentType: contentType,
			xhrFields: xhrFields,
			error: function(jqXHR, textStatus, errorThrown ) { 
				ajaxCounter--;
				if (ajaxCounter<1) {
					yada.loaderOff();
				}
				// textStatus is "error", "timeout", "abort", or"parsererror"
				var responseText = jqXHR.responseText!= null ? jqXHR.responseText.trim() : jqXHR.responseText;
				if (jqXHR.status==503 && responseText!=null && yada.startsWith(responseText, "<html")) {
					showFullPage(responseText);
					return;
				}
				if (textStatus==="timeout") {
					yada.showErrorModal(yada.messages.connectionError.title, yada.messages.connectionError.message);
				} else if (errorThrown==="Forbidden") {
					yada.showErrorModal(yada.messages.forbiddenError.title, yada.messages.forbiddenError.message);
				} else {
					var title = yada.messages.serverError.title;
					var message = extractError(responseText);
					if (message==null || message=="") {
						message = yada.messages.serverError.message;
					}
					yada.showErrorModal(title, message + (textStatus!=null&&textStatus!='error'?' ('+textStatus+')':''));
				}
			},
			success: function(responseText, statusText, jqXHR) {
				ajaxCounter--;
				var responseTrimmed = "";
				var responseObject = null;
				if (responseText instanceof Blob) {
					var contentDisposition = jqXHR.getResponseHeader("Content-Disposition");
					var filename = yada.getAfter(contentDisposition, "filename=");
					yada.downloadBlob(responseText, filename);
					if (ajaxCounter<1) {
						yada.loaderOff(); // Hide the loader only if there are no other ajax calls running
					}
					return;
				}
				if (typeof responseText == "string") {
					responseTrimmed = responseText.trim();
				} else if (typeof responseText == "object") {
					responseObject = responseText;
				}
				if (yada.startsWith(responseTrimmed, "/yada/")) {
					console.warn("Yada path detected in ajax result: you may need to remove @ResponseBody");
				}
				if (yada.showAjaxErrorIfPresent(responseTrimmed, statusText, responseObject)==true) {
					if (ajaxCounter<1) {
						yada.loaderOff();
					}
					return;
				}
				if ("reload" == responseTrimmed) {
					yada.reload();
					return;
				}
				if (yada.startsWith(responseTrimmed, "{\"redirect\":")) {
					const newTab = ajaxRedirect(responseTrimmed);
					if (!newTab) {
						// Redirect didn't open a new tab, so no need to keep handling ajax for the current page
						return;
					}
					// The redirect opened a new tab so we need to keep handling the current one
				}
				// Putting the returned HTML inside a <div> so that $find() works when multiple root elements are returned. 
				// - not sure it is a good idea but legacy code needs it now.
				// The bad thing is that the enclosing div is stripped when updateOnSuccess is called, so the successHandler
				// can receive both versions (with or without root div) depending on the presence of the updateOnSuccess call.
				// The reason for stripping it is that "replaceWith" and other successHandler functions move the children from the
				// added top <div> element, so it can't be returned anyway because it would be empty.
				var responseHtml=$(yadaAjaxResponseHtmlRoot).html(responseTrimmed);
				//
				const getOut = doDeprecatedStuff(responseTrimmed, responseHtml, responseText, url, data, successHandler, method);
				if (getOut) {
					return;
				}
				//
				// Se è stato ritornato un confirm, lo mostro e abilito l'esecuzione dell'ajax e dell'handler
				if (yada.handleModalConfirm(responseHtml, url, data, successHandler, method)) {
					if (ajaxCounter<1) {
						yada.loaderOff();
					}
					return;
				}

				// Always initialize all handlers on the returned content
				yada.initHandlersOn(responseHtml);

				// Il successHandler viene eseguito solo se non c'è un errore, oppure se il flag executeAnyway è true
				if (successHandler != null) {
					if (!yada.isNotifyError(responseHtml) || successHandler.executeAnyway==true) {
						// yada.initAjaxHandlersOn(responseHtml);
						// Non c'era un login, eseguo l'handler, se passato
						successHandler(responseText, responseHtml);
						// Keep going...
					}
				}
				// If it is a full page, overwrite the current one. The class .yadafullPage must not be on the body.
				// No: The result is a full page if it has "<!doctype" in the first 50 characters
				// This is not true because I can return a "<!doctype" via ajax
				// const isFullPage = responseText.substring(0, 50).toLowerCase().indexOf("<!doctype") > -1;
				if ($('.yadafullPage', responseHtml).length>0 || $('.s_fullPage', responseHtml).length>0) {
					showFullPage(responseText);
					if (ajaxCounter<1) {
						yada.loaderOff();
					}
					return;
				}

				// Per mostrare una notification al ritorno dalla get, basta che il Controller ritorni "/yada/modalNotify"
				// dopo aver chiamato ad esempio yadaWebUtil.modalOk()
				var notify=yada.handleNotify(responseHtml);
				if (notify) {
					return;
				}
				// Open any other modal, excluding any embedded confirm modal
				var $loadedModalDialog=$(responseHtml).find(".modal > .modal-dialog").first();
				if ($loadedModalDialog.length==1) {
					handleAjaxLoadedModal($loadedModalDialog, responseHtml, responseText);
					return;
				}
				
				// If the result is "closeModal", close all open modals
				if (responseTrimmed == 'closeModal') {
					$(".modal:visible").modal('hide');
				}
				if (ajaxCounter<1) {
					yada.loaderOff();
				}
				// End of ajax success
			},
			timeout: yada.devMode?0:timeout,
			traditional: true, // Serve per non avere id[] : '12' ma id : '12'
			xhr: function() {
				// Changes the bootstrap progress bar width
				$(".loader .progress-bar").css("width", 0);
				var xhr = $.ajaxSettings.xhr() ;
				xhr.upload.onprogress = function(evt){
					$(".loader .progress-bar").css("width", evt.loaded/evt.total*100+"%");
				} ;
				// xhr.upload.onload = function(){ console.log('DONE!') } ;
				return xhr ;
			}
		});
		
	}

	/**
	 * Handles ajax redirects
	 * @param {*} responseTrimmed 
	 * @returns true if the redirect opened a new tab
	 */
	function ajaxRedirect(responseTrimmed) {
		var redirectObject = JSON.parse(responseTrimmed);
		// Get the redirect url and remove any "redirect:" prefix from the url
		var targetUrl = yada.getAfter(redirectObject.redirect, "redirect:");
		if (redirectObject.newTab!="true") {
			const currentServer = window.location.origin; // https://www.example.com:8080
			const redirectServer = yada.getServerAddress(targetUrl);
			const currentPathSearch = window.location.pathname + window.location.search;
			const redirectPathSearch = yada.removeHash(targetUrl);
			const currentHashValue = yada.getHashValue(window.location.hash); // '' or 'value'
			const redirectHashValue = yada.getHashValue(targetUrl);
			window.location.href=targetUrl;
			// When only the #anchor changes between current and new url, browsers
			// might not reload the page so we force a reload
			if (currentServer==redirectServer || redirectServer=='') {
				if (currentPathSearch==redirectPathSearch)	{
					// Automatic reloading only happens when there are no hashes
					// or when the current hash is removed.
					// So we force a reload only when a hash is added/modified.
					if (redirectHashValue!='') {
						window.location.reload(true);
					}
				}					
			}
			return false; // Needed to prevent flashing of the loader
		} else {
			if (ajaxCounter<1) {
				yada.loaderOff();
			}
			var win = window.open(targetUrl, '_blank');
			if (win) {
				// Browser has allowed it to be opened
				win.focus();
			} else {
				// Browser has blocked it
				alert('Please allow popups for this website');
			}
		}
		return true;
	}

	function handleAjaxLoadedModal($loadedModalDialog, responseHtml, responseText) {
		$("#loginModal").remove(); // TODO still needed?
		// A modal was returned. Is it a "sticky" modal?
		var stickyModal = $loadedModalDialog.hasClass(yada.stickyModalMarker);
		
		// Remove any currently downloaded modals (markerAjaxModal) if they are open and not sticky
		var $existingModals = $(".modal.show."+markerAjaxModal+":has(.modal-dialog:not(."+yada.stickyModalMarker+"))");
		if ($existingModals.length==0) {
			// Try Bootstrap 3 selector
			$existingModals = $(".modal.in."+markerAjaxModal+":has(.modal-dialog:not(."+yada.stickyModalMarker+"))");
		}
		if ($existingModals.length>0) {
			$existingModals.modal("hide"); // Remove the background too
			// $existingModals.remove(); // This prevents removal of the modal background sometimes
			$existingModals.on('hidden.bs.modal', function (e) {
				$existingModals.remove(); // Remove the existing modal after it's been closed
			});
		}
		
		// modals are appended to the body
		const $modalObject = $(responseHtml).find(".modal").first();
		// Add the marker class 
		$modalObject.addClass(markerAjaxModal);
		if (stickyModal) {
			// This container is needed to keep the scrollbar when a second modal is closed
			var $container = $("<div class='modal-open'></div>");
			$container.append($modalObject);
			$("body").prepend($container);
			$modalObject.on('hidden.bs.modal', function (e) {
				$container.remove(); // Remove modal on close
			});
		} else {
			$("body").prepend($modalObject);
			$modalObject.on('hidden.bs.modal', function (e) {
				$modalObject.remove(); // Remove modal on close
			});
		}
		
		// Adding the modal head elements to the main document
		if (responseText.indexOf('<head>')>-1) {
			var parser = new DOMParser();
			var htmlDoc = parser.parseFromString(responseText, "text/html");
			var headNodes = $(htmlDoc.head).children();
			$("head").append(headNodes);
			removeHeadNodes(headNodes, $modalObject) // Needed a closure for headNodes (?)
		}
		
		// We need to show the modal after a delay or it won't show sometimes (!)
		var modalIsHidden = !$modalObject.is(':visible');
		if (modalIsHidden) {
			setTimeout(function() {
				$modalObject.modal('show');
				if (stickyModal) {
					// Need to fix the z-index to allow other modals to show on top and shade it
					var $background = $(".modal-backdrop.fade.show").last();
					var z = $background.css("z-index"); // 1040
					$modalObject.css("z-index", z-1); // 1039, must be less than 1040 to be behind a future background
					$background.css("z-index", z-2);
				}
				// The loader is removed after the modal is opened to prevent background flickering (if the loader background is not transparent)
				$modalObject.on('shown.bs.modal', function (e) {
					if (ajaxCounter<1) {
						yada.loaderOff();
					}
				})
			}, 100);
		} else {
			if (ajaxCounter<1) {
				yada.loaderOff();
			}
		}
		// This should not be needed because handlers have already been initialized on all the returned html
		// yada.initAjaxHandlersOn($modalObject);
		// Scroll the modal to an optional anchor (delay was needed for it to work)
		// or scroll back to top when it opens already scrolled (sometimes it happens)
		setTimeout(function() {
			var hashValue = window.location.hash; // #234
			if (hashValue.length>1 && !isNaN(hashValue.substring(1))) {
				try {
					$modalObject.animate({
						scrollTop: $(hashValue).offset().top
					}, 1000);
				} catch (e) {}
			} else if ($modalObject.scrollTop()>0) {
				// Scroll back to top when already scrolled
				$modalObject.animate({
					scrollTop: 0
				}, 500);
			}
		}, 500);
	}

	/**
	 * Performs some deprecated actions that should be removed one day
	 * @param {*} responseTrimmed 
	 * @param {*} responseHtml 
	 * @param {*} responseText 
	 * @param {*} url 
	 * @param {*} data 
	 * @param {*} successHandler 
	 * @param {*} method 
	 * @returns true if the ajax method should terminate
	 */
	function doDeprecatedStuff(responseTrimmed, responseHtml, responseText, url, data, successHandler, method) {
		// Deprecated - to be removed:
		// Check if we just did a login.
		// A successful login can also return a redirect, which will skip the PostLoginHandler
		// The "loginSuccess" string is not returned anymore.
		if ("loginSuccess" == responseTrimmed) {
			// @Deprecated. Should use the generic modal instead of the login modal
			$("#loginModal").modal("hide");
			yada.loaderOff();
			// window.location.reload(true); // true = skip cache // Non va bene perchè se è stata fatta una post, viene ripetuta!
			yada.handlePostLoginHandler(responseHtml, responseText);
			return true;
		}
		//
		if (openLoginModalIfPresent(responseHtml)) {
			// @Deprecated. Should use the generic modal instead of the login modal
			yada.loaderOff();
			return true;
		}
		// Controllo se è stata ritornata la home con una richiesta di login
		if ((typeof responseText == 'string' || responseText instanceof String) && responseText.indexOf('s_loginRequested') !== -1) {
			// @Deprecated. Should use the generic modal instead of the login modal
			yada.openLoginModal(url, data, successHandler, method); // E' necessario il login. Viene fatto, e poi eseguito l'handler.
			yada.loaderOff();
			return true;
		}
		
		// Gestisce la pwd scaduta
		var pwdChange=$(responseHtml).find("body.yadaChangePassword");
		if (pwdChange.length>0) {
			$("#loginModal").remove();
			showFullPage(responseText);
			yada.loaderOff();
			return true;
		}			

		return false;
	}

	function removeHeadNodes(headNodes, $modalObject) {
		$modalObject.on('hidden.bs.modal', function (e) {
			if (headNodes!=null) {
				try {
					headNodes.remove(); // Cleanup on modal close
				} finally {};
			}
		});
	}

	/**
	 * Se esiste un confirm nel response, lo visualizza e, in caso l'utente confermi, esegue la chiamata originale aggiungendo "confirmed" ai parametri.
	 * WARNING: any modal will be closed and its close-handlers invoked before showing the confirm dialog
	 * @param data can be either a string or an object or null
	 */
	yada.handleModalConfirm = function(responseHtml, url, data, successHandler, type) {
		var $modalConfirm=$(responseHtml).find(".s_modalConfirm .modal");
		if ($modalConfirm.length>0) {
			// Close all non-sticky modals
			const $currentModals = $(".modal:not(."+yada.stickyModalMarker+"):visible");			
			// If the modal has been loaded via yada.ajax, on hide it will be removed
			// so we clone the content in order to keep the event handlers.
			// Don't find just the ajaxModals or we would complicate the restore section later.
			const clonedModalContentArray = $currentModals.map(function() {
				return $(this).find('.modal-content').first().clone(true, true); 
			}).get();  
			$currentModals.modal('hide'); // Hide any modal that might be already open
			if ($("#yada-confirm .modal").length==0) {
				console.error("[yada] No confirm modal found: did you include it?");
			}
			$("#yada-confirm .modal").children().remove();
			$("#yada-confirm .modal").append($(".modal-dialog", $modalConfirm));
			$("#yada-confirm .modal").modal('show');
			$("#yada-confirm .okButton").click(function() {
				// $("#yada-confirm .okButton").off();
				// ?????????? A cosa servono questi postXXXX ??????????????????
				postLoginUrl = null;
				postLoginData = null;
				yada.postLoginHandler = null;
				postLoginType = null;
				if (typeof(data)=='string') {
					data = yada.addUrlParameterIfMissing(data, "yadaconfirmed", "true", false);
				} else {
					if (data==null) {
						data = {};
					}
					data.yadaconfirmed=true;
				}
				yada.ajax(url, data, successHandler, type);
			});
			$("#yada-confirm .cancelButton").click(function() {
				$('#yada-confirm .modal').one('hidden.bs.modal', function (e) {
					// $currentModals.modal('show');
					$currentModals.each(function(index) {
    					if (isRemoved($(this))) {
							// The modal was removed with all the nested handlers, so I replace the
							// content with the cloned one to restore the handlers.
							$('.modal-content', this).replaceWith(clonedModalContentArray[index]);
    					}
    					$(this).modal('show');
					});
					$('#yada-confirm .modal').off('hidden.bs.modal'); // Useless?
				});
				// $("#yada-confirm .modal").modal('hide');
			});
			return true;
		}
		return false;
	}
	
	function extractError(responseText) {
		var errorKeyword = 'yadaError:';
		if (typeof responseText === "string") {
			var trimmedText = responseText.trim();
			if (yada.startsWith(trimmedText, errorKeyword)) {
				return trimmedText.substring(errorKeyword.length);
			}
		}
		return null;
	}
	
	// Apre un errore se il risultato di una chiamata ajax contiene l'oggetto ajaxError o lo stato è diverso da success
	yada.showAjaxErrorIfPresent = function(responseText, statusText, errorObject) {
		var errorMessage = null;
		if (typeof responseText == "object" && responseText.error!=null) {
			errorMessage = responseText.error;
		} else if (errorObject!=null && errorObject.yadaError!=null) {
			errorMessage = errorObject.yadaError.error;
		} else {
			errorMessage = extractError(responseText);
		}
		
		if (errorMessage!=null) {
			yada.showErrorModal("Error", errorMessage!=""?errorMessage:"Generic Error");
			return true;
		}
		return false;
	};
	
//	// Chiamato se le chiamate ajax vanno in errore
//	// TODO deprecato da eliminare? Vedi se lo usa ancora ldm.js
//	yada.ajaxNetworkError = function(jqXHR, textStatus, errorThrown) {
//		yada.showErrorModal("Errore (" + errorThrown + ")", "Si è verificato un errore imprevisto. Ricarica la pagina e riprova");
//	};
	
	// Ritorna true se nell'html c'è un notify di tipo error
	yada.isNotifyError = function(responseHtml) {
		return $('.yadaNotify span.glyphicon.error', responseHtml).not('.hidden').length>0;
	}
	
	// Se un ritorno da una chiamata ajax ha un notify, lo mostra.
	// Per mostrare un notify al ritorno dalla get, basta che il Controller ritorni "/yada/modalNotify" 
	// dopo aver chiamato ad esempio yadaWebUtil.modalOk()
	// Ritorna true se la notify è stata mostrata.
	yada.handleNotify = function(responseHtml) {
		var notification=$(responseHtml).find(".yadaNotify");
		if (notification.length==1) {
			// Mostro la notification
			$('.modal:visible').modal('hide'); // Close any current modals
			$('#yada-notification').children().remove();
			$('#yada-notification').append(notification);
			// We need to show the modal after a delay or it won't show sometimes (!)
			setTimeout(function() {
				$('#yada-notification').on('show.bs.modal', function (e) {
					// Keep the loader open until the modal is fully shown, to prevent "flashing".
					// This should become a configurable option maybe
					if (!notification.hasClass("yadaLoaderKeep")) {
						if (ajaxCounter<1) {
							yada.loaderOff();
						}
                    }
				});
				$('#yada-notification').modal('show');
			}, 200);
			return true;
		}
		return false;
	}
	
	/**
	 * Return the data in a table inside a yadaResponseData, or an empty array
	 */
	yada.getEmbeddedResult = function(html) {
		var result = {};
		$(".yadaResponseData table tr", html).each(function(){
			var key = $(".s_key", this).text();
			var value = $(".s_value", this).text();
			result[key] = value;
		});
		return result;
	}

}( window.yada = window.yada || {} ));
