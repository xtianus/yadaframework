********************
Internationalization
********************

Introduction
===================

Unless there is a very good reason to do otherwise, every web application should be internationalized, or at least be
ready for it.
Internationalization (i18n) means that the content of the site will change according to the *locale* of the user visiting it,
where *locale* stands for "language and regional settings".

These are the aspects of i18n that should be considered:

- language user choice
- search engine indexing
- immutable text
- database text fields
- enum values


Language user choice
----------------------
Browsers send the user system language to the server at each request. Actually, some browsers allow users to 
override the system language. Still, that's a very inconvenient way to choose language.
You should also give the user a way to choose a different language for your site using something like a dropdown menu.


Search engine indexing
----------------------
The chosen language should be reflected in the url for a number of reasons:

- better search engine indexing
- bookmarking

One way to put the language in the url would be to
add a request parameter like ``?lang=fr``; another option is to add the language code in the servlet path, 
like ``example.com/fr/home``. The Yada Framework supports both.

You can also use a full Locale code instead of the language code, like fr_CA for french in Canada.

Immutable (static) text
-----------------------
A typical web page usually shows some text that is statically typed, i.e. not added via a CMS backend but
typed directly into the HTML code. This may be a title, a menu entry, an image caption, or a description.
All this statically typed text can't be used in a i18n application: the text must change according to the
user language.

Database text fields
----------------------

.. todo:: TO BE CONTINUED...

Enum values
----------------------

.. todo:: TO BE CONTINUED...

Configuring "language in the path"
==================================

URL Example: 

.. code-block::

  http://www.example.com/it/myHome

Java code
-----------
If your project does not use YadaSecurity, change ``WebApplicationInitializer`` in order to add a servlet filter:

.. code-block:: java

  @Override
  protected Filter[] getServletFilters() {
  	// Locale in the path
  	// See https://stackoverflow.com/a/23847484/587641
  	return new Filter[] { new DelegatingFilterProxy("yadaLocalePathVariableFilter") }; 
  }

The above adds a filter to the Spring servlet engine. It is not needed when using YadaSecurity because the same is
already done in ``net.yadaframework.security.SecurityWebApplicationInitializer``.

Application configuration
-------------------------

Edit the conf.webapp.prod.xml configuration file (were 'prod' is the environment code for "production") adding a section
like the following:

.. code-block:: xml

  <i18n localePathVariable="true">
  	<locale>it</locale>
  	<locale default="true">en</locale>
  	<locale>de</locale>
  	<locale>es</locale>
  	<locale>fr</locale>
  	<locale>ru</locale>
  </i18n>

Note the use of the "default" attribute. This is **not** the locale that will be set when there is no appropriate 
value in the request (you'd get a 404 HTTP error). This is actually the language to use for 
strings retrieved from the database (see later).

.. todo:: link to the database section

Other than just the language, you can use a full locale code though this is rarely needed:

.. code-block:: xml

  <i18n localePathVariable="true">
  	<locale>it_IT</locale>
  	<locale default="true">en_GB</locale>
  	<locale>en_US</locale>
  	<locale>es_ES</locale>
  	<locale>fr_FR</locale>
  	<locale>fr_CA</locale>
  </i18n>

You can also configure a country to be added to the locale after the request has been received. This way you can still
use just the language code in the url but receive a full Locale in the java @Controller:


.. code-block:: xml

  <i18n localePathVariable="true">
  	<locale country="IT">it</locale>
  	<locale country="GB" default="true">en</locale>
  	<locale country="DE">de</locale>
  	<locale country="ES">es</locale>
  	<locale country="FR">fr</locale>
  	<locale country="RU">ru</locale>
  </i18n>
  
Javascript
----------
The language in the path variable can be changed via javascript using

.. code-block:: javascript

  yada.changeLanguagePathVariable(locale);

where "locale" is the ISO2 locale code. This code could be called when choosing from a list of languages.

Configuring "language request parameter"
========================================

URL Example: 

.. code-block::

  http://www.example.com/myHome?lang=it

This is easier to configure because you don't need to change the Java code.
The application configuration is the same but you need to set localePathVariable="false".

.. todo:: check that this stil works and what it does. I think YadaWebUtil.enhanceUrl() doesn't work properly

Coding with i18n text
==================================
Static text
----------------------
To implement localized static text just use the standard `Spring "MessageSource"`_ concept: store all text in different
``message.properties`` files, indexed by a key.

The Yada Framework expects message source files to be in the ``WEB-INF/messages`` folder, with a file name in the
``messages[_<lang>].properties`` format. Example:

.. code-block:: java

	messages_de.properties
	messages_fr.properties
	messages_ru.properties
	messages.properties

Each file stores the text of a different language. You don't need to add them all immediately: start
from the default language in ``messages.properties`` then add the translations when they become needed.
The default language can be any language that you consider to be the "base" language: all keys that are
not found in a specific language are searched in the default one; when not found, the key
is shown as text.

The content of the file is in the standard `Java "MessageFormat"`_ format:

<key> = <value>

Example:

.. code-block:: java

  validation.empty = This value can't be empty
  validation.password.length = Password can''t be shorter than {0} characters and longer than {1}
  files.total = There {0,choice,0#are no files|1#is one file|1<are {0,number,integer} files}.

In particular: 

- {0} and {1} are ways of passing parameters
- when a parameter is specified, a single quote must be escaped by another single quote
- there's a powerful way of specifying variations like singular/plural (choice format)

In production, files are reloaded every 600 seconds (10 minutes) to pick up changes.

.. hint:: The Message Source configuration is implemented in YadaAppConfig.messageSource()

.. _Spring "MessageSource": https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#context-functionality-messagesource

.. _Java "MessageFormat": https://docs.oracle.com/javase/8/docs/api/java/text/MessageFormat.html

Usage with Thymeleaf
^^^^^^^^^^^^^^^^^^^^

The syntax to show a localized string in Thymeleaf is ``#{<key>}``. Example:

.. code-block:: html

	<p th:text="#{validation.empty}">Any placeholder text here will be overwritten</p>

See the `Thymeleaf docs`_ for more details.

.. _Thymeleaf docs: https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#messages

Usage in Java
^^^^^^^^^^^^^

To get the localized text in java you first autowire a MessageSource bean, then use the getMessage() method:

.. code-block:: java

	@Autowired private MessageSource messageSource;

	public String someMethod(Locale locale) {
	  String msg1 = messageSource.getMessage("validation.empty", null, locale);
	  String msg2 = messageSource.getMessage("validation.password.length", new Object[]{5, 10}, locale);




