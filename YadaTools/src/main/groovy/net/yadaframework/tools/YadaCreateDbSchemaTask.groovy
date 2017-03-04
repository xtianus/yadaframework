package net.yadaframework.tools
import java.nio.file.CopyOption;
import java.nio.file.Files
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.Persistence;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class YadaCreateDbSchemaTask extends DefaultTask {
	Map properties;
	def outputfilename = "generated.sql";
	
	@TaskAction
	def createDbSchema() {
		// Workaround for @Entity discovery: copy the persistence.xml file to the classes folder
		File fromFile = project.sourceSets.main.resources.files.find({it.name=='persistence.xml'})
		// File fromFile = new File("$project.buildDir/resources/main/META-INF/persistence.xml")
		File toFolder = new File("$project.buildDir/classes/main/META-INF");
		toFolder.mkdirs();
		File toFile = new File(toFolder, "persistence.xml")
		Files.copy(fromFile.toPath(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING) 
		// Using JPA schema generation the output doesn't have ";" at line end for mysql, so it's not good
		// Persistence.generateSchema("yadaPersistenceUnit", properties);
		
		// Using the ant task directly:		
		ant.taskdef(name: 'hibernatetool',
			classname: 'org.hibernate.tool.ant.HibernateToolTask',
			classpath: project.configurations.hibtools.asPath
			)
		System.out.println("Creating file ${project.projectDir}/schema/" + outputfilename);
		ant.hibernatetool(destdir: "${project.projectDir}/schema") {
			ant.jpaconfiguration(persistenceunit: 'yadaPersistenceUnit')
			ant.hbm2ddl(drop: 'false', export: 'false', outputfilename: outputfilename)
		}
	}
	
}
