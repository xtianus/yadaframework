Ajax Modal
==========

:Purpose: 
	opening a modal after an ajax call

Description
------------

Make a modal open on the page as a result of an ajax call. 
For example, by clicking on an "Edit" button
a modal opens with a form to edit the chosen element.
 
HTML
------------

.. code-block:: java

	<div th:replace="/yada/modalGeneric::fragment(~{::modalTitle},~{::modalHeader},~{::modalBody},~{::modalFooter},~{::modalScript})">

		<div th:fragment="modalTitle">This is the title</div>
		
		<th:block th:fragment="modalBody">
			<div class="modal-body">
				This is the body
			</div>
		</th:block>
		
		<div th:fragment="modalFooter">
			<div class="modal-footer">
				The footer here
			</div>
		</div>
		
		<script th:fragment="modalScript">
			${"This is the script"}
		</script>
	
	</div>



Java
------------



TO BE CONTINUED
