package net.yadaframework.tools

import org.gradle.api.DefaultTask
import org.gradle.api.Project

class YadaProject extends DefaultTask {
	// Task parameters
	/** Project name, can have spaces - must be set */
	@Internal def projectName = "";
	/** Project acronym - must be set */
	@Internal def acronym = "";
	/** Project base package - must be set */
	@Internal def basePackage = "";

	// Optional configuration
	/** Database passwords */
	@Internal def dbPasswords = ['dev': 'devpwd', 'tst': 'tstpwd', 'prod': 'prodpwd'];
	/** Path of the folder that will contain the project files when running **/ 
	@Internal def basePath = "/srv";
	/** Environment configuration root folder */
	@Internal def envDirName = "env";
	@Internal def schemaDirName = "schema";
	/** Environment names: the first must be "development", the last must be "production" */
	@Internal def envs=['dev', 'tst', 'prod'];
	/** Resources folder inside webapp folder */
	@Internal def resDirName = "res";
	/** Messages folder inside WEB-INF folder */
	@Internal def messagesDirName = "messages";
	/** Classpath folder (inside /src/main/resources) where FlyWay database migrations are stored */
	@Internal def dbMigrationFolder = "database";

	// Not to be changed
	/** Position of the development environment in the envs array */
	@Internal final int devIndex = 0;
	/** Position of the production environment in the envs array */
	@Internal final int prodIndex = envs.size() - 1;
	@Internal final String RESOURCECONFIGROOT = "META-INF"; // Root folder for template configuration in the plugin jar, relative to "resources" - no leading slash!
	@Internal final String TEMPLATEDIRNAME = "template";
	@Internal final String DESTEMAILTEMPLATEFOLDER = "template/email"; // This must be the same as defined in YadaConstants.EMAIL_TEMPLATES_PREFIX + "/" + YadaConstants.EMAIL_TEMPLATES_FOLDER
	@Internal final String CONFIGURATIONDIRNAME = "config";
	@Internal final String SCRIPTDIRNAME = "script";
	@Internal final String INFODIRNAME = "info";
	@Internal final String CONFWEBAPPFILENAME = "conf.webapp.xml";
	@Internal final String TOMCATCONTEXTFILENAME = "context.xml";
	@Internal final String CREATEDBLINUX = "dropAndCreateDatabase.sh";
	@Internal final String LOGTESTCONFIGFILENAME = "logback-test.xml";
	@Internal final String LOGCONFIGFILENAME = "logback.xml";
	
	//
	// Project project;
	YadaToolsUtil yadaToolsUtil;
	
	YadaProject() {
		yadaToolsUtil = new YadaToolsUtil(project);
	}
	
	boolean isEclipseProject() {
		return project.plugins.hasPlugin('eclipse');
	}
	
	boolean isDevelopment(String env) {
		return envs[devIndex] == env;
	}
	
	boolean isProduction(String env) {
		return envs[prodIndex] == env;
	}

//	public void setProject(Project project) {
//		this.project = project;
//		yadaToolsUtil = new YadaToolsUtil(project);
//	}

}
