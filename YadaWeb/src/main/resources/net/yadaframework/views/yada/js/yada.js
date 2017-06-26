// yada.js
// TODO i18n: file js separati, caricati in base alla lingua impostata, contengono i messaggi localizzati in forma di variabili

(function( yada ) {
	"use strict";
	
	// Namespace trick explained here: http://stackoverflow.com/a/5947280/587641
	// For a public property or function, use "yada.xxx = ..."
	// For a private property use "var xxx = "
	// For a private function use "function xxx(..."
	
	yada.ignoreHistoryStateChange=false;
	yada.devMode = false; // Set to true in development mode (also via thymeleaf)
	yada.baseUrl = null;	// Set it via thymeleaf
	yada.resourceDir = null; // Set it via thymeleaf
	yada.messages = {};
	yada.messages.connectionError = { // Set it via thymeleaf
		"title": "Connection Error",
		"message": "Failed to contact server - please try again later"
	}; 
	yada.messages.serverError = { // Set it via thymeleaf
			"title": "Server Error",
			"message": "Something is wrong - please try again later"
	}; 
	yada.messages.confirmButtons = { // Set it via thymeleaf
			"ok": "Ok",
			"cancel": "Cancel"
	}; 
		
	// ?????????? A cosa servono questi postXXXX ??????????????????
	var postLoginUrl = null;
	var postLoginData = null;
	var postLoginHandler = null;
	var postLoginType = null;
	
	var parentSelector = "yadaParents:"; // Used to indicate that a CSS selector should be searched in the parents()
	
	$(document).ready(function() {
		initHandlers();
	});
	
	function initHandlers() {
		// Mostra il loader sugli elementi con le classi s_showLoaderClick oppure s_showLoaderForm
		$('body').on('click', '.s_showLoaderClick', yada.loaderOn);
		$('body').on('submit', '.s_showLoaderForm', yada.loaderOn);
		yada.enableScrollTopButton(); // Abilita il torna-su
		yada.initHandlersOn();
	}
	
	/**
	 * Init yada handlers on the specified element
	 * @param $element the element, or null for the entire body
	 */
	yada.initHandlersOn = function($element) {
		yada.enableShowPassword($element); // Abilita l'occhietto
		yada.enableRefreshButtons($element);
		yada.enableAjaxForms(null, $element);
		yada.enableAjaxLinks(null, $element);
		yada.enableConfirmLinks($element);
		yada.enableHelpButton($element);
		yada.enableTooltip($element);
		yada.handleCustomPopover($element);
	}
	
	yada.loaderOn = function() {
		$(".loader").show();
	};
	
	yada.loaderOff = function() {
		$(".loader").hide();
	};

	yada.enableTooltip = function($element) {
		if ($element==null) {
			$element = $('body');
		}
	    $('.s_tooltip', $element).tooltip();
	};
	
	yada.enableHelpButton = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		$('.yadaHelpButton', $element).popover();
	};

	/**
	 * Reloads the page.
	 * If the previous call was a non-ajax post and the current address has a #, the post is repeated.
	 * Otherwise posts are not repeated.
	 */
	yada.reload = function() {
		var hashPresent = yada.stringContains(window.location.href, '#');
		if (!hashPresent) {
			window.location.replace(window.location.href);
		} else {
			window.location.reload(true); // Careful that non-ajax POST is repeated
		}
	}
	
	/**
	 * Enables clicking on a refresh button (yadaRefresh) to call an ajax data-loading handler. 
	 * The handler is also called at document-ready
	 */
	yada.enableRefreshButtons = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		$('.yadaRefresh', $element).not('.yadaRefreshed').each(function(){
			var handlerName = $(this).attr('data-handler');
			var dataHandler = window[handlerName];
			if (typeof dataHandler === "function") {
				$(this).click(dataHandler);
				// Call it now also
				dataHandler();
			}
			$(this).addClass('yadaRefreshed');
		});
	}

	// Da chiamare per abilitare il pulsante di ritorno in cima
	yada.enableScrollTopButton = function() {
		var scrollTopButton = $('.yadaScrollTop');
		if (scrollTopButton.length>0) {
			$(scrollTopButton).off().click( function(e){
				e.preventDefault();
				$('html, body').animate({scrollTop:0}, 1000);
				$(this).hide();
			});
			$(document).scroll(function () {
				var y = $(this).scrollTop();
				if (y > 800) {
					$(scrollTopButton).fadeIn();
				} else {
					$(scrollTopButton).fadeOut();
				}
			});
		}
	};

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
	
	

	
	//////////////////
	/// Ajax Modal
	
	//Funzione che gestisce il back/forward del browser.
	//Deve essere usata per aprire i modal.
	yada.runOnBackForward = function(handler) {
		History.Adapter.bind(window,'statechange',function() {
			if (yada.ignoreHistoryStateChange!==true) {
				if (typeof handler != 'undefined') {
					handler();
				}
			} else {
				yada.ignoreHistoryStateChange=false; // resetta
			}
		});
	};
	
	// Questa funzione carica un modal dentro ad ajaxModal per la prima volta, e lo apre. Viene gestita la url e la history del browser
	// qualora il modal possa essere identificato da un singolo parametro (opzionale).
	// E' tipicamente chiamata al click di un pulsante, di un anchor, o di un elemento in pagina (e.g. riga di tabella).
	// Se invece si vuole aprire il modal alla sottomissione di un form, usare la classe formShowAjaxModal sul form.
	// modalUrl = la url da cui caricare via ajax il modal completo
	// paramName = nome del parametro. Se viene trovato nell'url invece che nei parametri, viene gestito come una pathVariable
	//          con la sintassi /<paramName>/<paramValue>
	// paramValue = valore del parametro
	// handler = callback opzionale da chiamare quando il modal è caricato e sta per essere aperto
	
	// NOTA questo metodo è simile al nuovo yada.ajax ma fa delle cose in più per la history e lo scroll.
	yada.openNewAjaxModal = function(modalUrl, paramName, paramValue, handler) {
		if (History.enabled && paramName!=null) {
			var previousState = History.getState();
			var paramAlreadyThere = yada.hasPathVariableWithValue(previousState.url, paramName);
			if (!paramAlreadyThere) {
				paramAlreadyThere = yada.hasUrlParameter(previousState.url, paramName);
			}
			if (!paramAlreadyThere) {
				// Il push causa la chiamata di quanto registrato con History.Adapter.bind ovvero eventualmente l'apertura di un modal,
				// per cui blocco la chiamata settando questo flag
				yada.ignoreHistoryStateChange=true;
				var newUrl = "";
				if (yada.hasPathVariable(previousState.url, paramName)) {
					newUrl = yada.setPathVariable(previousState.url, paramName, paramValue);
				} else {
					newUrl = yada.addUrlParameterIfMissing(previousState.url, paramName, paramValue);
				}
				History.pushState(null, null, newUrl); 
			} 
			$('#ajaxModal').on('hidden.bs.modal', function (e) {
				// Rimuovo il parametro per evitare che il modal venga riaperto in automatico dopo la sua chiusura
				var newUrl = "";
				if (yada.hasPathVariable(previousState.url, paramName)) {
					newUrl = yada.removePathVariable(previousState.url, paramName);
				} else {
					newUrl = yada.removeUrlParameters(previousState.url, paramName);
				}
				History.pushState(null, null, newUrl);
			});

		}
		yada.ajax(modalUrl, null, handler, null);

	};
	
	//////////////////////
	/// Url Parameters ///
	//////////////////////
	
	// Ritorna true se la url contiene il parametro indicato
	yada.hasUrlParameter = function(url, param) {
		return typeof yada.getUrlParameter(url, param) == "string";
	};
	
	
	/**
	 * Rimuove dalla url il parametro di request indicato e la ritorna (non funziona se il parametro è senza valore)
	 * @param url
	 * @param param
	 * @param value the current parameter value, or null for any value
	 */
	yada.removeUrlParameter = function(url, param, value) {
		if (value==null) {
			value="[^&]*";
		}
		var regex = new RegExp("[?|&]" + param + "=" + value + "&?", 'g');
		url = url.replace(regex, '&');
		if (ldm.endsWith(url, '&')) {
			url = url.substring(0, url.length-1);
		}
		// '?' needs to be reinserted if first param was removed
		var pos = url.indexOf('&');
		if (pos>-1 && url.indexOf('?')==-1) {
			url = ldm.replaceAt(url, pos, '?');
		}
	 return url;
	};
	
	// Rimuove dalla url tutti i parametri di request indicati e la ritorna (forse non funziona se il parametro è senza valore)
	yada.removeUrlParameters = function(url, param) {
		var regex = new RegExp("[?|&]" + param + "=[^&]+&?", 'g');
		url = url.replace(regex, '&');
		if (ldm.endsWith(url, '&')) {
			url = url.substring(0, url.length-1);
		}
		// '?' needs to be reinserted if first param was removed
		var pos = url.indexOf('&');
		if (url.indexOf('?')==-1 && pos>-1) {
			url = ldm.replaceAt(url, pos, '?');
		}
		return url;
	};
	
	/**
	 * Aggiunge un parametro di request alla url, anche se già presente. La url può anche essere solo la location.search e può essere vuota.
	 * Ritorna la url modificata. Se value==null, il parametro è aggiunto senza valore.
	 * @param url la url o la query string o il segmento da modificare
	 * @param param nome del parametro da aggiungere
	 * @param value valore da aggiungere, can be null
	 * @param addQuestionMark (optional) if explicitly false, do not add a question mark when missing
	 */
	yada.addUrlParameter = function(url, param, value, addQuestionMark) {
		addQuestionMark = addQuestionMark==false?false:true;
		var anchor="";
		if (url) {
			var anchorPos = url.indexOf('#');
			anchor = anchorPos>-1?url.substring(anchorPos):"";
			url = anchorPos>-1?url.substring(0, anchorPos):url;
			if (url.indexOf('?')==-1 && addQuestionMark==true) {
				url = url + '?';
			} else {
				url = url + '&';
			}
		} else if (addQuestionMark==true) {
			url = "?";
		}
		url = url + encodeURIComponent(param);
		if (value!=null) {
			url = url + '=' + encodeURIComponent(value);
		}
		return url+anchor;
	};
	
	// Modifica o aggiunge un parametro di request alla url. La url può anche essere solo la location.search e può essere vuota
	yada.addOrUpdateUrlParameter = function(url, param, value, addQuestionMark) {
		if (yada.hasUrlParameter(url, param)) {
			return yada.updateUrlParameter(url, param, value);
		}
		return yada.addUrlParameter(url, param, value, addQuestionMark);
	};
	
	// Aggiunge un parametro di request alla url, solo se NON già presente. La url può anche essere solo la location.search e può essere vuota
	yada.addUrlParameterIfMissing = function(url, param, value, addQuestionMark) {
		if (yada.hasUrlParameter(url, param)) {
			return url;
		}
		return yada.addUrlParameter(url, param, value, addQuestionMark);
	};
	
	// Cambia il valore di un parametro di request, ritornando la url nuova
	// Adattato da http://stackoverflow.com/questions/5413899/search-and-replace-specific-query-string-parameter-value-in-javascript
	yada.updateUrlParameter = function(url, param, value) {
	 var regex = new RegExp("([?|&]" + param + "=)[^\&]+");
	 return url.replace(regex, '$1' + value);
	};
	
	/**
	 * Restituisce il valore di un parametro di request, oppure false se non c'è
	 * Adapted from http://stackoverflow.com/questions/2090551/parse-query-string-in-javascript
	 * @param url can be a url, a query string or even part of it, but everything before "?" or "&" will be skipped
	 */
	yada.getUrlParameter = function(url, varName){
		 var queryStr = url + '&';
		 var regex = new RegExp('.*?[&\\?]' + varName + '=(.*?)[&#].*');
		 var val = queryStr.replace(regex, "$1");
		 return val == queryStr ? false : unescape(val);
	};
	
	//////////////////////
	/// Path Variables ///
	//////////////////////

	//Rimuove la pathVariable che segue il precedingSegment. Può anche non esistere.
	yada.removePathVariable = function(url, precedingSegment) {
		var regex = new RegExp("/" + precedingSegment + "(/[^/?]*)?"); // Match di /site/racconti, /site/racconti/, /site/racconti/123, /site/racconti/123?aaa
		return url.replace(regex, "/"+precedingSegment);
	}
	
	// Setta la pathVariable al valore indicato, aggiungendola se non esiste. La query string rimane inalterata.
	// Il precedingSegment può stare ovunque, e il valore che segue viene sempre settato al nuovo valore.
	// Però se il precedingSegment non è seguito da un valore, il metodo funziona solo se precedingSegment non è seguito da altri segmenti (vedi primo e ultimo esempio).
	// Esempi:
	// yada.setPathVariable('http://localhost:8080/site/racconti', 'racconti', 410); --> "http://localhost:8080/site/racconti/410"
	// yada.setPathVariable('http://localhost:8080/site/racconti/123', 'racconti', 410); --> "http://localhost:8080/site/racconti/410"
	// yada.setPathVariable('http://localhost:8080/site/racconti/567/belli/123', 'racconti', 410); --> "http://localhost:8080/site/racconti/410/belli/123"
	// yada.setPathVariable('http://localhost:8080/site/racconti/belli/123', 'racconti', 410); --> "http://localhost:8080/site/racconti/410/123" !!!!ERROR!!!!!
	yada.setPathVariable = function(url, precedingSegment, newValue) {
		var regex = new RegExp("/" + precedingSegment + "(/[^/?]*)?"); // Match di /site/racconti, /site/racconti/, /site/racconti/123, /site/racconti/123?aaa
		return url.replace(regex, "/"+precedingSegment+"/" + newValue);
	}
	
	// Ritorna true se il precedingSegment è seguito da un valore.
	// Quindi se url = "http://localhost:8080/site/racconti/410?page=2&size=36&sort=publishDate,DESC"
	// e precedingSegment = "racconti", il risultato è true; 
	// invece è false per url = "http://localhost:8080/site/racconti/" oppure url = "http://localhost:8080/site/register"
	yada.hasPathVariableWithValue = function(url, precedingSegment) {
		var value = yada.getPathVariable(url, precedingSegment);
		return value != null && value.length>0;
	}
	
	// Ritorna true se il precedingSegment esiste nell'url, anche se non seguito da un valore.
	// Quindi se url = "http://localhost:8080/site/racconti/410?page=2&size=36&sort=publishDate,DESC"
	// oppure url = "http://localhost:8080/site/racconti"
	// e precedingSegment = "racconti", il risultato è true; 
	// invece è false per url = "http://localhost:8080/site/register"
	yada.hasPathVariable = function(url, precedingSegment) {
		var value = yada.getPathVariable(url, precedingSegment);
		return value != null;
	}
	
	// Ritorna il valore che nell'url segue il precedingSegment.
	// Per esempio se url = "http://localhost:8080/site/racconti/410?page=2&size=36&sort=publishDate,DESC"
	// e precedingSegment = "racconti", il risultato è "410".
	// Se precedingSegment non è seguito da un valore, ritorna stringa vuota.
	// Se precedingSegment non c'è, ritorna null
	yada.getPathVariable = function(url, precedingSegment) {
		var segments = ldm.removeQuery(url).split('/');
		var found=false;
		for (var i=1; i<segments.length; i++) {
			if (segments[i]===precedingSegment) {
				found=true;
			} else if (found && segments[i-1]===precedingSegment) {
				return segments[i];
			}
		}
		return found?'':null;
	}
	
	// Ritorna il valore che nell'url segue il precedingSegment.
	// Per esempio se url = "http://localhost:8080/site/racconti/410?page=2&size=36&sort=publishDate,DESC"
	// e precedingSegment = "racconti", il risultato è "410"
	// Il valore deve essere numerico, in caso contrario ritorna null come se non l'avesse trovato
	yada.getPathVariableNumeric = function(url, precedingSegment) {
		var segments = url.split(/[\/\?#]/);
		for (var i=1; i<segments.length; i++) {
			if (segments[i-1]===precedingSegment) {
				if (!isNaN(segments[i])) {
					return segments[i];
				}
				return null;
			}
		}
		return null;
	}
	
	////////////////
	/// Facebook ///
	////////////////

	yada.fbInitDone=false;
	
	yada.facebookInit = function() {
		yada.fbInitDone=true;
	}
	
	yada.facebookLogout = function() {
		FB.getLoginStatus(function(response) {
			if (response && response.status === 'connected') {
				FB.logout();
			}
		});
	}
	
	// Fa la chiamata al nostro server per autenticare l'utente o andare sul form di registrazione precompilato.
	// - serverUrl = "/showFacebookConfirm"
	// name, surname, email = non usati
	function sendFacebookToServer(serverUrl, accessToken, name, surname, email) {
		$.get(serverUrl, 
			{ accessToken: accessToken }, 
			function(responseText, statusText) {
				yada.loaderOff();
				var responseHtml=$("<div>").html(responseText);
				var modalShown = handleLoadedModal(responseHtml); // Il risultato è il modal di registrazione social, che viene mostrato
				var callbackCalled = callYadaCallbackIfPresent(responseHtml); // Qui succede il redirect
				if (!modalShown && !callbackCalled) {
					// Il risultato è il contenuto originariamente richiesto, che viene passato all'handler, oppure se non c'è l'handler si ricarica la pagina corrente
					handlePostLoginHandler(responseHtml, responseText);
				}
			}
		);
	}
	
	function handleLoadedModal(responseHtml) {
		var loadedModalDialog=$(responseHtml).find(".modal-dialog");
		if (loadedModalDialog.length>0) {
			$("#ajaxModal").children().remove();
			$("#ajaxModal").append(loadedModalDialog);
			$('#ajaxModal:hidden').modal('show'); // Mostro il modal se non è già aperto
			return true;
		}
		return false;
	};
	
	function facebookLoginResult(response, serverUrl) {
		if (response.status === 'connected') {
			var accessToken = response.authResponse.accessToken;
			yada.loaderOn();
			FB.api('/me', function(response) {
				var name = response.first_name;
				var surname = response.last_name;
				var email = response.email;
				sendFacebookToServer(serverUrl, accessToken, name, surname, email);
			});
		} else {
			postLoginHandler = null;
			yada.loaderOff();
		}
	}
	
	//Usata quando si fa un facebook login button con il codice html di facebook, non con l'api
	yada.afterFacebookLoginButton = function(serverUrl) {
		FB.getLoginStatus(function(response) {
			facebookLoginResult(response, serverUrl);
		 });
	}
	
	// Abilita il pulsante di login facebook usando l'api
	// - serverUrl = url da chiamare lato server per autenticare
	// - handler = opzionale da chiamare a fine login con i dati ritornati
	yada.enableFacebookLoginButton = function(serverUrl, handler) {
		$('.facebookLoginButton').click(function(e) {
			e.preventDefault();
			yada.loaderOn();
			$('#loginModal').modal('hide');
			FB.login(function(response) {
				postLoginHandler = handler;
				facebookLoginResult(response, serverUrl);
			}, {scope: 'email'}); 
		});
	}
	
	////////////////////
	/// Login
	
	
	//Controlla se la sessione utente è scaduta, e in caso positivo fa una redirect all'url di timeout
	yada.checkSession = function(rootUrl) {
		$.get(rootUrl + "ajaxCheckSessionActive", function(responseText) {
			if (responseText == 'expired') {
				window.location.href = rootUrl + "timeout";
			}
		});	
	}
	
//	// Deprecated !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//	yada.enableEmbeddedLogin = function($theForm, handler) {
//		$theForm.submit(function(e) {
//			e.preventDefault();
//			$this=$(this);
//	        $.ajax({
//	            type: $this.attr('method'),
//	            url: $this.attr('action'),
//	            data: $this.serialize(),
//	            success: function (responseText) {
//	            	var responseHtml=$("<div>").html(responseText);
//	    			var dialog=$("#loginModalDialog", responseHtml);
//	    			if (dialog.length>0) {
//	    				$('#ajaxModal').modal('hide');
//	    				// La risposta è il form di login, che deve essere mostrato con gli errori
//	    				$("#loginModal").children().remove();
//	    				$("#loginModal").append(dialog);
//	    				$('#loginForm').ajaxForm(formLoginOptions); // Login POST via Ajax
//	    				$("#loginModal").modal('show');
//	    			    return;
//	    			}
//	    			// Il login ha avuto successo quindi si esegue l'handler
//	            	if (handler!=null) {
//	            		handler(responseText, responseHtml);
//	            	}
//	            }
//	        });
//	        return false;
//		});
//	};
	
	//Occhietto della password
	yada.enableShowPassword = function(element) {
		if (element==null) {
			element = $('body');
		}
		var $target = $('.yadaShowPassword', element).not('.yadaShowPassworded');
		// Cycle password visibility
		$target.click(function(e) {
			e.preventDefault();
			var $hiddenField = $(this).parent().parent().find("input[type='password']");
			// Show if not shown already
			var madeVisible=$hiddenField.attr('type', 'text').length>0;
			if (madeVisible) {
				// Restore hidden field before submit (to prevent browsers from storing it in field autocomplete history, maybe)
				$(this).parents('form').find('button[type="submit"]').click(function(e) {
					$hiddenField.attr('type', 'password');
				});
			} else {
				// Already visible: back to hidden
				$(this).parent().parent().find("input[type='text']").attr('type', 'password');
			}
		});
		$target.addClass('yadaShowPassworded');
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
		if (postLoginHandler != null) {
			if (!isError) { // Esegue l'handler solo se non ho ricevuto una notifica di errore
				postLoginHandler(responseText, responseHtml); 
			}
		} else {
			yada.loaderOn();
			window.location.href=yada.removeHash(window.location.href); // Ricarico la pagina corrente (senza ripetere la post) se non ho un handler
		}
		postLoginHandler = null;
	};
	
	// Apre il modal del login se è già presente in pagina.
	// handler viene chiamato quando il login va a buon fine.
	// return true se il modal è presente ed è stato aperto, false se il modal non c'è e non può essere aperto.
	yada.openLoginModal = function(url, data, handler, type) {
		if ($('#loginModal').length>0) {
			// ?????????? A cosa servono questi postXXXX ??????????????????
			postLoginUrl = url;
			postLoginData = data;
			postLoginHandler = handler;
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
		postLoginHandler = handler;
		$.get(loginFormUrl, function(responseText, statusText) {
			var responseHtml=$("<div>").html(responseText);
			var loginReceived = openLoginModalIfPresent(responseHtml);
			if (!loginReceived) {
				yada.showErrorModal(errorTitle, errorText);
			}
		});
	}
	
	////////////////////
	/// Ajax
	
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
	 * Transform links into confirm links: all anchors with a "data-confirm" attribute that don't have a class of "s_ajaxLink" 
	 * will show a confirm box before submission.
	 */
	yada.enableConfirmLinks = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		var markerClass = 's_dataConfirmed';
		$('a[data-confirm]', $element.parent()).not('.'+markerClass).not('.s_ajaxLink').each(function() {
			$(this).click(function(e) {
				var $link = $(this);
				e.preventDefault();
				var href = $link.attr("href");
				var confirmText = $link.attr("data-confirm");
				if (confirmText!=null) {
					var okButton = $link.attr("data-okButton") || yada.messages.confirmButtons.ok;
					var cancelButton = $link.attr("data-cancelButton") || yada.messages.confirmButtons.cancel;
					yada.confirm(confirmText, function(result) {
						if (result==true) {
							yada.loaderOn();
							window.location.replace(href);
						}
					}, okButton, cancelButton);
				} else {
					yada.loaderOn();
					window.location.replace(href);
				}
			});
			$(this).addClass(markerClass);
		});
	};
	
	/**
	 * Transform links into ajax links: all anchors with a class of "s_ajaxLink" will be sent via ajax.
	 * @param handler a function to call upon successful link submission, can be null
	 * @param $element the element on which to enable ajax links, can be null for the entire body
	 */
	yada.enableAjaxLinks = function(handler, $element) {
		if ($element==null) {
			$element = $('body');
		}
		$('.s_ajaxLink', $element.parent()).each(function() {
			$(this).removeClass('s_ajaxLink');
			yada.enableAjaxLink($(this), handler);
		});
	};
	
	/**
	 * Sends a link via ajax, it doesn't have to have class .s_ajaxLink.
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
		var markerClass = 's_ajaxLinked'; // To prevent double submission
		$link.not('.'+markerClass).click(function(e) {
			e.preventDefault();
			if ($(this).hasClass("yadaLinkDisabled")) {
				return false;
			}
			// Call, in sequence, the handler specified in data-successHandler and the one passed to this function
			var joinedHandler = function(responseText, responseHtml) {
				showFeedbackIfNeeded($link);
				var handlerNames = $link.attr("data-yadaSuccessHandler");
				if (handlerNames===undefined) {
					handlerNames = $link.attr("data-successHandler"); // Legacy
				}
				if (handlerNames!=null) {
					// Can be a comma-separated list of handlers, which are called in sequence
					var handlerNameArray = yada.listToArray(handlerNames);
					for (var i = 0; i < handlerNameArray.length; i++) {
						var dataHandler = window[handlerNameArray[i]];
						if (typeof dataHandler === "function") {
							dataHandler(responseText, responseHtml, $link[0]);
						}
					}
				}
				if (handler != null) {
					handler(responseText, responseHtml, $link[0]);
				}
				deleteOnSuccess($link);
				updateOnSuccess($link, responseHtml);
			}
			
			var url = $(this).attr('href');
			var confirmText = $link.attr("data-confirm");
			if (confirmText!=null) {
				var okButton = $link.attr("data-okButton") || yada.messages.confirmButtons.ok;
				var cancelButton = $link.attr("data-cancelButton") || yada.messages.confirmButtons.cancel;
				yada.confirm(confirmText, function(result) {
					if (result==true) {
						yada.ajax(url, null, joinedHandler==null?joinedHandler:joinedHandler.bind(this), null, $link.attr('data-timeout'));
					}
				}, okButton, cancelButton);
			} else {
				yada.ajax(url, null, joinedHandler==null?joinedHandler:joinedHandler.bind(this));
			}
			return true; // Run other listeners
		})
		$link.removeClass('s_ajaxLink');
		$link.not('.'+markerClass).addClass(markerClass);
	};
	
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
	 * Transform forms into ajax forms: all forms with a class of "s_ajaxForm" will be sent via ajax.
	 * @param handler a function to call upon successful form submission. It can also be specified as a data-successHandler attribute on the form
	 * @param $element the element on which to enable ajax forms, can be null for the entire body
	 */
	yada.enableAjaxForms = function(handler, $element) {
		if ($element==null) {
			$element = $('body');
		}
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
	 * Sends a form via ajax, it doesn't have to have class .s_ajaxForm.
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
		var markerClass = 's_ajaxFormed'; // To prevent double submission
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
				postLoginHandler = null;
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
		var errorObject = null;
		var errorPresent=(statusText!=null && statusText!=='success');
		if (typeof responseText === "string" && yada.startsWith(responseText, 'yadaError:')) {
			errorPresent = true;
			try {
				errorObject = JSON.parse(responseText.substring('yadaError:'.length));
			} catch (e) {
				// keep going
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
	
	////////////////////
	/// Selection
	
	// Returns the value of attributeName as an id (with a leading hash).
	// - element a selector for the element to search
	// - attributeName the attribute of the element that holds an id as its value
	yada.getIdWithHash = function(element, attributeName) {
		var id = $(element).attr(attributeName);
		if (!yada.startsWith(id, "#")) {
			id = "#" + id;
		}
		return id;
	}
	
	// Returns a likely unique id prefixed by the given string
	yada.getRandomId = function(prefix) {
		return prefix + Math.floor(Math.random() * 999999999);  
	} 
	
	/////////////////
	/// URL functions
	
	/**
	 * Joins two url segments taking care of the separator / character
	 */
	yada.joinUrls = function(left, right) {
		if (yada.endsWith(left, "/") && yada.startsWith(right, "/")) {
			return left + right.substring(1);
		}
		if (yada.endsWith(left, "/") || yada.startsWith(right, "/")) {
			return left + right;
		}
		return left + "/" + right;
	}
	
	yada.getResourcePath = function() {
		if (yada.baseUrl==null || yada.resourceDir==null) {
			yada.showErrorModal("Internal Error", "yada library not initialized in yada.getResourcePath()");
			return "";
		} 
		return yada.baseUrl + yada.resourceDir;
	}
	
	// Elimina l'hash da un url, se presente.
	yada.removeHash = function(someUrl) {
		var parts = someUrl.split('#');
		return parts[0];
	}
	
	// Transforms a string after the hash into an object, e.g. #story=132;command=message
	// becomes {story: '132', command : 'message'}
	// - windowLocationHash the url hash value, e.g. #story=132;command=message
	yada.hashToMap = function(windowLocationHash) {
		var result = {};
		var hashString = yada.getHashValue(windowLocationHash); // story=132;command=message
		if (hashString!=null) {
			var segments = hashString.split(';'); // ['story=132', 'command=message']
			for (var i = 0; i < segments.length; i++) {
				var parts = segments[i].split('='); // ['story', '132']
				result[parts[0]]=parts[1];
			}
		}
		return result;
	}
	
	////////////////////
	/// String functions
	
	/**
	 * Split a comma-separated string into an array. Commas can be followed by spaces.
	 * If the input is null, return an empty array.
	 */
	yada.listToArray = function(str) {
		if (str==null) {
			return [];
		}
		return str.split(/, */);
	}
	
	// Ritorna true se str contiene toFind
	yada.stringContains = function(str, toFind) {
		return str.indexOf(toFind) >= 0;
	}
	
	// Returns the last element of a delimiter-separated list of elements in a string.
	// Spaces around the delimiter are ignored.
	// Examples: 
	// yada.getLast("a, b, c", ",") --> "c"
	// yada.getLast("a", ",") --> "a"
	// yada.getLast("", ",") --> ""
	yada.getLast = function(source, separatorChar) {
		var regexp = new RegExp("\\s*" + separatorChar + "\\s*");
		return source.split(regexp).pop();
	}
	
	// Ritorna true se str inizia con prefix
	// http://stackoverflow.com/questions/646628/how-to-check-if-a-string-startswith-another-string
	yada.startsWith = function(str, prefix) {
		return str.lastIndexOf(prefix, 0) === 0;
	}

	/**
	 * Returns true if the string ends with the suffix
	 * http://stackoverflow.com/a/2548133/587641
	 */
	yada.endsWith = function(str, suffix) {
		return str.substr(-suffix.length) === suffix;
	}
	
	// Ritorna ciò che segue lo hash in una stringa. Se non c'è, ritorna undefined
	yada.getHashValue = function(str) {
		return str.split('#')[1];
	}
	
	/**
	 * Aggiunge al valore numerico di un elemento una quantità algebrica eventualmente limitandola a un minimo o massimo
	 * Esempio: yada.numberAdd('#totMessaggiCounter', -1, 0, true);
	 * @param elementSelector id dell'elemento incluso l'hash, e.g. #myCounter, oppure un suo selector qualsiasi come ".myElement > div"
	 * @param toAdd valore numerico da aggiungere algebricamente, può essere positivo o negativo
	 * @param limit valore opzionale oltre il quale il valore non deve passare
	 * @param removeAtLimit se true, quando il limit viene raggiunto, il valore viene rimosso totalmente invece che inserito
	 * @return the number of elements that have been modified (0 if no element found or limit reached)
	 */
	yada.numberAdd = function(elementSelector, toAdd, limit, removeAtLimit) {
		var result = 0;
		$(elementSelector).each(function(){
			var element = $(this);
			var text = element.text();
			var val = parseInt(text, 10);
			if (isNaN(val)) {
				val=0;
			}
			val = val + toAdd;
			var remove=false;
			if (limit != null) {
				if ((toAdd>0 && val>limit) || (toAdd<0 && val<limit)) {
					val=limit;
					remove = removeAtLimit;
				} else {
					result++;
				}
			} else {
				result++;
			}
			if (remove==true) {
				element.text('');
			} else {
				element.text(val);
			}
		});
		return result;
	}
	
	////////////////////
	/// Bootstrap Tweaks
	
	yada.handleCustomPopover = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		$("[data-yadaCustomPopoverId]", $element).not('.s_customPopovered').each(function(){
			var dataIdWithHash = yada.getIdWithHash(this, "data-yadaCustomPopoverId");
			var dataTitle = $(this).attr("data-title"); // Optional
			var hasTitle = $(this).attr("title")==null;
			var dataId = yada.getHashValue(dataIdWithHash);
			var shownFunction = null;
			try {
				shownFunction = eval(dataId+'Shown');
			} catch (e) {
			}
			var hiddenFunction = null;
			try {
				hiddenFunction = eval(dataId+'Hidden');
			} catch (e) {
			}
			var popoverId = yada.getRandomId("cp");
			$(this).popover({
				html : true,
				title: function() {
					var closeButton = '<button type="button" class="yadaclose" aria-hidden="true"><i class="fa fa-times fa-lg"></i></button>';
					// Se è stato specificato data-title, si usa quello, altrimenti si prende il primo div.
					var titleContent="<span>"+dataTitle+"</span>";
					if (dataTitle==null) {
						// Can't just return the div because it would be destroyed on close, so I need to clone it (with all events attached)
						titleContent = $(dataIdWithHash).children("div:first").clone(true);
					}
					return $("<div class='"+dataId+"Title'>").append(closeButton).append(titleContent);
				},
				content: function() {
					// E' sempre il secondo div
					var contentDiv = $(dataIdWithHash).children("div").eq(1);
					if (contentDiv.length==0) {
						return "Internal Error: no popover definition found with id = " + dataId;
					}
					// Can't just return the contentDiv because it would be destroyed on close, so I need to clone it (with all events attached)
					return $("<div class='"+dataId+"Content'>").append(contentDiv.clone(true));
				},
				template: '<div data-yadaid="'+popoverId+'" class="popover" role="tooltip"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content"></div></div>'
			});
			$(this).on('shown.bs.popover', makePopoverShownHandler(popoverId, shownFunction));
			$(this).on('hidden.bs.popover', makePopoverClosedHandler(popoverId, hiddenFunction));
		});
		$('[data-yadaCustomPopoverId]').not('.s_customPopovered').addClass('s_customPopovered');
		
		// Wrap the closure
		function makePopoverShownHandler(divId, shownFunction) {
			return function () {
				var popoverButton = $(this);
				var popoverDiv = $('[data-yadaid="'+divId+'"]');
				$(".popover.in").not(popoverDiv).popover('hide'); // Chiudi tutti gli altri popover
				// $("[data-yadaCustomPopoverId]").not(popoverButton).popover('hide'); // Chiudi tutti gli altri popover
				popoverButton.tooltip('hide'); // Serve nel caso ci sia un tooltip sul pulsante
				$("button.yadaclose", popoverDiv).click(function() {
					$(popoverDiv).popover("hide");
				});
				if (typeof shownFunction == 'function') {
					//var popoverId = $(popoverButton).attr('aria-describedby');
					shownFunction(popoverButton, popoverDiv);
				}
			}
		}
		function makePopoverClosedHandler(divId, hiddenFunction) {
			return function () {
				if (typeof hiddenFunction == 'function') {
					var popoverButton = $(this);
					// var popoverId = $(popoverButton).attr('aria-describedby');
					var popoverDiv = $('[data-yadaid="'+divId+'"]');
					hiddenFunction(popoverButton, popoverDiv);
				}
			}
		}
	}
	
	
	
	///////////////////////////////////
	/// Modal richiesta di conferma ///
	///////////////////////////////////
	//Deve essere incluso dalle pagine che lo usando con
	//<div layout:include="/fragments/modalConfirm :: modal" th:remove="tag"></div>
	
	/**
	 * @param message Text to show
	 * @param callback handler to call after choice. It receives true/false for ok/cancel
	 * @param okButtonText text for the ok button (optional)
	 * @param cancelButtonText text for the cancel button (optional)
	 * @param okShowsPreviousModal if true, shows previous modal (if any) after ok (optional)
	 */
	yada.confirm = function(message, callback, okButtonText, cancelButtonText, okShowsPreviousModal) {
		// okButtonText e cancelButtonText sono opzionali
		var $currentModals = $(".modal:visible");
		hideAllModals();
		// $('#yada-confirm').modal('hide'); // Eventualmente fosse già aperto
		$('#yada-confirm .modal-body p').html(message);
		var previousOkButtonText = $('#yada-confirm .okButton').text();
		if (okButtonText) {
			$('#yada-confirm .okButton').text(okButtonText);
		}
		var previousCancelButtonText = $('#yada-confirm .cancelButton').text();
		if (cancelButtonText) {
			$('#yada-confirm .cancelButton').text(cancelButtonText);
		}
		$('#yada-confirm .okButton').off().click(function(){
			if (okShowsPreviousModal) {
				$currentModals.modal('show');
			}
			if (callback) callback(true);
		});
		$('#yada-confirm .cancelButton').off().click(function(){
			$currentModals.modal('show');
			if (callback) callback(false);
		});
		var $modal = $('#yada-confirm .modal');
		if ($modal.length==0) {
			console.error("No confirm modal found: did you include it?");
		}
		$modal.modal('show');
		$modal.on('hidden.bs.modal', function (e) {
			$('#yada-confirm .okButton').text(previousOkButtonText);
			$('#yada-confirm .cancelButton').text(previousCancelButtonText);
		});		
	}
	
	//////////////////////////////////////
	/// Modal dei messaggi e notifiche ///
	//////////////////////////////////////
	
	yada.showErrorModal = function(title, message, redirectUrl) {
		showNotificationModal(title, message, 'error', redirectUrl);
	}
	
	yada.showOkModal = function(title, message, redirectUrl) {
		showNotificationModal(title, message, 'ok', redirectUrl);
	}
	
	yada.showInfoModal = function(title, message, redirectUrl) {
		showNotificationModal(title, message, 'info', redirectUrl);
	}
	
	function showNotificationModal(title, message, severity, redirectUrl) {
		hideAllModals();
		yada.loaderOff();
		var glyphNames = {ok : 'ok-circle', info : 'exclamation-sign', error : 'remove-circle'};
		// $('#yada-notification').modal('hide'); // Eventualmente fosse già aperto
		$('#yada-notification .modal-title').text(title);
		$('#yada-notification .modal-body').html('<p>'+message+'</p>')
		var icon = $('<span class="glyphicon glyphicon-'+glyphNames[severity]+' '+severity+'"></span>');
		$('#yada-notification span.glyphicon').replaceWith(icon);
		$('#yada-notification:hidden').modal('show');
		if (redirectUrl!=null) {
			$('#yada-notification').on('hidden.bs.modal', function (e) {
				// window.location.href=redirectUrl;
				window.location.replace(redirectUrl);
			});
		}
	}
	
	function hideAllModals() {
		$("#loginModal").modal('hide');
		$("#ajaxModal").modal('hide');
		$('#yada-notification').modal('hide');
		$('#yada-confirm').modal('hide');
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

	/////////////////////
	/// Local Storage ///
	/////////////////////
	
	/* Ritorna true se il local storage è disponibile
	 * @return true se disponibile, false otherwise
	*/
	yada.localStorageAvailable = function() {
		try {
			var storage = window['localStorage'], x = '__storage_test__';
			storage.setItem(x, x);
			storage.removeItem(x);
			return true;
		}
		catch(e) {
			return false;
		}
	}
	
	//////////////
	/// Cookie ///
	//////////////

	// NON USATI NE' CONTROLLATI nè visibili esternamente fatti così
	function setCookie(cname, cvalue, exdays) {
	    var d = new Date();
	    d.setTime(d.getTime() + (exdays*24*60*60*1000));
	    var expires = "expires="+d.toUTCString();
	    document.cookie = cname + "=" + cvalue + "; " + expires;
	}
	
	function getCookie(cname) {
	    var name = cname + "=";
	    var ca = document.cookie.split(';');
	    for(var i=0; i<ca.length; i++) {
	        var c = ca[i];
	        while (c.charAt(0)==' ') c = c.substring(1);
	        if (c.indexOf(name) == 0) return c.substring(name.length,c.length);
	    }
	    return "";
	}
	
//	function checkCookie() {
//	    var username=getCookie("username");
//	    if (username!="") {
//	        alert("Welcome again " + username);
//	    }else{
//	        username = prompt("Please enter your name:", "");
//	        if (username != "" && username != null) {
//	            setCookie("username", username, 365);
//	        }
//	    }
//	}

}( window.yada = window.yada || {} ));


