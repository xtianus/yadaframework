Ajax
=====
 
The Yada Framework support for ajax calls is implemented in ``yada.ajax.js``.
It is automatically included in production, merged with the other yada javascript files. To use it in development, add::

	<script th:if="${@config.developmentEnvironment}" yada:src="@{/yadares/js/yada.ajax.js}"></script>

Ajax Links
----------
To make an ajax request, add the "yadaAjax" class::

	<a href="/some/endpoint" class="yadaAjax">Click here</a>



Reference
---------

yadaAjax
	Change the standard behavior of the element so that it calls the server via ajax 

yadaAjaxButtonOnly
	When set on an ajax form, make the form ajax only if the clicked button also has the yadaAjax class.
	Otherwise the form will be sent with a normal non-ajax request.




TO BE CONTINUED
