package net.yadaframework.tools;

import java.io.File;
import java.net.URI
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems
import java.nio.file.Files;
import java.nio.file.Path
import java.nio.file.StandardCopyOption

import org.gradle.api.Project;

public class YadaToolsUtil {
	
	Project project;
	
	public YadaToolsUtil(Project project) {
		this.project = project;
	}
	
	/**
	 * List all files (without path) in a folder within a jar
	 * @param sourcePathInJar
	 * @return
	 */
	List listFilesInClasspathFolder(String sourcePathInJar) {
		List result = [];
		ClassLoader classLoader = this.getClass().getClassLoader();
		String templatePathString = classLoader.getResource(sourcePathInJar).toString().split("!")[0];
		FileSystem fileSystem = null;
		try {
			fileSystem = getOrCreateFileSystem(templatePathString);
			Path jarSourceFolder = fileSystem.getPath(sourcePathInJar);
			DirectoryStream<Path> stream = null;
			try {
				stream = Files.newDirectoryStream(jarSourceFolder)
				for (Path entry: stream) {
					result.add(entry.getFileName().toString());
				}
			} finally {
				if (stream!=null) {
					stream.close();
				}
			}
		} finally {
			if (fileSystem!=null) {
				fileSystem.close();
			}
		}
		return result;
	}

	/**
	 * Copy all files in a folder contained in the current jar
	 * @param sourcePathInJar the path of the folder from the start of the jar without leading /
	 * @param destinationFolder the folder in the filesystem where files need to be copied
	 */
	void copyAllFromClasspathFolder(String sourcePathInJar, File destinationFolder, boolean overwrite=false) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		String templatePathString = classLoader.getResource(sourcePathInJar).toString().split("!")[0];
		FileSystem fileSystem = null;
		try {
			fileSystem = getOrCreateFileSystem(templatePathString);
			Path jarSourceFolder = fileSystem.getPath(sourcePathInJar);
			DirectoryStream<Path> stream = null;
			try {
				stream = Files.newDirectoryStream(jarSourceFolder)
				for (Path entry: stream) {
					copyFile(entry, destinationFolder, overwrite)
				}
			} finally {
				if (stream!=null) {
					stream.close();
				}
			}
		} finally {
			if (fileSystem!=null) {
				fileSystem.close();
			}
		}
	}
	
	private FileSystem getOrCreateFileSystem(String pathString) {
		try {
			return FileSystems.getFileSystem(URI.create(pathString));
		} catch (FileSystemNotFoundException e) {
			return FileSystems.newFileSystem(URI.create(pathString), new HashMap<>());
		}
	} 
	
	void copyFileFromClasspathFolder(String sourcePathInJar, File destinationFolder, String destFilename=null, boolean overwrite=false) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		def resourceUrl = classLoader.getResource(sourcePathInJar);
		if (resourceUrl == null) {
			println "FAILED to locate resource on classpath: ${sourcePathInJar}";
			return;
		}
		destinationFolder.mkdirs();
		String entryPath = sourcePathInJar;
		String filename = destFilename ?: entryPath.substring(entryPath.lastIndexOf("/") + 1);
		Path targetPath = destinationFolder.toPath().resolve(filename);
		java.io.InputStream inputStream = null;
		try {
			inputStream = resourceUrl.openStream();
			if (!overwrite) {
				try {
					Files.copy(inputStream, targetPath);
					printCopied(targetPath.toString());
				} catch (java.nio.file.FileAlreadyExistsException e) {
					printSkipped(targetPath.toString());
				}
			} else {
				if (targetPath.toFile().exists()) {
					printReplaced(targetPath.toString());
				} else {
					printCopied(targetPath.toString());
				}
				Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}

	private copyFile(Path fileEntry, File destinationFolder, boolean overwrite) {
		String entryPath = fileEntry.toString();
		String filename = entryPath.substring(entryPath.lastIndexOf("/")+1);
		Path targetPath = destinationFolder.toPath().resolve(filename);
		if (!overwrite) {
			try {
				Files.copy(fileEntry, targetPath);
				printCopied(targetPath.toString());
			} catch (java.nio.file.FileAlreadyExistsException e) {
				printSkipped(targetPath.toString());
			}
		} else {
			if (targetPath.toFile().exists()) {
				printReplaced(targetPath.toString());
			} else {
				printCopied(targetPath.toString());
			}
			Files.copy(fileEntry, targetPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}
	
	void printReplaced(filename) {
		println "REPLACED ${filename-project.path}";
	}
	
	void printCopied(filename) {
		println "Copied ${filename-project.path}";
	}
	
	void printSkipped(filename) {
		println "Skipping existing file: ${filename-project.path}";
	}
}
