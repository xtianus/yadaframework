// yada.social.js
// Depends on yada.ajax.js

(function( yada ) {
	"use strict";
	
	// Namespace trick explained here: http://stackoverflow.com/a/5947280/587641
	// For a public property or function, use "yada.xxx = ..."
	// For a private property use "var xxx = "
	// For a private function use "function xxx(..."

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
			yada.postLoginHandler = null;
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
				yada.postLoginHandler = handler;
				facebookLoginResult(response, serverUrl);
			}, {scope: 'email'}); 
		});
	}
}( window.yada = window.yada || {} ));
