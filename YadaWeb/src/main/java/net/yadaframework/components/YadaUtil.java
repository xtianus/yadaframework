package net.yadaframework.components;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.jpeg.JpegDirectory;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import net.yadaframework.core.CloneableDeep;
import net.yadaframework.core.CloneableFiltered;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.exceptions.YadaInvalidValueException;
import net.yadaframework.exceptions.YadaSystemException;
import net.yadaframework.persistence.entity.YadaAttachedFile;
import net.yadaframework.raw.YadaIntDimension;
import sogei.utility.UCheckDigit;
import sogei.utility.UCheckNum;

@Component
public class YadaUtil {
	private final static Logger log = LoggerFactory.getLogger(YadaUtil.class);

	@Autowired private YadaConfiguration config;
    @Autowired private AutowireCapableBeanFactory autowireCapableBeanFactory; // For autowiring entities

    static private YadaFileManager yadaFileManager;

    static public ApplicationContext applicationContext; 	// To access the ApplicationContext from anywhere
    static public MessageSource messageSource; 				// To access the MessageSource from anywhere, injected by YadaAppConfig

    public final static long MILLIS_IN_MINUTE = 60*1000;
	public final static long MILLIS_IN_HOUR = 60*MILLIS_IN_MINUTE;
	public final static long MILLIS_IN_DAY = 24*MILLIS_IN_HOUR;

	private SecureRandom secureRandom = new SecureRandom();
	private final static char CHAR_AT = '@';
	private final static char CHAR_DOT = '.';
	private final static char CHAR_SPACE = ' ';

	private static Locale defaultLocale = null;

	private List<String> computedTimezoneOffsets = null;
	private List<String> computedTimezones = null;

	/**
	 * Instance to be used when autowiring is not available
	 */
	public final static YadaUtil INSTANCE = new YadaUtil();

	// @PostConstruct
	@EventListener(ContextRefreshedEvent.class) // Called after the context has been initialized
    public void init() {
		defaultLocale = config.getDefaultLocale();
		yadaFileManager = getBean(YadaFileManager.class);
    }
	
	/**
	 * Returns a list of files from a folder where the name contains the given string, sorted alphabetically
	 * @param folderPath the folder where to looks for files, excluding subfolders.
	 * @param contains a string that the name must contain, can be empty or null to accept any
	 * @return a list of files, can be empty
	 * @throws IOException
	 */
	public List<Path> getFilesInFolder(Path folderPath, String contains) throws IOException {
		try (Stream<Path> paths = Files.list(folderPath)) {
	        return paths.filter(Files::isRegularFile)
	            .filter(path -> contains == null || path.getFileName().toString().contains(contains))
	            .sorted(Comparator.comparing(Path::getFileName))
	            .collect(Collectors.toList());
		}
	}
	
	/**
	 * Returns a random string (currently an hex random number)
	 * @param minlen minimum length of the string. The maximum length is random.
	 * @return
	 */
	public String getRandomText(int minlen) {
		int random = getRandom(0, Integer.MAX_VALUE);
		return String.format("%0"+minlen+"X", random);
	}

	/**
	 * Joins a number of strings, adding a separator only when the strings are not empty.
	 * In other words, null or empty strings are skipped without adding a separator.
	 * @param separator
	 * @param toJoin
	 * @return
	 */
	public String joinIfNotEmpty(@NotNull String separator, String...toJoin) {
		StringBuilder result = new StringBuilder();
		for (String part : toJoin) {
			if (StringUtils.isNotEmpty(part)) {
				if (result.length()>0) {
					result.append(separator);
				}
				result.append(part);
			}
		}
		return result.toString();
	}

	/**
	 * Given a date in the past, returns a string like "12 minutes ago", "2 hours ago", "today at 12:51", "yesterday at 5:32"...
	 * For dates before yesterday, the full RFC_1123 format is used, as 'Tue, 3 Jun 2008 11:05:30 GMT'.
	 * No "x days ago" format is currently provided.
	 * @param timestamp
	 * @param locale
	 * @param maxHours the max value of x for using the "x hours ago" format after which the "today at hh:mm" format is used
	 * 			The default is 3 when null. There is no maximum value, in order to have a "76 hours ago" result if needed.
	 * @return
	 */
	public String getTimestampAsRelative(ZonedDateTime timestamp, Locale locale, Integer maxHours) {
		maxHours = maxHours==null?3:maxHours;
		final long MILLIS_PER_SECOND = 1000;
		final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND*60;
		final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE*60;
		// final long MILLIS_PER_DAY = MILLIS_PER_HOUR*24;
		long elapsedMillis = System.currentTimeMillis()-timestamp.toInstant().toEpochMilli();
		//
		// Small intervals up to maxHours
		if (elapsedMillis >= 0 && elapsedMillis<MILLIS_PER_SECOND) {
			return messageSource.getMessage("yada.timestamp.now", null, locale); // "now"
		}
		if (elapsedMillis >= 0 && elapsedMillis<MILLIS_PER_MINUTE) {
			Long value = elapsedMillis / MILLIS_PER_SECOND;
			return messageSource.getMessage("yada.timestamp.secondsago", new Object[] {value}, locale); // "3 seconds ago"
		}
		if (elapsedMillis >= 0 && elapsedMillis<MILLIS_PER_HOUR) {
			Long value = elapsedMillis / MILLIS_PER_MINUTE;
			return messageSource.getMessage("yada.timestamp.minutesago", new Object[] {value}, locale); // "3 minutes ago"
		}
		if (elapsedMillis >= 0 && elapsedMillis<(maxHours+1)*MILLIS_PER_HOUR) {
			Long value = elapsedMillis / MILLIS_PER_HOUR;
			return messageSource.getMessage("yada.timestamp.hoursago", new Object[] {value}, locale); // "3 hours ago"
		}
		//
		// Medium intervals from maxHours up to yesterday
		ZonedDateTime zonedNow = ZonedDateTime.now(timestamp.getZone());
		long elapsedDays = daysBetween(timestamp, zonedNow);
		if (elapsedDays>=0 && elapsedDays<2) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:m");
			String hm = timestamp.format(formatter);
			if (elapsedDays==0) {
				// Today
				return messageSource.getMessage("yada.timestamp.todayAt", new Object[] {hm}, locale); // "today at 12:43"
			} else if (elapsedDays==1) {
				// Yesterday
				return messageSource.getMessage("yada.timestamp.yesterdayAt", new Object[] {hm}, locale); // "yesterday at 12:43"
			}
		}
		//
		// Long intervals: just show the full date and time
		return timestamp.format(DateTimeFormatter.RFC_1123_DATE_TIME);
	}

	/**
	 * Parse a string as a double, using the correct decimal separator (if any).
	 * @param value a number that may have a decimal part
	 * @param locale
	 * @return a double
	 * @throws ParseException if the string is not a valid double in the locale specified
	 */
	public double stringToDouble(String value, Locale locale) throws ParseException {
		// From https://stackoverflow.com/a/16879667/587641
		NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
		ParsePosition parsePosition = new ParsePosition(0);
		Number number = numberFormat.parse(value, parsePosition);
		if (parsePosition.getIndex() != value.length()){
			throw new ParseException("Invalid double input: '" + value + "'", parsePosition.getIndex());
		}
		return number.doubleValue();
	}

	/**
	 * Add an element to the list only if the element is not null
	 * @param <T>
	 * @param list
	 * @param element
	 */
	public <T> void addIfNotNull(List<T> list, T element) {
		if (element!=null) {
			list.add(element);
		}
	}
	
	/**
	 * Add an element to the list only if not there already
	 * @param <T>
	 * @param list
	 * @param element
	 */
	public <T> void addIfMissing(List<T> list, T element) {
		if (!list.contains(element)) {
			list.add(element);
		}
	}

	/**
	 * Create a new TreeSet that sorts values according to the order specified in the parameter.
	 * Values that are missing from sortOrder are sorted alphabetically
	 * @param sortOrder
	 * @return an empty sorted set that can receive a subset of the values in the sortOrder and keep them sorted the same way
	 */
	public Set<String> getEmptySortedSet(List<String> sortOrder) {
    	Map<String, Integer> order = new HashMap<String, Integer>(); // From value to position
    	for (int j = 0; j < sortOrder.size(); j++) {
    		order.put(sortOrder.get(j), j);
		}
    	Set<String> result = new TreeSet<>(new Comparator<String>() {
			@Override
			public int compare(String left, String right) {
				try {
					return order.get(left).compareTo(order.get(right));
				} catch (Exception e) {
					// In case of error, fallback to alphabetical
					log.error("Can't compare {} with {} (ignored)", left, right);
					if (left!=null) {
						return left.compareTo(right);
					}
					return right!=null?1:0;
				}
			}
    	});
		return result;
	}

	/**
	 * Given a ISO date, a ISO time and a timezone, return the Date.
	 * @param isoDateString like '2011-12-03'
	 * @param isoTimeString like '10:15' or '10:15:30' (optional, can be null or empty)
	 * @param timezone the timezone where the date/time strings belong (optional, can be null)
	 * @return a Date representing the datetime in the timezone, or null when invalid
	 */
	public Date getDateFromDateTimeIsoString(String isoDateString, String isoTimeString, TimeZone timezone) {
		if (isoDateString==null) {
			return null;
		}
		isoTimeString = StringUtils.trimToNull(isoTimeString);
		String isoDateTimeString = isoDateString + (isoTimeString!=null? ("T" + isoTimeString) : "T00:00");
		try {
			LocalDateTime chosenDateTime = LocalDateTime.parse(isoDateTimeString);
			if (timezone == null) {
				timezone = TimeZone.getDefault();
			}
			ZonedDateTime chosenDateTimeZoned = chosenDateTime.atZone(timezone.toZoneId());
			return Date.from(chosenDateTimeZoned.toInstant());
		} catch (Exception e) {
			log.error("Invalid ISO date/time (returning null)", e);
			return null;
		}
	}

	/**
	 * Returns a string for date and time in the specified timezone and locale
	 * @param date the date to format
	 * @param timezone the timezone in which the date is to be considered
	 * @param locale the locale to use for formatting
	 * @return The RFC-1123 formatted date, such as 'Tue, 3 Jun 2008 11:05:30 GMT'.
	 */
	public String getRfcDateTimeStringForTimezone(Date date, TimeZone timezone, Locale locale) {
		DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(locale);
		ZoneId zoneId = timezone.toZoneId();
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(date.toInstant(), zoneId);
		return zonedDateTime.format(formatter);
	}

	/**
	 * Convert a date in the timezone to a ISO string, like '2011-12-03'
	 * @param date
	 * @param timezone
	 * @return
	 */
	public String getIsoDateStringForTimezone(Date date, TimeZone timezone) {
		return formatDateTimeForTimezone(date, timezone, DateTimeFormatter.ISO_LOCAL_DATE);
	}

	/**
	 * Convert a time in the timezone to a ISO string, like '10:15' or '10:15:30'
	 * @param date
	 * @param timezone
	 * @return
	 */
	public String getIsoTimeStringForTimezone(Date time, TimeZone timezone) {
		return formatDateTimeForTimezone(time, timezone, DateTimeFormatter.ISO_LOCAL_TIME);
	}

	/**
	 * Convert a datetime in the timezone to a ISO string, like '2011-12-03T10:15:30'
	 * @param date
	 * @param timezone
	 * @return
	 */
	public String getIsoDateTimeStringForTimezone(Date dateTime, TimeZone timezone) {
		return formatDateTimeForTimezone(dateTime, timezone, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}

	private String formatDateTimeForTimezone(Date datetime, TimeZone timezone, DateTimeFormatter formatter) {
		if (datetime==null) {
			return "";
		}
		ZoneId zoneId = timezone.toZoneId();
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(datetime.toInstant(), zoneId);
		return zonedDateTime.format(formatter);
	}

	/**
	 * Create a single empty "json" object for use in other methods.
	 * Json objects are actually maps that get converted by Spring on return.
	 * @return
	 */
	public Map<String, Object> makeJsonObject() {
			return new HashMap<String, Object>();
	}

	/**
	 * Given a json stored as a map, returns the json at the specified key.
	 * Non need for this method if the objectPath is a simple key: just use the Map get(key) method.
	 * @param jsonSource
	 * @param objectPath the name of a (nested) json property holding an object
	 * @return the object or null if it does not exist
	 * @see #makeJsonObject(parentObject, path)
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getJsonObject(Map<String, Object> jsonSource, String objectPath) {
		return (Map<String, Object>) getJsonPath(jsonSource, objectPath);
	}

	/**
	 * Given a json stored as a map, returns the json at the specified list index
	 * @param jsonSource
	 * @param listPath the path of the json property holing the list
	 * @param listIndex the list index
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getJsonObject(Map<String, Object> jsonSource, String listPath, int listIndex) {
		return (Map<String, Object>) getJsonArray(jsonSource, listPath).get(listIndex);
	}

	/**
	 * Given a json stored as a map, returns the json array at the specified key
	 * Non need for this method if the objectPath is a simple key: just use the Map get(key) method.
	 * @param jsonSource
	 * @param objectPath
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Object> getJsonArray(Map<String, Object> jsonSource, String objectPath) {
		return (List<Object>) getJsonPath(jsonSource, objectPath);
	}

	/**
	 * Given a json stored as a map, returns the String value at the specified key, with optional nesting.
	 * Non need for this method if the objectPath is a simple key: just use the Map get(key) method.
	 * @param jsonSource
	 * @param objectPath the path of the attribute, using dot notation and arrays. E.g. "order.amount[2].currency"
	 * @return
	 */
	public String getJsonAttribute(Map<String, Object> jsonSource, String objectPath) {
		return (String) getJsonPath(jsonSource, objectPath);
	}

	/**
	 * Creates an empty json object at the given path
	 * @param parentObject the json object containing the new object
	 * @param path where the object should be created, using dot notation and arrays. E.g. "order.amount[2].currency".
	 * Any missing array cells are also created with an empty object as needed.
	 * @return
	 */
	public Map<String, Object> makeJsonObject(Map<String, Object> parentObject, String path) {
		return (Map<String, Object>) setJsonAttributeRecurse(parentObject, path, null);
	}

	/**
	 * Sets a json property at the given path. Non need for this method if the path is a simple key: just use the Map put(key, value) method.
	 * @param jsonObject the json object that should contain the property
	 * @param path path of the property using dot notation and arrays. E.g. "order.amount[2].currency".
	 * Any missing array cells are also created with an empty object as needed.
	 * @param value the value to store, can also be a "json object"
	 */
	public void setJsonAttribute(Map<String, Object> jsonObject, String path, Object value) {
		boolean isString = value instanceof String;
		boolean isJsonObject = value instanceof Map;
		if (!isString && !isJsonObject) {
			value = String.valueOf(value);
//			throw new YadaInvalidValueException("\"value\" must be either a string or a map");
		}
		setJsonAttributeRecurse(jsonObject, path, value);
	}

	private Object setJsonAttributeRecurse(Map<String, Object> jsonObject, String path, Object value) {
		if (path.length()==0 && value==null) {
			return jsonObject; // makeJsonObject called
		}
		String[] parts = path.split("\\.", 2); // {"a", "b[2].c"}
		String segment = parts[0]; // "a", "b[2]", "c"
		int index = -1;
		if (segment.endsWith("]")) {
			// Array
			String[] split = segment.split("[\\[\\]]");
			segment = split[0]; // "b"
			String indexString = split[1]; // "2"
			try {
				index = Integer.parseInt(indexString);
			} catch (NumberFormatException e) {
				log.debug("Invalid index '{}' in segment '{}' (ignored)", indexString, segment);
			}
		}
		boolean lastSegment = (parts.length==1);
		if (lastSegment && value!=null) {
			return jsonObject.put(segment, value);
		}
		Object currentObject = jsonObject.get(segment); // Either Object, List or Null
		if (currentObject==null) {
			if (index>-1) {
				currentObject = new ArrayList<Object>();
			} else {
				currentObject = makeJsonObject();
			}
			jsonObject.put(segment, currentObject);
		}
		if (currentObject instanceof ArrayList) {
			if (index<0) {
				throw new YadaInvalidValueException("Not an array at {}", segment);
			}
			List<Object> currentList = (List<Object>) currentObject;
			for (int k = currentList.size(); k <= index; k++) {
				// Add missing cells if any
				currentList.add(makeJsonObject());
			}
			currentObject = currentList.get(index);
		}
		int dotPos = path.indexOf(".");
		String remainingPath = dotPos > -1 ? path.substring(dotPos+1) : "";
		return setJsonAttributeRecurse((Map<String, Object>)currentObject, remainingPath, value);
	}

	private Object getJsonPath(Map<String, Object> jsonSource, String objectPath) {
		Object result = jsonSource;
		String[] parts = objectPath.split("\\.");
		for (int i = 0; i < parts.length; i++) {
			String segment = parts[i];
			int index = -1;
			if (segment.endsWith("]")) {
				// Array
				String[] split = segment.split("[\\[\\]]");
				segment = split[0];
				String indexString = split[1];
				try {
					index = Integer.parseInt(indexString);
				} catch (NumberFormatException e) {
					log.debug("Invalid index '{}' in segment '{}' (ignored)", indexString, parts[i]);
				}
			}
			result = ((Map<String, Object>) result).get(segment);
			if (result==null) {
				log.debug("Null value at {}", segment);
				return null;
			}
			if (index>-1) { // Array
				try {
					result = ((List<Object>)result).get(index);
				} catch (IndexOutOfBoundsException e) {
					log.debug("Index out of bounds at {}[{}]", segment, index);
					return null;
				}
				if (result==null) {
					log.debug("Null value at {}[{}]", segment, index);
					return null;
				}
			}
		}
		return result;
	}

	/**
	 * Returns a list of user-friendly timezones like "Europe/Rome"
	 * @return
	 */
	public List<String> getTimezones() {
		if (computedTimezones==null) {
			computedTimezones = new ArrayList<String>();
			String[] allTimezones = TimeZone.getAvailableIDs();
			for (String timezone : allTimezones) {
				// Only timezones with a / that start with a continent, like "Europe/Rome"
				if (timezone.indexOf('/')>-1 && !timezone.startsWith("Etc/") && !timezone.startsWith("SystemV/")) {
					computedTimezones.add(timezone);
				}
			}
		}
		return computedTimezones;
	}

	/**
	 * Get a list of GMT/UTC time offsets from UTC-12:00 to UTC+14:00
	 * @param prefix use either "GMT" or "UTC"
	 * @return from "GMT-12:00" to "GMT+14:00"
	 */
	public List<String> getTimezoneOffsets(String prefix) {
		if (computedTimezoneOffsets==null) {
			computedTimezoneOffsets = new ArrayList<String>();
			// https://en.wikipedia.org/wiki/List_of_UTC_time_offsets
			for (int i=-12; i<=14; i++) {
				computedTimezoneOffsets.add(String.format("%s%+03d:00", prefix, i));
				if (i==-10 || i==-4 || i==3 || i==4 || i==5 || i==6 || i==9 || i==10) {
					computedTimezoneOffsets.add(String.format("%s%+03d:30", prefix, i+(i<0?1:0)));
				}
				if (i==5 || i==8 || i==12) {
					computedTimezoneOffsets.add(String.format("%s%+03d:45", prefix, i));
				}
			}
		}
		return computedTimezoneOffsets;
	}

	/**
	 * Simple email address syntax check: the format should be X@Y.Y
	 * where X does not contain @ and Y does not contain @, nor . at the edges.
	 * Also no spaces anywhere.
	 * @param email
	 * @return
	 */
	public boolean isEmailValid(String email) {
		if (email.indexOf(CHAR_SPACE)>-1) {
			return false;
		}
		int firstAtPos = email.indexOf(CHAR_AT);
		int lastAtPos = email.lastIndexOf(CHAR_AT);
		if (firstAtPos<0 || firstAtPos != lastAtPos || lastAtPos==email.length()-1) {
			return false; // No @ or more than one or one at the end
		}
		int lastDotPos = email.lastIndexOf(CHAR_DOT);
		if (lastDotPos<0 						|| // No DOT
			lastDotPos<firstAtPos 				|| // No DOT after the AT
			lastDotPos==email.length()-1 		|| // DOT at the end
			email.charAt(firstAtPos+1)==CHAR_DOT || // DOT after the AT
			email.charAt(lastDotPos-1)==CHAR_DOT   // Two consecutive dots
			) {
			return false;
		}
		return true;
	}

	/**
	 * Convert a map of strings to a commaspace-separated string of name=value pairs
	 * @param stringMap
	 * @return a name-value string like "n1=v1, n2=v2"
	 */
	public String mapToString(Map<String, String> stringMap) {
		String result = stringMap.toString(); // "{n1=v1, n2=v2}"
		result = StringUtils.chop(result); // Remove }
		result = StringUtils.removeStart(result, "{"); // Remove {
		return result;
	}

	/**
	 * Returns a random integer number
	 * @param minIncluded minimum value, included
	 * @param maxIncluded maximum value, included
	 * @return
	 */
	public int getRandom(int minIncluded, int maxIncluded) {
		if (maxIncluded==Integer.MAX_VALUE) {
			maxIncluded = Integer.MAX_VALUE - 1; // Needed to prevent overflow of maxExcluded below
		}
		int maxExcluded = maxIncluded - minIncluded + 1;
		return secureRandom.nextInt(maxExcluded) + minIncluded;
	}

	/**
	 * Given the instance of a "specific" class created specifying a single type T while extending a generic class,
	 * retrieve the class of the type T.
	 * It also works when looking for the generic super-super class at any hierarchy level.
	 * Example:
	 * the generic class is public abstract class Shape<T extends Color> {...}
	 * the specific class is public class Circle extends Shape<Red>
	 * the instance is new Circle()
	 * the returned value is Red.class
	 * @param specificClassInstance instance of the specific class, usually "this" when called from inside either the specific or the generic abstract class.
	 * @return the class T used to make the generic specific, or null if there is no generic superclass in the hierarchy
	 */
	public Class<?> findGenericClass(Object specificClassInstance) {
		Class<?> theClass = specificClassInstance.getClass();

		while (theClass!=null && !(theClass.getGenericSuperclass() instanceof ParameterizedType)) {
			theClass = theClass.getSuperclass();
		}
		if (theClass!=null) {
			return (Class<?>)((ParameterizedType)theClass.getGenericSuperclass()).getActualTypeArguments()[0];
		}
		return null;
	}

	/**
	 * Merges all files matched by a pattern, in no particular order.
	 * @param sourceFolder root folder where files are to be found
	 * @param sourceFilePattern regex pattern to match files, e.g. ".*.js"
	 * @param outputFile file that will contain the joined files
	 * @param depth (optional) max depth of folders: null or 1 for no recursion
	 * @param deleteSource (optional) Boolean.TRUE to attempt deletion of source files
	 * @throws IOException
	 */
	public void joinFiles(Path sourceFolder, String sourceFilePattern, File outputFile, Integer depth, Boolean deleteSource) throws IOException {
		depth = depth==null ? 1 : depth; // By default we don't look into subfolders
		FileOutputStream joinedStream = new FileOutputStream(outputFile);
		Iterator<Path> allFilesIter = java.nio.file.Files.find(sourceFolder, depth, (path, basicFileAttributes) -> path.toFile().getName().matches(sourceFilePattern)).iterator();
		while (allFilesIter.hasNext()) {
			Path filePath = allFilesIter.next();
			com.google.common.io.Files.copy(filePath.toFile(), joinedStream);
			if (Boolean.TRUE.equals(deleteSource)) {
				filePath.toFile().delete();
			}
		}
		joinedStream.close();
	}

	/**
	 * Creates a folder in the system temp folder. The name is prefixed with "yada".
	 * @return
	 * @throws IOException
	 * @see {@link Files#createTempDirectory(String, java.nio.file.attribute.FileAttribute...)}
	 */
	@Deprecated // This should not be used because the operation is not atomic
	// Use Files.createTempDirectory() instead
	public File makeTempFolder() throws IOException {
		File file = File.createTempFile("yada", "");
		file.delete();
        file.mkdir();
        return file;
	}

	/**
	 * Finds the path between two files or folders using forward (unix) slashes as a separator
	 * @param ancestorFolder
	 * @param descendantFolder
	 * @return
	 */
	public String relativize(File ancestorFolder, File descendantFolder) {
		return relativize(ancestorFolder.toPath(), descendantFolder.toPath());
	}

	/**
	 * Finds the path between two files or folders using forward (unix) slashes as a separator.
	 * It works even if the two arguments point to places in different trees.
	 * @param ancestorPath the root path e.g. "/a/b"
	 * @param descendantPath the final path e.g. "/a/b/c/the.gif"
	 * @return the relative path from ancestorPath to descendantPath e.g. "c/the.gif"
	 */
	public String relativize(Path ancestorPath, Path descendantPath) {
		// When on windows, if one path has a drive letter and the other doesn't, an exception would be thrown, so we fix that.
		if (ancestorPath.isAbsolute() && !descendantPath.isAbsolute()) {
			descendantPath = Paths.get(ancestorPath.getRoot().toString(), descendantPath.toString());
		} else if (!ancestorPath.isAbsolute() && descendantPath.isAbsolute()) {
			ancestorPath = Paths.get(descendantPath.getRoot().toString(), ancestorPath.toString());
		}
		String segment = ancestorPath.relativize(descendantPath).toString();
		return segment.replaceAll("\\\\", "/");
	}

	/**
	 * Split an HTML string in two parts, not breaking words, handling closing and reopening of html tags.
	 * Useful when showing some part of a text and the whole of it after a user clicks.
	 * For example, the string "&lt;p>Some text here&lt;/p> becomes ["&lt;p>Some text&lt;/p>","&lt;p>here&lt;/p>"].
	 * The HTML is not splitted exactly at splitPos if there's a word there, a tag, or if the paragraph ends in the next 20 characters:
	 * in such cases the split position is increased accordingly.
	 * Note: does not work in any possible scenario. For example &lt;ul>&lt;li> is not split properly because it creates two list entries
	 * if the split point is inside the li. Tag attributes are not currently handled properly.
	 * @param htmlToSplit The html text to split, must be well-formed (all opened tags must be closed properly)
	 * @param splitPos the minimum position, in number of characters including tags, where to split
	 * @return an array of two self-contained html parts, where each opened tag is correctly closed. The second part could be null.
	 * @see #splitAtWord(String, int)
	 */
	public String[] splitHtml(String htmlToSplit, int splitPos) {
		String[] result = new String[2];
		char[] charArray = htmlToSplit.toCharArray();
		int maxPos = charArray.length-1;
		boolean tag = false; // True when the current character is inside an HTML tag
		boolean tagOpen = false; // True when an HTML tag has been opened
		int pos = 0;
		Stack<String> tagsToCopy = new Stack<>();
		StringBuffer tagName = null;
		try {
			while (pos<maxPos) {
				char current = charArray[pos];
				boolean isSpace = current==' ';
				if (!isSpace && !tag && current=='<') {
					// We are at the start of an HTML tag: get the name and check if it's opening or closing
					tag = true;
					tagOpen = charArray[pos+1]!='/';
					if (tagOpen) {
						tagName = new StringBuffer();
					}
					// We get the name of opening tags only and presume that the HTML is well formed
				} else if (!isSpace && tag && current=='>') {
					// Last character of a tag. If it was an opening tag, add it to the stack of opened tags
					tag = false;
					if (tagOpen) {
						String tagNameString = tagName.toString();
						// br is not added because it does not need a closing tag
						// TODO what other html tags don't have a closing one?
						if (!"br".equals(tagNameString) && !tagNameString.contains("br/")) {
							tagsToCopy.add(tagNameString);
						}
						tagOpen=false;
					} else {
						// It was a close tag. We presume that it was the same as the last one on the stack and we forget it.
						if (!tagsToCopy.isEmpty()) {
							tagsToCopy.pop();
						}
					}
					continue;
				} else if (pos>=splitPos && !tag && isSpace) {
					// We reached or surpassed the split point outside of a tag and at a space character.
					// The HTML can be split here, unless there's a closing p in the next 20 character
					// TODO 20 should be a parameter?
					// TODO should be done for <li> too
					int closep = htmlToSplit.indexOf("</p>", pos);
					if (closep>-1 && closep-pos<20) {
						// Close the paragraph in the first part
						pos = closep + "</p>".length();
						// Forget all tags up to the opening paragraph because we assume that we skipped the closing ones
						while (!tagsToCopy.isEmpty()) {
							String tagToCopy = tagsToCopy.pop();
							if ("p".equals(tagToCopy)) {
								break;
							}
						}
					}
					// Split at a safe position
					result[0] = htmlToSplit.substring(0, pos);
					result[1] = htmlToSplit.substring(pos);
					// Add any needed closing tags to the first part and opening tags to the second part
					while (!tagsToCopy.isEmpty()) {
						String tagToCopy = tagsToCopy.pop();
						result[0] += "</" + tagToCopy + '>'; // Tag closed in the first part
						result[1] = "<" + tagToCopy + '>' + result[1]; // Tag reopened in the second part
					}
					return result;
				} else if (tagOpen) {
					tagName.append(current);
				}
				pos++;
			}
		} catch (Exception e) {
			// In case of error, the whole HTML is returned in the first part, and null in the second
			log.error("Can't split HTML (returned whole)", e);
		}
		result[0] = htmlToSplit;
		return result;
	}
	
	/**
	 * Ensure that the given filename has not been already used, by adding a counter.
	 * For example, if baseName is "dog" and usedNames is {"dog.jpg", "dog_1.jpg", "dog_2.jpg"}, the
	 * result will be "dog_3.jpg"
	 * The usedNames array doesn't have to contain identical or sequential baseNames: {"dog.jpg", "cat.jpg", "dog_2.jpg"}
	 * This version does not check if a file exists on disk. For that, see {@link #findAvailableName(File, String, String, String)}
	 * This method can be used with any strings, not necessarily filenames: just use null for the extension.
	 * @param baseName filename to add, without extension
	 * @param extensionNoDot filename extension without dot, can be empty or null if the extension is not needed
	 * @param counterSeparator string to separate the filename and the counter, can be empty or null
	 * @param usedNames filenames used so far, can start empty but never null, and will be modified by adding the new name
	 * @return the original filename with extension, or a new version with a counter added
	 * @throws IOException
	 * @see {@link #findAvailableName(File, String, String, String)}
	 */
	public String findAvailableFilename(String baseName, String extensionNoDot, String counterSeparator, Set<String> usedNames) throws IOException {
		counterSeparator = counterSeparator==null?"":counterSeparator;
		String extension = StringUtils.isAllBlank(extensionNoDot) ? "" : "." + extensionNoDot;
		String fullName = baseName + extension;
		int counter = 0;
		long startTime = System.currentTimeMillis();
		int timeoutMillis = 10000; // 10 seconds to find a result seems to be reasonable
		while (true) {
			if (!usedNames.contains(fullName)) {
				usedNames.add(fullName);
				return fullName;
			}
			counter++;
			fullName = baseName + counterSeparator + counter + extension;
			if (System.currentTimeMillis()-startTime > timeoutMillis) {
				throw new IOException("Timeout trying to create a unique name starting with " + baseName);
			}
		}
	}

	/**
	 * Check if a date is not more than maxYears years from now, not in an accurate way.
	 * Useful to check validity of a date coming from the browser.
	 * @param someDate
	 * @param maxYears max number of years (positive or negative) for this date to be valid. When null, defaults to 4000 years.
	 * @return false if the date is too distant from now
	 */
	public boolean dateValid(Date someDate, Integer maxYears) {
		long now = System.currentTimeMillis();
		long toCheck = someDate.getTime();
		long difference = Math.abs(now-toCheck);
		final long millisInYearInaccurate = 365*MILLIS_IN_DAY;
		maxYears = maxYears==null?4000:maxYears;
		return difference < maxYears*millisInYearInaccurate;
	}

	/**
	 * Gets image dimensions for given file, ignoring orientation flag
	 * @param imageFile image file
	 * @return dimensions of image, or YadaIntDimension.UNSET when not found
	 */
	// Adapted from https://stackoverflow.com/a/12164026/587641
	// The default jpeg image reader does not handle the exif Orientation flag properly
	// so a "vertical" image with an orientation flag of 6 is considered horizontal
	// and will have a width larger than the height
	// See https://www.impulseadventure.com/photo/exif-orientation.html
	public YadaIntDimension getImageDimensionDumb(File imageFile) {
		String suffix = getFileExtension(imageFile);
		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
		while (iter.hasNext()) {
			ImageReader reader = iter.next();
			try(ImageInputStream stream = new FileImageInputStream(imageFile))  {
				reader.setInput(stream);
				int width = reader.getWidth(reader.getMinIndex());
				int height = reader.getHeight(reader.getMinIndex());
				return new YadaIntDimension(width, height);
			} catch (IOException e) {
				log.debug("Error reading dimensions for {} using reader {}", imageFile, reader, e);
			} finally {
				reader.dispose();
			}
		}
		return YadaIntDimension.UNSET;
	}

	/**
	 * Gets the image dimensions considering the EXIF orientation flag.
	 * Remember to use the "-auto-orient" flag of the ImageMagick convert command.
	 * If the EXIF width and height information is missing, the getImageDimensionDumb() method is called instead.
 	 * See https://www.impulseadventure.com/photo/exif-orientation.html
	 * @param imageFile
	 * @return
	 */
	public YadaIntDimension getImageDimension(File imageFile) {
		String suffix = getFileExtension(imageFile);
		if (!ImageIO.getImageReadersBySuffix(suffix).hasNext()) {
			return YadaIntDimension.UNSET; // Not an image
		}

		try(InputStream stream = new FileInputStream(imageFile))  {
			Metadata metadata = ImageMetadataReader.readMetadata(stream);
			//			for (com.drew.metadata.Directory directory2 : metadata.getDirectories()) {
			//	            for (com.drew.metadata.Tag tag : directory2.getTags()) {
			//	            	if (tag.getTagName().equalsIgnoreCase("Orientation")) {
			//	            		System.out.println(tag.getTagName());
			//	            		System.out.println(tag);
			//
			//	            	}
			//	            }
			//			}
			boolean valid = false;
			int orientation = 1; // Default when can't be retrieved
			int width = -1, height = -1;
			ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			if (directory!=null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
				orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
			}
			ExifSubIFDDirectory directory2 = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			if (directory2!=null) {
				width = directory2.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH);
				height = directory2.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT);
				valid = true;
			} else {
				// If there is no exif directory, look for info in the gif header
				GifHeaderDirectory gifHeaderDirectory = metadata.getFirstDirectoryOfType(GifHeaderDirectory.class);
				if (gifHeaderDirectory!=null) {
					width = gifHeaderDirectory.getInt(GifHeaderDirectory.TAG_IMAGE_WIDTH);
					height = gifHeaderDirectory.getInt(GifHeaderDirectory.TAG_IMAGE_HEIGHT);
					valid = true;
				} else {
					// Look for info in the jpeg header
					JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
					if (jpegDirectory!=null) {
						width = jpegDirectory.getInt(JpegDirectory.TAG_IMAGE_WIDTH);
						height = jpegDirectory.getInt(JpegDirectory.TAG_IMAGE_HEIGHT);
						valid = true;
					}
				}
			}
			if (valid) {
				if (orientation==6 || orientation==8) {
					// Image is rotated 90° so dimensions must be swapped
					return new YadaIntDimension(height, width);
				}
				return new YadaIntDimension(width, height);
			}
		} catch (Exception e) {
			log.debug("Exception reading image dimensions for {}: {}}", imageFile, e.getMessage());
		}
		log.debug("Fallback to dumb version while reading image dimensions for {}", imageFile);
		return getImageDimensionDumb(imageFile);
	}

	/**
	 * Returns the current stack trace as a string, formatted on separate lines
	 * @return
	 */
	public String getCurrentStackTraceFormatted() {
		StringBuilder stringBuilder = new StringBuilder();
		for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
			stringBuilder.append("\tat ").append(element).append('\n');
		}
		return stringBuilder.toString();
	}

	/**
	 * Returns a random element from the list
	 * @param list
	 * @return a random element from the list, or null if the list is empty
	 */
	public <T> T getRandomElement(List<T> list) {
		if (list.size()>0) {
			int pos = secureRandom.nextInt(list.size());
			return list.get(pos);
		}
		return null;
	}

	/**
	 * Convert from an amount of time to a string in the format xxd:hh:mm:ss
	 * @param amount interval that needs to be formatted
	 * @param timeUnit the unit of the interval
	 * @return a formatted string representing the input interval
	 */
	public static String formatTimeInterval(long amount, TimeUnit timeUnit) {
		long totSeconds = timeUnit.toSeconds(amount);
		long seconds = totSeconds % 60;
		long totMinutes = timeUnit.toMinutes(amount);
		long minutes = totMinutes % 60;
		long totHours = timeUnit.toHours(amount);
		long hours = totHours % 24;
		long days = timeUnit.toDays(amount);
		String result = String.format("%02d:%02d", minutes, seconds);
		if (hours+days>0) {
			result = String.format("%02d:", hours) + result;
			if (days>0) {
				result = String.format("%dd:", days) + result;
			}
		}
		return result;
	}

	/**
	 * Perform autowiring of an instance that doesn't come from the Spring context, e.g. a JPA @Entity.
	 * Post processing (@PostConstruct etc) is also performed but initialization is not.
	 * @param instance to autowire
	 * @see AutowireCapableBeanFactory#autowireBean(Object)
	 */
	public void autowire(Object instance) {
		autowireCapableBeanFactory.autowireBean(instance);
	}

	/**
	 * Perform autowiring of an instance that doesn't come from the Spring context, e.g. a JPA @Entity or normal java instance made with new.
	 * Post processing (@PostConstruct etc) and initialization are also performed.
	 * Beans from the WebApplicationContext like @Controller are not injected because they don't belong to the root Application Context used here.
	 * If you need to do that, use {@link YadaWebUtil#autowireAndInitialize(Object)}
	 * @param instance to autowire
	 * @return the autowired/initialized bean instance, either the original or a wrapped one
	 * @see {@link YadaWebUtil#autowireAndInitialize(Object)}, {@link AutowireCapableBeanFactory#autowireBean(Object)}, {@link AutowireCapableBeanFactory#initializeBean(Object, String)}, {@link #autowire(Object)}
	 */
	public Object autowireAndInitialize(Object instance) {
		autowireCapableBeanFactory.autowireBean(instance);
		return autowireCapableBeanFactory.initializeBean(instance, instance.getClass().getSimpleName());
	}

	/**
	 * Remove a counter that has been added by {@link #findAvailableName}
	 * @param filename
	 * @param counterSeparator
	 * @return
	 */
	public static String stripCounterFromFilename(String filename, String counterSeparator) {
		String[] filenameParts = YadaUtil.splitFileNameAndExtension(filename);
		// If the filename has a counter attached, strip it
		String prefix = filenameParts[0]; // product_2
		int pos = prefix.lastIndexOf(counterSeparator);
		if (pos>-1 && pos<prefix.length()-1) {
			String left = prefix.substring(0, pos); // product
			String right = prefix.substring(pos+1); // 2
			try {
				Integer.parseInt(right);
				prefix = left;
			} catch (NumberFormatException e) {
				// Not a number, keep going
			}
		}
		return prefix;
	}
	
	/**
	 * Returns a file that doesn't already exist in the specified folder
	 * with the specified leading characters (baseName) and optional extension.
	 * The file will always have a number at the end that is higher than any other numbers on
	 * similar files in the folder.
	 * Can be used to find both files and folders.
	 * @param targetFolder
	 * @param baseName
	 * @param extensionNoDot
	 * @param counterSeparator
	 * @return a file with a name like baseName_0001 or baseName_0003.txt
	 * @throws YadaInvalidUsageException when targetFolder is not a folder
	 */
	public File findAvailableNameHighest(File targetFolder, String baseName, String extensionNoDot, String counterSeparator) {
		if (!targetFolder.isDirectory()) {
            throw new YadaInvalidUsageException(targetFolder + " is not a directory.");
        }

        String regexPattern = "^" + Pattern.quote(baseName) + Pattern.quote(counterSeparator) + "(\\d{4})";
        regexPattern += (extensionNoDot != null) ? "\\." + extensionNoDot + "$" : "$";
        Pattern pattern = Pattern.compile(regexPattern);

        File[] currentFiles = targetFolder.listFiles();
        if (currentFiles==null) {
        	throw new YadaSystemException("Can't read folder {}", targetFolder);
        }
        Optional<Integer> maxNumber = Arrays.stream(currentFiles)
            .filter(file -> pattern.matcher(file.getName()).matches())
            .map(file -> {
				Matcher matcher = pattern.matcher(file.getName());
				matcher.find();
				return Integer.parseInt(matcher.group(1));
            })
            .max(Comparator.naturalOrder());

        int nextNumber = maxNumber.map(n -> n + 1).orElse(1);
        
        String filename = (extensionNoDot != null) 
            ? String.format("%s%s%04d.%s", baseName, counterSeparator, nextNumber, extensionNoDot)
            : String.format("%s%s%04d", baseName, counterSeparator, nextNumber);
		return new File(targetFolder, filename);
	}

	/**
	 * Creates an empty file that doesn't already exist in the specified folder
	 * with the specified leading characters (baseName).
	 * A counter may be appended to make the file unique.
	 * This operation is thread safe.
	 * @param targetFolder the folder where the file has to be placed
	 * @param baseName the leading characters for the file, like "product"
	 * @param extension the extension without a dot, like "jpg"
	 * @param counterSeparator the separator to be used before appending the number, e.g. "_"
	 * @return a new file in that folder, with a name like "product_2.jpg" or "product.jpg"
	 * @throws IOException
	 */
	public static File findAvailableName(File targetFolder, String baseName, String extensionNoDot, String counterSeparator) throws IOException {
		String extension = "." + extensionNoDot;
		String filename = baseName + extension;
		int counter = 0;
		long startTime = System.currentTimeMillis();
		int timeoutMillis = 10000; // 10 seconds to find a result seems reasonable
		while (true) {
			File candidateFile = new File(targetFolder, filename);
			try {
				if (candidateFile.createNewFile()) {
					return candidateFile;
				}
			} catch (IOException e) {
				log.error("Can't create file {}", candidateFile);
				throw e;
			}
			counter++;
			filename = baseName + counterSeparator + counter + extension;
			if (System.currentTimeMillis()-startTime > timeoutMillis) {
				throw new IOException("Timeout trying to create a unique file starting with " + baseName + " in folder " + targetFolder);
			}
		}
	}

	/**
	 * Creates a file with a unique filename by appending a number after the specified separator if needed.
	 * If the targetFile exists already, a new file is created with a proper counter at the end. The counter may be stripped
	 * altogether (if the original file had a counter and no file without counter exists) or added or incremented.
	 * The new counter might not be higher than the original one, nor sequential. It depends on what's already on
	 * the filesystem.
	 * This operation is thread safe.
	 * @param targetFile the file that we want to create.
	 * @param counterSeparator (optional) when null, "_" is used.
	 * @return a File that doesn't already exist
	 * @throws IOException
	 */
	public static File findAvailableName(File targetFile, String counterSeparator) throws IOException {
		if (counterSeparator==null) {
			counterSeparator="_";
		}
		String targetFilename = targetFile.getName();
		String extension = splitFileNameAndExtension(targetFilename)[1];
		String strippedName = stripCounterFromFilename(targetFilename, counterSeparator);
		return findAvailableName(targetFile.getParentFile(), strippedName, extension, counterSeparator);
	}

	/**
	 * Force initialization of localized strings implemented with Map&lt;Locale, String>.
	 * It must be called in a transaction.
	 * @param fetchedEntity object fetched from database that may contain localized strings
	 * @param targetClass type of fetchedEntity element
	 */
	public static <targetClass> void prefetchLocalizedStrings(targetClass fetchedEntity, Class<?> targetClass, String...attributes) {
		if (fetchedEntity!=null) {
			List<targetClass> list = new ArrayList<>();
			list.add(fetchedEntity);
			prefetchLocalizedStringList(list, targetClass, attributes);
		}
	}

	/**
	 * Force initialization of localized strings implemented with Map&lt;Locale, String>.
	 * It must be called in a transaction.
	 * @param fetchedEntity object fetched from database that may contain localized strings
	 * @param targetClass type of fetchedEntities elements
	 * @param attributes the localized string attributes to prefetch (optional). If missing, all attributes of the right type are prefetched.
	 */
	public static <targetClass> void prefetchLocalizedStringsRecursive(targetClass fetchedEntity, Class<?> targetClass, String...attributes) {
		if (fetchedEntity!=null) {
			List<targetClass> list = new ArrayList<>();
			list.add(fetchedEntity);
			prefetchLocalizedStringListRecursive(list, targetClass, attributes);
		}
	}

	/**
	 * Force initialization of localized strings implemented with Map&lt;Locale, String>.
	 * It must be called in a transaction.
	 * @param entities objects fetched from database that may contain localized strings
	 * @param entityClass type of fetchedEntities elements
	 * @param attributes the localized string attributes to prefetch (optional). If missing, all attributes of the right type are prefetched.
	 */
	public static <entityClass> void prefetchLocalizedStringListRecursive(List<entityClass> entities, Class<?> entityClass, String...attributes) {
		// TODO I don't actually get how this works.
		//      It looks like it's prefetching all first-level local strings, then
		//      if an attribute is not a generic, it will fetch all first-level local strings there.
		//      It doesn't make sense.
		//      It should instead recurse on itself for all attributes that are neither primitive not local strings,
		//		unrolling collections and arrays.
		if (entities==null || entities.isEmpty()) {
			return;
		}
		// Prefetch first level strings for all objects
		prefetchLocalizedStringList(entities, entityClass, attributes);
		// Look for strings in all attributes recursively
		ReflectionUtils.doWithFields(entityClass, new ReflectionUtils.FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				for (Object object : entities) {
					if (object!=null) {
						try {
							field.setAccessible(true);
							Object fieldValue = field.get(object);
							if (fieldValue!=null) {
								Class fieldClass = field.getType();
								List secondLevel = new ArrayList<>();
								secondLevel.add(fieldValue);
								// TODO shouldn't this call prefetchLocalizedStringListRecursive()?
								prefetchLocalizedStringList(secondLevel, fieldClass, attributes);
							}
						} catch (Exception e) {
							log.error("Failed to initialize field {} for object {} (ignored)", field, object);
						}
					}
				}
			}
		}, new ReflectionUtils.FieldFilter() {
			@Override
			public boolean matches(Field field) {
				Type type = field.getGenericType();
				return !(type instanceof ParameterizedType);
			}
		});
	}

	/**
	 * Force initialization of localized strings implemented with Map&lt;Locale, String>.
	 * It must be called in a transaction.
	 * @param entities objects fetched from database that may contain localized strings
	 * @param entityClass type of fetchedEntities elements
	 * @param attributes the localized string attributes to prefetch (optional). If missing, all attributes of the right type are prefetched.
	 */
	public static <entityClass> void prefetchLocalizedStringList(Collection<entityClass> entities, Class<?> entityClass, String...attributes) {
		List<String> attributeNames = Arrays.asList(attributes);
		if (entities==null || entities.isEmpty()) {
			return;
		}
		// Look for fields of type Map<Locale, String>
		ReflectionUtils.doWithFields(entityClass, new ReflectionUtils.FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				for (Object object : entities) {
					if (object!=null) {
						try {
							field.setAccessible(true);
							Object fieldValue = field.get(object);
							if (fieldValue!=null) {
								Method sizeMethod = Map.class.getMethod("size");
								sizeMethod.invoke(fieldValue); // Load all the map
							}
						} catch (NoSuchMethodException | SecurityException | InvocationTargetException e) {
							log.error("Failed to initialize field {} for object {} (ignored)", field, object);
						}
					}
				}
			}
		}, new ReflectionUtils.FieldFilter() {
			@Override
			public boolean matches(Field field) {
				if (attributeNames.size()>0 && !attributeNames.contains(field.getName())) {
					return false; // Handle only the specified attributes
				}
				Type type = field.getGenericType();
				if (type instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType) type;
					Type[] params = parameterizedType.getActualTypeArguments();
					return params.length==2 && Locale.class.equals(params[0]) && String.class.equals(params[1]);
				}
				return false;
			}
		});
	}
	
	/**
	 * Returns a string representation of the object as Object.toString() does, even if toString() has been overridden
	 * @param object
	 * @return
	 */
	// Must be static to be used by getLocalValue()
	public static String getObjectToString(Object object) {
		return object.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(object));
	}

	/**
	 * Returns the localized value from a map of Locale -> String.
	 * Used in entities with localized string attributes.
	 * If a default locale has been configured with <code>&lt;locale default='true'></code>, then that locale is attempted when
	 * there is no value (null or "") for the needed locale (and they differ)
	 * @param LocalizedValueMap
	 * @return the localized value, or the empty string if no value has been defined and no default locale has been configured
	 */
	public static String getLocalValue(Map<Locale, String> LocalizedValueMap) {
		return YadaUtil.getLocalValue(LocalizedValueMap, LocaleContextHolder.getLocale());
	}
	
	/**
	 * Returns the localized value from a map of Locale -> String.
	 * Used in entities with localized string attributes.
	 * If a default locale has been configured with <code>&lt;locale default='true'></code>, then that locale is attempted when
	 * there is no value for the needed locale (and they differ)
	 * @param localizedValueMap
	 * @param locale the needed locale for the value, can be null for the current request locale
	 * @return the localized value, or the empty string if no value has been defined and no default locale has been configured
	 */
	public static String getLocalValue(Map<Locale, String> localizedValueMap, Locale locale) {
		String result=null;
		try {
			if (locale==null) {
				locale = LocaleContextHolder.getLocale();
			}
			result = localizedValueMap.get(locale);
		} catch (Exception e) {
			// By default use a safe plain version of the toString() method
			String localizedValueMapName = getObjectToString(localizedValueMap);
			try {
				localizedValueMapName=localizedValueMap.toString();
			} catch (Exception e1) {
				// Swallow this exception because that's not the real cause;
			}
			log.debug("Exception while getting localized value from {} with locale={} (ignored): {}", localizedValueMapName, locale, e.getMessage());
			// Keep going
		}
		if (StringUtils.isEmpty(result) && defaultLocale!=null && !defaultLocale.equals(locale)) {
			result = localizedValueMap.get(defaultLocale);
		}
		return result==null?"":result;
	}

	/**
	 * Deletes a file without reporting any errors.
	 * @param file the file. It could also be an empty foder. Folders containing files are not deleted.
	 */
	public boolean deleteFileSilently(Path file) {
		if (file!=null) {
		try {
			return Files.deleteIfExists(file);
		} catch (Throwable e) {
				log.debug("File {} not deleted: " + e.getMessage(), file);
			}
		}
			return false;
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
	 * @see java.nio.file.Files#copy(Path, Path, java.nio.file.CopyOption...)
	 * @see org.apache.commons.io.IOUtils#copy(InputStream, OutputStream)
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
	 * Get the Field of a given class, even from a superclass but not "nested" in a path
	 * @param rootClass
	 * @param attributeName
	 * @return the Field found or null
	 * @throws YadaInvalidValueException if attributeName is a path (with a dot in it)
	 */
	// Probably can be replaced by PropertyUtils.getSimpleProperty() from Commons BeanUtils
	public Field getFieldNoTraversing(Class rootClass, String attributeName) {
		if (attributeName.indexOf('.')>-1) {
			throw new YadaInvalidValueException("Attribute name expected, attribute path found: {}", attributeName);
		}
		Field field = null;
		while (field==null && rootClass!=null) {
			try {
				field = rootClass.getDeclaredField(attributeName);
			} catch (NoSuchFieldException e) {
				rootClass = rootClass.getSuperclass();
			}
		}
		return field;
	}

	/**
	 * Reflection to get the type of a given field, even nested or in a superclass.
	 * @param rootClass
	 * @param attributePath field name like "surname" or even a path like "friend.name"
	 * @return
	 * @throws NoSuchFieldException if the field is not found in the class hierarchy
	 * @throws SecurityException
	 */
	public Class getType(Class rootClass, String attributePath) throws NoSuchFieldException, SecurityException {
		if (StringUtils.isBlank(attributePath)) {
			return rootClass;
		}
		String attributeName = StringUtils.substringBefore(attributePath, ".");
		Field field = null;
		NoSuchFieldException exception = null;
		while (field==null && rootClass!=null) {
			try {
				field = rootClass.getDeclaredField(attributeName);
			} catch (NoSuchFieldException e) {
				if (exception==null) {
					exception=e;
				}
				rootClass = rootClass.getSuperclass();
				// TODO sometimes the attribute is not in the superclass but in the subclass. How do we get that?
			}
		}
		if (field==null) {
			if (exception!=null) {
				throw exception;
			} else {
				throw new NoSuchFieldException("No field " + attributeName + " found in hierarchy");
			}
		}
		Class attributeType = field.getType();
		// If it's a list, look for the list type
		if (java.util.List.class.equals(attributeType) || java.util.Map.class.equals(attributeType)) {
			// TODO check if the attributeType is an instance of java.util.Collection
			ParameterizedType parameterizedType = (ParameterizedType)field.getGenericType();
			if (parameterizedType!=null) {
				Type[] types = parameterizedType.getActualTypeArguments();
				if (types.length>0) {
					if (java.util.Map.class.equals(attributeType)) {
						// For maps, skip the key path element
						attributePath = StringUtils.substringAfter(attributePath, ".");
					}
					// For Maps, we get the type of the value
					attributeType = (Class<?>) types[types.length-1];
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
	public static String getMessage(String key, @Nullable Object ... params) {
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
			throw new YadaInternalException("Class not implemented: " + simpleClassName);
		} catch (Exception e) {
			log.error("Instantiation error for class {}", objectClass, e);
			throw new YadaInternalException("Error while creating instance of " + fullClassName);
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
	 * @param args constructor arguments, can be null or not present
	 * @return
	 */
	public static <T> T getBean(Class<T> beanClass, Object ... args) {
		String beanName = StringUtils.uncapitalize(beanClass.getSimpleName());
		return (T) getBean(beanName, args);
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
		log.debug("No applicationContext injected in getBean() yet - returning null for '{}'", nameInApplicationContext);
		return null;
	}

	/**
	 * Get any bean defined in the Spring ApplicationContext
	 * @param nameInApplicationContext the Class.getSimpleName() starting lowercase, e.g. "processController"
	 * @return
	 */
	public static Object getBean(String nameInApplicationContext) {
		if (applicationContext!=null) {
			return applicationContext.getBean(nameInApplicationContext);
		}
		log.debug("No applicationContext injected in getBean() yet - returning null for '{}'", nameInApplicationContext);
		return null;
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
	 * Delete all files in a folder that have the specified prefix
	 * @param folder
	 * @param prefix
	 * @return true if all files have been deleted, false if at least one file has not been deleted
	 */
	public boolean deleteAll(File folder, String prefix) {
		File[] files = folder.listFiles((dir1, name) -> (prefix == null || name.startsWith(prefix)));
		if (files == null) {
			throw new YadaInvalidUsageException("Not a folder or I/O error while deleting files in {}", folder);
        }
		boolean deletedAll = true;
		for (File file : files) {
            try {
				if (!file.delete()) {
					log.debug("File {} not deleted", file);
					deletedAll = false;
				}
			} catch (Exception e) {
				log.debug("File {} not deleted: " + e.getMessage(), file);
				deletedAll = false;
			}
        }
		return deletedAll;
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
				log.debug("File {} not deleted: " + e.getMessage(), file);
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
	 * The folder itself is not removed.
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
	 * Removes files from a folder starting with the prefix (can be an empty string) and older than the given date.
	 * The folder itself is not removed.
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
	 * Returns the file name given the file path
	 * @param fileWithPath
	 * @return
	 */
	public String getFileNoPath(String fileWithPath) {
		if (StringUtils.trimToNull(fileWithPath)==null) {
			return null;
		}
		File file = new File(fileWithPath);
		return file.getName();
	}

	/**
	 * Splits a filename in the prefix and the extension parts. If there is no extension, the second array cell is the empty string
	 * @param filename
	 * @return an array with [ filename without extension, extension without dot]
	 */
	public static String[] splitFileNameAndExtension(String filename) {
		String[] result = new String[] {"", ""};
		if (!StringUtils.isBlank(filename)) {
			int dotpos = filename.lastIndexOf('.');
			if (dotpos>-1) {
				result[0] = filename.substring(0, dotpos);
				if (filename.length()>dotpos+1) {
					result[1] = filename.substring(dotpos+1);
				}
			} else {
				result[0] = filename;
			}
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

	public String getFileExtension(File file) {
		return getFileExtension(file.getName());
	}

	public int shellExec(String command, List<String> args, Map substitutionMap, ByteArrayOutputStream outputStream) throws IOException {
		return shellExec(command, args, substitutionMap, outputStream, 60); // Default timeout is 60 seconds
	}

	/**
	 * Run an external shell command.
	 * @param command the shell command to run, without parameters
	 * @param args optional command line parameters. Can be null for no parameters. Each parameter can have spaces without delimiting quotes.
	 * @param optional substitutionMap key-value of placeholders to replace in the parameters. A placeholder is like ${key}, a substitution
	 * pair is like "key"-->"value". If the value is a collection, arguments are unrolled so key-->collection will result in key0=val0 key1=val1...
	 * @param timeoutSeconds timeout in seconds after which ExecuteException is thrown. Use -1 for default timeout of 60 seconds and 0 for infinite timeout
	 * @param optional outputStream ByteArrayOutputStream that will contain the command output (out + err)
	 * @return the command exit value (maybe not)
	 * @throws org.apache.commons.exec.ExecuteException when the exit value is 1 or the timeout is triggered
	 * @throws IOException
	 */
	public int shellExec(String command, List<String> args, Map substitutionMap, ByteArrayOutputStream outputStream, int timeoutSeconds) throws IOException {
		if (outputStream==null) {
			// The outputstream is needed so that execution does not block. Will be discarded.
			outputStream = new ByteArrayOutputStream();
		}
		timeoutSeconds = timeoutSeconds<0?60:timeoutSeconds;
		ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutSeconds==0?ExecuteWatchdog.INFINITE_TIMEOUT:timeoutSeconds*1000);
		try {
			CommandLine commandLine = new CommandLine(command);
			if (args!=null) {
				Pattern keyPattern = Pattern.compile("\\$\\{([^}]+)}"); // ${PARAMNAME}
				for (String arg : args) {
					boolean added=false;
					// Convert collections to multiple arguments when needed
					if (substitutionMap!=null) {
						Matcher m = keyPattern.matcher(arg);
						if (m.find()) {
							String key = m.group(1); // PARAMNAME
							Object values = substitutionMap.get(key);
							if (values instanceof Collection) {
								// The parameter had a collection in the substitution map
								int countArg=0;
								added=true;
								for (Object extractedValue : (Collection)values) {
									String newKey = key + countArg; // PARAMNAME0
									String newArg = "${" + newKey  + "}"; // ${PARAMNAME0}
									// The original parameter is replaced with a new indexed parameter and its value is added to the substitution map
									commandLine.addArgument(newArg, false); // Don't handle quoting
									substitutionMap.put(newKey, extractedValue);
									countArg++;
								}
							}
						}
					}
					if (!added) {
						// Add a parameter that didn't have a collection parameter in it
						commandLine.addArgument(arg, false); // Don't handle quoting
					}
				}
			}
			if (log.isDebugEnabled() && substitutionMap!=null) {
				for (Object keyObj : substitutionMap.keySet()) {
					String key = (String) keyObj;
					log.debug("{}={}", key, substitutionMap.get(key));
					if (key.startsWith("{") || key.startsWith("${")) { // Checking { just for extra precaution
						log.error("Invalid substitution {}: should NOT start with ${", key);
					}
				}
			}
			if (substitutionMap!=null) {
				commandLine.setSubstitutionMap(substitutionMap);
			}
			DefaultExecutor executor = new DefaultExecutor();
			// Kill after timeoutSeconds, defaults to 60. 0 means no timeout
			executor.setWatchdog(watchdog);
			// Output and Error go together
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, outputStream);
			executor.setStreamHandler(streamHandler);
			log.debug("Executing shell command: {} {}", command, StringUtils.join(args, " "));
			int exitValue = executor.execute(commandLine);
			if (exitValue!=0) {
				log.error("Shell command exited with {}", exitValue);
			}
			log.debug("Shell command output: \"{}\"", outputStream.toString());
			return exitValue;
		} catch (IOException e) {
			log.error("Shell command output: \"{}\"", outputStream.toString());
			log.error("Failed to execute shell command: {} {} {}", command, args!=null?args.toArray():"", substitutionMap!=null?substitutionMap:"", e);
			if (watchdog.killedProcess()) {
				log.error("Process was killed by watchdog after {} seconds timeout", timeoutSeconds);
			}
			throw e;
		} finally {
			closeSilently(outputStream); // This may not be needed
		}
	}

	/**
	 * Run an external shell command without keyword substitution in the parameters.
	 * @param command the shell command to run, without parameters
	 * @param args command line literal parameters. Can be null for no parameters. Each parameter can have spaces without delimiting quotes.
	 * @param optional outputStream ByteArrayOutputStream that will contain the command output (out + err)
	 * @return the command exit value
	 * @throws IOException
	 */
	public int shellExec(String command, List<String> args, ByteArrayOutputStream outputStream) throws IOException {
		return shellExec(command, args, null, outputStream);
	}

	private String getExecutable(String shellCommandKey) {
		boolean mac = SystemUtils.IS_OS_MAC;
		boolean linux = SystemUtils.IS_OS_LINUX;
		boolean windows = SystemUtils.IS_OS_WINDOWS;
		String executable = mac ? config.getString(shellCommandKey + "/executable[@mac='true']") :
			linux ? config.getString(shellCommandKey + "/executable[@linux='true']") :
			windows ? config.getString(shellCommandKey + "/executable[@windows='true']") : null;
		if (executable==null) {
			executable = config.getString(shellCommandKey + "/executable[not(@mac) and not(@linux) and not(@windows)]"); // Fallback to generic OS
		}
		return executable;
	}

	/**
	 * Run an external shell command that has been defined in the configuration file.
	 * The command must be as in the following example:
	 * <pre>
 	&lt;imageConvert timeoutseconds="20">
		&lt;executable windows="true">magick&lt;/executable>
		&lt;executable mac="true" linux="true">/usr/local/bin/magick&lt;/executable>
		&lt;arg>convert&lt;/arg>
		&lt;arg>${FILENAMEIN}&lt;/arg>
		&lt;arg>${FILENAMEOUT}&lt;/arg>
	&lt;/imageConvert>
	 * </pre>
	 * Be aware that args can not contain "Commons Configuration variables" because they clash with placeholders as defined below.
	 * See the yadaframework documentation for full syntax.
	 * @param shellCommandKey xpath key of the shell command, e.g. "config/shell/cropImage"
	 * @param substitutionMap optional key-value of placeholders to replace in the parameters. A placeholder is like ${key}, a substitution
	 * pair is like "key"-->"value". If the value is a collection, arguments are unrolled so key-->collection will result in key0=val0 key1=val1...
	 * @return the command exit value
	 * @throws IOException
	 */
	public int shellExec(String shellCommandKey, Map substitutionMap) throws IOException {
		return shellExec(shellCommandKey, substitutionMap, null);
	}

	/**
	 * Run an external shell command that has been defined in the configuration file.
	 * The command must be as in the following example:
	 * <pre>
 	&lt;imageConvert timeoutseconds="20">
		&lt;executable windows="true">magick&lt;/executable>
		&lt;executable mac="true" linux="true">/usr/local/bin/magick&lt;/executable>
		&lt;arg>convert&lt;/arg>
		&lt;arg>${FILENAMEIN}&lt;/arg>
		&lt;arg>${FILENAMEOUT}&lt;/arg>
	&lt;/imageConvert>
	 * </pre>
	 * See the yadaframework documentation for full syntax.
	 * @param shellCommandKey xpath key of the shell command, e.g. "config/shell/cropImage"
	 * @param substitutionMap optional key-value of placeholders to replace in the parameters. A placeholder is like ${key}, a substitution
	 * pair is like "key"-->"value". If the value is a collection, arguments are unrolled so key-->collection will result in key0=val0 key1=val1...
	 * @param outputStream optional ByteArrayOutputStream that will contain the command output (out + err)
	 * @return the command exit value
	 * @throws IOException
	 */
	public int shellExec(String shellCommandKey, Map substitutionMap, ByteArrayOutputStream outputStream) throws IOException {
		String executable = getExecutable(shellCommandKey);
		// NO Need to use getProperty() to avoid interpolation on ${} arguments
		List<String> args = config.getConfiguration().getList(String.class, shellCommandKey + "/arg", null);
		//		Object argsObject = config.getConfiguration().getProperty(shellCommandKey + "/arg");
		//		List<String> args = new ArrayList<>();
		//		if (argsObject!=null) {
		//			if (argsObject instanceof List) {
		//				args.addAll((Collection<? extends String>) argsObject);
		//			} else {
		//				args.add((String) argsObject);
		//			}
		//		}
		//		List<String> interpolatedArgs = new ArrayList<>();
		//		int pos=1;
		//		for (String arg : args) {
		//			String interpolatedArg = arg;
		//			if (arg.contains("${")) {
		//				interpolatedArg = config.getConfiguration().getString(shellCommandKey + "/arg[" + pos + "]");
		//			}
		//		    pos++;
		//			interpolatedArgs.add(interpolatedArg);
		//		}
		Integer timeout = config.getInt(shellCommandKey + "/@timeoutseconds", -1);
		return shellExec(executable, args, substitutionMap, outputStream, timeout);
	}

	/**
	 * Esegue un comando di shell
	 * @param command comando
	 * @param args lista di argomenti (ogni elemento puo' contenere spazi), puo' essere null
	 * @param substitutionMap key-value of placeholders to replace in the command. A placeholder in the command is like ${key}, a substitution
	 * pair is like "key"-->"value" . If the value is a collection, arguments are unrolled.
	 * @param outputStream ByteArrayOutputStream che conterrà l'output del comando (out + err)
	 * @return the error message (will be empty for a return code >0), or null if there was no error
	 */
	@Deprecated // use shellExec() instead
	public String exec(String command, List<String> args, Map substitutionMap, ByteArrayOutputStream outputStream) {
		int exitValue=1;
		ExecuteWatchdog watchdog = new ExecuteWatchdog(60000); // Kill after 60 seconds
		try {
			CommandLine commandLine = new CommandLine(command);
			if (args!=null) {
				Pattern keyPattern = Pattern.compile("\\$\\{([^}]+)}"); // ${PARAMNAME}
				for (String arg : args) {
					boolean added=false;
					// Convert collections to multiple arguments when needed
					if (substitutionMap!=null) {
						Matcher m = keyPattern.matcher(arg);
						if (m.find()) {
							String key = m.group(1); // PARAMNAME
							Object values = substitutionMap.get(key);
							if (values instanceof Collection) {
								int countArg=0;
								added=true;
								for (Object extractedValue : (Collection)values) {
									String newKey = key + countArg; // PARAMNAME0
									String newArg = "${" + newKey  + "}"; // ${PARAMNAME0}
									commandLine.addArgument(newArg, false); // Don't handle quoting
									substitutionMap.put(newKey, extractedValue);
									countArg++;
								}
							}
						}
					}
					if (!added) {
						commandLine.addArgument(arg, false); // Don't handle quoting
					}
				}
			}
			if (log.isDebugEnabled()) {
				for (Object keyObj : substitutionMap.keySet()) {
					String key = (String) keyObj;
					log.debug("{}={}", key, substitutionMap.get(key));
					if (key.startsWith("{") || key.startsWith("${")) { // Checking { just for extra precaution
						log.error("Invalid substitution {}: should NOT start with ${", key);
					}
				}
			}
			commandLine.setSubstitutionMap(substitutionMap);
			DefaultExecutor executor = new DefaultExecutor();
			executor.setWatchdog(watchdog);
			// Output and Error go together
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, outputStream);
			executor.setStreamHandler(streamHandler);
			log.debug("Executing shell command: {}", StringUtils.join(commandLine, " "));
			exitValue = executor.execute(commandLine);
		} catch (Exception e) {
			log.error("Failed to execute shell command: " + command + " " + args, e);
			String message = e.getMessage();
			if (watchdog.killedProcess()) {
				log.error("Processed killed by watchdog for timeout after 60 seconds");
				message += " - timeout after 60 seconds";
			}
			return message;
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
	@Deprecated // use shellExec() instead
	public String exec(String command, List<String> args, ByteArrayOutputStream outputStream) {
		int exitValue=1;
		try {
			CommandLine commandLine = new CommandLine(command);
			if (args!=null) {
				for (String arg : args) {
					commandLine.addArgument(arg, false); // Don't handle quoting
				}
			}
			DefaultExecutor executor = new DefaultExecutor();
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
			executor.setStreamHandler(streamHandler);
			log.debug("Executing shell command: {}", commandLine);
			if (args!=null) {
				log.debug("Command args: {}", args.toArray());
			}
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
	 * @param substitutionMap key-value of placeholders to replace in the command. A placeholder in the command is like ${key}, a substitution
	 * pair is like "key"-->"value"
	 * @return true if successful
	 */
	@Deprecated // use shellExec() instead
	public boolean exec(String shellCommandKey, Map substitutionMap) {
		String executable = getExecutable(shellCommandKey);
		// Need to use getProperty() to avoid interpolation on ${} arguments
		// List<String> args = config.getConfiguration().getList(String.class, shellCommandKey + "/arg", null);
		List<String> args = (List<String>) config.getConfiguration().getProperty(shellCommandKey + "/arg");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			String error = exec(executable, args, substitutionMap, outputStream);
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

	/**
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

	/**
	 * Cuts the input string at the given length, optionally keeping the whole last word and adding some dots at the end
	 * @param string text to shorten
	 * @param length characters to keep starting from the beginning
	 * @param breakAtWord true to keep the whole last word, false to eventually cut it
	 * @return
	 */
	public String abbreviate(String string, int length, boolean breakAtWord) {
		return abbreviate(string, length, breakAtWord, null);
	}

	/**
	 * Cuts the input string at the given length, optionally keeping the whole last word and adding some characters at the end
	 * @param string text to shorten
	 * @param length characters to keep starting from the beginning
	 * @param breakAtWord true to keep the whole last word, false to eventually cut it
	 * @param ellipsis the characters to add at the end, use null for " [...]"
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
	 * Copy (clone) an object via getter/setter.
	 * The object must implement CloneableFiltered and optionally state what fields should be excluded and left null.
	 * The id is always excluded.
	 * All collections are recreated with the same instances unless their classes implement CloneableDeep, in which case the instances are copied with this same method.
	 * All objects are shallow copied unless they implement CloneableDeep, in which case they are copied with this same method.
	 * Map keys are never cloned.
	 *
	 * NOTE: the object doesn't have to be an @Entity, despite the method name
	 * NOTE: collection fields of an @Entity must be initialized to an empty instance in the class, or they won't be cloned
	 * NOTE: any @Entity in the hierarchy is cloned without id and should be explicitly persisted after cloning unless there's PERSIST propagation.
	 * NOTE: this method works quite well and should be trusted to copy even complex hierarchies.
	 * NOTE: a transaction should be active to copy entities with lazy associations
	 *
	 * Questo metodo crea la copia di un oggetto TRAMITE I SUOI GETTER (anche privati), facendo in modo che alcune collection/mappe vengano copiate pur restando indipendenti.
	 * In pratica le collection/mappe sono ricreate come istanze nuove con i medesimi oggetti di quelle originali.
	 * Questo permette di condividere gli oggetti tra le copie, ma di mantenere le associazioni slegate.
	 * Così se copio un Prodotto, mi trovo gli stessi componenti dell'originale (gli Articolo sono gli stessi) ma posso in seguito toglierli/aggiungerli
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
	// why not use SerializationUtils.clone(..) of commons-lang?
	// This is many times slower than writing clone methods by hand on all objects in your object graph.
	// However, for complex object graphs, or for those that don't support deep cloning this can be a simple alternative implementation. Of course all the objects must be Serializable.
	public static Object copyEntity(CloneableFiltered source) {
		return copyEntity(source, false);
	}

	/**
	 *
	 * @param source
	 * @param setFieldDirectly true to copy using fields and not getter/setter
	 * @return
	 */
	public static Object copyEntity(CloneableFiltered source, boolean setFieldDirectly) {
		return copyEntity(source, null, setFieldDirectly);
	}

	/**
	 * Questo metodo crea la copia di un oggetto TRAMITE I SUOI GETTER (anche privati), facendo in modo che alcune collection/mappe vengano copiate pur restando indipendenti.
	 * In pratica le collection/mappe sono ricreate come istanze nuove con i medesimi oggetti di quelle originali.
	 * Questo permette di condividere gli oggetti tra le copie, ma di mantenere le associazioni slegate.
	 * Così se copio un Prodotto mi trovo gli stessi componenti dell'originale (gli Articolo sono gli stessi) ma posso in seguito toglierli/aggiungerli
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
	 * @param classObject class to use to create the new clone when the source is inside a HibernateProxy
	 * @return
	 */
	public static Object copyEntity(CloneableFiltered source, Class classObject) {
		return copyEntity(source, classObject, false);
	}

	public static Object copyEntity(CloneableFiltered source, Class classObject, boolean setFieldDirectly) {
		Map<CloneableFiltered, Object> alreadyCopiedMap = new HashMap<>();
		return copyEntity(source, classObject, setFieldDirectly, alreadyCopiedMap, null);
	}

	/**
	 *
	 * @param source the instance to copy
	 * @param classObject class to use to create the new clone when the source is inside a HibernateProxy
	 * @param setFieldDirectly false to use getter/setter, true to access the Field directly
	 * @param yadaAttachedFileCloneSet when not null, all files are copied to a temp folder.
	 * 	      This is useful when the final path depends on the id
	 * 		  of a cloned object so it can't be determined during cloning.
	 * 		  The method yadaAttachedFileCloneSet.moveAll() will have to be called after the clone has been persisted.
	 * @return
	 */
	public static Object copyEntity(CloneableFiltered source, Class classObject, boolean setFieldDirectly, YadaAttachedFileCloneSet yadaAttachedFileCloneSet) {
		Map<CloneableFiltered, Object> alreadyCopiedMap = new HashMap<>();
		return copyEntity(source, classObject, setFieldDirectly, alreadyCopiedMap, yadaAttachedFileCloneSet);
	}

	private static Object copyEntity(CloneableFiltered source, Class classObject, boolean setFieldDirectly, Map<CloneableFiltered, Object> alreadyCopiedMap, YadaAttachedFileCloneSet yadaAttachedFileCloneSet) {
		if (source==null) {
			return null;
		}
		Class<?> sourceClass = source.getClass();

		// Needed to avoid infinite recursion if a value holds a reference to the parent
		Object alreadyCopied = alreadyCopiedMap.get(source);
		if (alreadyCopied!=null) {
			log.debug("Reusing already copied object {}", alreadyCopied);
			return alreadyCopied;
		}

		try {
			// The constructor may be private, so don't just use newInstance()
	        // Object target = sourceClass.newInstance();
			Constructor<?> constructor = sourceClass.getDeclaredConstructor(new Class[0]);
	        constructor.setAccessible(true);
			Object target = constructor.newInstance(new Object[0]);
			if(target instanceof org.hibernate.proxy.HibernateProxy && classObject!=null) {
				target = classObject.newInstance();
			}

			alreadyCopiedMap.put(source, target); // Needed to avoid infinite recursion if a value holds a reference to the parent
			copyFields(source, sourceClass, target, setFieldDirectly, alreadyCopiedMap, yadaAttachedFileCloneSet);
			Class<?> superclass = sourceClass.getSuperclass();
			while (superclass!=null && superclass!=Object.class) {
				sourceClass = superclass;
				copyFields(source, sourceClass, target, setFieldDirectly, alreadyCopiedMap, yadaAttachedFileCloneSet);
				superclass = sourceClass.getSuperclass();
			}
			if (isType(sourceClass, YadaAttachedFile.class)) {
				// Also copy files on disk for YadaAttachedFiles
				target = yadaFileManager.duplicateFiles((YadaAttachedFile) target, yadaAttachedFileCloneSet);
			}
			
			return target;
		} catch (Exception e) {
			String msg = "Can't duplicate object '" + source + "'";
			log.error(msg + ": " + e);
			throw new YadaInternalException(msg, e);
		}
	}

	/**
	 * Shallow copy a value either by getter or by field
	 * @param setFieldDirectly
	 * @param field
	 * @param getter
	 * @param setter
	 * @param source object containing the value to copy
	 * @param target object where to copy the value
	 * @param args optional values to set on the target. When empty, the value is taken from the source.
	 */
	private static void copyValueShallow(boolean setFieldDirectly, Field field, Method getter, Method setter, Object source, Object target, Object... args) {
		try {
			if (setFieldDirectly) {
				if (args.length==0) {
					field.set(target, field.get(source));
				} else {
					field.set(target, args);
				}
			} else {
				if (args.length==0) {
					setter.invoke(target, getter.invoke(source));
				} else {
					setter.invoke(target, args);
				}
			}
		} catch (Exception e) {
			log.debug("Failed to set field {}", field.getName());
		}
	}

	private static void copyProvidedValue(boolean setFieldDirectly, Field field, Method getter, Method setter, Object newValue, Object target) {
		try {
			if (setFieldDirectly) {
				field.set(target, newValue);
			} else {
				setter.invoke(target, newValue);
			}
		} catch (Exception e) {
			log.debug("Failed to set field {}", field.getName());
		}
	}

//	private static void copyFields(CloneableFiltered source, Class<?> sourceClass, Object target) {
//		copyFields(source, sourceClass, target, false);
//	}

	/**
	 * Copy all (not-excluded) fields from the source object to the target clone. 
	 * @param source the object to get fields from
	 * @param sourceClass
	 * @param target the object to copy fields to
	 * @param setFieldDirectly true to bypass the use of getter/setter
	 * @param alreadyCopiedMap holds all already-cloned objects in order to avoid loops
	 * @param yadaAttachedFileCloneSet holds all the cloned YadaAttachedFile objects for later copying files on disk
	 */
	private static void copyFields(CloneableFiltered source, Class<?> sourceClass, Object target, boolean setFieldDirectly, Map<CloneableFiltered, Object> alreadyCopiedMap, YadaAttachedFileCloneSet yadaAttachedFileCloneSet) {
		log.debug("Cloning object {} of type {}", getObjectToString(source), sourceClass);
		Field[] fields = sourceClass.getDeclaredFields();
		// Excluded fields are totally ignored and will be either null or zero (or whatever the default is) in the target object
		Field[] excludedFields = source.getExcludedFields(); // See CloneableFiltered.java
		List<Field>filteredFields = excludedFields!=null? (List<Field>) Arrays.asList(excludedFields):new ArrayList<>();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			field.setAccessible(true);
			// Do not copy a field annotated with @YadaCopyNot
			boolean skipField = field.isAnnotationPresent(YadaCopyNot.class);
			// Also don't copy a static or final field
			int modifiers = field.getModifiers();
			skipField |= Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers);
			// Also don't copy id of entities
			skipField |= "id".equals(field.getName()) && sourceClass.isAnnotationPresent(Entity.class);
			if (skipField || filteredFields.contains(field)) {
				log.debug("Skipping field {}", field.getName());
				continue; // Skip the filtered fields
			}
			log.debug("Copying field {}", field.getName());
			boolean copyShallow = field.isAnnotationPresent(YadaCopyShallow.class);
			try {
				// Retrieve public getter/setter methods
				Class<?> fieldType = field.getType();
				String prefix = (fieldType==boolean.class || fieldType==Boolean.class)?"is":"get";
				String getterName = prefix + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
				String setterName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
				try {
					Method getter = null;
					Method setter = null;
					if (!setFieldDirectly) {
						try{
							getter = sourceClass.getDeclaredMethod(getterName);
						} catch(NoSuchMethodException exc){
							//per i boolean posso avere il getXXXX anzichè l' isXXXXXXX
							if  (fieldType==boolean.class || fieldType==Boolean.class){
								getterName = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
								getter = sourceClass.getDeclaredMethod(getterName);
							} else {
									throw exc;
							}
						}
						getter.setAccessible(true);
						setter = sourceClass.getDeclaredMethod(setterName, fieldType);
						setter.setAccessible(true);
					}

					if (copyShallow 
							 || fieldType.isPrimitive()
							 || fieldType==Boolean.class
							 || fieldType==Integer.class
							 || fieldType==Long.class
							 || fieldType==Byte.class
							 || fieldType==Character.class
							 || fieldType==Short.class
							 || fieldType==Float.class
							 || fieldType==Double.class
							) {
						Object[] clonedAncestor = new Object[]{}; 
						if (copyShallow) {
							// Check if the shallow value to copy has already been cloned, in which case we use the clone.
							// This allows cloned children to attach to the cloned parent instead of the original parent.
							// It works for parent of parent etc. at any level.
							Object sourceFieldValue = setFieldDirectly ? field.get(source) : getter.invoke(source);
							Object clonedFieldValue = alreadyCopiedMap.get(sourceFieldValue);
							if (clonedFieldValue!=null) {
								clonedAncestor = new Object[]{clonedFieldValue};
							}
						}
						// Just copy
						copyValueShallow(setFieldDirectly, field, getter, setter, source, target, clonedAncestor);
//						setter.invoke(target, getter.invoke(source));
					} else {
						if (isType(fieldType, Collection.class)) {
							// E' una collection, quindi copio solo i contenuti.
							// E' importante che il costruttore del target abbia istanziato il field con una collection vuota.
							// Collection sourceCollection = (Collection) getter.invoke(source); // La collection di partenza, serve per i contenuti
							// Collection targetCollection = (Collection) getter.invoke(target); // La collection di destinazione da riempire
							Collection sourceCollection = setFieldDirectly ? (Collection) field.get(source) : (Collection) getter.invoke(source);
							Collection targetCollection = setFieldDirectly ? (Collection) field.get(target) : (Collection) getter.invoke(target);

							if (targetCollection==null) {
								boolean invalid = false;
								try {
									Class sourceCollectionClass = sourceCollection.getClass();
									if (sourceCollectionClass.getTypeName().startsWith("org.hibernate.collection")) {
										invalid = true;
									} else {
										targetCollection = (Collection) sourceCollectionClass.newInstance();
									}
								} catch (Exception e) {
									log.error("Can't clone collection", e);
									invalid = true;
								}
								if (invalid) {
									if (setFieldDirectly) {
										throw new YadaInvalidUsageException("The field '{}' on a new instance of {} should not be null but should be an empty collection for cloning", field.getName(), sourceClass);
									}
									throw new YadaInvalidUsageException("The getter of '{}' on a new instance of {} should not return null but an empty collection for cloning", field.getName(), sourceClass);
								}
								copyValueShallow(setFieldDirectly, field, getter, setter, source, target, targetCollection);
								// The getter should have returned a new empty instance.
								// We could
//								targetCollection = new ArrayList();
							}
							// Faccio la copia shallow di tutti gli elementi che non implementano CloneableDeep;
							// per questi faccio la copia deep.
							for (Object value : sourceCollection) {
								if (isType(value.getClass(), CloneableDeep.class)) {
									Object clonedValue = YadaUtil.copyEntity((CloneableFiltered) value, null, false, alreadyCopiedMap, yadaAttachedFileCloneSet); // deep
									// For YadaAttachedFile objects, duplicate the file on disk too
									// if (isType(value.getClass(), YadaAttachedFile.class)) {
									// 	clonedValue = yadaFileManager.duplicateFiles((YadaAttachedFile) clonedValue, yadaAttachedFileCloneSet);
									// }
									int previousSize = targetCollection.size();
									targetCollection.add(clonedValue);
									if (previousSize==targetCollection.size()) {
										// If the target collection didn't grow, it means that it is probably a Set and the element doesn't
										// implement a unique hashCode function: it may be using the id field of an Entity for example, that is null now.
										log.debug("It looks like you should implement a better .equals() and .hashCode() function in {}", value.getClass());
										throw new YadaInvalidUsageException("Cloned collection not growing when cloning: "
											+ "the {}.hashCode() function is returning {}", value.getClass(), clonedValue.hashCode());
									}
								} else {
									targetCollection.add(value); // shallow
								}
							}
//									targetCollection.addAll(sourceCollection);
						} else if (isType(fieldType, Map.class)) {
							Map sourceMap = setFieldDirectly ? (Map) field.get(source) : (Map) getter.invoke(source);
							Map targetMap = setFieldDirectly ? (Map) field.get(target) : (Map) getter.invoke(target);
							if (targetMap==null) {
								// Se il costruttore non istanzia la mappa, ne creo una arbitrariamente di tipo HashMap
								targetMap = new HashMap();
								copyValueShallow(setFieldDirectly, field, getter, setter, source, target, targetMap);
//								setter.invoke(target, targetMap);
							}
							// Faccio la copia shallow di tutti gli elementi che non implementano CloneableDeep;
							// per questi faccio la copia deep.
							if (sourceMap!=null) {
								for (Object key : sourceMap.keySet()) {
									Object value = sourceMap.get(key);
									if (isType(value.getClass(), CloneableDeep.class)) {
										Object clonedValue = YadaUtil.copyEntity((CloneableFiltered) value, null, false, alreadyCopiedMap, yadaAttachedFileCloneSet); // deep
										// For YadaAttachedFile objects, duplicate the file on disk too
										// if (isType(value.getClass(), YadaAttachedFile.class)) {
										// 	clonedValue = yadaFileManager.duplicateFiles((YadaAttachedFile) clonedValue, yadaAttachedFileCloneSet);
										// }
										targetMap.put(key, clonedValue);
									} else {
										targetMap.put(key, value); // shallow
									}
								}
							}
//							targetMap.putAll(sourceMap);
						} else {
							// No collection nor map
							Object fieldSourceValueObject = setFieldDirectly ? field.get(source) : getter.invoke(source);
							Object fieldTargetValueObject = setFieldDirectly ? field.get(target) : getter.invoke(target);
							if (fieldSourceValueObject==null && fieldTargetValueObject==null) {
								continue; // Prevent possible errors later
							}
							if (isType(fieldType, CloneableDeep.class)) {
								// Deep copy
								CloneableFiltered fieldValue = (CloneableFiltered) fieldSourceValueObject;
								Object clonedValue = YadaUtil.copyEntity(fieldValue, null, setFieldDirectly, alreadyCopiedMap, yadaAttachedFileCloneSet); // deep but detached
								// For YadaAttachedFile objects, duplicate the file on disk too
								// if (isType(fieldType, YadaAttachedFile.class)) {
								// 	clonedValue = yadaFileManager.duplicateFiles((YadaAttachedFile) clonedValue, yadaAttachedFileCloneSet);
								// }
								copyValueShallow(setFieldDirectly, field, getter, setter, source, target, clonedValue);
							} else if (isType(fieldType, StringBuilder.class)) {
								// String builder/buffer is cloned otherwise changes to the original object would be reflected in the new one
								StringBuilder fieldValue = (StringBuilder) fieldSourceValueObject;
								StringBuilder fieldClone = new StringBuilder(fieldValue.toString());
								copyProvidedValue(setFieldDirectly, field, getter, setter, fieldClone, target);
							} else if (isType(fieldType, StringBuffer.class)) {
								// String builder/buffer is cloned otherwise changes to the original object would be reflected in the new one
								StringBuffer fieldValue = (StringBuffer) fieldSourceValueObject;
								StringBuffer fieldClone = new StringBuffer(fieldValue.toString());
								copyProvidedValue(setFieldDirectly, field, getter, setter, fieldClone, target);
							} else {
								// Plain object, just copy the reference (no cloning)
								copyValueShallow(setFieldDirectly, field, getter, setter, source, target);
							}
						}
					}
				} catch (NoSuchMethodException e) {
					// Just skip it
					// Non loggo perché uscirebbe il log anche in casi giusti
				}
			} catch (Exception e) {
				log.error("Can't copy field {} (ignored)", field, e);
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

	private static boolean isTypeNoSuperclass(Class<?> fieldType, Class<?> requiredType) {
		if (fieldType==null) {
			return false;
		}
		if (fieldType.equals(requiredType)) {
			return true;
		}
		Class<?>[] interfaces = fieldType.getInterfaces();
		for (Class<?> iface : interfaces) {
			if (isTypeNoSuperclass(iface, requiredType)) {
				return true;
			}
		}
		return false;
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

	/**
	 * Check if a date is within two dates expressed as month/day, regardless of the year and of the validity of such dates.
	 * @param dateToCheck for example new GregorianCalendar()
	 * @param fromMonth 0-based, better use Calendar.JANUARY etc.
	 * @param fromDayInclusive 1-based
	 * @param toMonth 0-based, better use Calendar.JANUARY etc.
	 * @param toDayExcluded 1-based
	 * @return
	 */
	public static boolean dateWithin(Calendar dateToCheck, int fromMonth, int fromDayInclusive, int toMonth, int toDayExcluded) {
		if (fromMonth<0 || fromMonth>11) {
			throw new YadaInvalidUsageException("Month must be in the range 0-11");
		}
		if (toMonth<0 || toMonth>11) {
			throw new YadaInvalidUsageException("Month must be in the range 0-11");
		}
		if (fromDayInclusive<1 || fromDayInclusive>31) {
			throw new YadaInvalidUsageException("Day must be in the range 1-31");
		}
		if (toDayExcluded<1 || toDayExcluded>31) {
			throw new YadaInvalidUsageException("Day must be in the range 1-31");
		}
		boolean sameYear = fromMonth<=toMonth;
		int monthToCheck = dateToCheck.get(Calendar.MONTH);
		if (sameYear && (monthToCheck<fromMonth || monthToCheck>toMonth)) {
			return false;
		}
		if (!sameYear && (monthToCheck<fromMonth && monthToCheck>toMonth)) {
			return false;
		}
		// The month is within range, keep checking...
		int dayToCheck = dateToCheck.get(Calendar.DAY_OF_MONTH);
		if ((monthToCheck==fromMonth && dayToCheck<fromDayInclusive) || (monthToCheck==toMonth && dayToCheck>=toDayExcluded)) {
			return false;
		}
		return true;
	}

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
	 * Returns the days between two dates.
	 * It doesn't take into consideration the time component, so the difference between some time yesterday and any other time today
	 * will always be 1
	 * @param olderDate
	 * @param earlierDate
	 * @return
	 * @see ZonedDateTime#until(java.time.temporal.Temporal, java.time.temporal.TemporalUnit)
	 */
	public long daysBetween(ZonedDateTime olderDate, ZonedDateTime earlierDate) {
		ZonedDateTime olderDateBack = olderDate.truncatedTo(ChronoUnit.DAYS);
		ZonedDateTime earlierDateBack = earlierDate.truncatedTo(ChronoUnit.DAYS);
		return ChronoUnit.DAYS.between(olderDateBack, earlierDateBack);
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
	 * Returns the minutes between two dates.
	 * It is negative when the first argument is earlier than the second.
	 * @param recentDate
	 * @param oldDate
	 * @return
	 */
	public static long minutesDifference(Date recentDate, Date oldDate) {
		return (recentDate.getTime()-oldDate.getTime()) / MILLIS_IN_MINUTE;
	}

	/**
	 * Returns the absolute value of the minutes between two dates.
	 * It will always be positive.
	 * @param firstDate
	 * @param secondDate
	 * @return
	 */
	public static long minutesDifferenceAbs(Date firstDate, Date secondDate) {
		return Math.abs(firstDate.getTime()-secondDate.getTime()) / MILLIS_IN_MINUTE;
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
	 * @param calendar the calendar to change: the parameter will be modified by this method
	 * @return the input calendar modified.
	 */
	public static Calendar roundBackToMidnight(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}

	/**
	 * Returns the last midnight
	 * @return
	 */
	public static Calendar getLastMidnight() {
		return YadaUtil.roundBackToMidnight(new GregorianCalendar());
	}

	/**
	 * Adds or removes the days. The original object is cloned.
	 * @param calendar
	 * @param days
	 * @return
	 */
	public static Calendar addDaysClone(Calendar source, int days) {
		return addDays((Calendar) source.clone(), days);
	}

	/**
	 * Adds or removes the days. The original object is modified.
	 * @param calendar
	 * @param days
	 * @return
	 */
	public static Calendar addDays(Calendar calendar, int days) {
		calendar.add(Calendar.DAY_OF_YEAR, days);
		return calendar;
	}

	/**
	 * Adds or removes the minutes. The original object is modified.
	 * @param calendar
	 * @param minutes
	 * @return
	 */
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

	/**
	 * Create a zip of a set of files using an external process. The process must be configured as "config/shell/zipWithRename"
	 * and should use zip and zipnote (for renaming). See the /YadaWeb/scripts folder for an example.
	 * @param zipFile the zip file that has to be created
	 * @param sourceFiles the files to add to the zip
	 * @param filenames optional names to give to each added file, in order
	 * @param fixNames when true, any repeated name will be given an incremental number (regardless or renaming)
	 * 		  and if filenames is provided, the renamed file will be forced to have the same
	 * 		  extension of the source file (existing extensions will be removed).
	 * @return true if the zip file has been created
	 * @throws IOException
	 * @throws YadaInvalidUsageException when the length of filenames is greater than zero but different from the length of sourceFiles
	 */
	public boolean createZipProcess(File zipFile, File[] sourceFiles, String[] filenames, boolean fixNames) throws IOException {
		if (filenames!=null && filenames.length>0 && filenames.length!=sourceFiles.length) {
			throw new YadaInvalidUsageException("When provided, there must be as many filenames as source files");
		}
		Map<String, String> params = new HashMap<>();
		String shellCommandKey = "config/shell/zipWithRename";
		File folder = zipFile.getParentFile(); // We create all temporary files in the same folder of the target zip
		File tempZip = java.nio.file.Files.createTempFile(folder.toPath(), "_tmp_", ".zip").toFile();
		// The zip file must not exist yet, so delete it
		tempZip.delete();
		File tempRename = java.nio.file.Files.createTempFile(folder.toPath(), "_tmp_", ".txt").toFile();
		// String sourceNames = Arrays.stream(sourceFiles).map(File::getAbsolutePath).collect(Collectors.joining(" "));

		// Create the rename file and the source names list
		Set<String> addedFilenames = new HashSet<>();
		StringBuilder sourceNames = new StringBuilder();
		try (BufferedWriter renameWriter = new BufferedWriter(new FileWriter(tempRename))) {
			for (int i=0; i<sourceFiles.length; i++) {
				File sourceFile = sourceFiles[i];
				if (sourceFile!=null && sourceFile.canRead()) {
					sourceNames.append(sourceFile.getAbsolutePath()).append(" ");
					String sourceFilename = sourceFile.getName();
					String sourceExtensionNoDot = getFileExtension(sourceFilename); // jpg
					// When there is no filenames and no fixNames, the target file name is the same as the source file name
					String targetName = sourceFilename;
					if (filenames!=null) {
						// When new names are provided, the target file name is the provided name
						targetName = filenames[i];
					}
					if (fixNames) {
						// Make names unique and use source extension as target
						String targetNameNoExtension = splitFileNameAndExtension(targetName)[0];
						targetName = findAvailableFilename(targetNameNoExtension, sourceExtensionNoDot, "_", addedFilenames);
					}
					// zipnote format
					renameWriter.append("@ "+sourceFilename+"\n");
					renameWriter.append("@="+targetName+"\n");
					renameWriter.append("@ (comment above this line)\n");
				} // sourceFile!=null
			}
		} // try

		params.put("ZIPFILE", tempZip.getAbsolutePath());
		params.put("ZIPNOTEFILE", tempRename.getAbsolutePath());
		params.put("FILES", sourceNames.toString());
		try {
			int returnCode = shellExec(shellCommandKey, params, null);
			if (returnCode!=0) {
				log.error("Failed to create zip file {} from {}: return code is {}", tempZip, sourceFiles, returnCode);
				tempZip.delete();
				tempRename.delete();
				return false;
			}
		} catch (Exception e) {
			log.error("Failed to create zip file {} from {}", tempZip, sourceFiles);
			tempZip.delete();
			tempRename.delete();
			throw e;
		}
		// Move the zip back into the intended place
		Files.move(tempZip.toPath(), zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		// Delete the zipnote file
		tempRename.delete();
		return true;
	}

	/**
	 * Create a zip file of a folder
	 * @param zipFile the target zip file
	 * @param foldersToZip the folders to zip
	 * @throws IOException
	 */
	public void createZipFileFromFolders(File zipFile, File[] foldersToZip) throws IOException {
		try (ZipOutputStream zs = new ZipOutputStream(new FileOutputStream(zipFile))) {
			for (int i = 0; i < foldersToZip.length; i++) {
				Path parentOfFolder = foldersToZip[i].getParentFile().toPath();
				Path pathToZip = foldersToZip[i].toPath();
				Files.walk(pathToZip).filter(path -> !Files.isDirectory(path)).forEach(path -> {
					String pathInZip = parentOfFolder.relativize(path).toString();
					log.debug("Zipping {} with path {}", path, pathInZip);
					ZipEntry zipEntry = new ZipEntry(pathInZip);
					try {
						zs.putNextEntry(zipEntry);
						zs.write(Files.readAllBytes(path));
						zs.closeEntry();
					} catch (Exception e) {
						log.error("Can't create zip file", e);
						throw new YadaSystemException("Can't create zip file", e);
					}
				});
			}
		}
	}

	/**
	 * Create a zip of a list of files.
	 * An exception is thrown when a source file is not readable.
	 * Adapted from http://www.exampledepot.com/egs/java.util.zip/CreateZip.html
	 * @param zipFile zip file to create
	 * @param sourceFiles files to zip
	 * @param filenamesNoExtension optional list of names to give to zip entries. The name extension is also optional: it will be taken from the source file
	 */
	public void createZipFile(File zipFile, File[] sourceFiles, String[] filenamesNoExtension) {
		createZipFile(zipFile, sourceFiles, filenamesNoExtension, false);
	}

	/**
	 * Create a zip of a list of files.
	 * Adapted from http://www.exampledepot.com/egs/java.util.zip/CreateZip.html
	 * @param zipFile zip file to create
	 * @param sourceFiles files to zip
	 * @param filenamesNoExtension optional list of names to give to zip entries. The name extension is also optional: it will be taken from the source file
	 * @param ignoreErrors true to ignore a file error and keep going with the next file
	 */
	public void createZipFile(File zipFile, File[] sourceFiles, String[] filenamesNoExtension, boolean ignoreErrors) {
		byte[] buf = new byte[1024]; // Create a buffer for reading the files
		// Create the ZIP file
		Set<String> addedFilenames = new HashSet<>();
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
		    // Compress the files
		    for (int i=0; i<sourceFiles.length; i++) {
		    	if (sourceFiles[i]!=null && sourceFiles[i].canRead()) {
			        // Add ZIP entry to output stream.
			        String entryName;
			        if (filenamesNoExtension!=null) {
			        	// The filenamesNoExtension array should not contain the extensions, but we check just in case
			        	String targetNameNoExtensionMaybe = filenamesNoExtension[i];
			        	String extensionNoDot = getFileExtension(sourceFiles[i]); // jpg
			        	String sourceExtension = "." + extensionNoDot; // Extension of the file to zip, e.g. ".jpg"
			        	boolean targeHasExtension = targetNameNoExtensionMaybe.toLowerCase().endsWith(sourceExtension);
			        	String targetNameNoExtension = targeHasExtension ? splitFileNameAndExtension(targetNameNoExtensionMaybe)[0] : targetNameNoExtensionMaybe;
			        	// Add a counter for duplicated names
			        	entryName = findAvailableFilename(targetNameNoExtension, extensionNoDot, "_", addedFilenames);
			        } else {
			        	String[] filenameAndExtension = splitFileNameAndExtension(sourceFiles[i].getName());
			        	// Add a counter for duplicated names
			        	entryName = findAvailableFilename(filenameAndExtension[0], filenameAndExtension[1], "_", addedFilenames);
			        }
			        try (FileInputStream in = new FileInputStream(sourceFiles[i])) {
						out.putNextEntry(new ZipEntry(entryName));
						// Transfer bytes from the file to the ZIP file
						int len;
						while ((len = in.read(buf)) > 0) {
						    out.write(buf, 0, len);
						}
						// Complete the entry
						out.closeEntry();
					} catch (Exception e) {
						if (!ignoreErrors) {
							log.error("Error while adding file {} to zip {}" , entryName, zipFile.getAbsolutePath(), e);
							throw e;
						} else {
							log.debug("Error while adding file {} to zip {}" , entryName, zipFile.getAbsolutePath(), e);
						}
					}
		    	}
		    }
		} catch (IOException e) {
			log.error("Can't create zip file", e);
			throw new YadaSystemException("Can't create zip file", e);
		}
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

	@Deprecated // To be removed from Yada Framework
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

	@Deprecated // To be removed from Yada Framework
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
		SortedSet<Entry<String,String>> result = new TreeSet<>(new Comparator<Entry<String, String>>() {
			@Override
			public int compare(Entry<String, String> element1, Entry<String, String> element2) {
				return element1.getValue().compareTo(element2.getValue());
			}
		});
		try {
			result.addAll(data.entrySet());
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
		SortedSet<Entry<String,String>> result = new TreeSet<>(new Comparator<Entry<String, String>>() {
			@Override
			public int compare(Entry<String, String> element1, Entry<String, String> element2) {
				return element1.getKey().compareTo(element2.getKey());
			}
		});
		try {
			result.addAll(data.entrySet());
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
	 * @deprecated Does not produce the same results of all OS
	 * @see #ensureSafeFilename(String)
	 */
	@Deprecated // Does not produce the same results of all OS
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
	 * @deprecated Does not produce the same results of all OS
	 * @see #ensureSafeFilename(String, boolean)
	 */
	@Deprecated // Does not produce the same results of all OS
	public static String reduceToSafeFilename(String originalFilename, boolean toLowercase) {
		if (originalFilename==null) {
			return "null";
		}
		// If the filename is a path, keep the last portion
		// WARNING: this is wrong because different results are produced on different OS
		int pos = originalFilename.indexOf(File.separatorChar);
		if (pos>-1) {
			try {
				originalFilename = originalFilename.substring(pos+1);
			} catch (Exception e) {
				// The name ends with a separator char
				log.debug("Name is empty");
				return "";
			}
		}
		//originalFilename = YadaWebUtil.removeHtmlStatic(originalFilename);
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


	/**
	 * Converts a candidate filename so that it is valid on all operating systems and browsers, if needed, and also to lowercase.
	 * @param originalFilename the name to process
	 * @return either the lowercase original string or something similar. It returns "noname" when the originalFilename is blank.
	 */
	public String ensureSafeFilename(String originalFilename) {
		return ensureSafeFilename(originalFilename, true);
	}
	
	/**
	 * Converts a candidate filename so that it is valid on all operating systems and browsers, if needed.
	 * @param originalFilename the name to process
	 * @param toLowercase true to convert to lowercase
	 * @return either the original string or something similar. It returns "noname" when the originalFilename is blank.
	 */
	public String ensureSafeFilename(String originalFilename, boolean toLowercase) {
		if (StringUtils.isBlank(originalFilename)) {
			return "noname";
		}
        // Normalize the filename to decompose characters using NFKD
        String normalizedFilename = Normalizer.normalize(originalFilename, Normalizer.Form.NFKD);
        // Remove diacritical marks (accents)
        String withoutDiacritics = normalizedFilename.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Replace invalid characters with underscores
        String safeFilename = withoutDiacritics.replaceAll("[^a-zA-Z0-9._-]", "_");
        // Convert to lowercase if needed
        if (toLowercase) {
            safeFilename = safeFilename.toLowerCase();
        }
        return safeFilename;
	}
}
