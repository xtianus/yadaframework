package net.yadaframework.web;

import java.util.HashMap;
import java.util.Map;

public class YadaCropDefinition {
	private Map<String, String> desktopCrop = new HashMap<>();
	private Map<String, String> mobileCrop = new HashMap<>();
	public Map<String, String> getDesktopCrop() {
		return desktopCrop;
	}
	public void setDesktopCrop(Map<String, String> desktopCrop) {
		this.desktopCrop = desktopCrop;
	}
	public Map<String, String> getMobileCrop() {
		return mobileCrop;
	}
	public void setMobileCrop(Map<String, String> mobileCrop) {
		this.mobileCrop = mobileCrop;
	}
}