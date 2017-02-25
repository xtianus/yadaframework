package net.yadaframework.tools

import org.gradle.api.DefaultTask
import org.gradle.api.Project

class YadaProject extends DefaultTask {
	// Task parameters
	/** Project name, can have spaces - must be set */
	def projectName = "";
	/** Project acronym - must be set */
	def acronym = "";
	/** Project base package - must be set */
	def basePackage = "";

	// Optional configuration
	/** Database passwords */
	def dbPasswords = ['dev': 'devpwd', 'col': 'colpwd', 'prod': 'prodpwd'];
	/** Path of the folder that will contain the project files when running **/ 
	def basePath = "/srv";
	/** Environment configuration root folder */
	def envDirName = "env";
	def schemaDirName = "schema";
	/** Environment names */
	def envs=['dev', 'col', 'prod'];
	/** Position of the development environment in the envs array */
	def devIndex = 0;
	/** Position of the production environment in the envs array */
	def prodIndex = 2;
	/** Resources folder inside webapp folder */
	def resDirName = "res";
	/** Messages folder inside WEB-INF folder */
	def messagesDirName = "messages";

	// Not to be changed
	final String RESOURCECONFIGROOT = "META-INF"; // Root folder for template configuration in the plugin jar, relative to "resources" - no leading slash!
	final String TEMPLATEDIRNAME = "template";
	final String DESTEMAILTEMPLATEFOLDER = "template/email"; // This must be the same as defined in YadaConstants.EMAIL_TEMPLATES_PREFIX + "/" + YadaConstants.EMAIL_TEMPLATES_FOLDER
	final String CONFIGURATIONDIRNAME = "config";
	final String SCRIPTDIRNAME = "script";
	final String INFODIRNAME = "info";
	final String CONFWEBAPPFILENAME = "conf.webapp.xml";
	final String TOMCATCONTEXTFILENAME = "context.xml";
	final String CREATEDBLINUX = "dropAndCreateDatabase.sh";
	final String LOGTESTCONFIGFILENAME = "logback-test.xml";
	final String LOGCONFIGFILENAME = "logback.xml";
	
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
