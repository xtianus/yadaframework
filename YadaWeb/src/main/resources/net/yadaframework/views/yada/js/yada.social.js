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
		// TODO replace with yada.ajax and get rid of yada.handlePostLoginHandler
		$.get(serverUrl, 
			{ accessToken: accessToken }, 
			function(responseText, statusText) {
				yada.loaderOff();
				var responseHtml=$("<div>").html(responseText);
				var modalShown = handleLoadedModal(responseHtml); // Il risultato è il modal di registrazione social, che viene mostrato
				var callbackCalled = yada.callYadaCallbackIfPresent(responseHtml); // Qui succede il redirect
				if (!modalShown && !callbackCalled) {
					// Il risultato è il contenuto originariamente richiesto, che viene passato all'handler, oppure se non c'è l'handler si ricarica la pagina corrente
					yada.handlePostLoginHandler(responseHtml, responseText);
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
	
	/**
	 * Google log out (still valid for 2023 version)
	 * See https://developers.google.com/identity/sign-in/web/sign-in#sign_out_a_user
	 */
	yada.googleSignOut = function() {
		if (gapi!=undefined && gapi.auth2!=undefined) {
			var authInstance = gapi.auth2.getAuthInstance();
			if (authInstance!=undefined) {
				authInstance.signOut();
			}
		}
	}
	
	/**
	 * Function called by the google login button - old
	 */
	var handleGoogleLoginButtonDEPRECATED = function(serverUrl, googleUser) {
		var profile = googleUser.getBasicProfile();
		//		console.log('ID: ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
		//		console.log('Name: ' + profile.getName());
		//		console.log('Image URL: ' + profile.getImageUrl());
		//		console.log('Email: ' + profile.getEmail()); // This is null if the 'email' scope is not present.
		var accessToken = googleUser.getAuthResponse().id_token;
		yada.ajax(serverUrl, { accessToken: accessToken }, function(responseText, responseHtml) {
			// Nothing to do (remove the function)
		}, "POST");
	}

	/**
	 * Enables the google login button via Google API. - old
	 * @param id the element that will be replaced by the generated button
	 * @param serverUrl the application url where the token will be sent
	 * @param options the google render options: https://developers.google.com/identity/sign-in/web/reference#gapisignin2renderid-options
	 */
	yada.enableGoogleLoginButtonDEPRECATED = function(serverUrl, id, options) {
		options.onsuccess = function(googleUser) {
			handleGoogleLoginButton(serverUrl, googleUser);
		}
		gapi.signin2.render(id, options);
	}
	
	/**
	 * Enables the facebook login button via Facebook SDK.
	 * @param serverUrl the application url where the token will be sent
	 * @param handler a function to call after login
	 */
	yada.enableFacebookLoginButton = function(serverUrl, handler) {
		$('.facebookLoginButton').click(function(e) {
			e.preventDefault();
			yada.loaderOn();
			$('#loginModal').modal('hide');
			FB.login(function(response) {
				yada.postLoginHandler = handler; // To be removed
				facebookLoginResult(response, serverUrl);
			}, {scope: 'email', auth_type: 'rerequest'}); // auth_type is because if someone doesn't give email permission, access would be prevented forever 
		});
	}
}( window.yada = window.yada || {} ));
