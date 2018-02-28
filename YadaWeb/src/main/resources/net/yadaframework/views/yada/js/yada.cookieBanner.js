/* Script to include when you need to show a cookie banner
 * It has no dependencies. (IE9+)
 * Usage:
 * yada.cookieBanner("Optional text for the banner", "Optional text for the Accept button")
 * the banner text can contain HTML and a link to the privacy page, which has to have the class ".privacyLink" for it not to close the banner
 * The banner can by styled by the "#yadaCookieBanner" selector.
 * The close icon can be styled, if needed, with the ".closeIcon" class.
 * Example:
 * yada.cookieBanner("This site uses cookies. More details <a class='privacyLink' target='_blank' href='http://example.com/privacy.html'>here</a>.");
 * CSS Example:
 * 
	#yadaCookieBanner {
		background-color: black;
		color: white;
	    position: absolute;
	    top: 0;
	    right: 0;
	    left: 0;
	    padding: 0 0 10px 5px;
	    z-index: 9876543;
	}
	#yadaCookieBanner .closeIcon {
		position: absolute;
	    right: 10px;
	    top: -8px;
	    cursor: pointer;
	    width: 20px;
	    height: 20px;
	    background-image: url("/a/close.png");
	    background-repeat: no-repeat;
	    margin-top: 5px;
	}
	#yadaCookieBanner p {
		display: inline-block;
	    margin-right: 10px;
	    margin-top: 20px;
	}
	#yadaCookieBanner a {
		color: white;
	}

 */

(function( yada ) {
	"use strict";
	
	// Namespace trick explained here: http://stackoverflow.com/a/5947280/587641
	// For a public property or function, use "yada.xxx = ..."
	// For a private property use "var xxx = "
	// For a private function use "function xxx(..."

	var cookieAcceptName="yadacba";
	var bannerId = "yadaCookieBanner";

	/**
	 * Function to call externally for showing the cookie banner when needed
	 * @param infoText
	 * @param acceptButtonText can be null for the default text, empty for no accept link.
	 * @param noHideOnScroll true to prevent banner close on scroll
	 * @param noHideOnClick true to prevent banner close on site click
	 */
	yada.cookieBanner = function(infoText, acceptButtonText, noHideOnScroll, noHideOnClick) {
		// If cookie not present, insert banner in page
		if (document.cookie.indexOf(cookieAcceptName+"=true")<0) {
			if (infoText==null) {
				infoText = 'This site uses cookies. Do not use this site if you don\'t agree to the use of cookies.';
			}
			if (acceptButtonText==null) {
				acceptButtonText = 'Accept';
			}
			var bannerHtml = '<div id="'+bannerId+'">'
				+ '<p>' + infoText + '</p>'
				+ '<div class="closeIcon"></div>'
				;
			if (acceptButtonText!="") {
				bannerHtml+= '<a href="#">' + acceptButtonText + '</a>';
			}
			bannerHtml+= '</div>';
			
			document.body.insertAdjacentHTML('afterbegin', bannerHtml);

			// Add the handlers
			if (noHideOnClick!=true) {
				document.body.addEventListener("click", bodyClickHandler);
			}
			if (noHideOnScroll!=true) {
				window.addEventListener("scroll", scrollHandler);
			}
		}
	}
	
	// Hide the banner when clicking anywhere on the page
	function bodyClickHandler(e) {
		if (e.target.className!="privacyLink") {
			document.body.removeEventListener("click", bodyClickHandler);
			bannerDismiss();
		}
	};
	
	// Hide the banner when scrolling
	function scrollHandler(e) {
		window.top.removeEventListener("scroll", scrollHandler);
		bannerDismiss();
	};
	
	// Hide the banner
	function bannerDismiss() {
		document.cookie = cookieAcceptName + "=true; expires=Thu, 01 Jan 2100 00:00:00 UTC; path=/";
		var banner = document.getElementById(bannerId);
		if (banner) {
			banner.parentNode.removeChild(banner);
		}
	}

}( window.yada = window.yada || {} ));


