package net.yadaframework.components;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.stereotype.Component;

import net.yadaframework.core.CloneableDeep;
import net.yadaframework.core.CloneableFiltered;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.InternalException;
import sogei.utility.UCheckDigit;
import sogei.utility.UCheckNum;

@Component
public class YadaUtil {
	private final static Logger log = LoggerFactory.getLogger(YadaUtil.class);
	
	@Autowired private YadaConfiguration config;
    static ApplicationContext applicationContext; 	// To access the ApplicationContext from anywhere
    static public MessageSource messageSource; 		// To access the MessageSource from anywhere
	
    public final static long MILLIS_IN_MINUTE = 60*1000; 
	public final static long MILLIS_IN_HOUR = 60*MILLIS_IN_MINUTE; 
	public final static long MILLIS_IN_DAY = 24*MILLIS_IN_HOUR; 
	
	private SecureRandom secureRandom = new SecureRandom();
	
	private static Locale defaultLocale = null;
	
	@PostConstruct
    public void init() {
		defaultLocale = config.getDefaultLocale();
    }
	 
	/**
	 * Returns the localized value from a map of Locale -> String.
	 * Used in entities with localized string attributes.
	 * @param LocalizedValueMap
	 * @param locale the needed locale for the value, can be null for the current locale
	 * @return the localized value, or the empty string if no value has been defined and no default locale has been set
	 */
	public static String getLocalValue(Map<Locale, String> LocalizedValueMap, Locale locale) {
		if (locale==null) {
			locale = LocaleContextHolder.getLocale();
		}
		String result = LocalizedValueMap.get(locale);
		if (result==null && defaultLocale!=null && !defaultLocale.equals(locale)) {
			result = LocalizedValueMap.get(defaultLocale);
		}
		return result==null?"":result;
	}
	
	/**
	 * Close a closeable ignoring exceptions and null.
	 * @param closeable the object to close(), can be null
	 * @return true if closed cleanly (or null), false in case of exception
	 */
	public boolean closeSilently(Closeable closeable) {
		try {
			if (closeable!=null) {
				closeable.close();
			}
			return true;
		} catch (Exception e) {
			log.debug("Closeable exception (ignored)", e.getMessage());
		}
		return false;
	}
	
	public void sleepRandom(long minMilliseconds, long maxMilliseconds) {
		sleep(minMilliseconds + (long)(Math.random()*(maxMilliseconds-minMilliseconds)));
	}
	
	public void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			log.debug("Sleep interrupted (ignored)", e);
		}
	}

	/**
	 * Create a MD5 hash of a string (from http://snippets.dzone.com/posts/show/3686)
	 * @param clear the source text
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public String md5Hash(String clear) throws NoSuchAlgorithmException {
		MessageDigest m = MessageDigest.getInstance("MD5");
		byte[] data = clear.getBytes(); 
		m.update(data,0,data.length);
		BigInteger i = new BigInteger(1,m.digest());
		return String.format("%1$032X", i);
	}

	/**
	 * Copies an inputStream to an outputStream. 
	 * This method blocks until input data is available, end of file is detected, or an exception is thrown. 
	 * Streams are not closed.
	 * @param inputStream
	 * @param outputStream
	 * @param bufferSize the size in bytes of the temporary buffer to use on the copy loop; null for the default of 4096 bytes.
	 * Use a small buffer (256) when data is over the internet to prevent timeouts somewhere. Use a big buffer for in-memory or disk operations.
	 * @param sizeLimit the maximum number of bytes to read (inclusive)
	 * @return the number of bytes read, or -1 if the sizeLimit has been exceeded.
	 * @throws IOException 
	 */
	public long copyStream(InputStream inputStream, OutputStream outputStream, Integer bufferSize, Long sizeLimit) throws IOException {
		long totBytes = 0;
		if (inputStream!=null) {
			int size = bufferSize==null?4096:bufferSize;
			byte[] buffer = new byte[size];
			int len;
			while ((len = inputStream.read(buffer)) != -1) {
				totBytes+=len;
				if (sizeLimit!=null && totBytes>sizeLimit) {
					return -1;
				}
				outputStream.write(buffer, 0, len);
			}
		}
		return totBytes;
	}

	/**
	 * Reflection to get the type of a given field, even nested
	 * @param rootClass
	 * @param attributePath field name like "surname" or even a path like "friend.name"
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	public Class getType(Class rootClass, String attributePath) throws NoSuchFieldException, SecurityException {
		if (StringUtils.isBlank(attributePath)) {
			return rootClass;
		}
		String attributeName = StringUtils.substringBefore(attributePath, ".");
		Field field = rootClass.getDeclaredField(attributeName);
		Class attributeType = field.getType();
		// If it's a list, look for the list type
		if (attributeType == java.util.List.class) {
			// TODO check if the attributeType is an instance of java.util.Collection
			ParameterizedType parameterizedType = (ParameterizedType)field.getGenericType();
			if (parameterizedType!=null) {
				Type[] types = parameterizedType.getActualTypeArguments();
				if (types.length==1) {
					attributeType = (Class<?>) types[0];
				}
			}
		}
		return getType(attributeType, StringUtils.substringAfter(attributePath, "."));
	}
	
	/**
	 * Ritorna un messaggio localizzato da un contesto fuori da Spring (ad esempio in un enum o un Entity)
	 * @param key
	 * @param params
	 * @return
	 */
	public static String getMessage(String key, Object ... params) {
		return messageSource.getMessage(key, params, LocaleContextHolder.getLocale());
	}

	/**
	 * Create an instance of a class that belongs to the same package of some given class
	 * @param anyClassInPackage a class that is in the same package of the one to instantiate (it could be the one to instantiate)
	 * @param simpleClassName the simple name of the class to instantiate, like "UserProfile"
	 * @return
	 */
	public Object getNewInstanceSamePackage(Class anyClassInPackage, String simpleClassName) {
		String packageString = anyClassInPackage.getPackage().getName();
		String fullClassName = packageString + "." + simpleClassName;
		Class objectClass = null;
		try {
			objectClass = Class.forName(fullClassName);
			return objectClass.newInstance();
		} catch (ClassNotFoundException e) {
			log.error("Class {} not found in package {}", simpleClassName, packageString, e);
			throw new InternalException("Class not implemented: " + simpleClassName);
		} catch (Exception e) {
			log.error("Instantiation error for class {}", objectClass, e);
			throw new InternalException("Error while creating instance of " + fullClassName);
		}
	}

	/**
	 * Return all the classes of a given package.
	 * @param thePackage
	 * @return
	 * @see http://stackoverflow.com/a/21430849/587641
	 */
	public static List<Class> getClassesInPackage(Package thePackage) {
		List<Class> result = new ArrayList<>();
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
		final Set<BeanDefinition> classes = provider.findCandidateComponents(thePackage.getName());
		for (BeanDefinition bean: classes) {
			try {
				result.add(Class.forName(bean.getBeanClassName()));
			} catch (ClassNotFoundException e) {
				log.debug("Class not found for bean {} (ignored)", bean);
			}
		}
		return result;
	}
	
	/**
	 * Get any bean defined in the Spring ApplicationContext
	 * @param beanClass
	 * @return
	 */
	public static Object getBean(Class beanClass, Object ... args) {
		String beanName = StringUtils.uncapitalize(beanClass.getSimpleName());
		return getBean(beanName, args);
	}
	
	/**
	 * Get any bean defined in the Spring ApplicationContext
	 * @param nameInApplicationContext the Class.getSimpleName() starting lowercase, e.g. "processController"
	 * @return
	 */
	public static Object getBean(String nameInApplicationContext, Object ... args) {
		if (applicationContext!=null) {
			return applicationContext.getBean(nameInApplicationContext, args);
		}
		log.debug("No applicationContext injected in getBean() yet - returning null");
		return null;
	}
	
	/**
	 * Genera una password casuale di 16 caratteri
	 * @return a string like "XFofvGEtBlZIa5sH"
	 */
	public String generateClearPassword() {
		return generateClearPassword(16);
	}

	/**
	 * Genera una password casuale
	 * @param length max password length
	 * @return a string like "XFofvGEtBlZIa5sH"
	 */
	public String generateClearPassword(int length) {
		// http://stackoverflow.com/a/8448493/587641
		return RandomStringUtils.random(length, 0, 0, true, true, null, secureRandom);
	}
	
	/**
	 * Ritorna la data nel passato per il numero di minuti indicati
	 * @param days numero di minuti fa
	 * @return
	 */
	public Date minutesAgo(int minuti) {
		return new Date(System.currentTimeMillis() - minuti*MILLIS_IN_MINUTE);
	}
	
	/**
	 * Ritorna la data nel passato per il numero di giorni indicati
	 * @param days numero di giorni fa
	 * @return
	 */
	public Date daysAgo(int days) {
		return new Date(System.currentTimeMillis() - days*MILLIS_IN_DAY);
	}
	
	/**
	 * Delete a file ignoring errors.
	 * @param file the file to delete, can be null
	 * @return true if the file has been deleted, false if the file didn't exist or was null or in case of exception
	 */
	public boolean deleteSilently(File file) {
		if (file!=null) {
			try {
				return file.delete();
			} catch (Exception e) {
				log.debug("File {} not deleted: " + e.getMessage(), file);;
			}
		}
		return false;
	}

	/**
	 * Deleted a folder only when empty
	 * @param folder
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteIfEmpty(Path folder) {
	    try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder)) {
	        if (!directoryStream.iterator().hasNext()) {
	        	Files.delete(folder);
	        	return true;
	        }
	    } catch (final Exception e) { // empty
	    }
	    return false;
	}

	/**
	 * Removes files from a folder starting with the prefix (can be an empty string)
	 * @param folder
	 * @param prefix the initial part of the filename or "" for any file
	 * return the number of deleted files
	 */
	public int cleanupFolder(Path folder, String prefix) {
		int result = 0;
	    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder, prefix + "*")) {
	        for (final Path item : directoryStream) {
	            Files.delete(item);
	            result++;
	        }
	    } catch (final Exception e) {
	    	log.info("Exception while cleaning folder {}", folder, e);
	    }
	    return result;
	}
	
	/**
	 * Removes files from a folder starting with the prefix (can be an empty string) and older than the given date
	 * @param folder
	 * @param prefix
	 * @param olderThan
	 * @return the number of deleted files
	 */
	public int cleanupFolder(Path folder, String prefix, Date olderThan) {
		int result = 0;
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder, prefix + "*")) {
			for (final Path item : directoryStream) {
				if (Files.getLastModifiedTime(item).toMillis()>olderThan.getTime()) {
					Files.delete(item);
					result++;
				}
			}
		} catch (final Exception e) {
			log.info("Exception while cleaning folder {}", folder, e);
		}
		return result;
	}
	
	/**
	 * Da un nome tipo abcd.JPG ritorna "jpg"
	 * @param filename
	 * @return l'estensione, oppure null
	 */
	public String getFileExtension(String filename) {
		String result = null;
		if (!StringUtils.isBlank(filename)) {
			int dotpos = filename.lastIndexOf('.');
			if (dotpos>-1 && filename.length()>dotpos+1) {
				result = filename.substring(dotpos+1).toLowerCase();
			}
		}
		return result;
	}
	
	/**
	 * Esegue un comando di shell
	 * @param command comando
	 * @param args lista di argomenti (ogni elemento puo' contenere spazi), puo' essere null
	 * @param outputStream ByteArrayOutputStream che conterrà l'output del comando (out + err)
	 * @return the error message (will be empty for a return code >0), or null if there was no error
	 */
	public String exec(String command, List<String> args, Map<String, ?> substitutionMap, ByteArrayOutputStream outputStream) {
		int exitValue=1;
		try {
			CommandLine commandLine = new CommandLine(command);
			if (args!=null) {
				for (String arg : args) {
					commandLine.addArgument(arg);
				}
			}
			if (log.isDebugEnabled()) {
				for (String key : substitutionMap.keySet()) {
					log.debug("{}={}", key, substitutionMap.get(key));
					if (key.startsWith("{") || key.startsWith("${")) { // Checking { just for extra precaution
						log.error("Invalid substitution {}: should NOT start with ${", key);
					}
				}
			}
			commandLine.setSubstitutionMap(substitutionMap);
			DefaultExecutor executor = new DefaultExecutor();
			ExecuteWatchdog watchdog = new ExecuteWatchdog(60000); // Kill after 60 seconds
			executor.setWatchdog(watchdog);
			// Output and Error go together
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, outputStream);
			executor.setStreamHandler(streamHandler);
			log.debug("Executing shell command: {}", StringUtils.join(commandLine, " "));
			exitValue = executor.execute(commandLine);
		} catch (Exception e) {
			log.error("Failed to execute shell command: " + command + " " + args, e);
			return e.getMessage();
		}
		return (exitValue>0)?"":null;
	}

	/**
	 * Esegue un comando di shell
	 * @param command comando
	 * @param args lista di argomenti (ogni elemento puo' contenere spazi), puo' essere null
	 * @param outputStream ByteArrayOutputStream che conterrà l'output del comando
	 * @return the error message (will be empty for a return code >0), or null if there was no error
	 */
	public String exec(String command, List<String> args, ByteArrayOutputStream outputStream) {
		int exitValue=1;
		try {
			CommandLine commandLine = new CommandLine(command);
			if (args!=null) {
				for (String arg : args) {
					commandLine.addArgument(arg);
				}
			}
			DefaultExecutor executor = new DefaultExecutor();
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
			executor.setStreamHandler(streamHandler);
			log.debug("Executing shell command: {}", commandLine);
			exitValue = executor.execute(commandLine);
		} catch (Exception e) {
			log.error("Failed to execute shell command: " + command + " " + args, e);
			return e.getMessage();
		}
		return exitValue>0?"":null;
	}
	
	/**
	 * Esegue il comando configurato
	 * @param shellCommandKey chiave completa xpath del comando shell da eseguire e.g. "config/shell/processTunableWhiteImage"
	 * @param params mappa nome-valore delle variabili da sostituire nel comando, per esempio "${NAME}"="pippo"
	 */
	public boolean exec(String shellCommandKey, Map<String, String> params) {
		String executable = config.getString(shellCommandKey + "/executable");
		// Need to use getProperty() to avoid interpolation on ${} arguments
		// List<String> args = config.getConfiguration().getList(String.class, shellCommandKey + "/arg", null);
		List<String> args = (List<String>) config.getConfiguration().getProperty(shellCommandKey + "/arg");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			String error = exec(executable, args, params, outputStream);
			String commandOutput = outputStream.toString();
			if (error!=null) {
				log.error("Can't execute shell command \"{}\": {} - {}", shellCommandKey, error, commandOutput);
				return false;
			}
			log.debug(commandOutput);
			return true;
		} finally {
			closeSilently(outputStream);
		}
	}	
	
//	/**
//	 * Esegue il comando configurato
//	 * @param shellCommandKey chiave completa xpath del comando shell da eseguire e.g. "config/shell/processTunableWhiteImage"
//	 * @param params mappa nome-valore delle variabili da sostituire nel comando, per esempio "{NAME}"="pippo"
//	 */
//	public boolean exec(String shellCommandKey, Map<String, String> params) {
//		String executable = config.getString(shellCommandKey + "/executable");
//		List<String> args = (List) config.getList(shellCommandKey + "/arg", null);
//		List<String> newArgs = new ArrayList<String>();
//		// Replace degli argomenti, che possono anche non esserci
//		Pattern paramPattern = Pattern.compile("\\{\\w+\\}"); // parentesi graffe con dentro [a-zA-Z_0-9]+
//		for (String arg : args) {
//			Matcher m = paramPattern.matcher(arg);
//			while (m.find()) {
//				String paramName = m.group();
//				String paramValue = params.get(paramName);
//				if (paramValue!=null) {
//					arg = arg.replace(paramName, paramValue);
//				}
//			}
//			newArgs.add(arg);
//		}
//		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//		try {
//			String error = exec(executable, newArgs, outputStream);
//			String commandOutput = outputStream.toString();
//			if (error!=null) {
//				log.error("Can't execute shell command \"{}\": {} - {}", shellCommandKey, error, commandOutput);
//				return false;
//			}
//			log.debug(commandOutput);
//			return true;
//		} finally {
//			try {
//				outputStream.close();
//			} catch (Exception e) {
//				// ignored
//				log.debug("Can't close outputStream (ignored): " + e);
//			}		
//		}
//	}	
	
	public boolean isCodiceFiscaleValid(String codiceFiscale) {
		try {
			if (StringUtils.isBlank(codiceFiscale)) {
				return false;
			}
			codiceFiscale = codiceFiscale.trim().toUpperCase();
			if (codiceFiscale.length()==16) {
				UCheckDigit uCheckDigit = new UCheckDigit(codiceFiscale); // ATTENZIONE deve essere UPPERCASE
				return uCheckDigit.controllaCorrettezza();
			}
			if (codiceFiscale.length()==11) {
				UCheckNum uCheckCfNum = new UCheckNum(codiceFiscale);
				boolean maybeValid = uCheckCfNum.controllaCfNum();
				if (maybeValid) {
					String c = uCheckCfNum.trattCfNum();
					return !(c.equals("2") || c.equals("5")); 
				}
			}
		} catch (Exception e) {
			log.error("Errore di validazione del codice fiscale {}", codiceFiscale, e);
		}
		return false;
	}

	/**
	 * Trova l'eccezione che ha causato questa, nella catena delle eccezioni
	 * @param exception eccezione conseguente
	 * @return throwable originario, preso con getCause()
	 */
	public Throwable getRootException(Throwable exception) {
		Throwable root = exception;
		for (int i=0; i<50; i++) { // Al massimo 50, per non andare in loop
			Throwable cause = root.getCause();
			if (cause==null) {
				break;
			}
			root = cause;
		}
		return root;
	}
	
	/*
	 * Spezza una stringa in due, circa al carattere splitPoint, ma a fine parola.
	 */
	public String[] splitAtWord(String value, int splitPoint) {
		try {
			int maxLen = value.length();
			while (value.charAt(splitPoint)!=' ' && splitPoint<maxLen) {
				splitPoint++; // Cerco lo spazio
			}
			return new String[] { value.substring(0, splitPoint), value.substring(splitPoint) };
		} catch (Exception e) {
			log.debug("ERROR while splitting at {} the string \"{}\" (ignored)", splitPoint, value);
			return new String[] { value, "" };
		}
	}
	
	public String abbreviate(String string, int length, boolean breakAtWord) {
		return abbreviate(string, length, breakAtWord, null);
	}
	
	/**
	 * Accorcia una stringa mettendo "...", eventualmente senza troncare le parole
	 * @param string
	 * @param length
	 * @param breakAtWord true per non troncare le parole
	 * @return
	 */
	// Adattato da http://stackoverflow.com/questions/7738157/trim-string-in-java-while-preserve-full-word
	public String abbreviate(String string, int length, boolean breakAtWord, String ellipsis) {
		if (ellipsis==null) {
			ellipsis = " [...]";
		}
		int elen = ellipsis.length();
	    try {
			if(StringUtils.trimToNull(string) == null){
			    return string;
			}
			if(string.length() > length){
				StringBuffer sb = new StringBuffer(string);
				int newLen = length - elen;
				if (breakAtWord) {
					int endIndex = sb.indexOf(" ", newLen);
					if (endIndex>-1) {
						return sb.insert(endIndex, ellipsis).substring(0, endIndex+elen);
					}
					// Se lo spazio non c'è evidentemente siamo alla fine della stringa, per cui va presa tutta
					return sb.toString();
				}
			    return sb.insert(newLen, ellipsis).substring(0, length);
			}
		} catch (Exception e) {
			log.error("Can't abbreviate '{}' (ignored)", string, e);
		}
	    return string;
	}	
	
	/**
	 * Questo metodo crea la copia di un oggetto TRAMITE I SUOI GETTER (anche privati), facendo in modo che alcune collection/mappe vengano copiate pur restando indipendenti.
	 * In pratica le collection/mappe sono ricreate come istanze nuove con i medesimi oggetti di quelle originali.
	 * Questo permette di condividere gli oggetti tra le copie, ma di mantenere le associazioni slegate.
	 * Così se copio un Prodotto (in Artemide), mi trovo gli stessi componenti dell'originale (gli Articolo sono gli stessi) ma posso in seguito toglierli/aggiungerli
	 * senza influire sull'altra istanza da cui son partito a copiare.
	 * 
	 * E' possibile specificare quali attributi non copiare grazie all'interfaccia CloneableFiltered. La id non è mai copiata.
	 * Gli attributi senza getter/setter non sono copiati a prescindere dal filtro.
	 * Per esempio se l'oggetto che si vuole copiare ha l'attributo pippo e l'attributo pluto, si può fare in modo che sia copiato solo pippo e non pluto (che rimane quindi null).
	 * 
	 * Se una collection/mappa deve essere clonata anche nel contenuto, i suoi elementi devono implementare CloneableDeep (vedi LocalString).
	 * Per esempio se l'attributo pippo è una collection di oggetti Cane che non implementa ClonableDeep, nella copia verrà creata una nuova collection
	 * di oggetti Cane che saranno gli stessi della collection di partenza. Se invcece Cane implementa ClonableDeep, allora gli oggetti Cane contenuti
	 * nella copia di pippo sono essi stessi delle copie che seguono le stesse regole qui indicate.
	 * 
	 * ATTENZIONE: 
	 * - da verificare se gli attributi dei parent sono duplicati pure loro
	 *  
	 * @param source
	 * @return
	 */
	// TODO why not use SerializationUtils.clone(..) of commons-lang?
	public static Object copyEntity(CloneableFiltered source) {
		return copyEntity(source, null);
	}
	
	/**
	 * Questo metodo crea la copia di un oggetto TRAMITE I SUOI GETTER (anche privati), facendo in modo che alcune collection/mappe vengano copiate pur restando indipendenti.
	 * In pratica le collection/mappe sono ricreate come istanze nuove con i medesimi oggetti di quelle originali.
	 * Questo permette di condividere gli oggetti tra le copie, ma di mantenere le associazioni slegate.
	 * Così se copio un Prodotto (in Artemide), mi trovo gli stessi componenti dell'originale (gli Articolo sono gli stessi) ma posso in seguito toglierli/aggiungerli
	 * senza influire sull'altra istanza da cui son partito a copiare.
	 * 
	 * E' possibile specificare quali attributi non copiare grazie all'interfaccia CloneableFiltered. La id non è mai copiata.
	 * Per esempio se l'oggetto che si vuole copiare ha l'attributo pippo e l'attributo pluto, si può fare in modo che sia copiato solo pippo e non pluto (che rimane quindi null).
	 * 
	 * Se una collection/mappa deve essere clonata anche nel contenuto, i suoi elementi devono implementare CloneableDeep (vedi LocalString).
	 * Per esempio se l'attributo pippo è una collection di oggetti Cane che non implementa ClonableDeep, nella copia verrà creata una nuova collection
	 * di oggetti Cane che saranno gli stessi della collection di partenza. Se invcece Cane implementa ClonableDeep, allora gli oggetti Cane contenuti
	 * nella copia di pippo sono essi stessi delle copie che seguono le stesse regole qui indicate.
	 * 
	 * ATTENZIONE: 
	 * - da verificare se gli attributi dei parent sono duplicati pure loro
	 *  
	 * @param source
	 * @param classObject classe da usare per creare il clone quando il source è nascosto dentro a un HibernateProxy
	 * @return
	 */
	// TODO why not use SerializationUtils.clone(..) of commons-lang?
	public static Object copyEntity(CloneableFiltered source, Class classObject) {
		if (source==null) {
			return null;
		}
		Class<?> sourceClass = source.getClass();
			
		try {
			// The constructor may be private, so don't just use newInstance()
	        // Object target = sourceClass.newInstance();
			Constructor constructor = sourceClass.getDeclaredConstructor(new Class[0]);
	        constructor.setAccessible(true);
			Object target = constructor.newInstance(new Object[0]);
			if(target instanceof org.hibernate.proxy.HibernateProxy && classObject!=null)
				target = classObject.newInstance();	
				
			copyFields(source, sourceClass, target);
			Class<?> superclass = sourceClass.getSuperclass();
			while (superclass!=null && superclass!=Object.class) {
				sourceClass = superclass;
				copyFields(source, sourceClass, target);
				superclass = sourceClass.getSuperclass();
			}
			return target;
		} catch (Exception e) {
			String msg = "Can't duplicate object '" + source + "'";
			log.error(msg + ": " + e);
			throw new InternalException(msg, e);
		}
	}

	private static void copyFields(CloneableFiltered source, Class<?> sourceClass, Object target) {
		log.debug("Copio oggetto {} di tipo {}", source, sourceClass);
		Field[] fields = sourceClass.getDeclaredFields();
		Field[] excludedFields = source.getExcludedFields();
		List<Field>filteredFields = excludedFields!=null? (List<Field>) Arrays.asList(excludedFields):new ArrayList<Field>();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			// "id" viene filtrato d'ufficio, per detacchare l'oggetto
			if ("id".equals(field.getName()) || filteredFields.contains(field)) {
				continue; // Skip the filtered fields
			}
			try {
				// Cerco i getter/setter pubblici per il campo
				Class<?> fieldType = field.getType();
				String prefix = (fieldType==boolean.class || fieldType==Boolean.class)?"is":"get";
				String getterName = prefix + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
				String setterName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
				try {
					Method getter=null;
					try{
						getter= sourceClass.getDeclaredMethod(getterName);
					}catch(NoSuchMethodException exc){
						//per i boolean posso avere il getXXXX anzichè l' isXXXXXXX
						if  (fieldType==boolean.class || fieldType==Boolean.class){
							getterName = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
							getter = sourceClass.getDeclaredMethod(getterName);
						} else {
							throw exc;
						}
					}
					getter.setAccessible(true);
					Method setter = sourceClass.getDeclaredMethod(setterName, fieldType);
					setter.setAccessible(true);		
					if (fieldType.isPrimitive() 
							 || fieldType==Boolean.class 
							 || fieldType==Integer.class
							 || fieldType==Long.class
							 || fieldType==Byte.class
							 || fieldType==Character.class
							 || fieldType==Short.class
							 || fieldType==Float.class
							 || fieldType==Double.class
							) {
						// Mi immagino che isPrimitive() sia veloce, per cui lo controllo prima dei giochi sulle interfacce
						// Just copy
						setter.invoke(target, getter.invoke(source));
					} else {
						if (isType(fieldType, Collection.class)) {
							// E' una collection, quindi copio solo i contenuti.
							// E' importante che il costruttore del target abbia istanziato il field con una collection vuota.
							Collection sourceCollection = (Collection) getter.invoke(source); // La collection di partenza, serve per i contenuti
							Collection targetCollection = (Collection) getter.invoke(target); // La collection di destinazione da riempire
							if (targetCollection==null) {
								// Se il costruttore non istanzia la mappa, ne creo una arbitrariamente di tipo ArrayList
								targetCollection = new ArrayList();
								setter.invoke(target, targetCollection);
							}
							// Faccio la copia shallow di tutti gli elementi che non implementano CloneableDeep;
							// per questi faccio la copia deep.
							for (Object value : sourceCollection) {
								if (isType(value.getClass(), CloneableDeep.class)) {
									targetCollection.add(YadaUtil.copyEntity((CloneableFiltered) value)); // deep
								} else {
									targetCollection.add(value); // shallow
								}
							}
//									targetCollection.addAll(sourceCollection);
						} else if (isType(fieldType, Map.class)) {
							Map sourceMap = (Map) getter.invoke(source);
							Map targetMap = (Map) getter.invoke(target);
							if (targetMap==null) {
								// Se il costruttore non istanzia la mappa, ne creo una arbitrariamente di tipo HashMap
								targetMap = new HashMap();
								setter.invoke(target, targetMap);
							}
							// Faccio la copia shallow di tutti gli elementi che non implementano CloneableFiltered;
							// per questi faccio la copia deep.
							for (Object key : sourceMap.keySet()) {
								Object value = sourceMap.get(key);
								if (isType(value.getClass(), CloneableDeep.class)) {
									targetMap.put(key, YadaUtil.copyEntity((CloneableFiltered) value)); // deep
								} else {
									targetMap.put(key, value); // shallow
								}
							}
//								targetMap.putAll(sourceMap);
						} else {
							// Non è una collection nè una mappa.
							if (isType(fieldType, CloneableDeep.class)) {
								// Siccome implementa CloneableDeep, lo duplico deep
								setter.invoke(target, YadaUtil.copyEntity((CloneableFiltered) getter.invoke(source))); // deep but detached
							} else {
								// E' un oggetto normale, per cui copio il riferimento
								setter.invoke(target, getter.invoke(source)); // shallow
							}
						}
					}
				} catch (NoSuchMethodException e) {
					// Just skip it
					// Non loggo perché uscirebbe il log anche in casi giusti
				}
			} catch (Exception e) {
				log.error("Can't copy field {} (ignored): {}", field, e);
			}
		}
	}
	
	/**
	 * Check if a class is of a given type, considering superclasses and interfaces (of superclasses)
	 * @param fieldType
	 * @param requiredType
	 * @return
	 */
	// Ritorna true se fieldType coincide con requiredType o una sua superclass oppure se requiredType � tra le interfacce di fieldType o delle sue superclassi
	public static boolean isType(Class<?> fieldType, Class requiredType) {
		boolean found = false;
		while (!found && fieldType!=null) {
			found = isTypeNoSuperclass(fieldType, requiredType);
			fieldType = fieldType.getSuperclass();
		}
		return found; 
	}
		
	private static boolean isTypeNoSuperclass(Class<?> fieldType, Class requiredType) {
		return fieldType!=null && (fieldType.equals(requiredType) || (Arrays.asList(fieldType.getInterfaces()).contains(requiredType)));
	}

//	/**
//	 * Copy all fields from the first argument to the second, without modifying the timezone.
//	 * The purpose is to have the same time in a different timezone.
//	 * @param fromCal
//	 * @param toCal
//	 */
//	public void copyFields(Calendar fromCal, Calendar toCal) {
//		
//	}
	
	/** Ritorna l'ora più vicina nel passato alla data specificata
	 * @return
	 */
	@Deprecated // Timezone is important because it could be not aligned to the hour of the default timezone
	public Date roundBackToHour(Date date) {
		return roundBackToHour(date, TimeZone.getDefault());
	}
	
	/**
	 * Returns the same calendar object aligned to the next hour
	 * @param calendar
	 * @return
	 */
	public Calendar roundForwardToHour(Calendar calendar) {
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.add(Calendar.HOUR, 1);
		return calendar;
	}
	
	/**
	 * Returns the same calendar object aligned to the previous hour
	 * @param calendar
	 * @return
	 */
	public Calendar roundBackToHour(Calendar calendar) {
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}
	
	/**
	 * Resets minutes to zero
	 * @param date
	 * @param timezone
	 * @return
	 */
	public Date roundBackToHour(Date date, TimeZone timezone) {
		Calendar calendar = new GregorianCalendar(timezone);
		calendar.setTime(date);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	/**
	 * Counts the days interval between two dates. Time component is ignored.
	 * @param date1
	 * @param date2
	 * @return
	 */
	public int daysDifference(Date date1, Date date2) {
		// FIXME non è né efficiente né precisa ma funziona
		// Presa da http://stackoverflow.com/questions/13198609/java-calendar-getting-difference-between-two-dates-times-off-by-one
		try {
			// Non credo sia necessario passare per un formatter: basta azzerare i componenti di ora, minuti etc. usando Calendar
			DateFormat formatter= new SimpleDateFormat("MM/dd/yyyy");
			String truncatedDateString1 = formatter.format(date1);
			Date truncatedDate1 = formatter.parse(truncatedDateString1);
			String truncatedDateString2 = formatter.format(date2);
			Date truncatedDate2 = formatter.parse(truncatedDateString2);
			long timeDifference = truncatedDate2.getTime()- truncatedDate1.getTime();
			return Math.abs((int)(timeDifference / (24*60*60*1000)));
		} catch (ParseException e) {
			// Should never be
			log.error("Failed to compute time difference", e);
		}
		return 0;
	}
	
	/**
	 * Returns the minutes between two dates
	 * @param recentDate
	 * @param oldDate
	 * @return
	 */
	public static long minutesDifference(Date recentDate, Date oldDate) {
		return (recentDate.getTime()-oldDate.getTime()) / MILLIS_IN_MINUTE;
	}

	/**
	 * Returns the number of milliseconds since midnight
	 */
	public static long millisSinceMidnight(Calendar calendar) {
		long totMillis = calendar.getTimeInMillis();
		return totMillis - YadaUtil.roundBackToMidnight(calendar).getTimeInMillis();
	}


	public static Date roundBackToMidnight(Date date, TimeZone timezone) {
		GregorianCalendar calendar = new GregorianCalendar(timezone);
		calendar.setTime(date);
		return roundBackToMidnight(calendar).getTime();
	}
	
	/**
	 * Create a new calendar rounded back to the start of the day.
	 * @param calendar the calendar to copy
	 * @return a new calendar
	 */
	public static Calendar roundBackToMidnightClone(Calendar source) {
		return roundBackToMidnight((Calendar) source.clone());
	}
	
	/**
	 * Rounds back the calendar to the start of the day.
	 * @param calendar the calendar to change
	 * @return the input calendar modified.
	 */
	// TODO to prevent errors, better to return void
	public static Calendar roundBackToMidnight(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}
	
	public static Calendar addDaysClone(Calendar source, int days) {
		return addDays((Calendar) source.clone(), days);
	}
	
	public static Calendar addDays(Calendar calendar, int days) {
		calendar.add(Calendar.DAY_OF_YEAR, days);
		return calendar;
	}
	
	public static Calendar addMinutes(Calendar calendar, int minutes) {
		calendar.add(Calendar.MINUTE, minutes);
		return calendar;
	}
	
	/**
	 * Aggiunge (o rimuove) i minuti indicati dalla data
	 * @param date
	 * @param hours numero di minuti, può essere negativo
	 * @return
	 */
	public static Date addMinutes(Date date, int minutes) { 
		return new Date(date.getTime()+minutes*MILLIS_IN_MINUTE);
	}
	
	/**
	 * Aggiunge (o rimuove) le ore indicate dalla data
	 * @param date
	 * @param hours numero di ore, può essere negativo
	 * @return
	 */
	public static Date addHours(Date date, int hours) { 
		return new Date(date.getTime()+hours*MILLIS_IN_HOUR);
	}
	
	/**
	 * Aggiunge (o rimuove) i giorni indicati dalla data
	 * @param date
	 * @param days numero di giorni, può essere negativo
	 * @return
	 */
	public static Date addDays(Date date, int days) { 
		return new Date(date.getTime()+days*MILLIS_IN_DAY);
	}

	/**
	 * Aggiunge (o rimuove) gli anni indicati dalla data (approssimato)
	 * @param date
	 * @param years numero di 365 giorni, può essere negativo
	 * @return
	 */
	public static Date addYears(Date date, int years) { 
		final long millisInYear = MILLIS_IN_DAY * 365;
		return new Date(date.getTime()+millisInYear);
	}
	
	/**
	 * Returns true if the two dates are on the same day
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean sameDay(Date a, Date b) {
		Calendar aCalendar = new GregorianCalendar();
		Calendar bCalendar = new GregorianCalendar();
		aCalendar.setTime(a);
		bCalendar.setTime(b);
		int aDay = aCalendar.get(Calendar.DAY_OF_YEAR);
		int aYear = aCalendar.get(Calendar.YEAR);
		int bDay = bCalendar.get(Calendar.DAY_OF_YEAR);
		int bYear = bCalendar.get(Calendar.YEAR);
		return aDay==bDay && aYear == bYear;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
	// Tutti i metodi sotto non sono ancora usati (spostarli a mano a mano che si usano)
	///////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
	
	public static Calendar roundForwardToAlmostMidnight(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar;
	}
	
	public static Calendar roundBackToLastMonthStart(Calendar calendar) {
		calendar.add(Calendar.MONTH, -1);
		return roundBackToMonth(calendar);
	}
	
	public static Calendar roundBackToMonth(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		return calendar;
	}

	public static Date roundBackToMonth(Date date, TimeZone timezone) {//timezone = TimeZone.getTimeZone("GMT");
		GregorianCalendar calendar = new GregorianCalendar(timezone);
		calendar.setTime(date);
		return roundBackToMonth(calendar).getTime();
	}
	
	public static Date roundFowardToMonth(Date date, TimeZone timezone) {//timezone = TimeZone.getTimeZone("GMT");
		GregorianCalendar calendar = new GregorianCalendar(timezone);
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.add(Calendar.MONTH, 1);
		
		return calendar.getTime();
	}

	public static String normalizzaCellulareItaliano(String cellulare) {
		if (cellulare==null || cellulare.trim().length()==0) {
			return cellulare;
		}
		cellulare = cellulare.replaceAll(" ", ""); // Tolgo spazi
		cellulare = cellulare.replaceAll("\\.", ""); // Tolgo punti
		cellulare = cellulare.replaceAll("/", ""); // Tolgo slash
		if (cellulare.startsWith("+") || cellulare.startsWith("00")) {
			return cellulare;
		}
		return "+39"+cellulare; // Metto prefisso
	}
	
	public static boolean validaCellulare(String cellulare) {
		try {
			if (cellulare.startsWith("+")) {
				cellulare = cellulare.substring(1); // tolgo il + iniziale
			}
			Long.parseLong(cellulare);
			return true;
		} catch (Exception e) {
			// Ignored
		}
		return false;
	}
	
	// Trasforma una mappa qualunque in un set ordinato in base ai valori
	public static SortedSet<Entry<String,String>> sortByValue(Map data) {
		// Uso il giro del TreeSet per sortare in base al value
		SortedSet<Entry<String,String>> result = new TreeSet<Entry<String,String>>(new Comparator<Entry<String, String>>() {
			public int compare(Entry<String, String> element1, Entry<String, String> element2) {
				return element1.getValue().compareTo(element2.getValue());
			}
		});
		try {
			result.addAll((Collection<Entry<String, String>>) data.entrySet());
		} catch (RuntimeException e) {
			String msg = "Can't sort map by value";
			log.error(msg, e);
			throw e;
		}
		return result;
	}
	
	// Trasforma una mappa qualunque in un set ordinato in base alle chiavi
	public static SortedSet<Entry<String,String>> sortByKey(Map data) {
		// Uso il giro del TreeSet per sortare in base al value
		SortedSet<Entry<String,String>> result = new TreeSet<Entry<String,String>>(new Comparator<Entry<String, String>>() {
			public int compare(Entry<String, String> element1, Entry<String, String> element2) {
				return element1.getKey().compareTo(element2.getKey());
			}
		});
		try {
			result.addAll((Collection<Entry<String, String>>) data.entrySet());
		} catch (RuntimeException e) {
			String msg = "Can't sort map by key";
			log.error(msg, e);
			throw e;
		}
		return result;
	}

	/**
	 * Ritorna un riferimento all'ApplicationContext per l'utilizzo fuori dal Container (ad esempio negli Entity)
	 * @return
	 */
	public static ApplicationContext getApplicationContext() {
		return YadaUtil.applicationContext;
	}

	@Autowired 
	public void setApplicationContext(ApplicationContext applicationContext) {
		YadaUtil.applicationContext = applicationContext;
	}

	/**
	 * Converte un filename in modo che sia valido sia per il filesystem (unix/dos) sia per il browser.
	 * E' molto distruttiva in quanto i caratteri non previsti vengono eliminati. Per questo si chiama "reduce" :-)
	 * Converte anche a lowercase.
	 * @param originalFilename
	 * @return un filename safe, dove i caratteri speciali sono scomparsi
	 */
	public static String reduceToSafeFilename(String originalFilename) {
		return reduceToSafeFilename(originalFilename, true);
	}
	
	/**
	 * Converte un filename in modo che sia valido sia per il filesystem (unix/dos) sia per il browser.
	 * E' molto distruttiva in quanto i caratteri non previsti vengono eliminati. Per questo si chiama "reduce" :-)
	 * Converte anche a lowercase.
	 * @param originalFilename
	 * @param toLowercase true for a lowercase name
	 * @return un filename safe, dove i caratteri speciali sono scomparsi
	 */
	public static String reduceToSafeFilename(String originalFilename, boolean toLowercase) {
		if (originalFilename==null) {
			return "null";
		}
		char[] resultChars = originalFilename.toCharArray();
		char[] lowerChars = originalFilename.toLowerCase().toCharArray();
		for (int i = 0; i < resultChars.length; i++) {
			char c = lowerChars[i]; // test on the lowercase version
			if (c==224 || c==225) { // à, á
				c='a';
			} else if (c==232 || c==233) { // é, è
				c='e';
			} else if (c==236 || c==237) { // ì, í
				c='i';
			} else if (c==242 || c==243) { // ò, ó
				c='o';
			} else if (c==249 || c==250) { // ù, ú
				c='u';
			} else if (c==167) {
				c='s';
			} else if (c==' ') {
				c='_';
			} else	if (c!='.' && c!='+' && !Character.isDigit(c) && (c<'a' || c>'z')) {
				c='_';
			} else {
				if (!toLowercase) {
					c = resultChars[i]; // Not changed
				}
			}
			// TODO raffinare con altri casi
			
			resultChars[i]=c;
		}
		return new String(resultChars).replaceAll("__+", "_").replaceAll("--+", "-");
	}


}
