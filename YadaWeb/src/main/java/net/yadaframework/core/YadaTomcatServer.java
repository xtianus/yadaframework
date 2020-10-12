package net.yadaframework.core;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yadaframework.exceptions.YadaInvalidUsageException;

/**
 * Tomcat Embedded.
 *
 */
public class YadaTomcatServer {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
    private Tomcat tomcat;
    private long startTime;
    
    /**
     * Starts the standalone server on port 8080
     * @param args 
     *        - acronym+environment used for the shutdown command
     *        - relative path of the webapp folder in eclipse ("src/main/webapp"), or the full path elsewhere
     *        - the last argument is optional in Eclipse, otherwise it must be the full path of the temp folder for Tomcat data (where the war is exploded)
     * @throws Exception
     */
	public static void main(String[] args) throws Exception {
		if (args.length == 0 || args.length>3) {
			throw new YadaInvalidUsageException("Command line parameter missing. Usage: {} <acroenv> <webappFolder> [<baseDir>]", YadaTomcatServer.class.getName());
		}
		String acroenv = args[0];
		String webappFolder = args[1];
		String baseDir=null;
		boolean dev = true;
		if (args.length>2) {
			dev = false;
			baseDir = args[2];
			if (!new File(baseDir).canWrite()) {
				throw new YadaInvalidUsageException("The baseDir {} must exist and be writable", new File(baseDir));
			}
		}
		YadaTomcatServer yadaTomcatServer = new YadaTomcatServer();
		yadaTomcatServer.configure(webappFolder, baseDir, dev);
		yadaTomcatServer.start(acroenv);
	}
	
	private void start(String acroenv) throws LifecycleException {
		try {
			tomcat.start();
			log.info("Tomcat embedded server started in {} ms: ready for connections", System.currentTimeMillis() - startTime);
			tomcat.getServer().setPort(8005);
			String shutdownCommand = acroenv+"down";
			tomcat.getServer().setShutdown(shutdownCommand);
			tomcat.getServer().await();
		} catch (LifecycleException e) {
			tomcat.destroy();
			log.error("Server exited in error", e);
		}
	}

	private void configure(String webappFolder, String baseDir, boolean dev) throws ServletException, MalformedURLException, IOException {
		startTime = System.currentTimeMillis();
		log.info("Starting Tomcat embedded server...");
		System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");
		tomcat = new Tomcat();
		if (baseDir!=null) {
			tomcat.setBaseDir(baseDir);
		}
		tomcat.setPort(8080);
		tomcat.getConnector().setThrowOnFailure(true);
		tomcat.setAddDefaultWebXmlToWebapp(false); // Usa web.xml
		StandardContext ctx = (StandardContext) tomcat.addWebapp("", new File(webappFolder).getAbsolutePath());
		if (dev) {
			// Only needed in Eclipse because classes are found in the "bin" folder
			File additionWebInfClasses = new File("bin/main");
			// File additionWebInfClasses = new File("build/classes");
	        WebResourceRoot resources = new StandardRoot(ctx);
	        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", additionWebInfClasses.getAbsolutePath(), "/"));
	        ctx.setResources(resources);
		}

        // AJP Connector
        Connector ajpConnector = new Connector("AJP/1.3");
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setSecretRequired(false);
        ajpConnector.setScheme("ajp");
        ajpConnector.setRedirectPort(8443);
        // See https://tomcat.apache.org/tomcat-8.5-doc/config/ajp.html
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setMaxConnections(8192);
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setMaxThreads(200);
        // TODO verificare se sia necessario incrementare questo valore per postare immagini etc.
        // ajpConnector.setMaxPostSize(maxPostSize); 2097152 (2 megabytes) default
        ajpConnector.setPort(8009);
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setAddress(InetAddress.getByAddress(new byte[] {0,0,0,0}));
        tomcat.getService().addConnector(ajpConnector);

	}
	
}

