package net.yadaframework.tools

import org.gradle.api.Plugin
import org.gradle.api.Project

class YadaToolsPlugin implements Plugin<Project> {
    void apply(Project project) {
		//project.extensions.create("configuration", YadaProjectConfiguration)
        YadaInitTask yadaInitTask = project.task('yadaInit', type: YadaInitTask);
//		yadaInitTask.setProject(project);
		if (!isWebProject(project)) {
			throw new Exception("Please apply the 'war' or 'ear' plugin to the build file");
		}
		//
		project.task('yadaDbSchema', type: YadaCreateDbSchemaTask);
    }

	boolean isWebProject(Project project) {
    	return project.plugins.hasPlugin('war') || project.plugins.hasPlugin('ear');
    }
}
