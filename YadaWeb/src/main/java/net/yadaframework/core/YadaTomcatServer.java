package net.yadaframework.core;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.Arrays;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yadaframework.exceptions.YadaInvalidUsageException;

/**
 * Tomcat Embedded. Use the args constructor to accept the provided configurator. To create a different configuration, extend this class.
 * HTTPS is only enabled in "dev mode".
 * Default ports and other config params can be changed by providing a "yadaTomcatServer.properties" file in the current working directory
 * with these optional parameters:
 * 	port.http = 8080
 * 	port.https = 8443
 * 	port.ajp = 8009
 * 	port.ajp.redirect = 8443
 * 	port.shutdown = 8005
 * 	keystore.file = /srv/devtomcatkeystore
 * 	keystore.password = changeit
 *
 */
public class YadaTomcatServer {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final String KEYSTOREFILE = "/srv/devtomcatkeystore"; // Needed for HTTPS - See comments below
	private final String CONFIGFILE = "yadaTomcatServer.properties"; // Optional

    private Tomcat tomcat;
    private String acroenv;

    /**
     * Starts the standalone server on port 8080. All parameters can be specified in a file yadaTomcatServer.json found in the cwd
     * @param args
     *        - acronym+environment used for the shutdown command
     *        - relative path of the webapp folder in eclipse ("src/main/webapp"), or the full path elsewhere
     *        - the third argument is optional in Eclipse, otherwise it must be the full path of the temp folder for Tomcat data (where the war is exploded)
     *        When the third argument is not provided, "dev mode" is assumed.
     *        When the third argument is specified, an additional fourth argument of "dev" starts in developer mode
     * @throws Exception
     */
	public static void main(String[] args) throws Exception {
		YadaTomcatServer yadaTomcatServer = new YadaTomcatServer(args);
		yadaTomcatServer.start();
	}

	/**
	 * Create an instance of the server for custom configuration
	 */
	public YadaTomcatServer() {
		this.tomcat = new Tomcat();
	}

	/**
	 * Create an instance of the server using the configuration specified in the args parameter
	 * @param args
	 * @throws ServletException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ConfigurationException 
	 */
	public YadaTomcatServer(String[] args) throws ServletException, MalformedURLException, IOException, ConfigurationException {
		this();
		
		// Loading optional configuration
		BasicConfigurationBuilder<PropertiesConfiguration> builder = new BasicConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class);
		Configuration tomconf = builder.getConfiguration(); // Default empty config
		File configFile = new File(CONFIGFILE);
		if (configFile.canRead()) {
			log.warn("Loading Tomcat configuration from {}", configFile.getAbsolutePath());
			Configurations configurations = new Configurations();
			tomconf = configurations.properties(configFile);
		}
		
		log.debug("Starting Tomcat server with args: {}", Arrays.asList(args));
		if (args.length == 0 || args.length>4) {
			throw new YadaInvalidUsageException("Command line parameter missing. Usage: {} <acroenv> <webappFolder> [<baseDir> [dev]]", YadaTomcatServer.class.getName());
		}
		this.acroenv = args[0];
		String webappFolder = args[1];
		String baseDir=null;
		boolean dev = true;
		if (args.length>2) {
			baseDir = args[2];
			// Is there an additional "dev" argument? If not, dev is false
			dev = args.length>3 && "dev".equals(args[3]);
			if (!new File(baseDir).canWrite()) {
				throw new YadaInvalidUsageException("The baseDir {} must exist and be writable", new File(baseDir));
			}
		}
		this.configure(webappFolder, baseDir, dev, tomconf);
	}

	public void start() throws LifecycleException {
		try {
			log.info("Starting Tomcat embedded server...");
			long startTime = System.currentTimeMillis();
			tomcat.start();
			Connector[] connectors = tomcat.getService().findConnectors();
			String connectorPorts = "";
			for (int i = 0; i < connectors.length; i++) {
				connectorPorts += connectors[i].getPort() + "(" + connectors[i].getScheme() + ")";
				if (i<connectors.length-1) {
					connectorPorts += ", ";
				}
			}
			log.info("Tomcat started in {} ms on ports {}", System.currentTimeMillis() - startTime, connectorPorts);
			tomcat.getServer().await();
		} catch (LifecycleException e) {
			tomcat.destroy();
			log.error("Server exited in error", e);
		}
	}

	/**
	 * Compression only works on the standard HTTP connector, not for AJP.
	 * Override this method to add compressable mime types or to disable compression.
	 * @param connector the connector on which to enable compression
	 * @param the compressable mime types as a comma-separated list.
	 * 		  The empty string disables compression, the null value uses the default mime types for compression.
	 */
	protected void setCompressableMimeType(Connector connector, String mimeTypeList) {
		if (!"".equals(mimeTypeList)) {
			connector.setProperty("compression", "on");
			if (mimeTypeList==null) {
				connector.setProperty("compressableMimeType",
						"text/html,"
						+ "text/xml,"
						+ "text/plain,"
						+ "text/css,"
						+ "application/xml,"
						+ "text/javascript,"
						+ "application/javascript,"
						+ "application/x-javascript,"
						+ "application/pdf,"
						+ "application/json,"
						+ "text/json,"
						+ "application/octet-stream");
			} else {
				connector.setProperty("compressableMimeType", mimeTypeList);
			}
		}
	}

	/**
	 * Default configurator. Write your own when using the no-args constructor.
	 * @param webappFolder
	 * @param baseDir
	 * @param dev
	 * @param tomconf 
	 * @throws ServletException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	protected void configure(String webappFolder, String baseDir, boolean dev, Configuration tomconf) throws ServletException, MalformedURLException, IOException {
		log.debug("webappFolder={}", webappFolder);
		log.debug("baseDir={}", baseDir);
		log.debug("dev={}", dev);
		System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");
		if (baseDir!=null) {
			tomcat.setBaseDir(baseDir);
		}
		tomcat.setPort(tomconf.getInt("port.http", 8080));
		tomcat.getConnector().setThrowOnFailure(true);
		setCompressableMimeType(tomcat.getConnector(), null);
		tomcat.setAddDefaultWebXmlToWebapp(false); // Use web.xml
		StandardContext ctx = (StandardContext) tomcat.addWebapp("", new File(webappFolder).getAbsolutePath());
		if (dev) {
			File eclipseClasses = new File("bin/main");
			if (eclipseClasses.canRead() && eclipseClasses.list().length>0) {
				WebResourceRoot resources = new StandardRoot(ctx);
				// Needed in Eclipse because classes are found in the "bin" folder
				log.warn("Adding eclipse bin folder to classpath");
				resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", eclipseClasses.getAbsolutePath(), "/"));
				ctx.setResources(resources);
			}
		}

        // AJP Connector
        Connector ajpConnector = new Connector("AJP/1.3");
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setSecretRequired(false);
        ajpConnector.setScheme("ajp");
        ajpConnector.setRedirectPort(tomconf.getInt("port.ajp.redirect", 8443));
        // See https://tomcat.apache.org/tomcat-8.5-doc/config/ajp.html
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setMaxConnections(8192);
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setMaxThreads(200);
        // TODO verificare se sia necessario incrementare questo valore per postare immagini etc.
        // ajpConnector.setMaxPostSize(maxPostSize); 2097152 (2 megabytes) default
        ajpConnector.setPort(tomconf.getInt("port.ajp", 8009));
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setAddress(InetAddress.getByAddress(new byte[] {0,0,0,0}));
        tomcat.getService().addConnector(ajpConnector);

        // HTTPS Connector
        if (dev && new File(KEYSTOREFILE).canRead()) {
	        Connector httpsConnector = new Connector("HTTP/1.1");
	        httpsConnector.setPort(tomconf.getInt("port.https", 8443));
	        httpsConnector.setSecure(true);
	        httpsConnector.setScheme("https");
	        httpsConnector.setProperty("SSLEnabled", "true");
	        httpsConnector.setProperty("clientAuth", "false");
	        httpsConnector.setProperty("sslProtocol", "TLS");
	        // Create keystore with
	        // keytool -genkey -noprompt -alias tomcat -dname "CN=localhost, OU=Unknown, O=Unknown, L=Unknown, S=Unknown, C=Unknown" -keystore /srv/devtomcatkeystore -storepass changeit -keypass changeit -keyalg RSA
	        // Export certificate with
	        // keytool -export -noprompt -keystore /srv/devtomcatkeystore -alias tomcat -storepass changeit -file /tmp/tomcat.cer
	        // and double click the file to import it in the browser
	        httpsConnector.setProperty("keystoreFile", tomconf.getString("keystore.file", KEYSTOREFILE));
	        httpsConnector.setProperty("keystorePass", tomconf.getString("keystore.password", "changeit"));
	        tomcat.getService().addConnector(httpsConnector);
	        log.debug("HTTPS Connector enabled");
        } else {
        	log.debug("HTTPS Connector not enabled");
        }
        
        // 
		tomcat.getServer().setPort(tomconf.getInt("port.shutdown", 8005));
		String shutdownCommand = acroenv+"down";
		tomcat.getServer().setShutdown(shutdownCommand);
		log.info("Shutdown port is {}, shutdown command is '{}'", tomcat.getServer().getPort(), shutdownCommand);
	}

}

