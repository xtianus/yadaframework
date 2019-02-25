**********
Ajax Modal
**********

.. rubric::
	Opening a modal with an ajax call

Description
------------

Make a Bootstrap Modal open as a result of an ajax call. 
For example, by clicking on an "Edit" button
a modal opens with a form to edit the chosen element.
The modal content is generated on the server. The browser just opens it.

.. image:: _static/img/modal-example.jpg

The implementation needs the following parts:

- HTML placeholder
	The page must contain the standard HTML placeholder for opening modals. This is 
	usually inserted in a common footer so that you don't need to remember adding it to the page.

- HTML for the ajax call
	The ajax call can be made in many ways: with a :doc:`DataTable </datatables>` button, with an :doc:`ajax link </ajax>`, via custom javascript ...

- Java for the @Controller
	The modal must be returned by a @RequestMapping method of a @Controller.
 
- HTML of the modal
	It specifies the title, header, body, footer and script sections (all are optional)

HTML placeholder
----------------

The HTML placeholder must be the ``class="modal"`` node of a Bootstrap Modal and have ``id="ajaxModal"``:

.. code-block:: html

	<div id="ajaxModal" class="modal fade " tabindex="-1" role="dialog" aria-hidden="true"></div>


HTML for the ajax call
----------------------

The modal can be opened with any ajax call performed by Yada. In this example we show an ajax link:

.. todo:: Ajax link example

Java for the @Controller
------------------------

The Java @Controller must return a full Bootstrap Modal. The content of the ``class="modal"`` node will be inserted in the HTML placeholder and shown.

.. todo:: @Controller example

HTML of the modal
-----------------

You can use your own Bootstrap Modal html or take advantage of the Yada Generic Modal.
With the Yada Generic Modal you only define the section fragments that you need, producing a full Bootstrap Modal.

The fragments are:

- modal title
	Visible in the modal header as an <h4> when no custom modal header fragment is given
- custom modal header
	A full Boostrap modal-header fragment. The modal title is ignored when using this fragment
- modal body
	The body content
- modal footer
	The html for the bottom part of the modal. Usually Submit and Close buttons are located here. By default, there's just the close button.
- modal script
	Some custom script to be run when the modal code is inserted on page.
	
The code uses standard Thymeleaf `fragment <https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#template-layout>`_ syntax.
To remove a fragment from the modal, use the ~{} expression. To keep the default value use the _ character: 

In the following example, three fragments have been defined in the same html file and the footer has been removed.

.. code-block:: html

	<!DOCTYPE html>
	<html xmlns:th="http://www.thymeleaf.org">
	<head>
		<meta charset="UTF-8"/>
	</head>
	<body>

	<div th:replace="/yada/modalGeneric::fragment(~{::modalTitle},_,~{::modalBody},~{},~{::modalScript})">

		<div th:fragment="modalTitle">This is the title</div>
		
		<th:block th:fragment="modalBody">
			<div class="modal-body">
				This is the body
			</div>
		</th:block>
		
		<script th:fragment="modalScript">
			/* This is the script */
		</script>
	
	</div>
	</body>
	</html>


.. image:: _static/img/modal-example.nofooter.jpg




