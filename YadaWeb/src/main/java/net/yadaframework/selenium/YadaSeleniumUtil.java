package net.yadaframework.selenium;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaConfigurationException;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.raw.YadaHttpUtil;
import net.yadaframework.raw.YadaRegexUtil;

@Component
public class YadaSeleniumUtil {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	public final static int DRIVER_FIREFOX=0;
	public final static int DRIVER_CHROME=1;
    
	@Autowired private YadaConfiguration config;
	@Autowired private YadaUtil yadaUtil;
	
	private YadaRegexUtil yadaRegexUtil = new YadaRegexUtil();
	private YadaHttpUtil yadaHttpUtil = new YadaHttpUtil();
	private Pattern matchNonEmptyText = Pattern.compile(".*\\w+.*"); // Match any string that contains at least a word character

	  /**
	   * Run some javascript.
	   *
	   * <p>
	   * If the script has a return value (i.e. if the script contains a <code>return</code> statement),
	   * then the following steps will be taken:
	   *
	   * <ul>
	   * <li>For an HTML element, this method returns a WebElement</li>
	   * <li>For a decimal, a Double is returned</li>
	   * <li>For a non-decimal number, a Long is returned</li>
	   * <li>For a boolean, a Boolean is returned</li>
	   * <li>For all other cases, a String is returned.</li>
	   * <li>For an array, return a List&lt;Object&gt; with each object following the rules above. We
	   * support nested lists.</li>
	   * <li>For a map, return a Map&lt;String, Object&gt; with values following the rules above.</li>
	   * <li>Unless the value is null or there is no return value, in which null is returned</li>
	   * </ul>
	   *
	   * <p>
	   * Arguments must be a number, a boolean, a String, WebElement, or a List of any combination of
	   * the above. An exception will be thrown if the arguments do not meet these criteria. The
	   * arguments will be made available to the JavaScript via the "arguments" magic variable, as if
	   * the function were called via "Function.apply"
	   *
	   * @param script The JavaScript to execute
	   * @param webDriver
	   * @param args The arguments to the script. May be empty. They will be available to the script as the arguments[] array.
	   * @return One of Boolean, Long, Double, String, List, Map or WebElement. Or null.
	 */
	public Object runJavascript(String script,  WebDriver webDriver, Object...args) {
		JavascriptExecutor javascriptExecutor = (JavascriptExecutor) webDriver;
		return javascriptExecutor.executeScript(script, args);
	}

	/**
	 * Returns true if the current url matches the specified pattern
	 * @param urlPattern
	 * @param webDriver
	 * @return
	 */
	public boolean urlMatches(Pattern urlPattern, WebDriver webDriver) {
		Matcher m = urlPattern.matcher(webDriver.getCurrentUrl());
		return m.matches();
	}
	
	/**
	 * Returns true if the current url contains the specified string at any position
	 * @param urlSegment
	 * @param webDriver
	 * @return
	 */
	public boolean urlContains(String urlSegment, WebDriver webDriver) {
		return webDriver.getCurrentUrl().contains(urlSegment);
	}
	
	/**
	 * Returns a part of the page source.
	 * @param startPattern regular expression that matches the start of the search area, null for the beginning of the page.
	 * The matched text is not part of the search area.
	 * @param endPattern regular expression that matches the end of the search area, null for the end of the page
	 * The matched text is not part of the search area.
	 * @param extractPattern regular expression with one capturing group to search in the search area. Use null to just return the search area.
	 * @param webDriver
	 * @return
	 */
	public String getSourceSnippet(String startPattern, String endPattern, String extractPattern, WebDriver webDriver) {
		String pageSource = webDriver.getPageSource();
		return yadaRegexUtil.extractInRegion(pageSource, startPattern, endPattern, extractPattern);
	}
	
	/**
	 * Return a value calculated via javascript.
	 * @param javascriptCode Any valid javascript code with a return value
	 * @param webDriver
	 * @return
	 */
	public String getByJavascript(String javascriptCode, WebDriver webDriver) {
		JavascriptExecutor javascriptExecutor = (JavascriptExecutor) webDriver;
		return (String) javascriptExecutor.executeScript(javascriptCode);
	}
	
	
	private void sleepRandomShort() {
    	yadaUtil.sleepRandom(50, 600); // min-max sleep
    }
	
	/**
	 * Insert some text slowly into a field
	 * @param inputField
	 * @param text
	 */
	public void typeAsHuman(WebElement inputField, String text) {
		for (Character letter : text.toCharArray()) {
			sleepRandomShort();
			inputField.sendKeys(letter.toString());
		}
	}

	/**
	 * From now on, page load timeout will be set to the "slow" value defined in the "slowPageLoadSeconds" confg property
	 * @param webDriver
	 */
	public void setSlowPageLoadTimeout(WebDriver webDriver) {
		setPageloadTimeoutSeconds(webDriver, config.getSeleniumTimeoutSlowPageLoadSeconds());
	}

	/**
	 * From now on, page load timeout will be set to the "normale" value defined in the "pageLoadSeconds" confg property
	 * @param webDriver
	 */
	public void setNormalPageLoadTimeout(WebDriver webDriver) {
		setPageloadTimeoutSeconds(webDriver, config.getSeleniumTimeoutPageLoadSeconds());
	}
	
	/**
	 * Set the pageload timeout for the following requests
	 * @param webDriver
	 * @param timeoutSeconds timeout in seconds
	 */
	public void setPageloadTimeoutSeconds(WebDriver webDriver, long timeoutSeconds) {
		webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeoutSeconds));
		log.debug("Selenium page load timeout set to {}", timeoutSeconds);
	}
	
	/**
	 * Return the first element matched by the selector, or null if not found
	 * @param from a WebElement to start the search from, or the WebDriver to search in all the page
	 * @param by
	 * @return
	 */
	public WebElement findOrNull(SearchContext from, By by) {
		List<WebElement> webElements = from.findElements(by);
		if (webElements.isEmpty()) {
			return null;
		}
		return webElements.get(0);
	}
	
	/**
	 * Search for text
	 * @param from a WebElement to start the search from, or the WebDriver to search in all the page
	 * @param by
	 * @return the text found, or ""
	 */
	public String getTextIfExists(SearchContext from, By by) {
		try {
			WebElement webElement = from.findElement(by);
			return webElement.getText();
		} catch (Exception e) {
			return "";
		}
	}
	
	/**
	 * Returns true if an element contains the given text (literally)
	 * @param webElements to search text int
	 * @param text to search
	 * @return true if the text is found
	 */
	public boolean foundByText(List<WebElement> webElements, String text) {
		for (WebElement webElement : webElements) {
			if (webElement.getText().equals(text)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get an element by id
	 * @param id
	 * @param webDriver
	 * @return
	 */
	public WebElement findById(String id, WebDriver webDriver) {
		List<WebElement> contents = webDriver.findElements(By.id(id));
		return contents.isEmpty()?null:contents.get(0);
	}
	
	/**
	 * Check if an element with a given id exists
	 * @param id
	 * @param webDriver
	 * @return
	 */
	public boolean foundById(String id, WebDriver webDriver) {
		List<WebElement> contents = webDriver.findElements(By.id(id));
		return !contents.isEmpty();
	}
	
	/**
	 * Check if at least one element with the given class exists
	 * @param className without initial dot
	 * @param webDriver
	 * @return
	 */
	public boolean foundByClass(String className, WebDriver webDriver) {
		List<WebElement> contents = webDriver.findElements(By.className(className));
		return !contents.isEmpty();
	}
	
	/**
	 * Create a new browser instance positioning the window 
	 * @param customProfileDir the folder where to store the user profile, can be null to use the default temporary profile. The folder is created when missing.
	 * @param proxyToUse the address of the proxy, or null for direct connection
	 * @param cookiesToSet cookies to set after the first get of a document. Can be null or empty. Cookies are set only when a 
	 * cookie with the same name has not been received. It's not possible to set cookies BEFORE the first get (by design of WebDriver).
	 * @param driverType DRIVER_FIREFOX, DRIVER_CHROME
	 * @return
	 */
	public WebDriver makeWebDriver(File customProfileDir, InetSocketAddress proxyToUse, String proxyUser, String proxyPassword, Set<Cookie> cookiesToSet, int driverType) {
		return this.makeWebDriver(customProfileDir, proxyToUse, proxyUser, proxyPassword, cookiesToSet, driverType, null);
	}	
	
	/**
	 * Create a new browser instance positioning the window 
	 * @param customProfileDir the folder where to store the user profile, can be null to use the default temporary profile. The folder is created when missing.
	 * @param proxyToUse the address of the proxy, or null for direct connection
	 * @param proxyUser can be set for SOCKS5 proxies only
	 * @param proxyPassword can be set for SOCKS5 proxies only
	 * @param cookiesToSet cookies to set after the first get of a document. Can be null or empty. Cookies are set only when a 
	 * cookie with the same name has not been received. It's not possible to set cookies BEFORE the first get (by design of WebDriver).
	 * @param driverType DRIVER_FIREFOX, DRIVER_CHROME
	 * @param userAgent the user agent string, null for keeping the current browser's default. Not implemented for Firefox.
	 * @return
	 */
	public WebDriver makeWebDriver(File customProfileDir, InetSocketAddress proxyToUse, String proxyUser, String proxyPassword, Set<Cookie> cookiesToSet, int driverType, String userAgent) {
		final Set<Cookie> initialCookies = new HashSet<Cookie>();
		if (cookiesToSet!=null) {
			// Make a copy because we need to clear the set later
			initialCookies.addAll(cookiesToSet);
		}
		Proxy browserProxy = null;
		if (proxyToUse!=null) {
			browserProxy = new Proxy();
			String proxyHost = proxyToUse.getHostName();
			if (proxyHost.equals("0:0:0:0:0:0:0:0")) {
				proxyHost = "localhost";
			}
			int proxyPort = proxyToUse.getPort();
			log.debug("Setting browser proxy to {}:{}", proxyHost, proxyPort);
			// Only socks proxies can have authentication (limitation of Selenium API?)
			// and if a proxy has authentication it must be socks5
			if (proxyUser!=null && proxyPassword!=null) {
				browserProxy.setSocksVersion(5);
				browserProxy.setSocksProxy(proxyHost + ":" + proxyPort);
				browserProxy.setSocksUsername(proxyUser);
				browserProxy.setSocksPassword(proxyPassword);
			} else {
				browserProxy.setHttpProxy(proxyHost + ":" + proxyPort);
				browserProxy.setSslProxy(proxyHost + ":" + proxyPort);
			}
			// browserProxy.setProxyType(ProxyType.MANUAL);
		}
		
		MutableCapabilities capability;
		switch (driverType) {
		case DRIVER_FIREFOX:
			capability = new FirefoxOptions();
			if (customProfileDir!=null) {
				// Creating the folder is wrong because of permissions mismatch
				// customProfileDir.mkdirs();
				String path = customProfileDir.getAbsolutePath();
				log.debug("Setting Firefox user profile folder to {}", path);
				((FirefoxOptions)capability).addArguments("-profile", path);
				// capability.setCapability(FirefoxOptions.FIREFOX_OPTIONS, options);
			}
			if (userAgent!=null) {
				log.error("Custom User Agent for Firefox not implemented yet"); // Don't know how to implement it
			}
			break;
		case DRIVER_CHROME:
			capability = new ChromeOptions();
			if (customProfileDir!=null) {
				// Creating the folder is wrong because of permissions mismatch
				// customProfileDir.mkdirs();
				String path = customProfileDir.getAbsolutePath();
				log.debug("Setting Chrome user profile folder to {}", path);
				((ChromeOptions)capability).addArguments("user-data-dir=" + path);
				if (config.isDevelopmentEnvironment()) {
					// Fixes "plugin crashed" error when using symbolic links
					((ChromeOptions)capability).addArguments("--no-sandbox");
				}
				// capability.setCapability(ChromeOptions.CAPABILITY, options);
			}
			if (userAgent!=null) {
				log.debug("Setting Chrome user agent to {}", userAgent);
				((ChromeOptions)capability).addArguments("user-agent=" + userAgent);
			}
			break;
		default:
			throw new YadaInternalException("Invalid WebDriver type: " + driverType);
		}
		
		// Attenzione: da Firefox 48 sembra che il proxy si debba settare diversamente:
		// http://www.seleniumhq.org/docs/04_webdriver_advanced.jsp
		
		if (browserProxy!=null) {
			capability.setCapability(CapabilityType.PROXY, browserProxy);
		}
		URL remoteAddress;
		try {
			remoteAddress = new URL(config.getSeleniumHubAddress());
		} catch (MalformedURLException e1) {
			throw new YadaConfigurationException("Invalid Selenium Hub Address: {}", config.getSeleniumHubAddress(), e1);
		}
		WebDriver driver = new RemoteWebDriver(remoteAddress, capability) {
			@Override
			public void get(String url) {
				super.get(url);
				// Setting cookies
				// Remember: cookies can't be set with selenium before visiting the domain (this is by design of the WebDriver protocol:
				// they say it's for browser compatibility).
				// https://github.com/SeleniumHQ/selenium-google-code-issue-archive/issues/1953
				// This could be overcome (maybe) by modifying the driver so that it doesn't throw an exception, but the safest bet
				// is to use a proxy (littleProxy in my case) that sets cookies at first run.
				// Spiegazione:
				// i cookie NON possono essere settati PRIMA di fare una get, per cui uso LittleProxy (passato come proxyToUse) altrove
				// per iniettare i cookie la prima volta. Poi però LittleProxy non gestisce più i cookie dopo la prima iniezione,
				// ma si lascia che sia il browser a farlo. Per fare in modo che sia il browser a farlo, quei cookie devono essere
				// settati qui dentro al browser (verranno usati alla prossima get). Vengono però settati solo se dopo la prima get
				// non sono stati modificati, perché in tal caso ci pensa già il browser da solo a gestirli, o cancellati.
				// Per esempio, magari il session cookie viene settato da Littleproxy, ma poi se non viene ritornato dal server, 
				// deve essere qui settato nel browser altrimenti viene perso.
				if (!initialCookies.isEmpty()) {
					Set<Cookie> currentCookies = super.manage().getCookies();
					for (Iterator<Cookie> iterator = initialCookies.iterator(); iterator.hasNext();) {
						Cookie cookie = iterator.next();
						try {
							if (!currentCookies.contains(cookie)) {
								log.debug("WebDriver setta cookie nel browser: {}", cookie);
								super.manage().addCookie(cookie);
							}
							iterator.remove();
						} catch (Exception e) {
							log.debug("Can't set cookie {} on the current page {} (ignored, will retry later): " + e.toString(), cookie, url);
							// Keep going
						}
					}
				}
			}
		};
		// WebDriver driver = new FirefoxDriver();
		// driver.manage().timeouts().pageLoadTimeout(config.getSeleniumTimeoutPageLoadSeconds(), TimeUnit.SECONDS);
		// driver.manage().timeouts().setScriptTimeout(config.getSeleniumTimeoutScriptSeconds(), TimeUnit.SECONDS);
		// driver.manage().timeouts().implicitlyWait(config.getSeleniumTimeoutImplicitlyWaitSeconds(), TimeUnit.SECONDS);
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(config.getSeleniumTimeoutImplicitlyWaitSeconds()));
		driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(config.getSeleniumTimeoutScriptSeconds()));
		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getSeleniumTimeoutPageLoadSeconds()));

		return driver;
	}
	
	public void positionWindow(int posx, int posy, int width, int height, WebDriver webDriver) {
		try {
			webDriver.manage().window().setPosition(new Point(posx, posy));
		} catch (Exception e) {
			log.warn("Failed to set initial position (ignored)");
			log.debug("", e);
		}
		try {
			webDriver.manage().window().setSize(new Dimension(width, height));
		} catch (Exception e) {
			log.warn("Failed to set initial size (ignored)");
			log.debug("", e);
		}
	}
	
	/**
	 * Click on the given element using javascript. This is useful when any other form of clicking fails with the error 
	 * "Element is not clickable". Can't be used on HTML elements that don't have the "click()" method
	 * @param webElement
	 * @param webDriver
	 */
	public void clickByJavascript(WebElement webElement, WebDriver webDriver) {
		clickByJavascript(webElement, webDriver, 20, 80, 20, 80);
	}
	
	/**
	 * Click on the given element using javascript. This is useful when any other form of clicking fails with the error 
	 * "Element is not clickable". Can't be used on HTML elements that don't have the "click()" method
	 * @param webElement
	 * @param webDriver
	 * @param minPercentX e.g. 10
	 * @param maxPercentX e.g. 90
	 * @param minPercentY 
	 * @param maxPercentY
	 */
	public void clickByJavascript(WebElement webElement, WebDriver webDriver, int minPercentX, int maxPercentX, int minPercentY, int maxPercentY) {
		// Move the mouse over the element, just in case
		// // When using the W3C Action commands, offsets are from the center of element
		Dimension dimension = webElement.getSize();
		int offx = 0;
		int offy = 0;
		try {
			offx = (int) ThreadLocalRandom.current().nextDouble(dimension.width*minPercentX/100d, dimension.width*maxPercentX/100d)/2;
		} catch (Exception e) {
			// Ignored
		}
		try {
			offy = (int) ThreadLocalRandom.current().nextDouble(dimension.height*minPercentY/100d, dimension.height*maxPercentY/100d)/2;
		} catch (Exception e) {
			// Ignored
		}
		Actions actions = new Actions(webDriver); 
		actions.moveToElement(webElement, offx, offy);
		//
		JavascriptExecutor javascriptExecutor = (JavascriptExecutor) webDriver;
		javascriptExecutor.executeScript("arguments[0].dispatchEvent(new Event('click'));", webElement);
	}

	/**
	 * Click on the given element in a range between 20% and 80% of the dimensions
	 * @param webElement
	 * @param webDriver
	 */
	public void randomClick(WebElement webElement, WebDriver webDriver) {
		randomClick(webElement, webDriver, 20, 80, 20, 80);
	}
	
	/**
	 * Convert a range expressed as percentage from origin to a position from the center
	 * @param size
	 * @param minPercent
	 * @param maxPercent
	 * @return
	 */
	private int percentageToRandomOffsetFromCenter(int size, int minPercent, int maxPercent) {
		double min = size*minPercent/100d;
		double max = size*maxPercent/100d;
		double half = size/2d;
		double displacement = min-half;
		double from = 0d;
		double to = max - min;
		double random = ThreadLocalRandom.current().nextDouble(from, to);
		double positionFromCenter = random + displacement;
		return (int) positionFromCenter;
	}
	
	/**
	 * Click on the given element in a range between min and max % of the dimensions.
	 * @param webElement
	 * @param webDriver
	 * @param minPercentX e.g. 10
	 * @param maxPercentX e.g. 90
	 * @param minPercentY 
	 * @param maxPercentY
	 */
	public void randomClick(WebElement webElement, WebDriver webDriver, int minPercentX, int maxPercentX, int minPercentY, int maxPercentY) {
		Dimension dimension = webElement.getSize();
		// Clicco in un range compreso tra min% e max% della larghezza e altezza
		// When using the W3C Action commands, offsets are from the center of element.
		// Una volta l'offset era dall'angolo in basso a sinistra, ma adesso è dal centro quindi devo calcolare il nuovo offset partendo dalle percentuali
		// relative alla dimensione totale dell'elemento.
		
		try {
			if (dimension.width==0 || dimension.height==0) {
				log.debug("Using webElement.click() because element has no dimension");
				webElement.click();
			} else {
				int offx = percentageToRandomOffsetFromCenter(dimension.width, minPercentX, maxPercentX);
				int offy = percentageToRandomOffsetFromCenter(dimension.height, minPercentY, maxPercentY);
				if (log.isDebugEnabled()) {
					log.debug("Clicking on element with size {} at position ({},{}) from center", dimension, offx, offy);
				}
				// Clicco sull'elemento
				// When using the W3C Action commands, offsets are from the center of element
				Actions actions = new Actions(webDriver); 
				actions.moveToElement(webElement, offx, offy).click().build().perform();
			}
		} catch (org.openqa.selenium.TimeoutException e) {
			// The click worked but the page load failed
			throw e;
		} catch (WebDriverException e) {
			// Try an alternative clicking method
//			log.error("Click failed", e);
			log.warn("randomClick didn't work - trying via javascript");
//			log.debug("randomClick didn't work for element of type '{}' - trying with javascript (ricorda di inserire un wait dopo)", webElement.getTagName());
			// ((JavascriptExecutor)webDriver).executeScript("arguments[0].click();", webElement);
			clickByJavascript(webElement, webDriver);
			// In questo caso bisogna fare un wait di qualche tipo perché quello implicito non credo venga usato visto che è il js che naviga pagina
			// Attendo che il webElement scompaia, usando un trucco
			long timeoutMillis = 20*1000;
			boolean present = true;
			long start = System.currentTimeMillis();
			while (present && System.currentTimeMillis()-start<timeoutMillis) {
				try {
					webElement.getLocation(); // Questo lancia un'eccezione se l'elemento è sparito
					Thread.sleep(1000);
				} catch (Exception e1) {
					present = false;
				}
			}
		}
	}
	
	/**
	 * Waits until the attribute contains some non-empty text
	 * @param element the element to check
	 * @param attribute the attribute of the element that must not be empty
	 * @param webDriver
	 * @param timeOutInSeconds
	 */
	public void waitUntilAttributeNotEmpty(WebElement element, String attribute, WebDriver webDriver, long timeOutInSeconds) {
		WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(timeOutInSeconds), Duration.ofMillis(calcSleepTimeMillis(timeOutInSeconds)));
		// WebDriverWait webDriverWait = new WebDriverWait(webDriver, timeOutInSeconds, calcSleepTimeMillis(timeOutInSeconds));
		webDriverWait.until(ExpectedConditions.attributeToBeNotEmpty(element, attribute));
	}
	
	/**
	 * Waits until the selected element contains some non-empty text (warning: this method may not work as expected)
	 * @param element
	 * @param webDriver
	 * @param timeOutInSeconds
	 * @see #waitUntilAttributeNotEmpty
	 */
	public void waitWhileEmptyText(WebElement element, WebDriver webDriver, long timeOutInSeconds) {
		WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(timeOutInSeconds), Duration.ofMillis(calcSleepTimeMillis(timeOutInSeconds)));
		// WebDriverWait webDriverWait = new WebDriverWait(webDriver, timeOutInSeconds, calcSleepTimeMillis(timeOutInSeconds));
		webDriverWait.until(ExpectedConditions.not(ExpectedConditions.textToBePresentInElement(element, "")));
	}
	
	/**
	 * Waits until the selected element contains some non-empty text
	 * @param cssSelector
	 * @param webDriver
	 * @param timeOutInSeconds
	 */
	public void waitWhileEmptyText(String cssSelector, WebDriver webDriver, long timeOutInSeconds) {
		WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(timeOutInSeconds), Duration.ofMillis(calcSleepTimeMillis(timeOutInSeconds)));
		// WebDriverWait webDriverWait = new WebDriverWait(webDriver, timeOutInSeconds, calcSleepTimeMillis(timeOutInSeconds));
		webDriverWait.until(ExpectedConditions.textMatches(By.cssSelector(cssSelector), matchNonEmptyText));
	}
	
	/**
	 * Wait until the selector matches an element
	 * @param cssSelector
	 * @param webDriver
	 * @param timeOutInSeconds
	 */
	public void waitUntilPresent(String cssSelector, WebDriver webDriver, long timeOutInSeconds) {
		WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(timeOutInSeconds), Duration.ofMillis(calcSleepTimeMillis(timeOutInSeconds)));
		// WebDriverWait webDriverWait = new WebDriverWait(webDriver, timeOutInSeconds, calcSleepTimeMillis(timeOutInSeconds));
		webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssSelector)));
	}
	
	/**
	 * Wait until the selector matches a visible element
	 * @param cssSelector
	 * @param webDriver
	 * @param timeOutInSeconds
	 */
	public void waitUntilVisible(String cssSelector, WebDriver webDriver, long timeOutInSeconds) {
		WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(timeOutInSeconds), Duration.ofMillis(calcSleepTimeMillis(timeOutInSeconds)));
		// WebDriverWait webDriverWait = new WebDriverWait(webDriver, timeOutInSeconds, calcSleepTimeMillis(timeOutInSeconds));
		webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(cssSelector)));
	}
	
	/**
	 * Wait until the selector matches zero elements
	 * @param cssSelector
	 * @param webDriver
	 * @param timeOutInSeconds
	 */
	public void waitWhilePresent(String cssSelector, WebDriver webDriver, long timeOutInSeconds) {
		WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(timeOutInSeconds), Duration.ofMillis(calcSleepTimeMillis(timeOutInSeconds)));
		// WebDriverWait webDriverWait = new WebDriverWait(webDriver, timeOutInSeconds, calcSleepTimeMillis(timeOutInSeconds));
		webDriverWait.until(ExpectedConditions.numberOfElementsToBe(By.cssSelector(cssSelector), 0));
	}
	
	public void waitWhileVisible(String cssSelector, WebDriver webDriver, long timeOutInSeconds) {
		WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(timeOutInSeconds), Duration.ofMillis(calcSleepTimeMillis(timeOutInSeconds)));
		// WebDriverWait webDriverWait = new WebDriverWait(webDriver, timeOutInSeconds, calcSleepTimeMillis(timeOutInSeconds));
		webDriverWait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(cssSelector)));
	}
	
	public void waitWhileVisible(WebElement webElement, WebDriver webDriver, long timeOutInSeconds) {
		WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(timeOutInSeconds), Duration.ofMillis(calcSleepTimeMillis(timeOutInSeconds)));
		// WebDriverWait webDriverWait = new WebDriverWait(webDriver, timeOutInSeconds, calcSleepTimeMillis(timeOutInSeconds));
		webDriverWait.until(ExpectedConditions.invisibilityOf(webElement));
	}
	
	/**
	 * Waits until the element is no more attached to the DOM (stale).
	 * It happens when a new page is loaded, for example.
	 * @param webElement
	 * @param webDriver
	 * @param timeOutInSeconds
	 */
	public void waitUntilLost(WebElement webElement, WebDriver webDriver, long timeOutInSeconds) {
		WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(timeOutInSeconds), Duration.ofMillis(calcSleepTimeMillis(timeOutInSeconds)));
		// WebDriverWait webDriverWait = new WebDriverWait(webDriver, timeOutInSeconds, calcSleepTimeMillis(timeOutInSeconds));
		webDriverWait.until(ExpectedConditions.stalenessOf(webElement));
	}
	
	public void waitWhileVisible(List<WebElement> webElements, WebDriver webDriver, long timeOutInSeconds) {
		WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(timeOutInSeconds), Duration.ofMillis(calcSleepTimeMillis(timeOutInSeconds)));
		// WebDriverWait webDriverWait = new WebDriverWait(webDriver, timeOutInSeconds, calcSleepTimeMillis(timeOutInSeconds));
		webDriverWait.until(ExpectedConditions.invisibilityOfAllElements(webElements));
	}
	
	/**
	 * Sleep time between checks. Must be much less than timeOutInSeconds
	 * @param timeOutInSeconds
	 * @return
	 */
	private long calcSleepTimeMillis(long timeOutInSeconds) {
		long result = timeOutInSeconds*1000/10; // Ten times per timeout
		if (result>2000) {
			result = 2000; // Max 2 seconds
		} else if (result<500) {
			result = 500; // Min half second
		}
		return result;
	}
	
//	/**
//	 * Attende finché non appare un elemento con la classe indicata, oppure va in timeout
//	 * @param className
//	 * @param webDriver
//	 * @return
//	 */
//	@Deprecated // TODO rifare, vedi appunti in OneNote
//	public boolean waitForClass(String className, WebDriver webDriver) {
//		Wait<WebDriver> wait = new FluentWait<WebDriver>(webDriver)
//			.withTimeout(config.getTimeoutPageLoadSeconds(), TimeUnit.SECONDS)
//			.pollingEvery(2, TimeUnit.SECONDS);
//		return wait.until(new ExpectedCondition<Boolean>() {
//	        public Boolean apply(WebDriver d) {
//	            return !d.findElements(By.className(className)).isEmpty();
//	        }
//	    });
//	}

	/**
	 * Given a relative address, computes the new full address.
	 * If the relative address starts with / then it is considered relative to the server, not to the servlet context
	 * Do not use it to convert href attributes because this is done by WebDriver automatically.
	 * @param relativeAddress like ccc.go or /xxx/yyy.go
	 * @return
	 */
	public String relativeToAbsolute(String relativeAddress, WebDriver webDriver) {
		return yadaHttpUtil.relativeToAbsolute(webDriver.getCurrentUrl(), relativeAddress);
	}

	/**
	 * Take a browser screenshot and move it to the specified path
	 * @param webDriver
	 * @param toPath destination for the screenshot
	 */
	public void takeScreenshot(WebDriver webDriver, Path toPath) {
		try {
			File screenshotTmpFile = ((TakesScreenshot)webDriver).getScreenshotAs(OutputType.FILE);
			Files.move(screenshotTmpFile.toPath(), toPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (WebDriverException e) {
			log.error("Can't take screenshot (ignored)", e);
		} catch (IOException e) {
			log.error("Can't move screenshot file (ignored)", e);
		}
	}
	
}
