package net.yadaframework.components;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInvalidValueException;
import net.yadaframework.persistence.entity.YadaAttachedFile;

/**
 * Used when cloning some object that contains instances of YadaAttachedFile at any level.
 * All files are copied to a temp folder, then moved to the destination after cloning.
 * Needed when the destination folder is different from the source folder and the path contains
 * some id of the clone, which are not known during cloning.
 */
public class YadaAttachedFileCloneSet {
	// Map from the original relative path to the new instances
	private Map<String, Set<YadaAttachedFile>> allFiles = new HashMap<>();
	private Path tempFolder = null;
	private String tempRelativeFolderPath = null;
	
	public YadaAttachedFileCloneSet() throws IOException {
		YadaConfiguration config = (YadaConfiguration) YadaUtil.getBean("config"); // Substitute for autowiring
		File contentsFolder = config.getContentsFolder();
		tempFolder = Files.createTempDirectory(contentsFolder.toPath(), "yada");
		tempRelativeFolderPath = "/" + tempFolder.toFile().getName();
	}
	
	// Package visibility
	void handle(YadaAttachedFile yadaAttachedFile) {
		String mapKey = yadaAttachedFile.getRelativeFolderPath(); // Still same as the original before copy
		Set<YadaAttachedFile> copySet = allFiles.get(mapKey);
		if (copySet==null) {
			copySet = new HashSet<>();
			allFiles.put(mapKey, copySet);
		}
		copySet.add(yadaAttachedFile);
		yadaAttachedFile.setRelativeFolderPath(tempRelativeFolderPath); // Moved to temp folder
	}
	
	/**
	 * Move all collected files to some relative folder (will be created when missing)
	 * @param oldRelativeNewRelative map from old relative folder to new relative folder
	 * @throws IOException
	 */
	public void moveAll(Map<String, String> oldRelativeNewRelative) throws IOException {
		try {
			for (Entry<String, Set<YadaAttachedFile>> entry : allFiles.entrySet()) {
				String key = entry.getKey();
				Set<YadaAttachedFile> copySet = entry.getValue();
				String newRelativePath = oldRelativeNewRelative.get(key);
				oldRelativeNewRelative.remove(key);
				if (newRelativePath==null) {
					throw new YadaInvalidValueException("Value missing for source path {}", key);
				}
				for (YadaAttachedFile yadaAttachedFile : copySet) {
					yadaAttachedFile.move(newRelativePath);
				}
			}
		} finally {
			YadaUtil.INSTANCE.cleanupFolder(tempFolder, null);
			tempFolder.toFile().delete();
		}
		if (oldRelativeNewRelative.size()>0) {
			throw new YadaInvalidValueException("Unused values in oldRelativeNewRelative {}", oldRelativeNewRelative.keySet());
		}
	}
}
