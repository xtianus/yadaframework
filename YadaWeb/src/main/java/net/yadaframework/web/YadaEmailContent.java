package net.yadaframework.web;
import java.io.File;

import org.springframework.core.io.Resource;

public class YadaEmailContent {
	public String from;
	public String[] to;
	public String replyTo;
	public String[] cc;
	public String[] bcc;
	public String subject;
	public String body;
	public boolean html;
	public File[] inlineFiles;
	public String[] inlineFileIds;
	public Resource[] inlineResources;
	public String[] inlineResourceIds;
	public File[] attachedFiles;
	public String[] attachedFilenames;
}