package net.yadaframework.tools
import java.nio.file.CopyOption;
import java.nio.file.Files
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.Persistence;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
* Docs: https://docs.jboss.org/tools/latest/en/hibernatetools/html/ant.html
* @param outputfilename the name of the output file
* @param update (true/false) set to true for the schema delta (might not be accurate)
*/
class YadaCreateDbSchemaTask extends DefaultTask {
	@Internal
	Map properties; // Is this used?
	def outputfilename = "generated.sql";
	def update = false;
	
	@TaskAction
	def createDbSchema() {
		// Workaround for @Entity discovery: copy the persistence.xml file to the classes folder
		File fromFile = project.sourceSets.main.resources.files.find({it.name=='persistence.xml'})
		// File fromFile = new File("$project.buildDir/resources/main/META-INF/persistence.xml")
		File toFolder = new File("$project.buildDir/classes/java/main/META-INF");
		toFolder.mkdirs();
		File toFile = new File(toFolder, "persistence.xml")
		if (fromFile!=null && toFile!=null) { 
			Files.copy(fromFile.toPath(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING) 
		} else { 
			System.out.println("Not copying persistence.xml from " + fromFile);
		}
		// Using JPA schema generation the output doesn't have ";" at line end for mysql, so it's not good
		// Persistence.generateSchema("yadaPersistenceUnit", properties);
		
		// Using the ant task directly:		
		ant.taskdef(name: 'hibernatetool',
			classname: 'org.hibernate.tool.ant.HibernateToolTask',
			classpath: project.configurations.hibtools.asPath
			)
		System.out.println("Creating file ${project.projectDir}/schema/${outputfilename}");
		// System.out.println("Classpath: ${project.configurations.hibtools.asPath}");
		// Remove the file because the current version of the tools appends on the existing file.
		// See org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile
		File outputFile = new File("${project.projectDir}/schema/${outputfilename}");
		outputFile.delete();
		ant.hibernatetool(destdir: "${project.projectDir}/schema") {
			ant.jpaconfiguration(persistenceunit: 'yadaPersistenceUnit')
			ant.hbm2ddl(drop: 'false', update: update, export: 'false', outputfilename: outputfilename)
		}
	}
	
}
