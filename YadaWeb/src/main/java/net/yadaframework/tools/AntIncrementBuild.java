package net.yadaframework.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Questa classe incrementa di uno il valore che trova per la property indicata.
 * Per esempio "build=0004" diventa "build=0005"
 * Viene usata da Ant con il task 
 * java fork="true" classname="net.yadaframework.tools.AntIncrementBuild"
 */
public class AntIncrementBuild {
	private static final String PROPERTY_NAME = "build";
	File configFile;
	
	public AntIncrementBuild(File configFile) {
		this.configFile = configFile;
	}

	public static void main(String[] args) {
		try {
			String filename = args[0];
			File configFile = new File(filename);
			if (!configFile.canRead()) {
				System.out.println("Input file unreadable, creating new: " + configFile.getCanonicalPath());
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
					writer.write("# This is the build number that is automatically increased at each deploy\n");
					writer.write(PROPERTY_NAME+"=0\n");
				}
			}
			AntIncrementBuild antIncrementBuild = new AntIncrementBuild(configFile);
			System.exit(antIncrementBuild.execute());
		} catch (Exception e) {
			help();
		}
		System.exit(1);
	}
	
	private int execute() {
		try {
			File newFile = new File(configFile.getAbsolutePath()+".tmp");
			
			try(BufferedReader reader = new BufferedReader(new FileReader(configFile));
				BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));) {
				String line = reader.readLine();
				while (line!=null) {
					String origLine = line;
					line = line.trim().replace(" ", ""); // Rimuovo tutti gli spazi
					if (line.startsWith(PROPERTY_NAME+"=")) {
						String stringValue = line.substring(PROPERTY_NAME.length()+1);
						int build = Integer.parseInt(stringValue)+1;
						System.out.println("New build value = " + build);
						writer.write(PROPERTY_NAME+"="+String.format("%04d\n", build));
					} else {
						writer.write(origLine+"\n");
					}
					line = reader.readLine();
				}
			} catch (Exception e) {
				return 1;
			}
			Files.move(newFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//			newFile.renameTo(configFile);
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1;
	}

	static void help() {
		System.out.println("Syntax: AntIncrementBuild <filename>");
	}

}
