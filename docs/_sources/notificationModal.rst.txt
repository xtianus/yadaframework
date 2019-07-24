******************
Notification Modal
******************

.. rubric::
	Opening a modal with a message for the user

Description
===========

Show a feedback message for the user in a new modal by calling a java or javascript api.

The modal has a title, one or more messages, a severity for each message:

.. image:: _static/img/modal-notify-1.jpg

Prerequisites
====================

The HTML page must contain the following placeholder:

.. code-block:: html

	<div th:include="/yada/modalNotify :: modal" class="modal fade" id="yada-notification" role="dialog"></div>
	<script th:if="${YADA_NBODY!=null}" type="text/javascript">
		$('#yada-notification').modal('show');
	</script>
	<script type="text/javascript">
		$('#yada-notification').on('hidden.bs.modal', function (e) {
			$('#yada-notification .glyphicon').addClass("hidden");
		});
	</script>
	
This is usually inserted in a common footer so that you don’t need to remember adding it to each page.

.. todo:: make a dialect tag for this

Java API
==========

The modal can be opened after a standard or ajax HTTP request and can be shown on the browser after a normal HTTP response or after a redirect.
The notification can also close itself after a timeout, reload the current page when the user dismisses it, or perform a redirect on dismiss.
When used in an ajax request it has some optional functionality.

Notifications are handled by the YadaNotify @Component and therefore need this autowiring:

.. code-block:: java

	@Autowired private YadaNotify yadaNotify;

You can then add messages in your @RequestMapping methods with a syntax like the following:

.. code-block:: java

	yadaNotify.title("Login success", model).ok().message("You have been logged in").add();
 
The title() method is the first one to call because it starts the builder. It has many variations, some of which are used for localization (see the Javadoc).
The message() method accepts HTML tags so you will need to escape all HTML `reserved characters`_.

.. _reserved characters: https://developer.mozilla.org/en-US/docs/Glossary/Entity#Reserved_characters

You can set the message severity using one of these methods:

- ok()
	Normal severity
- info()
	Can either be a warning or something that needs special attention from the user
- error()
	Error message
	
All of them accept a boolean value so that the severity can be set conditionally. For example:

.. code-block:: java

	boolean failed=false;
	yadaNotify.title("Login " + failed?"failed":"success", model).ok(!failed).error(failed).message("You have {}been logged in", failed?"not ":"").add();

The previous example shows the use of `slf4j placeholders`_ in the message() method.

.. _slf4j placeholders: https://www.slf4j.org/faq.html#logging_performance

When calling the add() method you're actually creating a message with the values previously stored in the builder, adding it to the current HTTP Request. 
The add() method also terminates the builder chain and you can't call further methods after it.
If you want to return more than one message, you can use yadaNotify as many times as you want, even in different controllers:

.. code-block:: java

	yadaNotify.title("Login success", model).ok().message("You have been logged in").add();
	yadaNotify.title("Forbidden", model).info().message("Access to the dashboard has been revoked").add();

The resulting modal shows a panel for each message, and a top icon corresponding to the highest severity found in those messages:

.. image:: _static/img/modal-notify-2.jpg

Normal request / normal response
----------------------------------
In a normal request/response, just use the Model as seen above.
You can then return the thymeleaf view as usual: just after showing the target view, the browser will open the modal.

.. code-block:: java

	@RequestMapping("/dashboard")
	public String dashboard(Model model) {
		yadaNotify.title("Login success", model).ok().message("You have been logged in").add();
		return "/dashboard";

Normal request / redirect response
----------------------------------
If you want to perform a redirect when exiting the controller, use RedirectAttributes instead of Model:

.. code-block:: java
	
	@RequestMapping("/dashboard")
	public String dashboard(RedirectAttributes redirectAttributes) {
		yadaNotify.title("Login success", redirectAttributes).ok().message("You have been logged in").add();
		return "redirect:/console";

The browser will perform a redirect, call the server again, display the target view then show the modal. 
The server can test if a modal is pending with the following methods:

.. _redirectOnClose:

.. code-block:: java

	isNotificationPending(...)
	isErrorSet(...)

You can also activate a redirect only when the modal is closed by the user with ``redirectOnClose()``:

.. code-block:: java
	
	@RequestMapping("/dashboard")
	public String dashboard(Model model) {
		yadaNotify.title("Login success", model).ok().message("You have been logged in").redirectOnClose("/console").add();
		return "/dashboard";

Ajax request
----------------------------------
Returning from the Controller
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Ajax requests work roughly the same as normal requests. 
The notification will be shown only if the result contains the "/yada/modalNotify" modal.
This can be done in one of the following alternative ways:

.. code-block:: java

	return "/yada/modalNotify";
	return YadaViews.AJAX_NOTIFY;
	return yadaNotify.title("Login success", model).ok().message("You have been logged in").add();

The first version should of course be avoided for future compatibility. 
The last version is very convenient when returning a message at the end of the @RequestMapping method.

Returning additional HTML
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The problem with the above approach is that you might want to also return some other HTML, 
for example the original form with validation errors, or something to insert in the page. 
The solution is to add a conditional include of the modalNotify in your returned view:

.. code-block:: html

	<!-- Some other html that you need goes before or after -->
	<div class="yadaResponseData">
		<div th:if="${YADA_NBODY}" th:include="/yada/modalNotify :: modal" th:remove="tag"></div>
	</div>

The yadaResponseData element is hidden by `yada.css`.

.. todo:: Replace /yada/modalNotify with YadaViews.AJAX_NOTIFY in the yadaResponseData example above

Returning additional data
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
You might want to return, together with a notification, some key-value pairs for use in a javascript handler 
defined with ``yada:successHandler`` (see :ref:`ajax-postprocessing`).

You can achieve this by placing a Map called "resultMap" in the Model:

.. code-block:: java
	
	Map<String, String> resultMap = new HashMap<>();
	resultMap.put("deletedTaskId", taskId);
	model.addAttribute("resultMap", resultMap);

The data can be retrieved in the javascript handler with ``yada.getEmbeddedResult``:

.. code-block:: javascript

	function editTaskHandler(responseText, responseHtml, form, button) {
		var result = yada.getEmbeddedResult(responseHtml);
		var taskId = result['deletedTaskId'];
		$('#taskRow' + taskId).remove();
	}

Redirect
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
To show a notification with a redirect when returning from an ajax call, the only option is to perform the redirect 
on modal close with :ref:`redirectOnClose <redirectOnClose>`.

.. todo:: What happens if the controller returns "redirect:/xxx" on an ajax call?


Other functionality
----------------------------------
Vertically Center :yada-version:`0.3.3`
^^^^^^^^^^^^^^^^^
If you're using Bootstrap 4 you can vertically center the modal with the method ``center()``:

.. code-block:: java

	yadaNotify.title("Login success", model).ok().message("You have been logged in").center().add();

Generic modal classes :yada-version:`0.3.3`
^^^^^^^^^^^^^^^^^^^^^
You can add any class to the "modal-dialog" div by setting the ``extraDialogClasses`` Model attribute:

.. code-block:: java

	model.addAttribute("extraDialogClasses", "myclass1 myclass2");
	return yadaNotify.title("Saved", model).ok().message("Item saved").add();

.. todo:: Clearing all previous messages, "Chiamare javascript arbitrario decidendo lato server", Autoclose, modalReloadOnClose, 
	
Javascript API
==============
The notification modal can also be opened in javascript:

.. code-block:: javascript
	
	yada.showOkModal(title, message, redirectUrl)
	yada.showInfoModal(title, message, redirectUrl)
	yada.showErrorModal(title, message, redirectUrl)

title
	the modal title
message
	the modal message
redirectUrl
	optional url to redirect when the modal is closed
	
	
	

