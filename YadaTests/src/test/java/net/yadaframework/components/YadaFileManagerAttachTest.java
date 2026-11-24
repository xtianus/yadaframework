package net.yadaframework.components;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.persistence.repository.YadaAttachedFileDao;

/**
 * Attaching a file creates its target folder, also when an existing attachment is replaced
 * after the contents folder was lost (e.g. a database copied without its files).
 */
class YadaFileManagerAttachTest {
	@TempDir
	Path contentsRoot;

	@Test
	void attachReplaceCreatesAMissingTargetFolder() throws Exception {
		YadaConfiguration config = mock(YadaConfiguration.class);
		when(config.getContentPath()).thenReturn(contentsRoot.toString());
		YadaAttachedFileDao attachedFileDao = mock(YadaAttachedFileDao.class);
		when(attachedFileDao.save(any(YadaAttachedFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
		YadaFileManager fileManager = new YadaFileManager();
		ReflectionTestUtils.setField(fileManager, "config", config);
		ReflectionTestUtils.setField(fileManager, "yadaUtil", YadaUtil.INSTANCE);
		ReflectionTestUtils.setField(fileManager, "yadaAttachedFileDao", attachedFileDao);

		YadaAttachedFile existingAttachment = new YadaAttachedFile();
		ReflectionTestUtils.setField(existingAttachment, "config", config);
		existingAttachment.setId(1L);
		existingAttachment.setRelativeFolderPath("/configurationThumbnail");
		existingAttachment.setClientFilename("thumbnail.png");
		existingAttachment.setFilename("configuration-thumbnail_1_old.png");
		File targetFolder = contentsRoot.resolve("configurationThumbnail").toFile();
		assertFalse(targetFolder.exists());
		File uploadedFile = Files.writeString(contentsRoot.resolve("upload.png"), "png").toFile();

		YadaAttachedFile replaced = fileManager.attachReplace(true, existingAttachment, uploadedFile, null, "configuration-", null, null, null);

		assertNotNull(replaced.getFilename());
		assertTrue(new File(targetFolder, replaced.getFilename()).isFile());
		assertFalse(uploadedFile.exists());
	}
}
