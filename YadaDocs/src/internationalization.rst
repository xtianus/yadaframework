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

Immutable text
----------------------

.. todo:: TO BE CONTINUED...

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


