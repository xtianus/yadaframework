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
		yada.enableConfirmLinks($element);
		yada.enableHelpButton($element);
		yada.enableTooltip($element);
		yada.handleCustomPopover($element);
		if (typeof yada.initAjaxHandlersOn == "function") {
			yada.initAjaxHandlersOn($element);
		}
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
	yada.getPathVariable = function(url, precedingSegment) {
		var segments = yada.removeQuery(url).split('/');
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
	
	/**
	 * Transform links into confirm links: all anchors with a "data-confirm" attribute that don't have a class of "yadaAjax" 
	 * will show a confirm box before submission.
	 */
	yada.enableConfirmLinks = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		var markerClass = 's_dataConfirmed';
		$('a[data-confirm]', $element.parent()).not('.'+markerClass).not('.yadaAjax').each(function() {
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
		window.location.pathname = currentPath.replace(regex, "/"+language);
	}
	
	
}( window.yada = window.yada || {} ));


