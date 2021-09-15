package net.yadaframework.persistence.repository;

import java.io.File;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.persistence.entity.YadaManagedFile;
import net.yadaframework.raw.YadaIntDimension;

/**
 *
 */
@Repository
@Transactional(readOnly = true)
public class YadaFileManagerDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext private EntityManager em;
	@Autowired private YadaConfiguration config;
	@Autowired private YadaUtil yadaUtil;

	/**
	 * Delete all old YadaManagedFile that are temporary
	 */
	@Transactional(readOnly = false)
	public int deleteStale() {
		Calendar oldest = YadaUtil.addDays(new GregorianCalendar(), -2); // two days ago
		List<YadaManagedFile> stales = YadaSql.instance().selectFrom("from YadaManagedFile f")
			.where("f.temporary=true").and()
			.where("f.uploadTimestamp < :oldest").and()
			.setParameter("oldest", oldest.getTime())
			.query(em, YadaManagedFile.class).getResultList();
		for (YadaManagedFile yadaManagedFile : stales) {
			delete(yadaManagedFile);
		}
		return stales.size();
	}

	/**
	 * Deletes a file from disk and database
	 * @param yadaManagedFile
	 * @return true when deleted from disk
	 */
	@Transactional(readOnly = false)
	public boolean delete(YadaManagedFile yadaManagedFile) {
		if (yadaManagedFile==null) {
			return false;
		}
		if (yadaManagedFile.getId()!=null) {
			em.createNativeQuery("delete from YadaManagedFile where id = :id").setParameter("id", yadaManagedFile.getId()).executeUpdate();
		}
		return yadaManagedFile.getAbsoluteFile().delete();
	}

	/**
	 * After a file has been uploaded and copied to the upload directory, create a YadaManagedFile that points to it.
	 * @param multipartFile the multipart that was uploaded
	 * @param uploadedFile the file that has been uploaded already
	 * @param description user-entered description
	 * @return a new persisted YadaManagedFile
	 */
	@Transactional(readOnly = false)
    public YadaManagedFile createManagedFile(MultipartFile multipartFile, File uploadedFile, String description) {
		deleteStale(); // Cleanup
		String originalFilename = multipartFile.getOriginalFilename();
		long size = multipartFile.getSize();
		// Path uploadPath = config.getUploadsFolder().toPath();
		String relativeFolderPath = yadaUtil.relativize(config.getBasePath(), uploadedFile.getParentFile().toPath());
		// Path relativeFolderPath = .relativize(uploadedFile.toPath()).getParent(); // Can be null
		YadaIntDimension dimension = yadaUtil.getImageDimension(uploadedFile);
		//
		YadaManagedFile yadaManagedFile = new YadaManagedFile();
    	yadaManagedFile.setClientFilename(originalFilename);
    	yadaManagedFile.setDescription(description);
    	yadaManagedFile.setFilename(uploadedFile.getName());
    	yadaManagedFile.setRelativeFolderPath(relativeFolderPath);
    	yadaManagedFile.setUploadTimestamp(new Date());
    	yadaManagedFile.setDimension(dimension);
    	yadaManagedFile.setSizeInBytes(size);
    	em.persist(yadaManagedFile);
    	yadaUtil.autowire(yadaManagedFile);
    	return yadaManagedFile;
    }
	

}