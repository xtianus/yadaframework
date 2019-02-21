************ 
File Uploads
************ 
.. rubric::
	Uploading files to the server, handling and linking them

Description
===========
File Upload is handled via `Commons FileUpload`_. We don't use the Servlet 3 API because, when we had to make a choice, 
we couldn't figure out how to set the file size limit via our configuration file.

Yada offers some utility classes to more easily handle files *after* they have been uploaded:

- storing files on disk
- resizing images
- assigning files to Entity objects
- generating download links
- managing the pool of uploaded files

.. _Commons FileUpload: https://commons.apache.org/proper/commons-fileupload/

Configuration
=============
Maximum file size setting
-------------------------
You can set the maximum file size for an upload by means of the maxFileUploadSizeBytes configuration entry:


.. code-block:: xml

	<config>
		<maxFileUploadSizeBytes>3000000</maxFileUploadSizeBytes>

The default is 50MB.

Maximum file size check
-----------------------
When the upload limit is reached, a message is logged with a debug severity. You can ensure to see the message with the following logback configuration:

.. code-block:: xml

	<logger name="org.springframework.web.multipart.commons.YadaCommonsMultipartResolver" level="DEBUG"/>

A Request Attribute is also added to the Request with the name MaxUploadSizeExceededException and the MaxUploadSizeExceededException as a value.
All Request Parameters sent with the form are lost. 

By default, Tomcat will also drop the connection and no response will be sent to the browser. This will result in a low-level error shown by the browser.
The reason for this is explained `here`_ and can only avoided if you configure Tomcat not to drop the connection but keep uploading any excess data (`maxSwallowSize="-1"`).
The drawback would be that network bandwidth would be wasted and the user will have to wait until the whole file is uploaded before being told that it was too big.
The best solution would be to only check file size on the browser via javascript. This is not currently implemented in Yada but is something like the following:

.. code-block:: javascript

	function fileTooBig() {
		var file = $('input[name=attachment]')[0].files[0];
		if (file==null) {
			return false;
		}
		var sizeMega = file.size/1024/1024;
		if (sizeMega>3) {
			$('#fileTooBig').show();
			return true;
		}
		$('#fileTooBig').hide();
		return false;
	}
	$('input[name=attachment]').on('change', function() {
		fileTooBig();
	});
	$('#theForm').submit(function() {
		if (fileTooBig()) {
			event.preventDefault();
		}
	});

.. todo:: Automate javascript checking of file size

.. _here: https://www.mkyong.com/spring/spring-file-upload-and-connection-reset-issue/


HTML
===========
File upload starts from a *"multipart/form-data"* form. This is a standard form with a input element of type *"file"*:

.. code-block:: html

	<form method="POST" enctype="multipart/form-data" action="doUpload">
		File to upload: <input type="file" name="upfile"><br/> 
		<input type="submit" value="Press"> to upload the file!
	</form>

If you're using a *form backing bean* you can include a yada fragment for the input tag. The following example also shows any error:

.. code-block:: html

	<form th:action="@{/profile}" th:object="${formProfile}" enctype="multipart/form-data" method="post" 
	th:classappend="${#fields.hasErrors('*')}? has-error" role="form">
		<div th:replace="/yada/form/fileUpload::field(fieldName='avatarImage',label='Avatar')"></div>

JAVA
===========
After submission, the uploaded file will be processed by *Commons File Upload* and sent to the @Controller as a `MultipartFile` object.
You would normally add a field of that type to the *form backing bean*, but you can also handle it independently from the other form fields if you wish,
by adding it to the @RequestMapping signature.
 
In the @Controller you have many options. 

Just save the file
------------------
You can just save the file somewhere with YadaWebUtil.saveAttachment():

.. code-block:: java

	public String storeFile(MultipartFile submittedFile) {
		File destination = new File("someFolder");
		YadaWebUtil.saveAttachment(submittedFile, destination):
		
Then you will have to keep track of the file yourself somehow. The following sections show an alternative and more convenient way of dealing with file uploads.

YadaAttachedFile
----------------------
Usually the uploaded file has to be associated to some Entity in the database: a user avatar or CV, the image of a product, the pdf for a trip.
Use YadaAttachedFile to easily handle file attachments:

.. code-block:: java

	@Entity
	public class Product {

		@OneToOne(cascade=CascadeType.REMOVE, orphanRemoval=true)
		protected YadaAttachedFile icon;

		@OneToOne(cascade=CascadeType.REMOVE, orphanRemoval=true)
		protected YadaAttachedFile specSheet;
		
After doing this you can make use of the functionality of YadaFileManager explained below.
Strictly speaking, the above annotation is not needed because YadaAttachedFile instances can exist on their own, having the entity id as a field,
but this wouldn't enforce database integrity.

The YadaAttachedFile class stores some file-related information that you might want to keep:

- the original name of the file uploaded by the user
- the upload time
- localized title and description
- the folder where the file is stored
- the name of three versions of the file: the original one and the ones scaled for desktop and mobile
- the sort order relative to files of the same "group"
- the Entity to which the file is attached
- a "published" flag
- a locale if the file has to be made available only to some specific locale. This could be useful for pdf files in different languages

YadaFileManager
------------------
Introduction
^^^^^^^^^^^^^^^
The YadaFileManager @Component is the single entry to all operations on uploaded files.

Every time a file is uploaded, it is stored in a folder named "uploads" in the <basePath> configured directory. This folder is 
created automatically if the tomcat process has enough permissions, otherwise you have to create it manually.

Saving the file
^^^^^^^^^^^^^^^
Every file is stored using the original file name. To prevent name duplicates a number is automatically appended at the end.

.. code-block:: java

	public String updateProfile(MultipartFile thumbnailImage) {
		File managedFile = yadaFileManager.uploadFile(thumbnailImage);

The File can then be attached to an Entity:

.. code-block:: java

	yadaFileManager.attach(user.getId(), managedFile, "userData", "icon");

When the attach method is called, the original uploaded file is copied from the "uploads" folder into the target folder. 
The new file will have the new prefix specified and a sequential number in the name.
The original file is by default deleted from the "uploads" folder unless a specific configuration is set to false:

.. code-block:: xml

	<yadaFileManager>
		<deleteUploads>false</deleteUploads>
	</yadaFileManager>

Not deleting uploaded files allows the implementation of a filesystem-like feature where single files could be reused many times.

.. todo:: implement filesystem feature

Image variants
^^^^^^^^^^^^^^^
If the uploaded file is an image, it can be resized for desktop and mobile as needed by specifying the alternative dimensions:

.. code-block:: java

	yadaFileManager.attach(user.getId(), managedFile, "userData", "icon", "jpg", 1280, 768);

In the above example the image is converted to jpg and two additional versions are saved on disk.


TODO delete e url
 

 
 








