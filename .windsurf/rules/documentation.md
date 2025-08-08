---
trigger: model_decision
description: Write or update documentation
---

Documentation for the yada framework is implemented in Ascii Doctor format inside the .\YadaDocs\src\docs\asciidoc folder.
It currently only is in english.
Each file covers a topic. For example "ajax.adoc" covers ajax calls.
Some complex topic are documented in more than one file, grouped into a folder. For example, the "security" folder contains login and registration documentation in two separate files.


# Main Documentation Files
- index.adoc
Main documentation entry point introducing the Yada Framework, its technology stack (Java, Spring MVC, MySQL, Hibernate, Thymeleaf), general principles focused on productivity and simplicity

- newEclipseProject.adoc
Complete tutorial for creating a new Eclipse project with Yada Framework, including prerequisites (Java, MySQL, Eclipse), setup instructions, and deployment procedures

- ajax.adoc
Comprehensive guide to Ajax functionality in Yada Framework, covering ajax links, form submissions, response handling, and integration with the yada.ajax.js library

- security.adoc
Authentication and authorization setup using Spring Security, including user management, login handling, page protection, and configuration requirements

- internationalization.adoc
Complete i18n implementation guide covering language selection, URL localization, static text translation, database text fields, and locale handling

- ajaxModal.adoc
Instructions for opening Bootstrap modals via Ajax calls, including server-side modal content generation and client-side integration

- confirmationModal.adoc
Implementation of confirmation dialogs for dangerous operations like deletions, with modal setup and user confirmation workflows

- datatables.adoc
Integration guide for DataTables JavaScript library with Yada's Java Fluent API for creating complex interactive tables with server-side data loading

- emails.adoc
Email sending functionality using HTML templates, SMTP configuration, and internationalization support for various email scenarios

- json.adoc
Working with JSON data in Yada Framework, including serialization in HTML templates and Java object conversion

- notificationModal.adoc
System for displaying feedback messages to users via modals, with support for different message severities and titles

- misc.adoc
Miscellaneous features including temporary file downloads with automatic cleanup and other utility functions

- staging.adoc
Documentation snippets awaiting proper categorization, currently containing clipboard copy functionality

- troubleshooting.adoc
Common runtime problems and solutions, focusing on application startup issues and debugging approaches

- upgrade.adoc
Version upgrade information, noting current stable version (0.7.6) and development version (0.7.7) with technology stack updates

Subdirectories

- database: Contains files about database operations (embedded, overview, pagination)
- forms: Form handling documentation (field components, image galleries, uploads, slides, overview)
- security: Detailed security topics (login, registration, overview)
- examples: Tutorial examples including the bookstore case study

# Configuration Files
- .asciidoctorconfig.adoc
AsciiDoctor configuration settings
- docinfo-header.html & docinfo.html
HTML header and document info for generated documentation

Each file is written in AsciiDoc format and focuses on specific aspects of web development using the Yada Framework, providing both tutorial-style guidance and reference documentation.

