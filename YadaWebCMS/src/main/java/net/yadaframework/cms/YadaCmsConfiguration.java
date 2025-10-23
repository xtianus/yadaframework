package net.yadaframework.cms;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;

// TODO refactor using a pattern similar to YadaAiConfigurable and use the "config/yadaWebCms/" prefix in the configuration
public abstract class YadaCmsConfiguration extends YadaConfiguration {
	private static Logger log = LoggerFactory.getLogger(YadaCmsConfiguration.class);
	
	@Lazy // Lazy because it woud give a circular refecence exception otherwise
	@Autowired
	protected YadaWebUtil yadaWebUtil;
	
	private File assetManagerPrivatePath;
	private File assetManagerPublicPath;
	private String assetManagerPublicUrl;

	public File getAssetManagerPrivatePath() {
		if (assetManagerPrivatePath==null) {
			String folderName = configuration.getString("config/paths/assetManager/privateDir");
			assetManagerPrivatePath = new File(super.getBasePathString(), folderName);
		}
		return assetManagerPrivatePath;
	}
	
	public File assetManagerPublicPath() {
		if (assetManagerPublicPath==null) {
			String folderName = configuration.getString("config/paths/assetManager/publicDir");
			assetManagerPublicPath = new File(super.getBasePathString(), folderName);
		}
		return assetManagerPublicPath;
	}
	
	public String assetManagerPublicUrl() {
		if (assetManagerPublicUrl==null) {
			String urlSegment = configuration.getString("config/paths/assetManager/publicDir/@url");
			assetManagerPublicUrl = yadaWebUtil.makeUrl(super.getContentUrl(), urlSegment);
		}
		return assetManagerPublicUrl;
	}
	
}
