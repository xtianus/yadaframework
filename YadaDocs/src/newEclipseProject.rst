New Eclipse Project
===================

Introduction
------------

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
-------------

You should have `Java`_ and a working copy of `Eclipse`_.
To make your installation future-proof, save the java JDK in some folder like C:\Local\Javas\jdk1.8.0_152 and
make a symbolic link to it with a generic name, like

.. code-block:: bat

	mklink /D /J C:\Local\jdk8 C:\Local\Javas\jdk1.8.0_152

Now you can refer to C:\\Local\\jdk8 in your scripts and when you update the java (minor) version you just change the link, not the scripts.

.. _Java: https://www.oracle.com/technetwork/java/javase/downloads/index.html
.. _Eclipse: https://www.eclipse.org

The Project Skeleton
--------------------

These are the steps needed to create a new Eclipse project that uses the Yada Framework.

-  Add a Tomcat server to Eclipse
-  Follow the Eclipse [[Setup\|Setup]] procedure:

   -  Clone the yadaframework git repository locally
   -  Import the Yada projects in Eclipse from the local git clone with "File > Import… > Gradle > Existing Gradle project"

-  Create a local git repository to store your own sources
-  Create a new Gradle project in the workspace (let's call it "ExamplePrj")
-  Delete any example files that might have been created, like "Library.java"
-  Share ExamplePrj to git so that Eclipse moves all files to the git location: "Team > Share Project..." (No need to commit yet)
-  Edit .gitignore in the root of ExamplePrj to look like the following:

::

    /.gradle/
    /build/
    /bin/
    /.settings/
    /.classpath
    /.project
    !gradle-wrapper.jar

The Build File
--------------

-  Replace build.gradle with the following:

.. code-block:: groovy

    buildscript {
        repositories {
            mavenLocal()
        }
        dependencies {
            classpath 'net.yadaframework:YadaTools:+'
        }                    
    }

    plugins {
        id 'org.hidetake.ssh' version '2.9.0'
    }

    ext.acronym = '**myprj**'
    apply plugin: 'war'
    apply plugin: 'eclipse-wtp'
    apply plugin: 'net.yadaframework.tools'
    apply plugin: 'org.hidetake.ssh'        // https://gradle-ssh-plugin.github.io

    eclipse {
        jdt {
            sourceCompatibility = 1.8
            targetCompatibility = 1.8
            // https://stackoverflow.com/a/35302104/587641
            file {
                    File dir = file('.settings')
                    dir.mkdirs()
                    File f = file('.settings/org.eclipse.core.resources.prefs')
                if (!f.exists()) {
                        f.write('eclipse.preferences.version=1\n')
                        f.append('encoding/<project>=utf-8')
                    }
                }
        }
        wtp {
                component {
            contextPath = '/'
            }
            facet {
            facet name: 'jst.web', version: '3.1'
            // This is a workaround to remove the old facet from 
            // .settings/org.eclipse.wst.common.project.facet.core.xml
            def oldJstWebFacet = facets.findAll {
                        it.name -- 'jst.web' && it.version -- '2.4'
                    }
                    facets.removeAll(oldJstWebFacet)
            }
        }
    }

    compileJava.options.encoding = 'UTF-8'
    compileTestJava.options.encoding = 'UTF-8'

    def YadaWebLib = "$projectDir/../../yadaframework/YadaWeb";

    repositories {
        jcenter()
        mavenLocal()
    }

    dependencies {

        // Add here any library that you might need (then run "Refresh Gradle Project")
        // compile  'joda-time:joda-time:2.+'

        compile project(':YadaWeb'), project(':YadaWebSecurity'),
            'org.springframework:spring-webmvc:4.3.7.RELEASE',
            'org.springframework:spring-context-support:4.3.7.RELEASE',
            'org.springframework.data:spring-data-jpa:1.11.1.RELEASE',
            'org.springframework.security:spring-security-web:4.2.2.RELEASE',
            'org.hibernate:hibernate-entitymanager:5.2.9.Final',
            'mysql:mysql-connector-java:5.1.41',
            'com.fasterxml.jackson.core:jackson-annotations:2.9.+',
            'com.fasterxml.jackson.core:jackson-core:2.9.+',
            'com.fasterxml.jackson.core:jackson-databind:2.9.+',
                'org.thymeleaf:thymeleaf-spring4:3.0.3.RELEASE'
        
        // Needed in Tomcat 8 at runtime
        runtime 'commons-beanutils:commons-beanutils:1.9.2'
        runtime 'commons-jxpath:commons-jxpath:1.3'
            
        testCompile 'junit:junit:4.12'
    }

    yadaInit {
        projectName = rootProject.name
        acronym = project.acronym
        basePackage = '**com.example**'
        dbPasswords = [**'dev': 'mydevpwd', 'tst': 'mytstpwd', 'prod': 'myprodpwd'**]
        envs=[**'dev', 'tst', 'prod'**]
            // See YadaTools/src/main/groovy/net/yadaframework/tools/YadaProject.groovy 
            // for more configuration options
    }

    configurations {
        hibtools {
            extendsFrom configurations.compile
        }
    }
    dependencies {
        hibtools files("$buildDir/classes/java/main") // Needed for yadaPersistenceUnit
        hibtools 'org.hibernate:hibernate-tools:5.+'
    }
    task dbSchema(dependsOn: [classes], type: net.yadaframework.tools.YadaCreateDbSchemaTask) {
        inputs.files configurations.hibtools;
        outputfilename = "${acronym}.sql"
    }

(wtp syntax `here <https://docs.gradle.org/current/dsl/org.gradle.plugins.ide.eclipse.model.EclipseWtpComponent.html>`__ and `here <https://docs.gradle.org/current/dsl/org.gradle.plugins.ide.eclipse.model.EclipseWtpFacet.html>`__)

The ``**marked**``\ items should be edited to suit your needs.

The default environments are "dev" for "Development", "tst" for "Test" and "prod" for "Production". You can rename them (or also add/remove some) using the "envs" property as shown above, but the envs array must always have the "development" environtment first and the "production" environment last in order to produce a correct configuration.xml file.

[TODO: YadaConfiguration.isProductionEnvironment() and similar methods should use the configured environment names]

For a list of all other options for the yadaInit task see ``/YadaTools/src/main/groovy/net/yadaframework/tools/YadaProject.groovy``

-  Replace settings.gradle with the following:

.. code-block:: groovy

       rootProject.name = 'ExamplePrj'
       include 'YadaWeb'
       project(':YadaWeb').projectDir = "../../yadaframework/YadaWeb" as File
       include 'YadaWebSecurity'
       project(':YadaWebSecurity').projectDir = "../../yadaframework/YadaWebSecurity" as File

   This assumes that you cloned the yadaframework repository in the same root folder of your project repository, like:

.. code-block:: default

       rootfolder
        |--------- exampleProject
                         |--------- .git
                         |--------- ExamplePrj
        |--------- yadaframework
                         |--------- .git
                         |--------- YadaWeb
                         |--------- YadaWebSecurity

   This setup is needed to use YadaWeb class files directly instead of going through the jar, and is handy when you plan to work on the YadaWeb sources to fix and improve them. The YadaWebSecurity project is needed only if you plan to implement a password-protected restricted section, otherwise it can be omitted.

Code Generation (just a bit)
----------------------------

-  ensure you have these folders in your project before the next step:
	-  ``src/main/java``
	-  ``src/main/webapp``
-  run ``gradlew -q eclipse``, either from a command prompt or from the "Eclipse gradle tasks" view (under Gradle Tasks > ExamplePrj > ide > eclipse)
-  run the "YadaTools - uploadArchives" gradle task (under Gradle Tasks > YadaTools > upload > uploadArchives) so that the latest version of the YadaTools library is loaded to the local maven repository. The task should already be available in "Run > Run Configurations..." but can also be run from the command line with ``gradle uploadArchives`` from inside the YadaTools project folder
-  from the "ExamplePrj" folder run the task ``gradlew yadaInit``
	-  This task will add the java core Spring configuration and some default files that will have to be either deleted or customised
	-  You can run the task multiple times and it will never overwrite existing files: to revert a change, delete the file and run the task again
-  run either the ``gradle eclipse`` task, or more simply click the ``Gradle > Refresh Gradle Project`` project menu item in order to import all jar dependencies thus clearing compilation errors

Initial Customization
---------------------

-  edit the generated .xml/.html files to suit your needs. You can skip the "tst" and "prod" files until you're ready to deploy to a test/production server
	-  the ``env/dev`` and ``env/prod`` folders now have a couple of script files that you can use to create the initial database and db user
	-  the ``/src/main/resources/template/email`` folder now contains some typical email templates that you can decide to delete or customize

-  you can add all the dependencies that you need
-  run either the ``gradle eclipse`` task again, or more simply click the ``Gradle > Refresh Gradle Project`` project context menu item

Database Setup
--------------

-  create the local database by running the scripts inside ``env/dev`` (if you're not on windows, just copy the content and adapt it to your platform)
-  create the database schema by running the ``gradlew dbSchema`` task

   -  If you can't connect to the database check that /src/main/resources/META-INF/persistence.xml (and /src/main/webapp/META-INF/context.xml) has the right credentials

-  run the /env/dev/dropAndCreateDatabase.bat (or a linux equivalent) each time you want to create a new empty database with the generated schema
-  create a new Tomcat Server in Eclipse and add the ExamplePrj project, then start it
-  if the server starts with no errors, you can see the homepage placeholder at http://localhost:8080/
