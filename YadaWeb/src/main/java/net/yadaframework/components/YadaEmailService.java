package net.yadaframework.components;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.expression.ThymeleafEvaluationContext;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaConstants;
import net.yadaframework.exceptions.YadaEmailException;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.web.YadaEmailContent;
import net.yadaframework.web.YadaEmailParam;

@Service
// Deve stare in questo package perchè tirato dentro da YadaWebConfig, altrimenti SpringTemplateEngine non viene iniettato
public class YadaEmailService {
	private Logger log = LoggerFactory.getLogger(YadaEmailService.class);	
	
	@Autowired private YadaConfiguration config;
	@Autowired private JavaMailSender mailSender;
	
	// Using @Resource we don't need a Qualifier annotation
	@Resource private SpringTemplateEngine emailTemplateEngine;
    
    @Autowired private MessageSource messageSource;
    
//    @Autowired private ServletContext servletContext;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private YadaWebUtil yadaWebUtil;
//    @Autowired private YadaUtil yadaUtil;
    
    /**
     * Convert a site-relative link to absolute, because in emails we can't use @{}.
     * Example: th:href="${@yadaEmailService.buildLink('/read/234')}"
     * @param relativeLink
     * @return absolute link
     */
    public String buildLink(String relativeLink) {
    	String myServerAddress = config.getServerAddress();
    	String relative = StringUtils.prependIfMissing(relativeLink, "/");
    	return myServerAddress + relative;
    }

	public boolean sendSupportRequest(String username, String supportRequest, HttpServletRequest request, Locale locale) {
		final String emailName = "supportRequest";
		final String[] toEmail = config.getSupportRequestRecipients();
		final String[] subjectParams = {username};
		
		String clientIp = yadaWebUtil.getClientIp(request);
		String userAgent = request.getHeader("user-agent");

		final Map<String, Object> templateParams = new HashMap<String, Object>();
		templateParams.put("username", username);
		templateParams.put("supportRequest", supportRequest);
		templateParams.put("clientIp", clientIp);
		templateParams.put("userAgent", userAgent);

		Map<String, String> inlineResources = new HashMap<String, String>();
		inlineResources.put("logosmall", config.getEmailLogoImage());
		return sendHtmlEmail(toEmail, emailName, subjectParams, templateParams, inlineResources, locale, true);
	}

    public boolean sendHtmlEmail(String[] toEmail, String emailName, Object[] subjectParams, Map<String, Object> templateParams, Map<String, String> inlineResources, Locale locale, boolean addTimestamp) {
    	return sendHtmlEmail(toEmail, null, emailName, subjectParams, templateParams, inlineResources, locale, addTimestamp);
    }
    
    /**
     * Invia una email usando un template thymeleaf.
     * Il template è localizzato, per cui si può chiamare ad esempio <emailName>.html oppure <emailName>_de.html.
     * Il subject è localizzato e parametrizzato, e la sua chiave è email.subject.<emailName>
     * @param toEmail
     * @param replyTo can be null
     * @param emailName nome del template, ovvero del file html senza estensione e localizzazione _xx
     * @param subjectParams Valori opzionali da inserire nella stringa localizzata da usare come subject, può essere null
     * @param templateParams variabili da usare nel template, come fossero attributi del Model. Può essere null.
     * @param inlineResources mappa chiave-valore di immagini inline di tipo "cid:". Il valore è un path relativo al context-path, come per esempio "/res/img/pippo.jpg"
     * @param locale
     * @param addTimestamp true to add a timestamp to the subject
     * @return true se l'email è stata spedita
     */
	public boolean sendHtmlEmail(String[] toEmail, String replyTo, String emailName, Object[] subjectParams, Map<String, Object> templateParams, Map<String, String> inlineResources, Locale locale, boolean addTimestamp) {
		return sendHtmlEmail(config.getEmailFrom(), toEmail, replyTo, emailName, subjectParams, templateParams, inlineResources, null, locale, addTimestamp);
	}
	
    /**
     * Invia una email usando un template thymeleaf.
     * Il template è localizzato, per cui si può chiamare ad esempio <emailName>.html oppure <emailName>_de.html.
     * Il subject è localizzato e parametrizzato, e la sua chiave è email.subject.<emailName>
     * @param toEmail
     * @param replyTo can be null
     * @param emailName nome del template, ovvero del file html senza estensione e localizzazione _xx
     * @param subjectParams Valori opzionali da inserire nella stringa localizzata da usare come subject, può essere null
     * @param templateParams variabili da usare nel template, come fossero attributi del Model. Può essere null.
     * @param inlineResources mappa chiave-valore di immagini inline di tipo "cid:". Il valore è un path relativo al context-path, come per esempio "/res/img/pippo.jpg"
     * @param attachments mappa filename-File di file da inviare come attachment. Il filename deve avere la giusta estensione per avere il corretto mime type. 
     * @param locale
     * @param addTimestamp true to add a timestamp to the subject
     * @return true se l'email è stata spedita
     */
	public boolean sendHtmlEmail(String[] fromEmail, String[] toEmail, String replyTo, String emailName, Object[] subjectParams, Map<String, Object> templateParams, Map<String, String> inlineResources, Map<String, File> attachments, Locale locale, boolean addTimestamp) {
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
		return sendHtmlEmail(yadaEmailParam);
	}
	
    /**
     * Invia una email usando un template thymeleaf.
     * Il template è localizzato, per cui si può chiamare ad esempio <emailName>.html oppure <emailName>_de.html.
     * Il subject è localizzato e parametrizzato, e la sua chiave è email.subject.<emailName>
     * @return true se l'email è stata spedita
     */
	public boolean sendHtmlEmail(YadaEmailParam yadaEmailParam) {
		String[] fromEmail = yadaEmailParam.fromEmail;
		String[] toEmail = yadaEmailParam.toEmail;
		String replyTo = yadaEmailParam.replyTo;
		String emailName = yadaEmailParam.emailName;
		Object[] subjectParams = yadaEmailParam.subjectParams;
		Map<String, Object> templateParams = yadaEmailParam.templateParams;
		Map<String, String> inlineResources = yadaEmailParam.inlineResources;
		Map<String, File> attachments = yadaEmailParam.attachments;
		Locale locale = yadaEmailParam.locale;
		boolean addTimestamp = yadaEmailParam.addTimestamp;
		//
		final String emailTemplate = getMailTemplateFile(emailName, locale);
		final String subject = messageSource.getMessage("email.subject." + emailName, subjectParams,  locale);
//		String myServerAddress = yadaWebUtil.getWebappAddress(request);
//		final WebContext ctx = new WebContext(request, response, servletContext, locale);
		// Using Context instead of WebContext, we can't access WebContent files and can't use @{somelink}
		final Context ctx = new Context(locale);
		// This allows the use of @beans inside the email template
		ctx.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME, new ThymeleafEvaluationContext(applicationContext, null));
		// Non so come si registra un bean resolver dentro a ctx, quindi uso "config" invece di "@config"
		// TODO This "config" bean is deprecated and should be removed one day
		ctx.setVariable("config", config);
		//
		if (templateParams!=null) {
			for (Entry<String, Object> entry : templateParams.entrySet()) {
				ctx.setVariable(entry.getKey(), entry.getValue());
			}
		}
//		ctx.setVariable("beans", new Beans(applicationContext)); // So I can use "beans.myBean" in the template (workaround for the missing "@myBean" support) 
	    final String body = this.emailTemplateEngine.process("/" + YadaConstants.EMAIL_TEMPLATES_FOLDER + "/" + emailTemplate, ctx);
	    YadaEmailContent ec = new YadaEmailContent();
	    ec.from = fromEmail!=null?fromEmail:config.getEmailFrom();
	    if (replyTo!=null) {
	    	ec.replyTo = replyTo;
	    }
	    ec.to = toEmail;
	    ec.subject = subject + (addTimestamp?" (" + timestamp(locale) +")":"");
	    ec.body = body;
	    ec.html = true;
	    if (inlineResources!=null) {
		    ec.inlineResourceIds = new String[inlineResources.size()];
		    ec.inlineResources = new org.springframework.core.io.Resource[inlineResources.size()];
		    int i=0;
			for (Entry<String, String> entry : inlineResources.entrySet()) {
				ec.inlineResourceIds[i] = entry.getKey();
//				Must support fully qualified URLs, e.g. "file:C:/test.dat".
//				Must support classpath pseudo-URLs, e.g. "classpath:test.dat".
//				Should support relative file paths, e.g. "WEB-INF/test.dat". (This will be implementation-specific, typically provided by an ApplicationContext implementation.)
				DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader();
				ec.inlineResources[i] = defaultResourceLoader.getResource(entry.getValue());
				if (!ec.inlineResources[i].exists()) {
					log.error("Invalid resource: " + entry.getValue());
				}
//				ec.inlineResources[i] = new ServletContextResourceLoader(servletContext).getResource(entry.getValue());
				i++;
			}
	    }
	    if (attachments!=null && attachments.size()>0) {
	    	Set<String> keySet = attachments.keySet();
	    	int size = keySet.size();
	    	ec.attachedFilenames = new String[size];
	    	ec.attachedFiles = new File[size];
	    	int i=0;
	    	for (String filename : attachments.keySet()) {
	    		ec.attachedFilenames[i] = filename;
	    		ec.attachedFiles[i] = attachments.get(filename);
	    		i++;
			}
	    }
		return sendEmail(ec);
	}

    /**
     * Dato un nome di template senza estensione, ne ritorna il nome completo di localizzazione.
     * Per esempio "saluti" diventa "saluti_it" se esiste, altrimenti resta "saluti".
     * Se il template non esiste, lancia InternalException
     * @param templateNameNoHtml
     * @param locale
     * @return
     */
    private String getMailTemplateFile(String templateNameNoHtml, Locale locale) {
//    	String base = "/WEB-INF/emailTemplates/";
    	String prefix = templateNameNoHtml; // emailChange
    	String languagePart = "_" + locale.getLanguage(); // _it
    	String suffix = ".html";
    	String filename = prefix + languagePart + suffix; // emailChange_it.html
    	// TODO check if the / before filename is still needed
		ClassPathResource classPathResource = new ClassPathResource(YadaConstants.EMAIL_TEMPLATES_PREFIX + "/" + YadaConstants.EMAIL_TEMPLATES_FOLDER + "/" + filename);
		if (classPathResource.exists()) {
			return prefix + languagePart;
		}
		filename = prefix + suffix; // emailChange.html
		classPathResource = new ClassPathResource(YadaConstants.EMAIL_TEMPLATES_PREFIX + "/" + YadaConstants.EMAIL_TEMPLATES_FOLDER + "/" + filename);
		if (classPathResource.exists()) {
			return prefix;
		}
    	throw new YadaInternalException("Email template not found: " + templateNameNoHtml);
    }

    /**
     * Send an email by specifying the content directly, without a template
	 * @param from [address, name]
	 * @param to
	 * @param replyTo
	 * @param cc
	 * @param bcc
	 * @param subject
	 * @param body
	 * @param html
	 * @param inlineFile
	 * @param inlineFilename
	 * @return
	 */
	public boolean sendEmail(String[] from, String to, String replyTo, String cc, String bcc, String subject, String body, boolean html, File inlineFile, String inlineFilename) {
		YadaEmailContent content = new YadaEmailContent();
		content.from = from;
		content.replyTo = replyTo;
		content.to = new String[]{to};
		content.cc = new String[]{cc};
		content.bcc = new String[]{bcc};
		content.subject = subject;
		content.body = body;
		content.html = html;
		content.inlineFiles = new File[] {inlineFile};
		content.inlineFileIds = new String[] {inlineFilename};
		return sendEmail(content);
	}

	public boolean sendEmail(YadaEmailContent yadaEmailContent) {
		try {
			MimeMessage msg = createMimeMessage(yadaEmailContent);
			if (msg!=null) {
				log.debug("Sending email to '{}'...", Arrays.asList(yadaEmailContent.to));
				mailSender.send(msg);
				log.debug("Email sent to '{}'", Arrays.asList(yadaEmailContent.to));
				return true;
			}
		} catch (Exception e) {
			String to = ArrayUtils.toString(yadaEmailContent.to, "");
			log.error("Error while sending email message to '{}'", to, e);
			if (config.isEmailThrowExceptions()) {
				throw new YadaEmailException(e);
			}
		}
		return false;
	}

	// Non usato ancora
	public boolean sendEmailBatch(List<YadaEmailContent> yadaEmailContents) {
		boolean result = true;
		List<MimeMessage> messageList = new ArrayList<MimeMessage>();
		for (YadaEmailContent yadaEmailContent : yadaEmailContents) {
			try {
				MimeMessage mimeMessage = createMimeMessage(yadaEmailContent);
				messageList.add(mimeMessage);
			} catch (Exception e) {
				result = false;
				log.error("Error while creating batch email message to {} (ignored)", yadaEmailContent.to, e);
			}
		}
		if (messageList.size()>0) {
			mailSender.send(messageList.toArray(new MimeMessage[messageList.size()])); // Batch
		}
		return result;
	}
	
	private String[] purifyRecipients(String[] addresses, YadaEmailContent yadaEmailContent) {
		// Se <validEmail> esiste, mando solo se l'email è nella lista
		List<String> validEmail = config.getValidDestinationEmails();
		if (validEmail!=null && !validEmail.isEmpty()) {
			List<String> recipients = new ArrayList<String>(Arrays.asList(addresses)); // Il doppio passaggio serve perchè asList è fixed size
			// Tengo solo quelli validi
			if (recipients.retainAll(validEmail)) {
				// Alcuni sono stati rimossi. Logghiamo quelli rimossi
				List<String> invalidEmails = new ArrayList<String>(Arrays.asList(addresses)); // Il doppio passaggio serve perchè asList è fixed size
				invalidEmails.removeAll(validEmail);
				for (String address : invalidEmails) {
					log.warn("Email not authorized in configuration (not in <validEmail>). Skipping message for '{}' from='{}' subject='{}' body='{}'",
						new Object[]{address, yadaEmailContent.from, yadaEmailContent.subject, yadaEmailContent.body});
				}
			}
			return recipients.toArray(new String[0]);
		} else {
			return addresses;
		}
	}
	
	private MimeMessage createMimeMessage(YadaEmailContent yadaEmailContent) throws MessagingException {
		if (!config.isEmailEnabled()) {
			log.warn("Emails not enabled. Skipping message from='{}' to='{}' cc='{}' bcc='{}' subject='{}' body='{}'", 
					new Object[]{yadaEmailContent.from, yadaEmailContent.to, yadaEmailContent.cc, yadaEmailContent.bcc, yadaEmailContent.subject, yadaEmailContent.body});
			return null;
		}
		int totRecipients = 0;
		if (yadaEmailContent.to!=null) {
			yadaEmailContent.to = purifyRecipients(yadaEmailContent.to, yadaEmailContent);
			totRecipients += yadaEmailContent.to.length;
		}		
		if (yadaEmailContent.cc!=null) {
			yadaEmailContent.cc = purifyRecipients(yadaEmailContent.cc, yadaEmailContent);
			totRecipients += yadaEmailContent.cc.length;
		}		
		if (yadaEmailContent.bcc!=null) {
			yadaEmailContent.bcc = purifyRecipients(yadaEmailContent.bcc, yadaEmailContent);
			totRecipients += yadaEmailContent.bcc.length;
		}		
		if ( totRecipients == 0) {
			return null;
		}
		if (!config.isProductionEnvironment()) { 
			String env = config.getApplicationEnvironment();
			yadaEmailContent.subject = "[" + (StringUtils.isBlank(env)?"TEST":env.toUpperCase()) + "] " + yadaEmailContent.subject;
//			emailContent.body = "<h3>Questa email è stata inviata durante un test del sistema, si prega di ignorarla, grazie.</h3>" + 
//			       "<h3>This email has been sent during a system test, please ignore it, thank you.</h3>" + emailContent.body;
		}
		
		MimeMessage msg = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8"); // true = multipart
		try {
			helper.setFrom(yadaEmailContent.from[0], yadaEmailContent.from[1]);
		} catch (UnsupportedEncodingException e) {
			log.error("Invalid encoding - ignoring 'from' personal name", e);
			helper.setFrom(yadaEmailContent.from[0]);
		}
		if(yadaEmailContent.replyTo!=null) {
			helper.setReplyTo(yadaEmailContent.replyTo);
		}
		if(yadaEmailContent.to!=null) {
			helper.setTo(yadaEmailContent.to);
		}
		if(yadaEmailContent.cc!=null) {
			helper.setCc(yadaEmailContent.cc);
		}
		if(yadaEmailContent.bcc!=null) {
			helper.setBcc(yadaEmailContent.bcc);
		}
		helper.setSubject(yadaEmailContent.subject);
		helper.setText(yadaEmailContent.body, yadaEmailContent.html); // true = html
		if (yadaEmailContent.inlineFiles!=null && yadaEmailContent.inlineFileIds!=null && yadaEmailContent.inlineFiles.length==yadaEmailContent.inlineFileIds.length) {
			for (int i = 0; i < yadaEmailContent.inlineFiles.length; i++) {
				File file = yadaEmailContent.inlineFiles[i];
				String fileId = yadaEmailContent.inlineFileIds[i];
				helper.addInline(fileId, file);
			}
		}
		if (yadaEmailContent.inlineResources!=null && yadaEmailContent.inlineResourceIds!=null && yadaEmailContent.inlineResources.length==yadaEmailContent.inlineResourceIds.length) {
			for (int i = 0; i < yadaEmailContent.inlineResources.length; i++) {
				org.springframework.core.io.Resource resource = yadaEmailContent.inlineResources[i];
				String resourceId = yadaEmailContent.inlineResourceIds[i];
				helper.addInline(resourceId, resource);
			}
		}
		if (yadaEmailContent.attachedFiles!=null && yadaEmailContent.attachedFilenames!=null && yadaEmailContent.attachedFiles.length==yadaEmailContent.attachedFilenames.length) {
			for (int i = 0; i < yadaEmailContent.attachedFiles.length; i++) {
				File file = yadaEmailContent.attachedFiles[i];
				String filename = yadaEmailContent.attachedFilenames[i];
				helper.addAttachment(filename, file);
				// helper.addInline(filename, file);
			}
		}
		log.info("Sending email to={}, from={}, replyTo={}, cc={}, bcc={}, subject={}", new Object[] {yadaEmailContent.to, yadaEmailContent.from, yadaEmailContent.replyTo, yadaEmailContent.cc, yadaEmailContent.bcc, yadaEmailContent.subject});
		log.debug("Email body = {}", yadaEmailContent.body);
		return msg;
	}
	
	public String timestamp(Locale locale) {
		return DateFormatUtils.format(new Date(), "yyyy-MM-dd@HH:mm", locale);
	}
	
	public String timestamp() {
		return DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date());
	}

}
