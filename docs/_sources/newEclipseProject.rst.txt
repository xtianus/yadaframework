********************
New Eclipse Project
********************

Introduction
===================
The following procedure will guide you through the creation of a new Eclipse project for your web app. At the end you will have a Web Project that can be run in a Java EE server (e.g. Tomcat) with all the needed layers set up and ready for customization: 

* Database access layer with transactions (Hibernate / JPA / Spring)
* Web application controller (Spring)
* Page templates (Thymeleaf)
* Security (Spring)

You will also have a basic deployment procedure already set up:

* Database schema generator
* Configurations for development, test and production
* Deployment script

Prerequisites
===================
You should have `Java`_, `git`_, `MySQL Community Server`_ and a working copy of `Eclipse IDE for Enterprise Java Developers`_. Check the notes in this section before installing anything.

Java SDK
--------
Remove any Java installation you may have on your PC.


.. DANGER:: The biggest problem you may face with Java installation is to mix 32 and 64 bit versions: it gives unexpected results.
	So always download 64 bit versions of Java and Eclipse.
	Currently the Yada Framework source is tested with Java 8. Running on an untested version might give weird errors.
	At the time of writing we failed to run on Java 11.0.6 for problems during Gradle project import.

You should look on the Oracle site for "Java SE" installer, which is the "Standard Edition". 
You can download Java 8 from the "`Java Archive`_" section of the Oracle site. You will be required to create an
Oracle account and login before being able to download. 

.. note:: Before Java 11 there were two components in Java SE: the JDK (Development Kit) and the JRE (Runtime Environment). Since Java 11 the JDK is also a JRE.
	
The installation package will install both the JDK and the JRE. 
The JRE - 64 bit - can be any version and generally it will auto-upgrade to the latest.
To make your installation future-proof, install the java JDK in some folder like ``C:\Local\Javas\jdk1.8.0_152`` and
make a symbolic link to it with a generic name, like

.. code-block:: bat

	mklink /D /J C:\Local\jdk8 C:\Local\Javas\jdk1.8.0_152

Now you can refer to ``C:\Local\jdk8`` in your scripts and when you update the java (minor) version you just change the link, not the scripts.
The Java JRE can be installed to the default folder (``C:\Program Files``).


GIT
---
Install `git`_ for your OS.

.. _git: https://git-scm.com/downloads

MySQL
-----
Install `MySQL Community Server`_ v5.7: later versions have not been tested yet but this documentation uses MySQL 8 just to be ready.

For a future-proof installation you should not install MySQL as a Service, but run it manually when needed because one day
you might have more than one version to use. The steps to install MySQL 8 on windows are as follows:

- download and run the web installer
- choose the "Custom" installation
- select the latest MySQL Server 8 from the list on the left, click on the right arrow to move it to the right, then click on it: a new "Advanced Options" link should appear. Click on it and choose the installation folder, for example ``C:\Local\Mysqls\mysql-8.0.19-winx64``

.. image:: _static/img/newEclipseProject/MySQL-advanced.jpg

- when asked, choose "Show Advanced and Logging options"
- on the "Authentication Method" page choose "Use Legacy Authentication" (second option) to have an easier life (it's a dev PC after all)
- on the "Advanced Options" choose "Preserve Given Case" - this is important to prevent problems when running the DB on linux

.. image:: _static/img/newEclipseProject/MySQL-preservecase.jpg

After installation, make a symbolic link to it with a generic name, like

.. code-block:: bat

	mklink /D /J C:\Local\mysql80 C:\local\Mysqls\mysql-8.0.19-winx64

Now you can refer to ``C:\Local\mysql80`` in your scripts and when you update the MySQL (minor) version you just change the link, not the scripts.

In order to start/stop the MySQL server you should create two scripts and put their shortcut somewhere easy like the desktop or the taskbar.

``startMySql80.bat``

.. code-block:: bat

	set MYSQL_HOME=C:\Local\mysql80
	set PATH=%MYSQL_HOME%\bin;%PATH%
	C:\Local\mysql80\bin\mysqld.exe --defaults-file="%MYSQL_HOME%\my.ini" --console

``stopMySql80.bat``

.. code-block:: bat

	set MYSQL_HOME=C:\Local\mysql80
	C:\Local\mysql80\bin\mysqladmin.exe -u root shutdown

.. _Java: https://www.oracle.com/technetwork/java/javase/downloads/index.html
.. _Eclipse IDE for Enterprise Java Developers: https://www.eclipse.org/downloads/packages/
.. _Java Archive: https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html
.. _git: https://git-scm.com/downloads
.. _MySQL Community Server: https://dev.mysql.com/downloads/mysql/

Eclipse
--------
Install the latest version of `Eclipse IDE for Enterprise Java Developers`_. Be sure to install the 64 bit "Enterprise" version:

.. caution:: A 32 bit Eclipse will not run on a 64 bit Java installation and the non-enterprise version will not be able to 
	run a web application in a standalone Tomcat. 
	If you do not want to manage a standalone Tomcat installation and choose to use embedded Tomcat instead, you may actually download 
	the non-Enterprise version (not tested).

With a single installation of Eclipse you can have as many workspaces as you like. For big projects involving multiple branches and many people,
you might want to create at least two workspaces, one for your own development and another one for checking other people's work or for deployment.
It is very convenient to create a different shortcut for each workspace. Using the "-data" command line option you can specify which workspace you
want to use. For example:

``C:\local\Eclipses\eclipse-jee-2019-09-R\eclipse.exe -data "C:\work\wspaces\myCoding"``
``C:\local\Eclipses\eclipse-jee-2019-09-R\eclipse.exe -data "C:\work\wspaces\myRelease"``

Run Eclipse with the chosen workspace and click on the "Workbench" icon to the right of the "Welcome to Eclipse" page.

Update your installation with "Help > Check for Updates" and follow the instructions. Failing to do so might prevent
you from adding a Tomcat server later.

From the "Window > Preferences > Gradle" dialog set the "Java home" entry to your JDK installation, 
which would be ``C:\Local\jdk8`` if you followed the instructions above.

Set the editors file encoding to UTF-8, at least "General > Workspace > Text file encoding" and "Web > CSS Files > Encoding":

.. image:: _static/img/newEclipseProject/encoding.jpg

.. _Eclipse IDE for Enterprise Java Developers: https://www.eclipse.org/downloads/packages/

The "New text file line delimiter" shown in the above image is also better set to "Unix".

Add the donwloaded JRE as a new Runtime in "Window > Preferences > Java > Installed JREs" and set it as the default. 
Also set the "Window > Preferences > Java > Compiler > JDK Compliance" accordingly.

Node.js (optional)
-------------------
Node.js is not strictly needed but it can be useful.
Install the `latest LTS version`_.

.. _latest LTS version: https://nodejs.org/en/download/

Connect to GIT
==============
Add the repositories
--------------------
The "Yada Framework" sources are hosted on the public GitHub site.
To access the GitHub repository just use your GitHub credentials (create some) on the `yadaframework`_ repo.
The git url should be like ``https://github.com/xtianus/yadaframework.git``. 

.. _yadaframework: https://github.com/xtianus/yadaframework

Once you have the repository credentials, you can connect Eclipse: 

- Open the "git perspective" from "Window > Perspective > Open Perspective"
- On the left of the workspace you should see the "Git Repositories" panel
- Click on "Clone a Git repository" and add the needed information
 
.. image:: _static/img/newEclipseProject/clonegit.jpg
 
.. image:: _static/img/newEclipseProject/clonedialog1.jpg

On the "Branch Selection" dialog you should select just "master". You will later
add any branch that you need.

On the "Local Destination" dialog you just need to select a local folder where to clone the repository.
The following information assumes that you have already created a new Eclipse project called `MySiteProject` (will be explained later) and added it to a git repository
called `mysite` that you host somewhere.
It is very important that you **store all the repositories in a common parent folder** that is specific to the current project, for example
``C:\work\git-mysite``, otherwise relative paths in the build file won't work and you'll have to customise them.

.. image:: _static/img/newEclipseProject/cloneDialog2.jpg

At the end you should have a filesystem structure like the following, where "mysite" is any new project that uses the Yada Framework:

::

  C:\\work\\git-mysite
    mysite
      .git
      MySiteProject
    yadaframework 
      .git
      docs
      YadaDocs
      YadaTools
      YadaWeb
      ... and other folders	

When you have connected to all repositories, you can switch to the "Java Perspective" in Eclipse. 

Generate the YadaTools library
------------------------------
When you first setup the development environment, you currently need to generate the YadaTools library locally
because it has not been uploaded on a distribution site yet.
If you fail to do so, you'll get a weird compilation error like *"Could not fetch model of type 'GradleBuild' using Gradle distribution"*.
To generate the library, you first need to import the YadaTools project.

Use the "File > Import... > Gradle > Existing Gradle Project" menu to import YadaTools:

.. image:: _static/img/newEclipseProject/yadaTools.jpg

If the imported project has some errors, first of right-click and choose "Gradle > Refresh Gradle Project".
If it complains about some unbound system library, open "Properties > Java build path > Libraries" and remove the library.

To build the library, run the "YadaTools - uploadArchives" task from "Run > Run Configurations... > Gradle Task". If you have some
weird errors, try from the command line in the YadaTools folder and run ``gradlew uploadArchives --no-daemon`` because the Gradle
daemon sometimes gets in the way...

The First Project
=====================
Create a new Eclipse Project using "File > New > Project... > Gradle > Gradle Project" menu.
Use any name you like ("MySiteProject" in the above example) and accept all defaults. A new Java project will be created in your workspace.

Delete any example file and folders inside the "src/main/java" and "src/test/java" folders.

Edit the ``/MySiteProject/gradle/wrapper/gradle-wrapper.properties`` file changing the ``distributionUrl`` to match the version of
Gradle that you want to use. For example ``distributionUrl=https\://services.gradle.org/distributions/gradle-5.6.4-bin.zip``.

.. DANGER:: The currently recommended Gradle version is 5.6.4; Gradle 6.2 failed to load the YadaTools library during initial setup

Right-click the project and choose "Gradle > Refresh Gradle Project" to update the version of Gradle used.

You can choose to add an external Tomcat server or use the embedded version. In the first case, you should be using the "Enterprise" version of Eclipse.
If not, you should at least install the "Eclipse Web Tools Platform" (WTP) plugin and... hope for the best.

Import the Yada projects that you need to use. After connecting to the GitHub repository as explained above, you need to import the
needed projects using the "File > Import... > Gradle > Existing Gradle Project" menu as explained in :ref:`newEclipseProject:Generate the YadaTools library`.
To import all Yada projects at once you could just import the "YadaWebCommerce" project and rely on dependency resolution to automatically
import everything else:

.. image:: _static/img/newEclipseProject/importYada.jpg

You should now have, in your workspace, the following Yada projects:

- YadaTools
- YadaWeb
- YadaWebCMS
- YadaWebCommerce
- YadaWebSecurity

The next step is to create a git repository to store your projects. You can use any public provider like GitHub or a private installation
based for example on GitLab. You should definitely use git to store your files, also because they will be moved to the same folder of the
Yada projects and relative paths in the build file will work effortlessly.
When using "GitLab", let it create a default readme.md file so that you'll be able to check out the repository easily (there should be a
similar option on GitHub). Then add the repository location to the Git Perspective as done for the Yada Framework.
The local folder should be next to the Yada Framework local git, for example ``C:\work\git-mysite\mysite``.
To add your project to the local git repository right-click on it and choose "Team > Share Project...". 
In the dialog you should just select the correct repository and accept the defaults.
Finally edit .gitignore in the root of your project look like the following:

::

    /.gradle/
    /build/
    /bin/
    /.gitattributes
    /.settings/
    /.classpath
    /.project
    !gradle-wrapper.jar

The Build File
===================

Replace your ``build.gradle`` with the contents of ``/YadaTools/scripts/template.gradle``.

The ``// CHANGE THIS !!!`` items should be edited to suit your needs.

The default environments are "dev" for "Development", "tst" for "Test" and "prod" for "Production". 
You can rename them (or also add/remove some) using the "envs" property in the ``yadaInit`` task of the build, 
but the envs array must always have the "development" environtment first and the "production" environment last 
in order to create a correct configuration.xml file. For a list of all other options for the ``yadaInit`` task 
see ``/YadaTools/src/main/groovy/net/yadaframework/tools/YadaProject.groovy``

Replace your ``settings.gradle`` with the following:

.. code-block:: groovy

       rootProject.name = 'MySiteProject'
       include 'YadaWeb'
       project(':YadaWeb').projectDir = "../../yadaframework/YadaWeb" as File
       include 'YadaWebSecurity'
       project(':YadaWebSecurity').projectDir = "../../yadaframework/YadaWebSecurity" as File
       include 'YadaWebCMS'
       project(':YadaWebCMS').projectDir = "../../yadaframework/YadaWebCMS" as File
       include 'YadaWebCommerce'
       project(':YadaWebCommerce').projectDir = "../../yadaframework/YadaWebCommerce" as File

You should change the project name to whatever you used. The above assumes that you cloned the yadaframework repository 
in the same root folder of your project repository as explained in :ref:`newEclipseProject:Add the repositories`.
This setup is needed to use YadaWeb class files directly instead of going through the jar, 
and is handy when you plan to work on the YadaWeb sources to fix and improve them. 

More information on the wtp syntax `here <https://docs.gradle.org/current/dsl/org.gradle.plugins.ide.eclipse.model.EclipseWtpComponent.html>`__ and `here <https://docs.gradle.org/current/dsl/org.gradle.plugins.ide.eclipse.model.EclipseWtpFacet.html>`__.


Code Generation (just a bit)
======================================

Ensure you have these folders in your project before the next step:

-  ``src/main/java``
-  ``src/main/webapp``

Use the ``Gradle > Refresh Gradle Project`` project menu item to initialise the project.

Open a command prompt in the root folder of your project (e.g. ``C:\work\git-mysite\MySiteProject``) and run ``gradlew yadaInit --no-daemon``. 
This task will add the java core Spring configuration and some default files that will have to be either deleted or customised.
The "--no-daemon" option is to stay on the safe side.

.. note:: You can run the task multiple times and it will never overwrite existing files: to revert a change, delete the file and run the task again

If you see compilation errors ensure that you're just missing some classpath libraries and do a "Refresh Gradle Project" again. If you
still have errors, try to fix them ;-) For example you might need to remove the dependency on YadaWebSeurity classes if you didn't want to use it.

Initial Customization
======================================
Before starting the server for the first time, you should customise some generated files.
The bare minimum would be to edit these files:

- /src/main/resources/conf.webapp.dev.xml
	- **paths/basePath** is where your project files will be found
	- **setup/users/user/admin** is the initial user of your site (if YadaWebSecurity is being used). You should change the password at least
- /src/main/resources/logback.xml
	- you may want to change the log path

You can skip the "tst" and "prod" files until you're ready to deploy to a test/production server.

Database Setup
===================
Create the local database by running the scripts inside ``/env/dev`` (if you're not on windows, just copy the content and adapt it to your platform).

Create the database schema by running the ``gradlew dbSchema`` task.
You may get some compilation errors that need to be fixed.
If the schema generator can't connect to the database check that /src/main/resources/META-INF/persistence.xml (and /src/main/webapp/META-INF/context.xml) has the right DB credentials.

Run the ``/env/dev/dropAndCreateDatabase.bat`` (or a linux equivalent) to create a new empty database with the generated schema.

Tomcat server
===================
This section is about setting up a standalone Tomcat server that can be controlled from Eclipse. You don't need it if you're going to use the
embedded version of Tomcat.

Download `Apache Tomcat 8.5`_ "64-bit Windows zip" and unzip the folder to some place like ``C:\local\Tomcats\apache-tomcat-8.5.51``.

Create a new folder where you will keep all your web application deploys, like ``C:\local\Deploy``.

In Eclipse, while in the "Java Perspective", show the "Servers" view from "Window > Show View > Other... > Server > Servers".
You will see the link "No servers available. Click to create a new server...". Click that link. You will see a dialog
where you should choose "Apache > Tomcat v8.5 Server". In the Next dialog choose your "Tomcat installation directory", 
for example ``C:\local\Tomcats\apache-tomcat-8.5.51``, and finish.
Just to be safe, check that Tomcat works by running it and browsing to ``http://localhost:8080/``. If all is fine, you should see
an error from Tomcat:

.. image:: _static/img/newEclipseProject/tomcatError.jpg

Stop Tomcat then right-click on it and choose "Open". You will see the Overview:

.. image:: _static/img/newEclipseProject/tomcatOverview.JPG

On this page do the following:

- Under "Server locations" set "Use custom location > Server path" to ``C:\local\Deploy\myProject`` where "myProject" is anything you like
- Under "Server Options" uncheck "Modules auto reload by default"
- Under "Timeouts" add a trailing 0 to both timeouts so that 45 becomes 450 and 15 becomes 150
- Save with CTRL+S.

If your sources in the "Package Explorer" window don't have any red marks (no compilation errors), you can add the web application to Tomcat:

- Right-click on the Tomcat server in the "Servers" view
- Select "Add and Remove... > Add All >>"

If the server starts with no errors, you can see the homepage placeholder at http://localhost:8080/

.. _Apache Tomcat 8.5: https://tomcat.apache.org/download-80.cgi

Troubleshooting
===================

Compilation Errors
------------------
In case of compilation errors, the first thing to do is to run a "Refresh Gradle Project" on the affected project or the including project.
If errors persist, check that you have imported all the needed Yada projects.
Also be sure to have "Projects > Build Automatically" checked and try with a "Project > Clean...".

Validation Errors
-----------------
If you get an error like

``CHKJ3000E: WAR Validation Failed: org.eclipse.jst.j2ee.commonarchivecore.internal.exception.DeploymentDescriptorLoadException: WEB-INF/web.xml``

you may fix it just by forcing a validation on the project via the menu.

Tomcat Startup Errors
---------------------
If Tomcat doesn't start, it might have stale data. Try with a "Clean..." on the server. If everything fails, stop the server and delete the content of the Deploy folder,
for example ``C:\local\Deploy\myProject``. Then do a "Publish" on the server. If you can't delete some file because Windows says it's open, you'll need to quit Eclipse
and be sure that there are no ghost Tomcat processes running. In extreme cases, you might need to restart your PC.



