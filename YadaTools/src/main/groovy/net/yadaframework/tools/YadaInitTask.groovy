package net.yadaframework.tools

import groovy.text.StreamingTemplateEngine

import org.gradle.api.tasks.TaskAction

class YadaInitTask extends YadaProject {
	// TaskOutputs outputs = getOutputs()

	@TaskAction
	def initWebApp() {
		println 'projectName=' + projectName;
		println 'acronym=' + acronym;
		println 'basePackage=' + basePackage;

		if (projectName=="") {
			throw new Exception("'projectName' task property undefined");
		}
		if (acronym=="") {
			throw new Exception("'acronym' task property undefined");
		}
		if (basePackage=="") {
			throw new Exception("'basePackage' task property undefined");
		}
		//
		// Ensure folder structure
		//
		if (!isEclipseProject()) {
			println "Eclipse specific initialization skipped because eclipse plugin not added"
		} else {
			project.file("Launches").mkdir();
		}
		File webAppRootFolder = project.webAppDir; // "Doesn't compile" but works - injected by war plugin
		File resFolder = new File(webAppRootFolder, resDirName);
		File webinfFolder = new File(webAppRootFolder, "WEB-INF");
		File messagesFolder = new File(webinfFolder, messagesDirName);
		File viewsFolder = new File(webinfFolder, "views");
		File xmlFolder = new File(viewsFolder, "xml");
		File javaSourceFolder = project.sourceSets.main.java.srcDirs[0];
		File resourcesSourceFolder = project.sourceSets.main.resources.srcDirs[0];
		File databaseMigrationFolder = new File(resourcesSourceFolder, dbMigrationFolder);
		File emailTemplateFolder = new File(resourcesSourceFolder, "$DESTEMAILTEMPLATEFOLDER");
		File schemaFolder = project.file(schemaDirName);
		File cssImagesFolder = new File(resFolder, "css/images");
		File staticFolder = new File(webAppRootFolder, "static");
		resFolder.mkdirs();
		staticFolder.mkdirs();
		webinfFolder.mkdir();
		messagesFolder.mkdir();
		viewsFolder.mkdir();
		xmlFolder.mkdir();
		databaseMigrationFolder.mkdir();
		emailTemplateFolder.mkdirs();
		schemaFolder.mkdirs();
		String basePackageDirnames = basePackage.replaceAll('\\.', '/');
		File basePackageFolder = new File(javaSourceFolder, basePackageDirnames);
		basePackageFolder.mkdirs();
		new File(basePackageFolder, "persistence/entity").mkdirs();
		new File(basePackageFolder, "persistence/repository").mkdir();
		File javaWebFolder = new File(basePackageFolder, "web");
		File javaCoreFolder = new File(basePackageFolder, "core");
		File javaComponentsFolder = new File(basePackageFolder, "components");
		javaWebFolder.mkdir();
		javaCoreFolder.mkdir();
		javaComponentsFolder.mkdir();
//		new File(basePackageFolder, "components").mkdir();
		new File(resFolder, "ckeditor").mkdir();
		cssImagesFolder.mkdirs();
		new File(resFolder, "dataTables").mkdir();
		new File(resFolder, "dataTablesPlugin").mkdir();
		new File(resFolder, "font-awesome").mkdir();
		new File(resFolder, "fonts").mkdir();
		new File(resFolder, "jcrop").mkdir();
		new File(resFolder, "js").mkdir();
		yadaToolsUtil.copyAllFromClasspathFolder("$RESOURCECONFIGROOT/email", emailTemplateFolder);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/info/ckeditor.howto.txt", resFolder, true);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/info/static.txt", staticFolder, true);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/messages/messages.properties", messagesFolder);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/sitemap.xml", xmlFolder);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/web.xml", webinfFolder);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/build.properties", webinfFolder);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/home.html", viewsFolder);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/header.html", viewsFolder);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/footer.html", viewsFolder);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/modalLogin.html", viewsFolder);
		yadaToolsUtil.copyFileFromClasspathFolder("$RESOURCECONFIGROOT/example_gitignore", project.projectDir);
		//
		// Environment configuration
		//
		File envRootFolderFile = project.file(envDirName);
		envRootFolderFile.mkdir();
		// def envFolderFiles = [];
		for (env in envs) {
			File envFolderFile = new File(envRootFolderFile, env);
			envFolderFile.mkdir();
			// envFolderFiles.add(envFolderFile);
			copyEnvFiles(env, envFolderFile, resourcesSourceFolder, webAppRootFolder);
		}
		processTemplate(CONFIGURATIONDIRNAME, "configuration.xml", null, resourcesSourceFolder);
		//
		// Java Templates
		//
		List coreFiles = yadaToolsUtil.listFilesInClasspathFolder("$RESOURCECONFIGROOT/$TEMPLATEDIRNAME/java/core");
		for (filename in coreFiles) {
			def target = filename-".txt";
			if (target == "XXXConfiguration.java") {
				target = acronym.capitalize() + "XXXConfiguration.java"-"XXX";
			}
			processTemplate("java/core", filename, target, javaCoreFolder);
		}
		List webFiles = yadaToolsUtil.listFilesInClasspathFolder("$RESOURCECONFIGROOT/$TEMPLATEDIRNAME/java/web");
		for (filename in webFiles) {
			processTemplate("java/web", filename, filename-".txt", javaWebFolder);
		}
		List componentsFiles = yadaToolsUtil.listFilesInClasspathFolder("$RESOURCECONFIGROOT/$TEMPLATEDIRNAME/java/components");
		for (filename in componentsFiles) {
			processTemplate("java/components", filename, filename-".txt", javaComponentsFolder);
		}
	}

	private copyEnvFiles(env, File envFolderFile, File resourcesSourceFolder, File webAppRootFolder) {
		int pos = CONFWEBAPPFILENAME.lastIndexOf('.');
		String targetFilename = "${CONFWEBAPPFILENAME.substring(0, pos)}.$env${CONFWEBAPPFILENAME.substring(pos)}";
		
		if (isDevelopment(env)) {
			processTemplate(CONFIGURATIONDIRNAME, CONFWEBAPPFILENAME, targetFilename, resourcesSourceFolder, env);
			processTemplate(CONFIGURATIONDIRNAME, LOGTESTCONFIGFILENAME, LOGCONFIGFILENAME, resourcesSourceFolder, env);
			processTemplate(SCRIPTDIRNAME, "createDatabaseAndUser.bat", null, envFolderFile, env);
			processTemplate(SCRIPTDIRNAME, "dropAndCreateDatabase.bat", null, envFolderFile, env);
			processTemplate(CONFIGURATIONDIRNAME, "persistence.xml", null, new File(resourcesSourceFolder, "META-INF"), env);
			processTemplate(CONFIGURATIONDIRNAME, TOMCATCONTEXTFILENAME, null, new File(webAppRootFolder, "META-INF"), env);
		} else if (isProduction(env)) {
			processTemplate(CONFIGURATIONDIRNAME, CONFWEBAPPFILENAME, targetFilename, resourcesSourceFolder, env);
			processTemplate(CONFIGURATIONDIRNAME, LOGCONFIGFILENAME, null, envFolderFile, env);
			processTemplate(CONFIGURATIONDIRNAME, TOMCATCONTEXTFILENAME, null, envFolderFile, env);
			processTemplate(SCRIPTDIRNAME, CREATEDBLINUX, null, envFolderFile, env);
		} else {
			processTemplate(CONFIGURATIONDIRNAME, CONFWEBAPPFILENAME, targetFilename, envFolderFile, env);
			processTemplate(CONFIGURATIONDIRNAME, LOGCONFIGFILENAME, null, envFolderFile, env);
			processTemplate(CONFIGURATIONDIRNAME, TOMCATCONTEXTFILENAME, null, envFolderFile, env);
			processTemplate(SCRIPTDIRNAME, CREATEDBLINUX, null, envFolderFile, env);
			processTemplate(CONFIGURATIONDIRNAME, "robots.disallow.txt", "robots.txt", envFolderFile, env);
		}
	}
	
	/**
	 * Copy a template after injecting specific values
	 * @param sourceDirname relative to the "template" folder
	 * @param sourceFilename
	 * @param destFilename can be null to keep it like sourceFilename
	 * @param destFolder
	 * @param env optional environment name
	 */
	void processTemplate(String sourceDirname, String sourceFilename, String destFilename, File destFolder, String env="") {
		if (destFilename==null) {
			destFilename = sourceFilename;
		}
		destFolder.mkdirs();
		File outputFile = new File(destFolder, destFilename);
		if (!outputFile.exists()) {
			def binding = [
				env : env,
				envs : envs,
				acronym : acronym,
				basePath : basePath,
				schemaFolderPath : project.file(schemaDirName),
				basePackage : basePackage,
				projectName: projectName,
				dbpwd: dbPasswords[env]
			]
			ClassLoader classLoader = this.getClass().getClassLoader();
			InputStream inputStream = classLoader.getResourceAsStream("$RESOURCECONFIGROOT/$TEMPLATEDIRNAME/$sourceDirname/$sourceFilename");
			def engine = new StreamingTemplateEngine();
			def writer = engine.createTemplate(new InputStreamReader(inputStream)).make(binding);
			FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter(outputFile);
				writer.writeTo(fileWriter);
				yadaToolsUtil.printCopied("$outputFile");
			} catch (Exception e) {
				println "FAILED to write ${outputFile}: $e";
			} finally {
				if (fileWriter!=null) {
					fileWriter.close();
				}
			}
		} else {
			yadaToolsUtil.printSkipped("$outputFile");
		}
	}

}
