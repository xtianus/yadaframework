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
	
	var parentSelector = "yadaParents:"; // Used to indicate that a CSS selector should be searched in the parents()
	var siblingSelector = "yadaSiblings:"; // Used to indicate that a CSS selector should be searched in the siblings()
	var closestFindSelector = "yadaClosestFind:"; // Used to indicate that a two-part CSS selector should be searched with closest() then with find()
	
	$(document).ready(function() {
		// Be aware that all ajax links and forms will NOT be ajax if the user clicks while the document is still loading.
		// To prevent that, call yada.initAjaxHandlersOn($('form,a')); at the html bottom and just after including the yada.ajax.js script
		initHandlers();
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
		yada.enableParentForm($element);
		yada.enableShowPassword($element);
		yada.enableRefreshButtons($element);
		yada.enableConfirmLinks($element);
		yada.enableHelpButton($element);
		yada.enableTooltip($element);
		yada.handleCustomPopover($element);
		if (typeof yada.initAjaxHandlersOn == "function") {
			yada.initAjaxHandlersOn($element);
		}
		yada.enableHashing($element);
	}
	
	yada.loaderOn = function() {
		loaderStart = Date.now();
		$(".loader").show();
	};
	
	yada.loaderOff = function() {
		var elapsedMillis = Date.now() - loaderStart;
		if (elapsedMillis>100) {
			$(".loader").hide();
		} else {
			setTimeout(function(){ $(".loader").hide(); }, 100-elapsedMillis);
		}
	};
	
	/**
	 * Changes the browser url when an element is clicked
	 */
	yada.enableHashing = function($element) {
		if ($element==null) {
			$element = $('body');
		}
		$('a[data-yadaHash], button[data-yadaHash]', $element).not(".yadaHashed").click(function(){
			var hashValue = $(this).attr('data-yadaHash')
			history.pushState({'yadaHash': true}, null, window.location.pathname + '#' + hashValue)
		}).addClass("yadaHashed");
	}

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
	 * Returns an url parameter when found, null when not found
	 * Adapted from http://stackoverflow.com/questions/2090551/parse-query-string-in-javascript
	 * @param url can be a url, a query string or even part of it, but everything before "?" or "&" will be skipped
	 */
	yada.getUrlParameter = function(url, varName){
		 var queryStr = url + '&';
		 var regex = new RegExp('.*?[&\\?]' + varName + '=(.*?)[&#].*');
		 var val = queryStr.replace(regex, "$1");
		 return val == queryStr ? null : unescape(val);
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
		$('a[data-yadaConfirm], a[data-confirm]', $element.parent()).not('.'+markerClass).not('.yadaAjax').each(function() {
			$(this).click(function(e) {
				var $link = $(this);
				e.preventDefault();
				var href = $link.attr("href");
				var confirmText = $link.attr("data-yadaConfirm") || $link.attr("data-confirm");
				if (confirmText!=null) {
					var okButton = $link.attr("data-yadaOkButton") || $link.attr("data-okButton") || yada.messages.confirmButtons.ok;
					var cancelButton = $link.attr("data-yadaCancelButton") || $link.attr("data-cancelButton") || yada.messages.confirmButtons.cancel;
					var okShowsPreviousModal = $link.attr("data-yadaOkShowsPrevious")==null || $link.attr("data-yadaOkShowsPrevious")=="true";
					yada.confirm(confirmText, function(result) {
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
	
	// Ritorna ciò che segue lo hash in una stringa. Se non c'è nulla, ritorna ''
	yada.getHashValue = function(str) {
		if (str!=null) {
			return str.split('#')[1];
		}
		return str;
	}
	
	// Elimina l'hash (anchor) da un url, se presente.
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
		var hashString = yada.getHashValue(windowLocationHash); // 834753/myslug
		if (hashString!=null && hashString.length>0) {
			var segments = hashString.split(separator);
			for (var i = 0; i < propertyList.length; i++) {
				var name = propertyList[i];
				if (i<segments.length) {
					result[name] = segments[i];
				} else {
					result[name] = '';
				}
			}
		}
		return result;
	}
	
	////////////////////
	/// String functions
	
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
	 * @param message Text to show
	 * @param callback handler to call after choice. It receives true/false for ok/cancel
	 * @param okButtonText text for the ok button (optional)
	 * @param cancelButtonText text for the cancel button (optional)
	 * @param okShowsPreviousModal if true, shows previous modal (if any) after ok (optional)
	 */
	yada.confirm = function(message, callback, okButtonText, cancelButtonText, okShowsPreviousModal) {
		// okButtonText e cancelButtonText sono opzionali
		var $currentModals = $(".modal:visible");
		var okClicked = false;
		var cancelClicked = false;
		hideAllModals();
		// Turn off the loader else the confirm dialog won't show
		yada.loaderOff();
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
			okClicked=true;
			cancelClicked=false;
			if (callback) callback(true);
		});
		$('#yada-confirm .cancelButton').off().click(function(){
			cancelClicked=true;
			okClicked=false;
			if (callback) callback(false);
		});
		var $modal = $('#yada-confirm .modal');
		if ($modal.length==0) {
			console.error("No confirm modal found: did you include it?");
		}
		$modal.modal('show');
		$modal.off('hidden.bs.modal').on('hidden.bs.modal', function (e) {
			$('#yada-confirm .okButton').text(previousOkButtonText);
			$('#yada-confirm .cancelButton').text(previousCancelButtonText);
			if (cancelClicked || (okClicked && okShowsPreviousModal==true)) {
				$currentModals.modal('show');
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
		$("#loginModal:visible").modal('hide');
		$("#ajaxModal:visible").modal('hide');
		$('#yada-notification:visible').modal('hide');
		$('#yada-confirm:visible').modal('hide');
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

	yada.setCookie = function(name, value, expiryDays) {
	    var d = new Date();
	    // d.setTime(d.getTime() + (expiryDays*24*60*60*1000));
	    d.setDate(d.getDate() + expiryDays);
	    var expires = "expires="+d.toGMTString()+"; path=/";
	    document.cookie = name + "=" + value + "; " + expires;
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
        		$check.parent().append('<span class="glyphicon glyphicon-remove form-control-feedback" aria-hidden="true"></span>');
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
        $('span.form-control-feedback.glyphicon-remove', $check.parent()).remove();
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
	 * @param $fromElement the element to start from. Ignored if no yada prefix is used.
	 * @param selector the CSS selector prefixed with a yada prefix (or not)
	 */
	yada.extendedSelect = function($fromElement, selector) {
		if (selector == null || selector.trim()=="") {
			return $fromElement;
		}
		var fromParents = yada.startsWith(selector, parentSelector); // yadaParents:
		var fromSiblings = yada.startsWith(selector, siblingSelector); // yadaSiblings:
		var fromClosestFind = yada.startsWith(selector, closestFindSelector); // yadaClosestFind:
		if (fromParents==false && fromSiblings==false && fromClosestFind==false) {
			return $(selector);
		} else if (fromParents) {
			selector = selector.replace(parentSelector, "").trim();
			return $fromElement.parent().closest(selector);
		} else if (fromSiblings) {
			selector = selector.replace(siblingSelector, "").trim();
			return $fromElement.siblings(selector);
		} else if (fromClosestFind) {
			selector = selector.replace(closestFindSelector, "").trim();
			var splitSelector = selector.split(" ", 2);
			return $fromElement.parent().closest(splitSelector[0]).find(splitSelector[1]);
		}
		// Should never get here
		return $fromElement;
	}
	
	/**
	 * Enable yadaParentForm
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
			while ($childForm != null) {
				$childForm.children().filter("input, textarea, select").appendTo($form);
				$childForm = $childForm['yadaChildForm'];
			}
			// continue normal submission...
		});
	}
	

	
}( window.yada = window.yada || {} ));


