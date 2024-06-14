// yada.js

(function( yada ) {
	"use strict";
	
	// Namespace trick explained here: http://stackoverflow.com/a/5947280/587641
	// For a public property or function, use "yada.xxx = ..."
	// For a private property use "var xxx = "
	// For a private function use "function xxx(..."

	yada.baseLoaded = true; // Flag that this file has been loaded
	
	yada.devMode = false; // Set to true in development mode (also via thymeleaf)
	yada.baseUrl = null;	// Set it via thymeleaf
	yada.resourceDir = null; // Set it via thymeleaf
	var loaderStart = 0;
	yada.messages = {};
	yada.messages.connectionError = { // Set it via thymeleaf
		"title": "Connection Error",
		"message": "Failed to contact server - please try again later"
	}; 
	yada.messages.forbiddenError = { // Set it via thymeleaf
			"title": "Authorization Error",
			"message": "You don't have permission to access the requested page"
	}; 
	yada.messages.serverError = { // Set it via thymeleaf
			"title": "Server Error",
			"message": "Something is wrong - please try again later"
	}; 
	yada.messages.confirmButtons = { // Set it via thymeleaf
			"ok": "Ok",
			"cancel": "Cancel"
	}; 
	
	var siteMatcher=RegExp("(?:http.?://)?([^/:]*).*"); // Extract the server name from a url like "http://www.aaa.com/xxx" or "www.aaa.com"
	
	const findSelector = "yadaFind:"; // Used to indicate that a CSS selector should be searched in the children using find()
	const parentSelector = "yadaParents:"; // Used to indicate that a CSS selector should be searched in the parents()
	const siblingsSelector = "yadaSiblings:"; // Used to indicate that a CSS selector should be searched in the siblings()
	const closestFindSelector = "yadaClosestFind:"; // Used to indicate that a two-part CSS selector should be searched with closest() then with find()
	const siblingsFindSelector = "yadaSiblingsFind:"; // Used to indicate that a two-part CSS selector should be searched with siblings() then with find()
	
	const sessionStorageKeyTimezone = "yada.timezone.sent";
	const scrollTopParamName = "scrolltop";
	
	yada.stickyModalMarker = "yadaStickyModal";
	
	$(document).ready(function() {
		handleScrollTop();
		// Be aware that all ajax links and forms will NOT be ajax if the user clicks while the document is still loading.
		// To prevent that, call yada.initAjaxHandlersOn($('form,a')); at the html bottom and just after including the yada.ajax.js script
		initHandlers();
		if (typeof yada.initYadaDialect == "function") {
			yada.initYadaDialect();
		}
		
		// Send the current timezone offset to the server, once per browser session
		const timezoneSent = sessionStorage.getItem(sessionStorageKeyTimezone);
		if (!timezoneSent) {
			const data = {
				'timezone': Intl.DateTimeFormat().resolvedOptions().timeZone
			}
			jQuery.post("/yadaTimezone", data, function(){
				sessionStorage.setItem(sessionStorageKeyTimezone, true);
			});
		}
	});
	
	function initHandlers() {
		// Mostra il loader sugli elementi con le classi s_showLoaderClick oppure s_showLoaderForm
		$('body').on('click', ':not(form).yadaShowLoader', yada.loaderOn);
		$('body').on('submit', '.yadaShowLoader', yada.loaderOn);
		// Legacy
		$('body').on('click', '.s_showLoaderClick', yada.loaderOn);
		$('body').on('submit', '.s_showLoaderForm', yada.loaderOn);
		yada.enableScrollTopButton(); // Abilita il torna-su
		yada.initHandlersOn();
		// When the page is pulled from bfcache (firefox/safari) turn the loader off
		$(window).bind("pageshow", function(event) {
		    if (event.originalEvent.persisted) {
		        yada.loaderOff();
		    }
		});		
	}
	
	/**
	 * Init yada handlers on the specified element
	 * @param $element the element, or null for the entire body
	 */
	yada.initHandlersOn = function($element) {
		// Use case for $element being not null: after an ajax call in order to init just the added HTML
		yada.enableParentForm($element);
		yada.enableShowPassword($element);
		yada.enableRefreshButtons($element);
		yada.enableConfirmLinks($element);
		yada.enableHelpButton($element);
		yada.enableTooltip($element);
		yada.makeCustomPopover($element);
		if (typeof yada.initAjaxHandlersOn == "function") {
			yada.initAjaxHandlersOn($element);
		}
		yada.enableHashing($element);
		yada.enableFormGroup($element);
	}
	
	yada.log = function(message) {
		if (yada.devMode) {
			console.log("[yada] " + message);
		}
	}
	
	yada.loaderOn = function() {
		loaderStart = Date.now();
		$(".loader").show();
	};
	
	yada.loaderOff = function() {
		// The loader must be shown at least for 200 milliseconds or it gets annoying
		var elapsedMillis = Date.now() - loaderStart;
		if (elapsedMillis>200) {
			$(".loader").hide();
		} else {
			setTimeout(function(){ $(".loader").hide(); }, 200-elapsedMillis);
		}
	};

	
	/**
	 * Execute a comma-separated list of function names or an inline function (a function body).
	 * Each function is called only if the previous one didn't return null.
	 * See https://stackoverflow.com/a/359910/587641
	 * @param functionList comma-separated list of function names, in the window scope, that can have namespaces like "mylib.myfunc".
	 *			It can also be an inline function (with or without function(){} declaration).
	 * @param thisObject the object that will become the this object in the called function
	 * Any number of arguments can be passed to the function
	 * @return the functions return value in "and" (with null being true), or null if there was no function/body in the list.
	 */
	 yada.executeFunctionListByName = function(functionList, thisObject /*, optional args are also taken */) {
		var args = Array.prototype.slice.call(arguments, 2); // creates a new array containing all arguments starting from the third one
		var result = true;
		// Try the case where there's a list of function names
		var functionArray = yada.listToArray(functionList); // Split at comma followed by any spaces
		for (var i = 0; i < functionArray.length; i++) {
			const functionResult = yada.executeFunctionByName(functionArray[i], thisObject, ...args);
			if (functionResult==null) {
				result = null;
				break;
			}
			result &&= functionResult;
		}
		if (result==null && functionArray.length>1) {
			// Could be a function body containing a comma, so try the whole string
			 result = yada.executeFunctionByName(functionList, thisObject, ...args);
		}
		if (result==null) {
			yada.log("Invalid function list: " + functionList);
		}
		return result;
	}
		
	/**
	 * Execute function by name. Also execute an inline function (a function body).
	 * See https://stackoverflow.com/a/359910/587641
	 * @param functionName the name of the function, in the window scope, that can have namespaces like "mylib.myfunc".
	 *			It can also be an inline function (with or without function(){} declaration).
	 * @param thisObject the object that will become the this object in the called function
	 * Any number of arguments can be passed to the function
	 * @return the function return value (with null converted to true), or null in case of error calling the function (e.g. function not found or invalid function body)
	 */
	 yada.executeFunctionByName = function(functionName, thisObject /*, args */) {
		var context = window; // The functionName is always searched in the current window
		var args = Array.prototype.slice.call(arguments, 2); // creates a new array containing all arguments starting from the third one
		var namespaces = functionName.split(".");
		var func = namespaces.pop();
		for(var i = 0; i < namespaces.length && context!=null; i++) {
			context = context[namespaces[i]];
		}
		var functionObject = context?context[func]:null;
		if (functionObject==null) {
			// It might be a function body
			try {
				var functionBody = functionName.trim();
				// Strip any "function(){xxx}" declaration
				if (yada.startsWith(functionName, "function(")) {
					functionBody = functionName.replace(new RegExp("(?:function\\s*\\(\\)\\s*{)?([^}]+)}?"), "$1");
				}
				functionObject = new Function('responseText', 'responseHtml', 'link', functionBody); // Throws error when not a function body
			} catch (error) {
				// console.error(error);
				// yada.log("Function '" + func + "' not found (ignored)");
				return null;
			}
		}
		const returnValue = functionObject?.apply(thisObject, args);
		// null is converted to true (e.g. "keep going")
		return returnValue ?? true;
	}
	
	/**
	 * Changes the browser url when an element is clicked
	 */
	yada.enableHashing = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		$('[data-yadaHash]', $element).not(".yadaHashed").click(function(){
			var hashValue = $(this).attr('data-yadaHash')
			const newUrl = yada.replaceHash(window.location.href, hashValue);
			history.pushState({'yadaHash': true, 'hashValue' : hashValue}, null, newUrl);
		}).addClass("yadaHashed");
	}

	yada.enableTooltip = function($element) {
		if ($element==null) {
			$element = $('body');
		}
	    $('.s_tooltip', $element).tooltip && $('.s_tooltip', $element).tooltip();
	};
	
	yada.enableHelpButton = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		$('.yadaHelpButton', $element).popover && $('.yadaHelpButton', $element).popover();
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
			var handlerName = $(this).attr('data-yadaRefreshHandler');
			if (handlerName===undefined) {
				handlerName = $(this).attr('data-handler'); // Legacy
			}
			var dataHandler = window[handlerName];
			if (typeof dataHandler === "function") {
				$(this).click(dataHandler);
				// Call it now also
				dataHandler();
			}
			$(this).addClass('yadaRefreshed');
		});
	}
	
	/**
	 * Enable the "scroll to top" button.
	*/
	yada.enableScrollTopButton = function() {
		const $scrollTopButton = $('.yadaScrollTop');
		if ($scrollTopButton.length>0) {
			$scrollTopButton.off().click( function(e){
				e.preventDefault();
				window.scrollTo({ top: 0, left: 0, behavior: 'smooth'});
			});
			$(document).on('scroll', function() {
				const visible = $scrollTopButton.is(":visible");
				var y = $(this).scrollTop();
				if (y > 800) {
					visible || $scrollTopButton.fadeIn();
				} else {
					visible && $scrollTopButton.fadeOut();
				}
			});
		}
	};

	/**
	 * When a function can be called repeatedly but only the last call is useful, previous
	 * calls can be cancelled by next ones if within a given timeout.
	 * When the funcion takes too long to execute, the timeout is increased so that less calls are performed.
	 * Useful when making ajax calls.
	 * A small delay must be tolerated.
	 * @param domElement any dom element on which a flag can be set. Must be the same for repeated calls.
	 * @param functionToCall any function (can be an inline function)
	 */
	yada.dequeueFunctionCall = function(domElement, functionToCall) {
		// TODO see https://css-tricks.com/debouncing-throttling-explained-examples/#aa-debounce
		var callTimeout = 200;
		if (domElement.yadaDequeueFunctionCallRunning!=null) {
			// Ajax call still running, so delay a bit longer before the next one
			callTimeout = 2000;
		}
		clearTimeout(domElement.yadaDequeueFunctionTimeoutHandler);
		domElement.yadaDequeueFunctionTimeoutHandler = setTimeout(function(){
			domElement.yadaDequeueFunctionCallRunning = true;
			functionToCall.bind(domElement)();
			domElement.yadaDequeueFunctionCallRunning = null; // This may clear some other's call flag but don't care
		}, callTimeout);
	}

	/**
	 * Allow only certain characters to be typed into an input field based on a regexp
	 * Taken from https://stackoverflow.com/questions/995183/how-to-allow-only-numeric-0-9-in-html-inputbox-using-jquery/995193#995193
	 * See /YadaWeb/src/main/resources/net/yadaframework/views/yada/formfields/input.html for an example
	 */	
	$.fn.yadaInputFilter = function(inputFilter) {
		return this.on("input keydown keyup mousedown mouseup select contextmenu drop", function() {
			if (inputFilter(this.value)) {
				this.oldValue = this.value;
				this.oldSelectionStart = this.selectionStart;
				this.oldSelectionEnd = this.selectionEnd;
			} else if (this.hasOwnProperty("oldValue")) {
				this.value = this.oldValue;
				this.setSelectionRange(this.oldSelectionStart, this.oldSelectionEnd);
			} else {
				this.value = "";
			}
		});
	}
	
	//////////////////////
	/// Url Parameters ///
	//////////////////////
	
	yada.addScrollTop = function(url) {
		return yada.addOrUpdateUrlParameter(url, scrollTopParamName, $(window).scrollTop());
	}
	
	function handleScrollTop() {
		const scrollTopValue = yada.getUrlParameter(window.location.href, scrollTopParamName);
		if (scrollTopValue!=null) {
			$(window).scrollTop(scrollTopValue);
			history.replaceState(null, "", yada.removeUrlParameter(window.location.href, scrollTopParamName));
		}
	}
	
	// Ritorna true se la url contiene il parametro indicato
	yada.hasUrlParameter = function(url, param) {
		return yada.getUrlParameter(url, param) !=null;
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
		if (yada.endsWith(url, '&')) {
			url = url.substring(0, url.length-1);
		}
		// '?' needs to be reinserted if first param was removed
		var pos = url.indexOf('&');
		if (pos>-1 && url.indexOf('?')==-1) {
			url = yada.replaceAt(url, pos, '?');
		}
	 return url;
	};
	
	// Rimuove dalla url tutti i parametri di request indicati e la ritorna (forse non funziona se il parametro è senza valore)
	yada.removeUrlParameters = function(url, param) {
		var regex = new RegExp("[?|&]" + param + "=[^&]+&?", 'g');
		url = url.replace(regex, '&');
		if (yada.endsWith(url, '&')) {
			url = url.substring(0, url.length-1);
		}
		// '?' needs to be reinserted if first param was removed
		var pos = url.indexOf('&');
		if (url.indexOf('?')==-1 && pos>-1) {
			url = yada.replaceAt(url, pos, '?');
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
	
	// Modifica o aggiunge un parametro di request alla url. La url può anche essere solo la location.search e può essere vuota.
	// if papram is null or empty, the url is unchanged.
	yada.addOrUpdateUrlParameter = function(url, param, value, addQuestionMark) {
		if (param==null || param=="") {
			return url;
		}
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
	 * Returns an url parameter when found, null when not found or empty
	 * Adapted from http://stackoverflow.com/questions/2090551/parse-query-string-in-javascript
	 * @param url can be a url, a query string or even part of it, or null; everything before "?" or "&" will be skipped.
	 */
	yada.getUrlParameter = function(url, varName){
		 var queryStr = url + '&';
		 var regex = new RegExp('.*?[&\\?]' + varName + '=(.*?)[&#].*');
		 var val = queryStr.replace(regex, "$1");
		 return val == queryStr ? null : unescape(val);
	};

	/**
	 * Returns an URLSearchParams object: https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams.
	 * It can be iterated upon with:
	 * for (var nameValue of yada.getUrlParameters(url).entries()) {
	 * 		const name = nameValue[0];
	 * 		const value = nameValue[1];
	 * }
	 * @param url can be a url, a query string or even part of it, or null
	 */
	yada.getUrlParameters = function(url) {
		// Keep query string only
		url = yada.getAfter(url, "?", 0);
		url = yada.removeHash(url);
		return new URLSearchParams(url);
	};

	//Rimpiazza un singolo carattere
	yada.replaceAt = function(str, index, character) {
	 return str.substr(0, index) + character + str.substr(index+character.length);
	};

	// Ritorna l'url passata senza la query string
	yada.removeQuery = function(url) {
		return url.replace(/\?.*/, '');
	}

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
	// The anchor is stripped
	yada.getPathVariable = function(url, precedingSegment) {
		var segments = yada.removeQuery(yada.removeHash(url)).split('/');
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
	
	/**
	 * Transform links into confirm links: all anchors with a "data-yadaConfirm" attribute that don't have a class of "yadaAjax" 
	 * will show a confirm box before submission. If yadaAjax is present, see "yada.ajax.js"
	 */
	yada.enableConfirmLinks = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		var markerClass = 's_dataConfirmed';
		// For the ajax version see yada.ajax.js
		$('a[data-yadaConfirm], a[data-confirm]', $element.parent()).not('.'+markerClass).not('.yadaAjax').not('.yadaAjaxed').each(function() {
			$(this).click(function(e) {
				var $link = $(this);
				e.preventDefault();
				var href = $link.attr("href");
				var confirmText = $link.attr("data-yadaConfirm") || $link.attr("data-confirm");
				if (confirmText!=null) {
					var title = $link.attr("data-yadaTitle");
					var okButton = $link.attr("data-yadaOkButton") || $link.attr("data-okButton") || yada.messages.confirmButtons.ok;
					var cancelButton = $link.attr("data-yadaCancelButton") || $link.attr("data-cancelButton") || yada.messages.confirmButtons.cancel;
					var okShowsPreviousModal = $link.attr("data-yadaOkShowsPrevious")==null || $link.attr("data-yadaOkShowsPrevious")=="true";
					yada.confirm(title, confirmText, function(result) {
						if (result==true) {
							yada.loaderOn();
							window.location.replace(href);
						}
					}, okButton, cancelButton, okShowsPreviousModal);
				} else {
					yada.loaderOn();
					window.location.replace(href);
				}
			});
			$(this).addClass(markerClass);
		});
	};
	

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
	
	/**
	* Returns a likely unique id optionally prefixed by the given string
	*/
	yada.getRandomId = function(prefix) {
		return (prefix || "") + Math.floor(Math.random() * 99999999999).toString(16);  
	} 
	
	/////////////////
	/// URL functions
	
	/**
	 * Extract the server address from a url, for example "www.example.com" from "http://www.example.com/path"
	 */
	yada.getServerAddress = function(url) {
		return url.replace(siteMatcher, "$1");
	}
	
	/**
	 * Joins two url segments taking care of the separator / character
	 */
	yada.joinUrls = function(left, right) {
		if (right==null) {
			return left;
		}
		if (yada.endsWith(left, "/") && yada.startsWith(right, "/")) {
			return left + right.substring(1);
		}
		if (yada.endsWith(left, "/") || yada.startsWith(right, "/")) {
			return left + right;
		}
		return left + "/" + right;
	}
	
	/**
	 * Joins up to five urls taking care of correct slash separator.
	 * Parameters from 'two' onwards are optional.
	*/
	yada.joinManyUrls = function(one, two, three, four, five) {
		var result = yada.joinUrls(one, two);
		result = yada.joinUrls(result, three);
		result = yada.joinUrls(result, four);
		result = yada.joinUrls(result, five);
		return result;
	}
	
	yada.getResourcePath = function() {
		if (yada.baseUrl==null || yada.resourceDir==null) {
			yada.showErrorModal("Internal Error", "yada library not initialized in yada.getResourcePath()");
			return "";
		} 
		return yada.baseUrl + yada.resourceDir;
	}
	
	// Ritorna ciò che segue lo hash in una stringa. Se non c'è nulla, ritorna ''
	yada.getHashValue = function(str) {
		if (str!=null && str!='') {
			return str.split('#')[1];
		}
		return str;
	}
	
	/**
	 * Replaces the current hash value with the new one, or adds the new one if no hash is present.
	 * @param someUrl any string with an optional hash character
	 * @param newHashValue some string to place after the existing hash value, or to add at the end following a new hash character
	 */
	yada.replaceHash = function(someUrl, newHashValue) {
		return yada.removeHash(someUrl) + '#' + yada.removeHash(newHashValue);
	}
	
	// Elimina l'hash (anchor) da un url, se presente.
	yada.removeHash = function(someUrl) {
		var parts = someUrl.split('#');
		return parts[0];
	}
	
	/**
	 * Removes the current hash from the url, if any, without a page reload or a page scroll.
	 * @param replaceState true to replace the current history entry, false to add to the history
	 * See https://stackoverflow.com/a/5298684/587641
	 */
	yada.removeCurrentHash = function(replaceState) {
		const cleanUrl = window.location.origin + window.location.pathname + window.location.search;
		if (replaceState==true) {
			history.replaceState("", document.title, cleanUrl);
		} else {
			history.pushState("", document.title, cleanUrl);
		}
	}
	
	// Transforms a string after the hash into an object, e.g. #story=132;command=message
	// becomes {story: '132', command : 'message'}
	// - windowLocationHash the url hash value, e.g. #story=132;command=message
	yada.hashToMap = function(windowLocationHash) {
		var result = {};
		var hashString = yada.getHashValue(windowLocationHash); // story=132;command=message
		if (hashString!=null && hashString.length>0) {
			var segments = hashString.split(';'); // ['story=132', 'command=message']
			for (var i = 0; i < segments.length; i++) {
				var parts = segments[i].split('='); // ['story', '132']
				result[parts[0]]=parts[1];
			}
		}
		return result;
	}
	
	/**
	 * Convert a location hash value to a map. For example, if propertyList=['id', 'name'] and windowLocationHash='#123-joe' and separator='-',
	 * the result is {'id':'123', 'name':'joe'}
	 * When there are more properties than values, the extra properties are set to the empty string.
	 * When there are more values than properties, the extra values are ignored.
	 * @param propertyList an array of map keys
	 * @param windowLocationHash the value of location.hash (starts with #)
	 * @param separator the separator for the values in the hash
	 * @return an object where to each property in the list corresponds a value from the hash
	 */
	yada.hashPathToMap = function(propertyList, windowLocationHash, separator) {
		var result = {};
		var segments = [];
		var hashString = yada.getHashValue(windowLocationHash); // 834753/myslug
		if (hashString!=null && hashString.length>0) {
			segments = hashString.split(separator);
		}
		for (var i = 0; i < propertyList.length; i++) {
			var name = propertyList[i];
			if (i<segments.length) {
				result[name] = segments[i];
			} else {
				result[name] = '';
			}
		}
		return result;
	}
	
	////////////////////
	/// Array functions

	/**
	 * Check if an array contains some value, that can also be an object (comparing same keys)
	 */
	// TODO not tested !!!
	yada.arrayContains = function(array, value) {
		if (!(value instanceof Object)) {
			return array.includes(value);
		}
		const valueKeys = Object.keys(value);
		for (let i=0; i<array.length; i++) {
			const existing = array[i];
			if (existing instanceof Object) {
				// Check if objects have the same keys 
				const existingKeys = Object.keys(existing);
				// https://stackoverflow.com/a/7726509/587641
				const equal = $(valueKeys).not(existingKeys).length === 0 && $(existingKeys).not(valueKeys).length === 0;
				if (equal) {
					return true;
				}
			}
		}
		return false;
	}

	////////////////////
	/// String functions

	/**
	 * Returns true if the argument is a string that contains some non-whitespace characters
	 */
	yada.stringNotEmpty = function(str) {
		return str!=null && typeof str=="string" && str.trim().length>0;
	}

	/**
	 * Converts a sentence to title case: each first letter of a word is uppercase, the rest lowercase.
	 * It will also convert "u.s.a" to "U.S.A" and "jim-joe" to "Jim-Joe"
	 * Adapted from https://stackoverflow.com/a/196991/587641
	 */
	yada.titleCase = function(sentence) {
		 return sentence.replace(
			 /\w[^\s-.]*/g, // The regex means "word char up to a space or - or dot": https://www.w3schools.com/jsref/jsref_obj_regexp.asp
			 	function(txt) {
	                return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
	            }
	     );
	}

	/**
	 * Replaces a template like "My name is ${name}" with its value. The value can be a string or a number or an array of strings/numbers.
	 * @param template the template string
	 * @param replacements an object whose attributes have to be searched and replaced in the string, e.g. replacements.name="Joe"
	 * @returns
	 */
	 yada.templateReplace = function(template, replacements) {
		for (name in replacements) {
			if (name!=null) {
				var placeholder = '\\$\\{'+name+'\\}';
				var value = replacements[name];
				if (typeof value != 'object') {
					template = template.replace(new RegExp(placeholder, 'g'), value);
				} else {
					for (var i=0; i<value.length; i++) {
						if (i<value.length-1) {
							template = template.replace(new RegExp(placeholder, 'g'), value[i]+"${"+name+"}");
						} else {
							template = template.replace(new RegExp(placeholder, 'g'), value[i]);
						}
					}
				}
			}
		}
		return template;
	}

	/**
	 * Returns the portion of string that follows the first match of some substring
	 */
	yada.getAfter = function(str, toFind, fromIndex) {
		if (str==null) {
			return str;
		}
		var pos = str.indexOf(toFind, fromIndex);
		if (pos>=0) {
			return str.substring(pos+toFind.length);
		}
		return str;
	}
	
	/**
	 * Split a comma-separated string into an array. Commas can be followed by spaces.
	 * If the input is null, return an empty array. If the value is numeric, return an array with the value converted to string
	 */
	yada.listToArray = function(str) {
		if (str==null) {
			return [];
		}
		if (typeof str != "string") {
			return [str.toString()];
		}
		return str.split(/, */);
	}
	
	// Ritorna true se str contiene toFind
	yada.stringContains = function(str, toFind) {
		return str!=null && typeof str=="string" && str.indexOf(toFind) >= 0;
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
		return str!=null && typeof str=="string" && str.lastIndexOf(prefix, 0) === 0;
	}

	/**
	 * Returns true if the string ends with the suffix
	 * http://stackoverflow.com/a/2548133/587641
	 */
	yada.endsWith = function(str, suffix) {
		return str!=null && typeof str=="string" && str.substr(-suffix.length) === suffix;
	}
	
	/**
	 * Returns the smallest portion of the string inside the prefix and suffix, if found, otherwise return the empty string.
	 */
	yada.extract = function(str, prefix, suffix) {
		const regex = new RegExp(escapeRegExp(prefix) + "(.*?)" + escapeRegExp(suffix));
		const matched = regex.exec(str);
		if (matched!=null && matched.length>1 && matched[1]!=null) {
			return matched[1];
		}
		return "";
	}
	
	function escapeRegExp(string) {
		// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_Expressions
  		return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); // $& means the whole matched string
	}
	
	/**
	 * Increment a numeric value.
	 * @param elementSelector the jquery selector that identifies the element(s) to increment 
	 */
	yada.increment = function(elementSelector) {
		return yada.numberAdd(elementSelector, 1);
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

	/**
	 * Create a Bootstrap 5 popover with custom HTML content
	 * @param $element (optional) a jQuery element that contains one or more popover anchors; null for <body>
	 **/
	yada.makeCustomPopover = function($element) {
		if (typeof bootstrap != 'object' || typeof bootstrap.Popover != 'function' ) {
			return yada.handleCustomPopoverB3($element); // Bootstrap 3
		}
		$element = $element || $('body');
		$("[data-yadaPopover]", $element).not('.yadaPopovered').each(function(){
			const trigger = this;
			const $trigger = $(trigger);
			$trigger.addClass('yadaPopovered');
			const htmlIdWithHash = yada.getIdWithHash(trigger, "data-yadaPopover"); // id of the HTML for the popover
			if (htmlIdWithHash!=null && htmlIdWithHash!="" && htmlIdWithHash!="#") {
				//const htmlId = yada.getHashValue(htmlIdWithHash);
				const contentInstanceId = yada.getRandomId("yada");
				// Using "content" because the HTML is supposed to be in a <template>
				const originalTemplate = document.querySelector(htmlIdWithHash)?.content || $(htmlIdWithHash, $element)[0]?.content;
				const htmlTemplate =  originalTemplate.cloneNode(true);
				// const closeButton = '<button type="button" class="btn-close yadaclose" data-bs-toggle="popover" aria-label="Close"></button>';
				// Check if the anchor is in a modal
				var container = 'body';
				var $modalContainer = $trigger.closest(".modal-body").first();
				if ($modalContainer.length>0) {
					container = $modalContainer[0];
				}
				var defaultTitle = $trigger.attr("data-yadaTitle"); // Optional
				if (defaultTitle==null) {
					defaultTitle = htmlTemplate.querySelector("label:first-child")?.innerHTML;
				}
				const content = $("<div id='"+contentInstanceId+"'>").append($(htmlTemplate).children(":not(label:first-child)")).prop('outerHTML');
				const popoverObject = new bootstrap.Popover(trigger, {
					html : true,
	  				title: "<div>" + defaultTitle + "</div>", //+closeButton,
	  				container: container,
					content: content,
					sanitize: false // Bootstrap sanitizer is too restrictive and customizing allowList is too much work
	  				// template: '<div data-yadaid="'+popoverId+'" class="popover" role="tooltip"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content"></div></div>'
				})
				
				// Replace some listeners with improved ones
				const yadaEventListeners = getYadaEventHandlers($trigger);
				const insertedListener = yadaEventListeners['inserted.bs.popover']?.handler;
				const shownListener = yadaEventListeners['shown.bs.popover']?.handler;
				if (insertedListener!=null) {
					$trigger.off('inserted.bs.popover').on('inserted.bs.popover', makePopoverInsertedFunction(trigger, popoverObject, contentInstanceId, insertedListener));
				}
				if (shownListener!=null) {
					$trigger.off('shown.bs.popover').on('shown.bs.popover', makePopoverShownFunction(trigger, popoverObject, contentInstanceId, shownListener));
				}
			}
		})
	}
	function makePopoverInsertedFunction(trigger, popoverObject, contentInstanceId, listener) {
		return function(e) {
			const $popoverElement = $("#"+contentInstanceId).closest(".popover");
			$popoverElement.find("button.yadaclose").click(function(){popoverObject.hide()});			
			$popoverElement.find("[data-bs-toggle=popover]").click(function(){popoverObject.hide()});	
			if (listener!=null) {
				// Execute any "inserted" handler with proper arguments in the trigger context
				yada.executeFunctionByName(listener, trigger, e, $popoverElement, popoverObject);
			}		
		}
	}
	function makePopoverShownFunction(trigger, popoverObject, contentInstanceId, listener) {
		return function(e) {
			const $popoverElement = $("#"+contentInstanceId).closest(".popover");
			$(".popover.show").not($popoverElement).popover('hide'); // Close all other popovers
			if (listener!=null) {
				// Execute any "shown" handler with proper arguments in the trigger context
				yada.executeFunctionByName(listener, trigger, e, $popoverElement, popoverObject);
			}		
		}
	}
	
	///////////////////////
	/// Bootstrap 3 Popover
	
	// @Deprecated use yada.makeCustomPopover() instead
	yada.handleCustomPopover = function($element) {
		yada.log("yada.handleCustomPopover() is deprecated in favor of yada.makeCustomPopover()");
		return yada.makeCustomPopover($element);
	}
	
	yada.handleCustomPopoverB3 = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		$("[data-yadaCustomPopoverId]", $element).not('.s_customPopovered').each(function(){
			$(this).addClass('s_customPopovered');
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
	 * @param title optional title
	 * @param message Text to show
	 * @param callback handler to call after choice. It receives true/false for ok/cancel
	 * @param okButtonText text for the ok button (optional)
	 * @param cancelButtonText text for the cancel button (optional)
	 * @param okShowsPreviousModal if true, shows previous modal (if any) after ok (optional)
	 */
	yada.confirm = function(title, message, callback, okButtonText, cancelButtonText, okShowsPreviousModal) {
		// okButtonText e cancelButtonText sono opzionali
		var $currentModals = hideAllModals($("#yada-confirm"));
		var okClicked = false;
		var cancelClicked = false;
		// Turn off the loader else the confirm dialog won't show
		yada.loaderOff();
		// $('#yada-confirm').modal('hide'); // Eventualmente fosse già aperto
		if (title) {
			$('#yada-confirm .modal-header .confirm-title').html(title);
		}
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
			okClicked=true;
			cancelClicked=false;
			if (typeof callback == "function") callback(true);
		});
		$('#yada-confirm .cancelButton').off().click(function(){
			cancelClicked=true;
			okClicked=false;
			if (typeof callback == "function") callback(false);
		});
		var $modal = $('#yada-confirm .modal');
		if ($modal.length==0) {
			console.error("[yada] No confirm modal found: did you include it?");
		}
		$modal.modal('show');
		$modal.off('hidden.bs.modal').on('hidden.bs.modal', function (e) {
			$('#yada-confirm .okButton').text(previousOkButtonText);
			$('#yada-confirm .cancelButton').text(previousCancelButtonText);
			if (cancelClicked || (okClicked && okShowsPreviousModal==true)) {
				if ($currentModals.length>0) {
					// Show the previous modals again on cancel or when okShowsPreviousModal is true
					$currentModals.css('display', 'block');
				}
			} else {
				// Not cancelled and not okShowsPreviousModal.
				// Just for consistency, I restore the modals then hide them properly
				$currentModals.css('display', 'block');
				$currentModals.modal("hide"); 
			}
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
		$(".modal").modal("hide"); // Hide previous existing modals
		yada.loaderOff();
		var glyphNames = {ok : 'ok', info : 'info', error : 'error'};
		// $('#yada-notification').modal('hide'); // Eventualmente fosse già aperto
		$('#yada-notification .modal-title').text(title);
		$('#yada-notification .modal-body').html('<p>'+message+'</p>')
		var icon = $('<i class="yadaIcon yadaIcon-'+glyphNames[severity]+' '+severity+'"></i>');
		$('#yada-notification h4 i.yadaIcon').replaceWith(icon);
		$('#yada-notification:hidden').modal('show');
		if (redirectUrl!=null) {
			$('#yada-notification').on('hidden.bs.modal', function (e) {
				// window.location.href=redirectUrl;
				window.location.replace(redirectUrl);
			});
		}
	}
	
	/**
	 * Make all current visible modals not visible.
	 * @return the hidden modals
	*/
	function hideAllModals($notThese) {
		const $modals = $("#loginModal:visible")
			.add($(".modal.show."+yada.markerAjaxModal+":visible"))
			.add($("#yada-notification:visible"))
			.add($("#yada-confirm:visible"))
			.not($notThese);
		// Do not use modal('hide') because it may trigger some events that shouldn't be triggered
		$modals.css("display", "none");
		// Also remove the dark layer because it would be doubled otherwise
		// $(".modal-backdrop.fade.show").first().removeClass("show");
		return $modals;		
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

	/**
	 * Set a cookie on the document root
	 * @param name the cookie name
	 * @param value the cookie value
	 * @param expiryDays expiration in days from now. When null, create a session cookie
	**/
	yada.setCookie = function(name, value, expiryDays, domain) {
		var expires = "";
		if (expiryDays!=null) {
		    var d = new Date();
		    d.setDate(d.getDate() + expiryDays);
		    expires = ";expires="+d.toGMTString();
		}
		domain = domain!=null ? ";domain=" + domain : "";
	    document.cookie = name + "=" + value + domain + " ;path=/ " + expires;
	}
	
	yada.getCookie = function(cname) {
	    var name = cname + "=";
	    var ca = document.cookie.split(';');
	    for(var i=0; i<ca.length; i++) {
	        var c = ca[i];
	        while (c.charAt(0)==' ') c = c.substring(1);
	        if (c.indexOf(name) == 0) return c.substring(name.length,c.length);
	    }
	    return "";
	}
	
	yada.deleteCookie = function(name, domain) {
		yada.setCookie(name, "", 0, domain);
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

	////////////
	/// i18n ///
	////////////

	/**
	 * Change the current language by changing the language path variable, i.e. from /en/somePage to /de/somePage
	 * @param language the new language, e.g. "de"
	 */
	yada.changeLanguagePathVariable = function(language) {
		var currentPath = window.location.pathname; // /xx/somePage
		var regex = new RegExp("/[^/]+");
		var verifyLanguage = currentPath.match(regex);
		if (verifyLanguage[0].length != 3) {
			// The language path was missing - insert it.
			currentPath = "/"+language + currentPath;
		} else {
			// Replace the current language path with the new one
			currentPath = currentPath.replace(regex, "/"+language);
		}
		window.location.pathname = currentPath;
	}

	/////////////
	/// Forms ///
	/////////////

	/**
	 * Icon that shows a password field in cleartext
	 */
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
	
	
	function checkMatchingPasswords($oneForm) {
		var $pwd = $('input[name=password]', $oneForm);
		var $check = $('input[name=confirmPassword]', $oneForm);
		var $submit = $('button[type="submit"]', $oneForm);
		if ($pwd==null || $check==null) {
			return;
		}
		var v1 = $pwd.val();
		var v2 = $check.val();
        if(v1!="" && v2!="" && v1 == v2){
        	resetPasswordError($oneForm);
        } else {
        	$submit.attr("disabled", "disabled");
        	if (v1!="" || v2!="") {
        		$oneForm.addClass('has-error');
        		$oneForm.addClass('yada-password-mismatch');
        		// $check.parent().append('<span class="bi bi-x form-control-feedback" aria-hidden="true"></span>');
        	}
        }
    }

	function resetPasswordError($oneForm){
		var $pwd = $('input[name=password]', $oneForm);
		var $check = $('input[name=confirmPassword]', $oneForm);
		var $submit = $('button[type="submit"]', $oneForm);
        $submit.removeAttr("disabled");
        $oneForm.removeClass('has-error');
        $oneForm.removeClass('yada-password-mismatch');
        // $('span.form-control-feedback.bi-x', $check.parent()).remove();
    }

	/**
	 * Prevent form submission when the password field and its confirmation field don't match.
	 * The password field must be named "password". The confirmation field must be named "confirmPassword".
	 * The password confirmation field gets a 'has-error' class on its parent form-group (bootstrap standard).
	 * @param $forms the form on which to enable the check
	 */
	yada.enablePasswordMatch = function($forms) {
		// Start with the submit button disabled, then enable it when the two fields match
		var $submit = $('button[type="submit"]', $forms);
		$submit.attr("disabled", "disabled");
		//
		$('input[name=password], input[name=confirmPassword]', $forms).on("keyup", function(e){
			checkMatchingPasswords($(this).parents("form"));
	     })
	     .on("blur", function(){                    
	        // also check when the element looses focus (clicks somewhere else)
	        checkMatchingPasswords($(this).parents("form"));
	    })
//	    .on("focus", function(){
//	        // reset the error message when they go to make a change
//	        resetPasswordError($(this).parents("form"));
//	    })
	}
	
	/**
	 * Returns a jquery element searched using the extended yada selector prefixes. The empty selector is the $fromElement
	 * @param $fromElement the element to start from. Ignored if no yada prefix is used: the selector will be searched in all the document.
	 * @param selector the CSS selector prefixed with a yada prefix (or not)
	 */
	yada.extendedSelect = function($fromElement, selector) {
		if (selector == null || selector.trim()=="") {
			return $fromElement;
		}
		var fromChildren = yada.startsWith(selector, findSelector); // yadaFind:
		var fromParents = yada.startsWith(selector, parentSelector); // yadaParents:
		var fromSiblings = yada.startsWith(selector, siblingsSelector); // yadaSiblings:
		var fromClosestFind = yada.startsWith(selector, closestFindSelector); // yadaClosestFind:
		var fromSiblingsFind = yada.startsWith(selector, siblingsFindSelector); // yadaSiblingsFind:
		if (fromChildren==false && fromParents==false && fromSiblings==false && fromClosestFind==false && fromSiblingsFind==false) {
			return $(selector);
		} else if (fromChildren) {
			selector = selector.replace(findSelector, "").trim();
			return $fromElement.find(selector);
		} else if (fromParents) {
			selector = selector.replace(parentSelector, "").trim();
			return $fromElement.parent().closest(selector);
		} else if (fromSiblings) {
			selector = selector.replace(siblingsSelector, "").trim();
			return $fromElement.siblings(selector);
		} else if (fromClosestFind) {
			selector = selector.replace(closestFindSelector, "").trim();
			var splitSelector = selector.split(" ", 2);
			return $fromElement.parent().closest(splitSelector[0]).find(splitSelector[1]);
		} else if (fromSiblingsFind) {
			selector = selector.replace(siblingsFindSelector, "").trim();
			var splitSelector = selector.split(" ", 2);
			return $fromElement.siblings(splitSelector[0]).find(splitSelector[1]);
		}
		// Should never get here
		return $fromElement;
	}
	
	// Remove a field from a form, but only if it is going to be submitted
	function removeFormField(form, field) {
		// form and field are DOM elements, not jQuery
		// https://stackoverflow.com/a/53366001/587641
		if (field.disabled || ((field.type=='radio' || field.type=='checkbox') && !field.checked)) {
			return;
			// TODO need to check for list options?
		}
		const fieldName = field.name;
        Array.prototype.forEach.call(form.elements, function (element) {
             if (fieldName!='' && element.name === fieldName) {
                 element.parentNode.removeChild(element);
             }
        });
	}
	
	// Adds a field to a form, but only if it is not already present and enabled/checkd
	function addMissingFormField(form, field) {
		const existingField = form.elements[field.name];
		if (existingField!=null && existingField.disabled!=true) {
			return;
		}
		form.appendChild(field);
	}
	
	// This is used by yada.ajax.js too (for non-ajax submit)
	yada.addFormGroupFields = function($form, $formGroup) {
		// Append all other inputs to the current form and let it submit normally.
		// Non need to clone anything because the page will be reloaded anyway (not ajax here).
		if ($formGroup.length>1) {
			$formGroup.each(function() {
				let $eachForm = $(this);
				if (!$eachForm.is($form)) {
					$eachForm.find(":input").each(function() { // All inputs including textarea etc.
						let $field = $(this);
						$field.css("display", "none");
						// addMissingFormField($form.get(0), $field.get(0)); // No duplicates: same name is ignored
						removeFormField($form.get(0), $field.get(0)); // No duplicates: same name overrides previous
						$field.appendTo($form);
					});
				}
			});
		}
	}
	
	/**
	 * Enable form groups for non-ajax forms and any clickable element
	 */
	yada.enableFormGroup = function($root) {
		if ($root==null) {
			$root = $('body');
		}
		var $target = $root.parent();
		if ($target.length==0) {
			$target = $root;
		}
		$('form', $target).not('.yadaAjax').not('.yadaAjaxed').submit(function() {
			const $form = $(this);
			// Add all other form inputs in the group
			var yadaFormGroup = $form.attr('data-yadaFormGroup');
			if (yadaFormGroup!=null && this.yadaFormGroupAdded==null) {
				// Find all forms of the same group
				const $formGroup = $('form[data-yadaFormGroup='+yadaFormGroup+']');
				yada.addFormGroupFields($form, $formGroup);
			}
			// continue normal submission...
		});
		
		// Other clickable elements that are not forms and not ajax (usually an anchor)
		$('[data-yadaFormGroup]', $target).not('form').not('.yadaAjax').not('.yadaAjaxed').click(function(e) {
			const $clickedElement = $(this);
			var yadaFormGroup = $clickedElement.attr('data-yadaFormGroup');
			if (yadaFormGroup!=null) {
				const $formGroupForms = $('form[data-yadaFormGroup='+yadaFormGroup+']');
				// If there are no forms, do nothing special
				if ($formGroupForms.length==0) {
					return;
				}
				e.preventDefault(); // Prevent links from sending requests
				/* TODO not sure is needed.
				// Find all elements of the same group that are not forms, and gather name/value pairs
				const $formGroupNoforms = $('[data-yadaFormGroup='+yadaFormGroup+']').not('form');
				const nameValues = []; // e.g. [{product: 'shoe'}, {price: 132}, {...}]
				if ($formGroupNoforms.length>1) {
					$formGroupNoforms.each(function() {
						let $eachElement = $(this);
						if (!$eachElement.is($clickedElement)) {
							// Get name/value of input, checkbox, radio, select, textarea...
							// TODO not implemented yet. See yada.ajax.js in function makeAjaxCall() near "option:selected" for hints						
						}
					});
				} */
				// Submit any of the forms in the group, after adding the nameValues, using the url of the clicked element if any
				const $toSubmit = $formGroupForms.first();
				yada.addFormGroupFields($toSubmit, $formGroupForms);
				$toSubmit[0].yadaFormGroupAdded=true; // Prevent adding the form group inputs again 
				// The new action can also be on a data-href attribute
				const newAction = $clickedElement.attr("href") || $clickedElement.attr("data-href");
				if (newAction!=null) {
					$toSubmit.attr("action", newAction);
					// Add any url parameters to the form
					for (var nameValue of yada.getUrlParameters(newAction).entries()) {
						const name = nameValue[0];
						const value = nameValue[1];
						const input = document.createElement('input');
    					input.setAttribute('name', name);
    					input.setAttribute('value', value);
    					input.setAttribute('type', 'hidden');
    					removeFormField($toSubmit.get(0), input); // No duplicates: url param overrides form
						$toSubmit.append(input);
					}
				}
				/* TODO not sure is needed.
				if (nameValues.length>0) {
					// Not tested yet
					for (const nameValue of nameValues) {
						const keys = Object.keys(nameValue);
						 for (var i = 0; i < keys.length; i++) {
        					const name = keys[i];
        					const value = nameValue[name];
							const input = document.createElement('input');
        					input.setAttribute('name', name);
        					input.setAttribute('value', value);
        					input.setAttribute('type', 'hidden');
							$toSubmit.append(input);
						}
					}
				}
				*/
				$toSubmit.submit();
				return false;
			} // if (yadaFormGroup!=null)
		});
	}
	
	/**
	 * Enable yadaParentForm
	 * @Deprecated Should use "formGroup" instead
	 */
	yada.enableParentForm = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		var $target = $element.parent();
		if ($target.length==0) {
			$target = $element;
		}
		// Set the form hierarchy
		$('form[data-yadaParentForm]', $target).each(function() {
			$(this).submit(function(e) {
				// When the form is submitted, set this form as the child of the parent and submit the parent instead
				e.preventDefault();
				var $thisForm = $(this);
				var parentFormSelector = $thisForm.attr('data-yadaParentForm');
				// Find and submit all parent forms (only one parent makes sense generally)
				var $parentFormArray = yada.extendedSelect($thisForm, parentFormSelector); 
				if ($parentFormArray!=null) {
					for (var i = 0; i < $parentFormArray.length; i++) {
						var parentForm = $parentFormArray[i];
						if (parentForm.nodeName.toLowerCase()=="form") {
							parentForm['yadaChildForm'] = $thisForm; // Overwrite any previous child form
						}
					}
					$parentFormArray.submit();
//					var parentForm = $parentFormArray[0];
//					if (parentForm.nodeName.toLowerCase()=="form") {
//						// Replace the current form with a new form composed of all merged input elements
//						var $newform=$(parentForm).clone(true); // Clone also the submit handlers
//						$thisForm.children().appendTo($newform);
//						// Submit the new form and stop the current submission
//						e.preventDefault();
//						$newform.submit();
//					}
				}
				
			});
		});
		// Handle the submission of non-ajax forms
		$('form').not('.yadaAjax').not('.yadaAjaxed').submit(function(e) {
			// Recursively add all child forms
			var $form = $(this);
			var $childForm = this['yadaChildForm'];
			// This doesn't work properly because the parent form is modified and after a browser back it is still modified
			// therefore the previous child fields are submitted with the new ones.
			//			while ($childForm != null) {
			//				$childForm.children().filter("input, textarea, select").appendTo($form);
			//				$childForm = $childForm['yadaChildForm'];
			//			}
			//			// continue normal submission...
			//
			if ($childForm!=null) {
				// Replace the current form with a new form composed of all merged input elements
				var $newform=$($form).clone(true); // Clone also the submit handlers
				$(document.body).append($newform); // Needed to submit the form (https://stackoverflow.com/a/42081856/587641)
				$childForm.children().filter("input, textarea, select").clone().appendTo($newform);
				// Submit the new form and stop the current submission
				e.preventDefault();
				$newform.submit();
				return false;
			}
		});
	}
	
	/**
	 * Find a child from a given parent that can also be the first node (root) of the tree, 
     * where normally it would not be found by $.find('parentSelector childSelector', $html)
     * Note: we can't just always use $html.parent().find() because it gets any sibilings outside of $html
	 * @param $html where to perform the search, can be an array of root nodes
	 * @param parentSelector the CSS selector of the parent element
	 * @param childSelector the CSS selector of the child element
	*/
	yada.findFromParent = function(parentSelector, childSelector, $html) {
		var $root = $html.find(parentSelector);
		if ($root.length==0) {
			$root = $html.filter(parentSelector);
		}
		return $root.find(childSelector);
	}
	
	// From https://stackoverflow.com/a/69122877/587641
	// Neither tested not used - could be removed
	yada.toTimeAgo = function(dateInThePast, locale) {
		const date = (dateInThePast instanceof Date) ? dateInThePast : new Date(dateInThePast);
		const formatter = new Intl.RelativeTimeFormat(locale);
		const ranges = {
			years: 3600 * 24 * 365,
			months: 3600 * 24 * 30,
			weeks: 3600 * 24 * 7,
			days: 3600 * 24,
			hours: 3600,
			minutes: 60,
			seconds: 1
		};
		const secondsElapsed = (date.getTime() - Date.now()) / 1000;
		for (let key in ranges) {
			if (ranges[key] < Math.abs(secondsElapsed)) {
			  const delta = secondsElapsed / ranges[key];
			  return formatter.format(Math.round(delta), key);
			}
		}
	}
	
	/**
	* Returns the names of all event handlers added with data-yadaEventHandlers="eventName1:handler1, eventName2:handler2, ..."
	* @return an associative array of event/handler objects where the key is the event name.
	*/
	function getYadaEventHandlers($element) {
		const allHandlers = $element.attr("data-yadaEventHandlers"); // Comma separated of event:function couples
		const result = [];
		if (yada.stringNotEmpty(allHandlers)) {
			const segments = allHandlers.split(/ *, */); // commma with any number of spaces
			for (var i=0; i<segments.length; i++) {
				const listener = {};
				const nameValue = segments[i].split(/ *: */);
				if (nameValue.length==2) {
					listener.event = nameValue[0];
					listener.handler = nameValue[1];
					result[nameValue[0]]=listener;
				}
			}
		}
		return result;
	}
		
}( window.yada = window.yada || {} ));

// jquery extension: find including the root nodes
// https://stackoverflow.com/a/62190609/587641
// Usage: $(element).findWithSelf('.target')
// --> will also find the root element if it is a .target
// Does not work properly with selectors that match a child after self, like ".self .child"
jQuery.fn.findWithSelf = function(...args) {
  return this.pushStack(this.find(...args).add(this.filter(...args)));
};
