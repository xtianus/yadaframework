package net.yadaframework.tools

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Input

class YadaProject extends DefaultTask {
	// Task parameters
	/** Project name, can have spaces - must be set */
	@Input def projectName = "";
	/** Project acronym - must be set */
	@Input def acronym = "";
	/** Project base package - must be set */
	@Input def basePackage = "";

	// Optional configuration
	/** Database passwords */
	@Input def dbPasswords = ['dev': 'devpwd', 'tst': 'tstpwd', 'prod': 'prodpwd'];
	/** Path of the folder that will contain the project files when running **/ 
	@Input def basePath = "/srv";
	/** Environment configuration root folder */
	@Input def envDirName = "env";
	@Input def schemaDirName = "schema";
	/** Environment names: the first must be "development", the last must be "production" */
	@Input def envs=['dev', 'tst', 'prod'];
	/** Resources folder inside webapp folder */
	@Input def resDirName = "res";
	/** Messages folder inside WEB-INF folder */
	@Input def messagesDirName = "messages";
	/** Classpath folder (inside /src/main/resources) where FlyWay database migrations are stored */
	@Input def dbMigrationFolder = "database";

	// Not to be changed
	/** Position of the development environment in the envs array */
	protected final int devIndex = 0;
	/** Position of the production environment in the envs array */
	protected final int prodIndex = envs.size() - 1;
	protected final String RESOURCECONFIGROOT = "META-INF"; // Root folder for template configuration in the plugin jar, relative to "resources" - no leading slash!
	protected final String TEMPLATEDIRNAME = "template";
	protected final String DESTEMAILTEMPLATEFOLDER = "template/email"; // This must be the same as defined in YadaConstants.EMAIL_TEMPLATES_PREFIX + "/" + YadaConstants.EMAIL_TEMPLATES_FOLDER
	protected final String CONFIGURATIONDIRNAME = "config";
	protected final String HTMLDIRNAME = "html";
	protected final String SCRIPTDIRNAME = "script";
	protected final String INFODIRNAME = "info";
	protected final String CONFWEBAPPFILENAME = "conf.webapp.xml";
	protected final String TOMCATCONTEXTFILENAME = "context.xml";
	protected final String CREATEDBLINUX = "dropAndCreateDatabase.sh";
	protected final String LOGTESTCONFIGFILENAME = "logback-dev.xml";
	protected final String LOGCONFIGFILENAME = "logback.xml";
	
	//
	// Project project;
	@Internal YadaToolsUtil yadaToolsUtil;
	
	YadaProject() {
		yadaToolsUtil = new YadaToolsUtil(project);
	}
	
	@Internal
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
