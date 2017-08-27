// yada.ajax.js
// Depends on yada.js

(function( yada ) {
	"use strict";
	
	// Namespace trick explained here: http://stackoverflow.com/a/5947280/587641
	// For a public property or function, use "yada.xxx = ..."
	// For a private property use "var xxx = "
	// For a private function use "function xxx(..."
	
	yada.postLoginHandler = null; // Handler to run after login, if any
	
	var parentSelector = "yadaParents:"; // Used to indicate that a CSS selector should be searched in the parents()

	// ?????????? A cosa servono questi postXXXX ??????????????????
	var postLoginUrl = null;
	var postLoginData = null;
	var postLoginType = null;
	
	/**
	 * Init yada ajax handlers on the specified element
	 * @param $element the element, or null for the entire body
	 */
	yada.initAjaxHandlersOn = function($element) {
		yada.enableAjaxForms(null, $element);
		yada.enableAjaxLinks(null, $element);
		yada.enableAjaxSelects(null, $element);
	}

	////////////////////
	/// Form
	
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
				$("#loginModal").remove();
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
	
//	// Opzioni per il form di login, gestito via ajaxForms (TODO da riscrivere senza ajaxForms che ormai viene usato solo per il login)
//	var formLoginOptions = { 
//			
//			success: function(responseText, statusText, xhr, form) {
//				yada.loaderOff();
//				var responseHtml=$("<div>").html(responseText);
//				if ("loginSuccess"==responseText) {
//					$("#loginModal").remove();
//					// window.location.reload(true); // true = skip cache // Non va bene perchè se è stata fatta una post, viene ripetuta!
//					handlePostLoginHandler(responseHtml, responseText);
//					return;
//				}
//				var dialog=$("#loginModalDialog", responseHtml);
//				if (dialog.length>0) {
//					// La risposta è ancora il form di login, che deve essere aggiornato
//					$("#loginModal").children().remove();
//					$("#loginModal").append(dialog);
//					$('#loginForm').ajaxForm(formLoginOptions); // Login POST via Ajax
//					//$("#loginModal").modal('show');
//				    return;
//				}
//	
//				// Se è stato ritornato un confirm, lo mostro e abilito l'esecuzione dell'ajax e dell'handler
//				if (yada.handleModalConfirm(responseHtml, postLoginUrl, postLoginData, postLoginHandler, postLoginType)) {
//					return;
//				}
//				
//				// Gestisce la pwd scaduta
//				var pwdChange=$(responseHtml).find("form[name='form-change-password']");
//				if (pwdChange.length>0) {
//					$("#loginModal").modal('hide');
//					document.open();
//					document.write(responseText);
//					document.close();
//					return;
//				}			
//				
//				var loadedModalDialog=$(responseHtml).find(".modal-dialog");
//				if (loadedModalDialog.length>0) {
//					// La risposta è un qualunque modal, che viene mostrato alla chiusura del modal di login
//					// (se aprissi subito il nuovo modal, per qualche ragione non scrollerebbe)
//					$("#loginModal").on('hidden.bs.modal', function (e) {
//						$("#ajaxModal").children().remove();
//						$("#ajaxModal").append(loadedModalDialog);
//						$('#ajaxModal:hidden').modal('show'); // Mostro il modal se non è già aperto
//						enableAjaxForms(); // Abilita l'invio di un eventuale form via ajax
//					});
//					$("#loginModal").modal('hide');
//					return;
//				}
//				
//				// Era una chiamata ajax che è andata in session timeout e ha richiesto un form login.
//				// Viene eseguito l'eventale handler passandogli la pagina inizialmente richiesta.
//				$("#loginModal").modal('hide');
//				handlePostLoginHandler(responseHtml, responseText);
//				
//			}
//		}; 
	
	// Al ritorno di un post di login, mostra eventuali notify ed esegue l'eventuale handler, oppure ricarica la pagina corrente se l'handler non c'è.
	function handlePostLoginHandler(responseHtml, responseText) {
		var isError = yada.isNotifyError(responseHtml);
		yada.handleNotify(responseHtml);
		if (yada.postLoginHandler != null) {
			if (!isError) { // Esegue l'handler solo se non ho ricevuto una notifica di errore
				yada.postLoginHandler(responseText, responseHtml); 
			}
		} else {
			yada.loaderOn();
			window.location.href=yada.removeHash(window.location.href); // Ricarico la pagina corrente (senza ripetere la post) se non ho un handler
		}
		yada.postLoginHandler = null;
	};
	
	// Apre il modal del login se è già presente in pagina.
	// handler viene chiamato quando il login va a buon fine.
	// return true se il modal è presente ed è stato aperto, false se il modal non c'è e non può essere aperto.
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
	yada.openLoginModalAjax = function(loginFormUrl, handler, errorTitle, errorText) {
		yada.postLoginHandler = handler;
		$.get(loginFormUrl, function(responseText, statusText) {
			var responseHtml=$("<div>").html(responseText);
			var loginReceived = openLoginModalIfPresent(responseHtml);
			if (!loginReceived) {
				yada.showErrorModal(errorTitle, errorText);
			}
		});
	}
	
	// Chiama la funzione javascript yadaCallback() se presente nell'html ricevuto dal server.
	// - responseHtml = l'html ricevuto dal server, creato con $("<div>").html(responseText)
	function callYadaCallbackIfPresent(responseHtml) {
		// Cerco se c'è una funzione js da eseguire chiamata "yadaCallback".
		var scriptNodes = $(responseHtml).find("script#yadaCallback");
		if (scriptNodes.length>0) {
			$('#callbackJavascript').append(scriptNodes);
			yadaCallback();
			return true;
		}
		return false;
	}	
	
	/**
	 * Transform links into ajax links: all anchors with a class of "yadaAjax" will be sent via ajax.
	 * @param handler a function to call upon successful link submission, can be null
	 * @param $element the element on which to enable ajax links, can be null for the entire body
	 */
	yada.enableAjaxLinks = function(handler, $element) {
		if ($element==null) {
			$element = $('body');
		}
		$('a.yadaAjax', $element.parent()).each(function() {
			$(this).removeClass('yadaAjax');
			yada.enableAjaxLink($(this), handler);
		});
		// Legacy
		$('.s_ajaxLink', $element.parent()).each(function() {
			$(this).removeClass('s_ajaxLink');
			yada.enableAjaxLink($(this), handler);
		});
	};
	
	/**
	 * Enables ajax calls on select change.
	 * @param handler a function to call upon successful link submission, can be null
	 * @param $element the element on which to enable ajax, can be null for the entire body
	 */
	yada.enableAjaxSelects = function(handler, $element) {
		if ($element==null) {
			$element = $('body');
		}
		$('select.yadaAjax', $element.parent()).each(function() {
			$(this).removeClass('yadaAjax');
			yada.enableAjaxSelect($(this), handler);
		});
	};
	
	yada.enableAjaxSelect = function($select, handler) {
		// If array, recurse to unroll
		if ($select.length>1) {
			$select.each(function() {
				yada.enableAjaxSelect($(this), handler);
			});
			return;
		}
		// From here on the $link is a single anchor, not an array
		var markerClass = 'yadaAjaxed'; // To prevent double submission
		$select.not('.'+markerClass).change(function(e) {
			return makeAjaxCall(e, $select, handler);
		})
		$select.removeClass('yadaAjax');
		$select.not('.'+markerClass).addClass(markerClass);
	};

	
	/**
	 * Sends a link via ajax, it doesn't have to have class .yadaAjax.
	 * Links with a "yadaLinkDisabled" class are disabled.
	 * @param $link the jquery anchor (could be an array), e.g. $('.niceLink')
	 * @param handler funzione chiamata in caso di successo e nessun yadaWebUtil.modalError()
	 */
	yada.enableAjaxLink = function($link, handler) {
		// If array, recurse to unroll
		if ($link.length>1) {
			$link.each(function() {
				yada.enableAjaxLink($(this), handler);
			});
			return;
		}
		// From here on the $link is a single anchor, not an array
		var markerClass = 'yadaAjaxed'; // To prevent double submission
		$link.not('.'+markerClass).click(function(e) {
			return makeAjaxCall(e, $link, handler);
		})
		$link.removeClass('yadaAjax');
		$link.removeClass('s_ajaxLink'); // Legacy
		$link.not('.'+markerClass).addClass(markerClass);
	};
	
	function makeAjaxCall(e, $element, handler) {
		e.preventDefault();
		if ($element.hasClass("yadaLinkDisabled")) {
			return false;
		}
		// Call, in sequence, the handler specified in data-successHandler and the one passed to this function
		var joinedHandler = function(responseText, responseHtml) {
			showFeedbackIfNeeded($element);
			var handlerNames = $element.attr("data-yadaSuccessHandler");
			if (handlerNames===undefined) {
				handlerNames = $element.attr("data-successHandler"); // Legacy
			}
			if (handlerNames!=null) {
				// Can be a comma-separated list of handlers, which are called in sequence
				var handlerNameArray = yada.listToArray(handlerNames);
				for (var i = 0; i < handlerNameArray.length; i++) {
					var dataHandler = window[handlerNameArray[i]];
					if (typeof dataHandler === "function") {
						dataHandler(responseText, responseHtml, $element[0]);
					}
				}
			}
			if (handler != null) {
				handler(responseText, responseHtml, $element[0]);
			}
			deleteOnSuccess($element);
			updateOnSuccess($element, responseHtml);
		}
		
		var url = $element.attr('href');
		var confirmText = $element.attr("data-confirm");
		var data = null;
		// In a select, set the data object to the option
		var name = $element.attr("name");
		var value = $("option:selected", $element).val();
		if (name !=null && value !=null) {
			data = {};
			data[name] = value;
		}
		if (confirmText!=null) {
			var okButton = $element.attr("data-okButton") || yada.messages.confirmButtons.ok;
			var cancelButton = $element.attr("data-cancelButton") || yada.messages.confirmButtons.cancel;
			yada.confirm(confirmText, function(result) {
				if (result==true) {
					yada.ajax(url, data, joinedHandler==null?joinedHandler:joinedHandler.bind($element), null, $element.attr('data-timeout'));
				}
			}, okButton, cancelButton);
		} else {
			yada.ajax(url, data, joinedHandler==null?joinedHandler:joinedHandler.bind($element));
		}
		return true; // Run other listeners
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
			for (var count=0; count<selectors.length; count++) {
				var selector = selectors[count];
				if (selector=="") {
					$element.remove();
				} else {
					var fromParents = yada.startsWith(selector, parentSelector); // yadaParents:
					if (fromParents==false) {
						$(selector).remove();
					} else {
						selector = selector.replace(parentSelector, "").trim();
						$element.parents(selector).remove();
					}
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param $element the link or the form
	 * @param responseHtml
	 * @returns true if the "data-yadaUpdateOnSuccess" was present
	 */
	function updateOnSuccess($element, responseHtml) {
		// If "yadaUpdateOnSuccess" is set, replace its target; if it's empty, replace the original link.
		// The target can be a parent when the css selector starts with parentSelector (currently "yadaParents:").
		// The selector can be multiple, separated by comma. The replacement can be multiple, identified by _yadaReplacement_
		var updateSelector = $element.attr("data-yadaUpdateOnSuccess");
		if (updateSelector != null) {
			var selectors = updateSelector.split(',');
			var $replacementArray = $("._yadaReplacement_", responseHtml);
			if ($replacementArray.length==0) {
				$replacementArray = [responseHtml.children()]; // Uso .children() per skippare il primo div inserito da yada.ajax()
			}
			var $replacement;
			for (var count=0; count<selectors.length; count++) {
				var selector = selectors[count];
				if (count<$replacementArray.length) {
					$replacement = $replacementArray[count];
				}
				if (selector == "") {
					$element.replaceWith($replacement);
				} else {
					var fromParents = yada.startsWith(selector, parentSelector); // yadaParents:
					if (fromParents==false) {
						$(selector).replaceWith($replacement);
					} else {
						selector = selector.replace(parentSelector, "").trim();
						$element.parents(selector).replaceWith($replacement);
					}
				}
				yada.initHandlersOn($replacement);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Show a checkmark fading in and out
	 * @param $element
	 * @returns
	 */
	function showFeedbackIfNeeded($element) {
		// TODO specify timeouts in the tag
		// TODO specify icon in the tag
		var showFeedback = $element.attr("data-yadaShowAjaxFeedback");
		if (showFeedback!=undefined) {
			$("#yadaAjaxFeedback").fadeIn(800, function() {
				$("#yadaAjaxFeedback").fadeOut(400);
			});
		}
	}

	/**
	 * Transform forms into ajax forms: all forms with a class of "yadaAjax" will be sent via ajax.
	 * @param handler a function to call upon successful form submission. It can also be specified as a data-successHandler attribute on the form
	 * @param $element the element on which to enable ajax forms, can be null for the entire body
	 */
	yada.enableAjaxForms = function(handler, $element) {
		if ($element==null) {
			$element = $('body');
		}
		$('form.yadaAjax', $element.parent()).each(function() {
			$(this).removeClass('yadaAjax');
			yada.enableAjaxForm($(this), handler);
		});
		// Legacy
		$('.s_ajaxForm', $element.parent()).each(function() {
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
		// From here on the $form is a single anchor, not an array.
		var markerClass = 'yadaAjaxDone'; // To prevent double submission
		// Can't use document.activeElement to find the clicked button because of the possible "confirm" dialog
		// http://stackoverflow.com/a/33882987/587641
		var clickedButton = null;
		$form.find("button[type='submit']").click(function() {
			clickedButton = this;
		});
		$form.not('.'+markerClass).off('submit').submit(function(e) {
			e.preventDefault();
			// Check if it must be a multipart formdata
			var multipart = $form.attr("enctype")=="multipart/form-data";
			// Using FormData to send files too: https://developer.mozilla.org/en-US/docs/Web/API/FormData/FormData
			var data = multipart ? new FormData(this) : $(this).serializeArray();
			var buttonName = null;
			var buttonValue = null;
			if (clickedButton!=null) {
				buttonName = $(clickedButton).attr("name");
				buttonValue = $(clickedButton).attr("value") || "";
				if (multipart && buttonName!=null && !data.has('buttonName')) {
					data.append('buttonName', buttonValue);
				} else if (!multipart && buttonName!=null && data[buttonName]==null) {
					data.push({name: buttonName, value: buttonValue});
				}
			}
			if (!multipart) {
				data = $.param(data);
			}
			// Call, in sequence, the handler specified in data-successHandler and the one passed to this function.
			// Extend the handler to include form and button parameters
			var joinedHandler = (function(handlerButton) { 
				return function(responseText, responseHtml) {
					showFeedbackIfNeeded($form);
					var formHandlerNames = $form.attr("data-yadaSuccessHandler");
					if (formHandlerNames===undefined) {
						formHandlerNames = $form.attr("data-successHandler"); // Legacy
					}
					// var dataHandler = window[formHandlerName];
					var buttonHandlerNames = $(handlerButton).attr("data-yadaSuccessHandler");
					// var buttonDataHandler = window[buttonHandlerName];
					// The button handler has precedence over the form handler, which is called only if the former returns true
					// from all handlers.
					var runFormHandler = true;
					if (buttonHandlerNames != null) {
						// Can be a comma-separated list of handlers, which are called in sequence
						var handlerNameArray = yada.listToArray(buttonHandlerNames);
						for (var i = 0; i < handlerNameArray.length; i++) {
							var dataHandler = window[handlerNameArray[i]];
							if (typeof dataHandler === "function") {
								runFormHandler &= dataHandler(responseText, responseHtml, this, handlerButton);
							}
						}
					}
					if (runFormHandler == true && formHandlerNames!=null) {
						// Can be a comma-separated list of handlers, which are called in sequence
						var handlerNameArray = yada.listToArray(formHandlerNames);
						for (var i = 0; i < handlerNameArray.length; i++) {
							var dataHandler = window[handlerNameArray[i]];
							if (typeof dataHandler === "function") {
								dataHandler(responseText, responseHtml, this, handlerButton);
							}
						}
					}
					if (handler != null) {
						handler(responseText, responseHtml, this, handlerButton);
					}
					var deleted = deleteOnSuccess($(handlerButton));
					if (!deleted) {
						deleteOnSuccess($form);
					}
					var updated = updateOnSuccess($(handlerButton), responseHtml);
					if (!updated) {
						updateOnSuccess($form, responseHtml);
					}
				}
			})(clickedButton); // Create a closure for the button
			var method = $(this).attr('method') || "POST";
			// yada.ajax($(this).attr('action'), $.param(data), joinedHandler.bind(this), $(this).attr('method'), $(this).attr('data-timeout'));
			yada.ajax($(this).attr('action'), data, joinedHandler.bind(this), method, $(this).attr('data-timeout'));
			clickedButton = null;
			return false; // Important so that the form is not submitted by the browser too
		})
		// Set the confirm handlers on form buttons
	    $form.find("button[type='submit']").each(function() {
	    	var $button = $(this);
	    	var confirmText = $button.attr("data-confirm");
	    	if (confirmText!=null) {
	    		var okButton = $button.attr("data-okButton") || yada.messages.confirmButtons.ok;
	    		var cancelButton = $button.attr("data-cancelButton") || yada.messages.confirmButtons.cancel;
	    		$button.click(function() {
	    			yada.confirm(confirmText, function(result) {
	    				if (result==true) {
	    					$button.off("click");
	    					$button.click();
	    					// No $form.submit(); because of the button name (see clickedButton above)
	    				}
	    			}, okButton, cancelButton);
	    			return false; // Stop form submission
	    		});
	    	}
	    });
	};
	
	/**
	 * Esegue una get/post ajax passando data (stringa od oggetto). Gestisce il caso che sia necessario il login.
	 * Il metodo chiamato lato java può ritornare un notify chiamando yadaWebUtil.modalOk() o anche yadaWebUtil.modalError() etc.
	 * In caso di notify di un errore, l'handler non viene chiamato.
	 * @param url target url
	 * @param data dati da inviare (stringa od oggetto)
	 * @param successHandler(responseText, responseHtml);) funzione chiamata in caso di successo e nessun yadaWebUtil.modalError(). Viene chiamata anche in caso di errore se il suo flag executeAnyway è true
	 * @param type "POST" per il post oppure null o "GET" per il get
	 * @param timeout milliseconds timeout, null for default (set by the browser)
	 */
	yada.ajax = function(url, data, successHandler, type, timeout) {
		if (type==null) {
			type="GET"
		}
		if (timeout==null) {
			timeout=0; // Default
		}
		var processData = !(data instanceof FormData);  // http://stackoverflow.com/a/8244082/587641
		var contentType = data instanceof FormData ? false : undefined;
		yada.loaderOn();
		$.ajax({
			type: type,
			url: url,
			data: data,
			processData: processData,
			contentType: contentType,
			error: function(jqXHR, textStatus, errorThrown ) { 
				// textStatus is "error", "timeout", "abort", or"parsererror"
				// errorThrown is ''
				if (textStatus==="timeout") {
					yada.showErrorModal(yada.messages.connectionError.title, yada.messages.connectionError.message);
				} else {
					yada.showErrorModal(yada.messages.serverError.title, yada.messages.serverError.message + (textStatus!=null&&textStatus!='error'?' ('+textStatus+')':''));
				}
			},
			success: function(responseText, statusText, jqXHR) {
				var responseTrimmed = "";
				if (typeof responseText == "string") {
					responseTrimmed = responseText.trim();
				}
				yada.loaderOff();
				if (yada.showAjaxErrorIfPresent(responseTrimmed, statusText)==true) {
					return;
				}
				if ("reload" == responseTrimmed) {
					yada.reload();
					return;
				}
				var responseHtml=$("<div>").html(responseText);
				// Check if we just did a login
				if ("loginSuccess" == responseTrimmed) {
					$("#loginModal").remove();
					// window.location.reload(true); // true = skip cache // Non va bene perchè se è stata fatta una post, viene ripetuta!
					handlePostLoginHandler(responseHtml, responseText);
					return;
				}
				if (openLoginModalIfPresent(responseHtml)) {
					return;
				}
				// Controllo se è stata ritornata la home con una richiesta di login
				if ((typeof responseText == 'string' || responseText instanceof String) && responseText.indexOf('s_loginRequested') !== -1) {
					yada.openLoginModal(url, data, successHandler, type); // E' necessario il login. Viene fatto, e poi eseguito l'handler.
					return;
				}
				
				// Gestisce la pwd scaduta
				var pwdChange=$(responseHtml).find("form[name='form-change-password']");
				if (pwdChange.length>0) {
					$("#loginModal").remove();
					document.open();
					document.write(responseText);
					document.close();
					return;
				}			
				
				// Se è stato ritornato un confirm, lo mostro e abilito l'esecuzione dell'ajax e dell'handler
				if (yada.handleModalConfirm(responseHtml, url, data, successHandler, type)) {
					return;
				}
				
				// Il successHandler viene eseguito solo se non c'è un errore, oppure se il flag executeAnyway è true
				if (successHandler != null) {
					if (!yada.isNotifyError(responseHtml) || successHandler.executeAnyway==true) {
						// Non c'era un login, eseguo l'handler, se passato
						successHandler(responseText, responseHtml);
						// Keep going...
					}
				}
				// Per mostrare una notification al ritorno dalla get, basta che il Controller ritorni "/yada/modalNotify" 
				// dopo aver chiamato ad esempio yadaWebUtil.modalOk()
				if (yada.handleNotify(responseHtml)) {
					return;
				}
				// If it is a full page, overwrite the current one
				if ($('.s_fullPage', responseHtml).length>0) {
					document.open();
					document.write(responseText);
					document.close();
					return;
				}
				// Open any other modal
				var loadedModalDialog=$(responseHtml).find("> .modal > .modal-dialog");
//				var loadedModalDialog=$(responseHtml).find("> .modal:not(.s_fullPage) > .modal-dialog");
				if (loadedModalDialog.length==1) {
					// La risposta è un qualunque modal, che viene mostrato
					$("#loginModal").remove();
					$("#ajaxModal").children().remove();
					$("#ajaxModal").append(loadedModalDialog);
					$('#ajaxModal:hidden').modal('show'); // Mostro il modal se non è già aperto
					yada.enableAjaxForms();
					// Questo permette di scrollare all'anchor (ho dovuto mettere un ritardo altrimenti non scrollava)
					// e anche di far scendere il modal se per caso si apre scrollato (a volte capita, forse coi modal molto alti)
					setTimeout(function() {
						var hashValue = yada.getHashValue(window.location.href);
						if (hashValue!=null && hashValue.length>0) {
							$('#ajaxModal').animate({
								scrollTop: $('#' + hashValue).offset().top
							}, 1000);
						} else if ($('#ajaxModal').scrollTop()>0) {
							// Si è aperto in mezzo quindi lo scrollo in alto
							$('#ajaxModal').animate({
								scrollTop: 0
							}, 500);
						}
					}, 500);
					return;
				}
				// If the result is "closeModal", close all open modals
				if (responseTrimmed == 'closeModal') {
					$(".modal:visible").modal('hide');
				}
				// Otherwise it is a full page, that must be loaded in place of the current page
				// WRONG: in questo modo anche le chiamate ajax che ritornano frammenti riscrivono la pagina intera.
				// TODO aggiungere un qualcosa per indicare che la pagina va sovrascritta
//				document.open();
//				document.write(responseText);
//				document.close();
			},
			timeout: yada.devMode?0:timeout,
			traditional: true // Serve per non avere id[] : '12' ma id : '12'
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
			var $currentModals = $(".modal:visible");
			$currentModals.modal('hide'); // Hide any modal that might be already open
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
				$('#yada-confirm .modal').on('hidden.bs.modal', function (e) {
					$currentModals.modal('show');
				});
				// $("#yada-confirm .modal").modal('hide');
			});
			return true;
		}
		return false;
	}
	
	
	// Apre un errore se il risultato di una chiamata ajax contiene l'oggetto ajaxError o lo stato è diverso da success
	yada.showAjaxErrorIfPresent = function(responseText, statusText, xhr, form) {
		var errorKeyword = 'yadaError:';
		var errorObject = null;
		var errorPresent=(statusText!=null && statusText!=='success');
		if (typeof responseText === "string") {
			var errorPos = responseText.indexOf(errorKeyword);
			if (errorPos>-1) {
				// The error could be at the start or be appended at the end of the HTML when it happens in a th: fragment
				errorPresent = true;
				try {
					var errorObjectString = responseText.substring(errorPos + errorKeyword.length);
					errorObject = JSON.parse(errorObjectString);
				} catch (e) {
					// keep going
				}
			}
		}
		if (typeof responseText == "object" && responseText.error!=null) {
			errorPresent = true;
			errorObject = responseText;
		}
		if (errorPresent) {
			var errorMessage = "Generic Error";
			if (errorObject!=null) {
				errorMessage = errorObject.error;
			}
			yada.showErrorModal("Error", errorMessage);
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
		var notification=$(responseHtml).find(".s_modalNotify .yadaNotify");
		if (notification.length==1) {
			// Mostro la notification
			$('#ajaxModal').modal('hide'); // non si sa mai
			$('#yada-notification').children().remove();
			$('#yada-notification').append(notification);
			$('#yada-notification').modal('show');
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