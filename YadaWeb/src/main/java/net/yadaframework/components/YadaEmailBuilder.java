package net.yadaframework.components;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import net.yadaframework.web.YadaEmailParam;

/**
 * Api for building and sending emails using Thymeleaf templates. 
 * Start with {@link #instance(String, Locale, YadaEmailService)} and finish with {@link #send()}
 * @since 0.7.7
 */
public class YadaEmailBuilder {
	private String emailName;
	private String[] fromEmail;
    private String[] toEmail;
    private String replyTo;
    private Object[] subjectParams = null;
    private Map<String, Object> templateParams = new HashMap<>();
    private Map<String, String> inlineResources = new HashMap<>();
    private Map<String, File> attachments = new HashMap<>();
    private Locale locale;
    private boolean addTimestamp = false;
    private boolean batch = false;
    private YadaEmailService yadaEmailService;
    
    /**
     * Get a new instance of email builder to send an email.
     * @param emailName Email template name, which is the html file with no extension nor localization suffix
     * @param locale locale of the recipient
     * @param yadaEmailService instance of yadaEmailService
     * @return the builder
     */
    public static YadaEmailBuilder instance(String emailName, Locale locale, YadaEmailService yadaEmailService) {
    	YadaEmailBuilder instance = new YadaEmailBuilder();
    	instance.emailName = emailName;
    	instance.locale = locale;
    	instance.yadaEmailService = yadaEmailService;
    	return instance;
    }
    
    // Constructor must not be public
    private YadaEmailBuilder() {
    }

    /**
     * @param fromEmail The email to show as sender. When null, both these values are taken from config.getEmailFrom() 
     * @param fromName optional name to show as sender
     * @see YadaEmailBuilder#from(String ...fromNameAndEmail)
     */
    public YadaEmailBuilder from(String fromEmail, Optional<String> fromName) {
        this.fromEmail = new String[] { fromEmail, fromName.orElse(null)};
        return this;
    }
    
    /**
     * @param fromNameAndEmail an array where the first cell is the email to show as sender, the second cell is the name to show as sender.
     * @see YadaEmailBuilder#from(String, Optional)
     */
    public YadaEmailBuilder from(String ...fromNameAndEmail) {
    	this.fromEmail = fromNameAndEmail;
    	return this;
    }

    /**
     * @param toEmail The recipient email(s), must not be null or empty. A single email is sent to all the specified recipients unless
     * batch mode is used, in which case a copy of the same email is sent to each recipient.
     */
    public YadaEmailBuilder to(String...toEmail) {
        this.toEmail = toEmail;
        return this;
    }

    /**
     * Specify a "reply-to" field in the email. This is really useful only if the "from" email is different from this value.
     * Actually if they don't differ, better leave this out so that the from name is also used when replying in the email client.
     * @param replyTo The replyTo email. Can be null.
     * @return
     */
    public YadaEmailBuilder replyTo(String replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    /**
     * Assigns values to subject parameters as {0}, {1} etc. - can be omitted when the subject has no parameters.
     * @param subjectParams values
     */
    public YadaEmailBuilder subjectParams(Object...subjectParams) {
        this.subjectParams = subjectParams;
        return this;
    }
    
    /**
     * Thymeleaf model attribute to use in the template.
     * @param name attribute name
     * @param value attribute value
     */
    public YadaEmailBuilder addModelAttribute(String name, Object value) {
    	templateParams.put(name, value);
    	return this;
    }

    /**
     * Thymeleaf model attributes to use in the template - can be null.
     * @param templateParams a map of model attributes to use in the template
     */
    public YadaEmailBuilder modelAttributes(Map<String, Object> templateParams) {
        this.templateParams = templateParams;
        return this;
    }
    
    /**
     * Add an inline image to be used with src="cid:somekey"
     * @param cidname the image name, e.g. "somekey"
     * @param path the image context-relative path, e.g. "/res/img/mylogo.jpg" 
     */
    public YadaEmailBuilder addInlineResources(String cidname, String path) {
        inlineResources.put(cidname, path);
        return this;
    }

    /**
     * Key-value pairs of inline images to be used with "cid:somekey" as src. The value is a context-relative path, e.g. "/res/img/mylogo.jpg" 
     */
    public YadaEmailBuilder inlineResources(Map<String, String> inlineResources) {
        this.inlineResources = inlineResources;
        return this;
    }

    /**
     * Add a file attachment.
     * @param filename name of the file to add, with correct extension to derive the proper mime type
     * @param attachment payload
     */
    public YadaEmailBuilder addAttachment(String filename, File attachment) {
    	attachments.put(filename, attachment);
        return this;
    }

    /**
     * Key-value pairs of filename-File to send as attachment. The filename key should have the correct extension to produce the expected mime type.
     */
    public YadaEmailBuilder attachments(Map<String, File> attachments) {
        this.attachments = attachments;
        return this;
    }

    /**
     * Add a timestamp to the subject - defaults to false. 
     * Useful when sending many similar emails to the same recipient so they don't get collapsed by email clients under the same subject.
     * For example, when notification emails are critical.
     */
    public YadaEmailBuilder addTimestamp(boolean addTimestamp) {
        this.addTimestamp = addTimestamp;
        return this;
    }

    /**
     * Choose whether to send this email to all recipients as a batch of distinct emails
     * @param batch true or false
     */
    public YadaEmailBuilder batch(boolean batch) {
        this.batch = batch;
        return this;
    }

    /**
     * Send this email to all recipients as a batch of distinct emails
     */
    public YadaEmailBuilder batch() {
        this.batch = true;
        return this;
    }
    
    /**
     * Send the email.
     * @return true if the email has been sent correctly.
     */
    public boolean send() {
    	return yadaEmailService.sendHtmlEmail(getYadaEmailParam(), batch);
    }
    
    private YadaEmailParam getYadaEmailParam() {
		YadaEmailParam yadaEmailParam = new YadaEmailParam();
		yadaEmailParam.fromEmail = fromEmail;
		yadaEmailParam.toEmail = toEmail;
		yadaEmailParam.replyTo = replyTo;
		yadaEmailParam.emailName = emailName;
		yadaEmailParam.subjectParams = subjectParams;
		yadaEmailParam.templateParams = templateParams;
		yadaEmailParam.inlineResources = inlineResources;
		yadaEmailParam.attachments = attachments;
		yadaEmailParam.locale = locale;
		yadaEmailParam.addTimestamp = addTimestamp;
		return yadaEmailParam;
    }

}
