****
Ajax
****
 
.. rubric::
	Easy ajax operations even without javascript

Description
===========

The Yada Framework support for ajax calls is implemented in ``yada.ajax.js``.
It is automatically included in production, merged with the other yada javascript files. To use it in development, add:

.. code-block:: html

	<script th:if="${@config.developmentEnvironment}" yada:src="@{/yadares/js/yada.ajax.js}"></script>

Ajax is set up via css classes and data attributes in the HTML source. Some data attributes have yada-dialect equivalents which are more readable and easier to use with thymeleaf expressions.

Ajax Links
==========
Calling the backend
-----------------------
To make an ajax request, add the ``yadaAjax`` class:

.. code-block:: html

	<a href="/some/endpoint" class="yadaAjax">Click here</a>

When clicking on the anchor, the ``/some/endpoint`` url is called via ajax.

.. todo:: Check that I can use yada:ajax="url" and data-yadaHref="url" instead of href and class. href should be "javascript:;" in that case.

Miscellaneous
^^^^^^^^^^^^^^^^^^^^^^
timeout
  You can set a timeout on the ajax call with ``data-yadaTimeout="<milliseconds>"``
 
disable the link
	The class ``yadaDisabled`` disables the link

Returning from the backend
--------------------------
Controllers can return a standard thymeleaf view (html/xml) or some JSON data.
The resulting objects will be passed to any "successHandler" javascript method (see below)
when they don't have other special meaning.

**Returning JSON**

A String map can easily be converted to JSON using the standard Spring features:

.. code-block:: java

    @RequestMapping(path = "/people", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Map<String, Object> people() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", "Kim");
        map.put("age", 44);
        return map;
    }

Please note the needed ``@ResponseBody`` tag.

**Yada Commands**

The following return values, defined on the YadaViews java class, have special meaning in the context of an ajax call.
Please note that you should NOT use the ``@ResponseBody`` tag in this case.

.. list-table::
  :widths: 50 50
  :header-rows: 1

  *	- view name
	- description
  *	- AJAX_SUCCESS
	- Do nothing on the browser
  *	- AJAX_REDIRECT
	- Perform a redirect on the browser. It uses the Model attributes shown below
  *	- AJAX_RELOAD
	- Perform a page reload	 
  *	- AJAX_CLOSE_MODAL
	- Close any modal that might be open
  *	- AJAX_SERVER_ERROR
	- Opens a modal with an error message that by default is 'Server Error' unless a Model attribute with a custom message has been added. It uses the Model attributes shown below

The AJAX_REDIRECT and AJAX_SERVER_ERROR commands use these optional Model attributes:

.. list-table::
  :widths: 20 30 50
  :header-rows: 1

  *	- view name
	- attribute name
	- description
  *	- AJAX_REDIRECT
	- AJAX_REDIRECT_URL
	- The target absolute url
  *	- AJAX_REDIRECT
	- AJAX_REDIRECT_URL_RELATIVE
	- The target url relative to the webapp, used if AJAX_REDIRECT_URL is not set
  *	- AJAX_REDIRECT
	- AJAX_REDIRECT_NEWTAB
	- Set this attribute to true to open the redirect page in a new tab. Browser popups must be enabled by the user
  *	- AJAX_SERVER_ERROR
	- AJAX_SERVER_ERROR_DESCRIPTION
	- The custom error message to put in the Model
	
..	todo:: examples

.. _ajax-postprocessing:

Postprocessing
-------------------
After an ajax call, you usually want to do something on the page: update some div, show a modal, change a javascript variable etc.
The following ``data-`` attributes allow you to perform postprocessing when returning successfully (i.e. with no network errors and no ``YadaNotify`` errors) from the call.

.. list-table:: data- attributes for ajax postprocessing
  :widths: 25 25 50
  :header-rows: 1

  *	- name
	- value
	- description
  *	- ``data-yadaUpdateOnSuccess``
	- jQuery selector list
	- replace the selector targets with the result of the ajax call, or replace each selector target with a different part of the result (see below)
  *	- ``data-yadaDeleteOnSuccess``
	- jQuery selector list
	- delete the target elements
  *	- ``data-yadaSuccessHandler``
	- comma-separated list of function names
	- call the specified functions

Yada-dialect variants:

.. list-table::
  :widths: 25 25
  :header-rows: 0

  *	- ``data-yadaUpdateOnSuccess``
	- ``yada:updateOnSuccess``
  *	- ``data-yadaDeleteOnSuccess``
	- ``yada:deleteOnSuccess``
  *	- ``data-yadaSuccessHandler``
	- ``yada:successHandler``


Replacing and Deleting
^^^^^^^^^^^^^^^^^^^^^^
The "jQuery selector list" is a comma-separated list of jQuery selectors, like ``"#someId, .someClass > a"``.  
If the selector list is empty, the target is the element itself.
If the selector is an #id, you should ensure that the same id is not present in the returned ajax content or the result might be unexpected.

Each selector can also have the following special prefixes:

.. list-table::
  :widths: 25 50
  :header-rows: 1

  *	- name
	- description
  *	- ``yadaParents:``
	- the selector is searched in the parents of the current element using ``$.closest()``
  *	- ``yadaSiblings:``
	- the selector is searched in the siblings of the current element
  *	- ``yadaClosestFind:``
	- splits the selector at the first space then uses ``$.closest()`` with the first part and ``$.find()`` with the second


**Multiple replacement values**

If the selector list has many targets and the result contains as many elements tagged with the class ``yadaFragment``, then each target is given a different ``yadaFragment`` element. 
When there are more targets than replacements, the last replacement is repeated.

.. todo:: Examples (see OneNote)


Calling some Handler
^^^^^^^^^^^^^^^^^^^^^^
The success handlers are called in sequence and should have the following signature:

.. code-block:: html

	function someHandler(responseText, responseHtml, link) {

responseText
	either the unparsed text received from the ajax call, or a json object if the response text is json

responseHtml
	the ajax response converted to html objects

link
	the original anchor object (DOM, not jQuery)

Modal Dialog
^^^^^^^^^^^^^^^^^^^^^^
To open a modal returned by an ajax call, see :doc:`ajaxModal`.

Confirm Dialog
^^^^^^^^^^^^^^^^^^^^^^
You can show a confirm dialog before the ajax call is made. The user will be shown a text message and an option to confirm or abort the call.

.. list-table:: data- attributes for Confirm Dialog
  :widths: 25 50
  :header-rows: 1

  *	- name
	- description
  *	- ``data-yadaConfirm``
	- text to show in the dialog
  *	- ``data-okButton``
	- (optional) text of the confirm button
  *	- ``data-cancelButton``
	- (optional) text of the cancel button



Ajax Forms
==========

.. todo:: all. Remember that button handlers receive the button itself: function editTaskFormHandler(responseText, responseHtml, form, button) {

Ajax on other elements
========================
Ajax calls can also be made on other HTML elements like buttons and selects by means of the ``data-yadahref`` attribute or the equivalent ``yada:ajax`` dialect.

Ajax on checkbox
----------------
An ajax call can be originated by a state change in a checkbox. The checkbox must NOT be inside a form otherwise the form would be submitted instead.

.. code-block:: html
	
	<input yada:ajax="@{/product/onOff(productId=${product.id})}" 
		th:name="enabled" th:checked="${product.enabled}" type="checkbox" />




.. todo:: complete list of ajaxifyable elements. Is the yadaAjax class needed? Examples.
	showFeedbackIfNeeded

Ajax method
========================
You can call the low-level yada.ajax() method directly.

.. code-block:: javascript

	yada.ajax(url, data, successHandler, method, timeout, hideLoader, asJson, responseType)

- url
	the server address to call
- data
	(optional) string or object to send to the server
- successHandler
	(optional) javascript method to call after returning from the server (see below)
- method
	(optional) either "GET" (default) or "POST"
- timeout
	(optional) milliseconds timeout, null for default (set by the browser)
- hideLoader
	(optional) true for not showing the spinning loader (shown by default)
- asJson
	(optional) true to send the data object as json without splitting the attributes into request parameters
- responseType
	(optional) the XMLHttpRequest.responseType; use "blob" to download binary data like a pdf file

Everything that applies to the other forms of invocation (opening modals, showing login pages, ...) also applies.

URL
---
The url must point to the controller handling the request. If the javascript code is in an HTML file, the standard thymeleaf ``[[@{/path}]]`` syntax can be used.
If the code is in a js file, the url will have to be passed to the script using some global variable set inside the html file:

.. code-block:: html

    window.myUrl = [[@{/path}]]

data
----
The data object is a standard jQuery.ajax() data object. This means it will be converted using the jQuery conversion rules.

To send some name/value pairs you could therefore use the following code:

.. code-block:: javascript

    var data = {};
    data.name = "John";
    data.surname = "Doe";

The above would result in two request parameters named "name" and "surname" that can be read on the controller in the usual way:

.. code-block:: java

    @RequestMapping("/addUser")
    public String addUser(String name, String surname, Model model) {

To send a json object, the ``asJson`` flag must be true:

.. code-block:: javascript

    var data = {name: 'john', surname: 'Doe'};
    yada.ajax(url, data, null, "POST", null, false, true);

The controller will then be able to receive a converted Java object:

.. code-block:: java

    @RequestMapping("/addUser")
    public String addUser(@RequestBody NameSurname data, Model model) {

where ``NameSurname`` is a Java class with the ``name`` and ``surname`` String attributes.

To send a "multipart/form-data" request the data object must be a FormData:

.. code-block:: javascript

    var data = new FormData();
    data.append("someBinaryArray", blob);
    data.append("someText", text);
    yada.ajax(url, data, null, "POST");

This would be equivalent to sending a form via ajax after setting its fields.
The controller should have a ``MultipartFile`` argument for each binary part:

.. code-block:: java

    @RequestMapping("/addUser")
    public String addUser(MultipartFile someBinaryArray, String someText, Model model) {

More info on binary uploads can be found in :ref:`File Uploads <forms/uploads:JAVA>`.

Success Handler
----------------
The success handler is called when the server returns without errors:

.. code-block:: javascript

	successHandler(responseText, responseHtml)
	
- responseText
	the raw original text returned by the server, or a json object if json was returned
- responseHtml
	the original response converted to a div with jQuery.html()
	
The successHandler is not invoked if the call returns with a YadaNotify error, unless the ``executeAnyway`` flag is true:

.. code-block:: javascript

	successHandler.executeAnyway=true


Class Reference
===============

yadaAjax
	Change the standard behavior of the element so that it calls the server via ajax 

yadaAjaxButtonOnly
	When set on an ajax form, make the form ajax only if the clicked button also has the yadaAjax class.
	Otherwise the form will be sent with a normal non-ajax request.




TO BE CONTINUED
