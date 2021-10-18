package net.yadaframework.components;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
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
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

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

    static ApplicationContext applicationContext; 	// To access the ApplicationContext from anywhere
    static public MessageSource messageSource; 		// To access the MessageSource from anywhere

    public final static long MILLIS_IN_MINUTE = 60*1000;
	public final static long MILLIS_IN_HOUR = 60*MILLIS_IN_MINUTE;
	public final static long MILLIS_IN_DAY = 24*MILLIS_IN_HOUR;

	private SecureRandom secureRandom = new SecureRandom();
	private final static char CHAR_AT = '@';
	private final static char CHAR_DOT = '.';
	private final static char CHAR_SPACE = ' ';

	private static Locale defaultLocale = null;

	/**
	 * Instance to be used when autowiring is not available
	 */
	public final static YadaUtil INSTANCE = new YadaUtil();

	@PostConstruct
    public void init() {
		defaultLocale = config.getDefaultLocale();
		yadaFileManager = getBean(YadaFileManager.class);
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
	 * Example:
	 * the generic class is public abstract class Shape<T extends Color> {...}
	 * the specific class is public class Circle extends Shape<Red>
	 * the instance is new Circle()
	 * the returned value is Red.class
	 * @param specificClassInstance instance of the specific class, usually "this" when called from inside either the specific or the generic abstract class.
	 * @return the class T used to make the generic specific
	 */
	public Class<?> findGenericClass(Object specificClassInstance) {
		return (Class<?>)((ParameterizedType)specificClassInstance.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
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
	 */
	public File makeTempFolder() throws IOException {
		File file = File.createTempFile("yada", "");
		file.delete();
        file.mkdir();
        return file;
	}

	/**
	 * Finds the folder path between two folders. using forward (unix) slashes as a separator
	 * @param ancestorFolder
	 * @param descendantFolder
	 * @return
	 */
	public String relativize(File ancestorFolder, File descendantFolder) {
		String segment = ancestorFolder.toPath().relativize(descendantFolder.toPath()).toString();
		return segment.replaceAll("\\", "/");
	}

	/**
	 * Finds the folder path between two folders. using forward (unix) slashes as a separator
	 * @param ancestorFolder
	 * @param descendantFolder
	 * @return
	 */
	public String relativize(Path ancestorFolder, Path descendantFolder) {
		String segment = ancestorFolder.relativize(descendantFolder).toString();
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
	 * @param usedNames filenames used so far, must start empty and will be modified
	 * @return the original filename with extension, or a new version with a counter added
	 * @throws IOException
	 * @see {@link #findAvailableName(File, String, String, String)}
	 */
	// TODO remove the IOException and just use a random number on timeout
	public String findAvailableFilename(String baseName, String extensionNoDot, String counterSeparator, Set<String> usedNames) throws IOException {
		counterSeparator = counterSeparator==null?"":counterSeparator;
		String extension = StringUtils.isAllBlank(extensionNoDot) ? "" : "." + extensionNoDot;
		String fullName = baseName + extension;
		int counter = 0;
		long startTime = System.currentTimeMillis();
		int timeoutMillis = 1000; // 1 second to find a result seems to be reasonable
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
			ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			int orientation = 1;
			if (directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
				orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
			}
			ExifSubIFDDirectory directory2 = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			int width = directory2.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH);
			int height = directory2.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT);
			if (orientation==6 || orientation==8) {
				// Image is rotated 90° so dimensions must be swapped
				return new YadaIntDimension(height, width);
			}
			return new YadaIntDimension(width, height);
		} catch (Exception e) {
			log.debug("Error reading EXIF dimensions for {} - fallback to dumb version}", imageFile);
			return getImageDimensionDumb(imageFile);
		}
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
	 * @param instance to autowire
	 * @return the autowired/initialized bean instance, either the original or a wrapped one
	 * @see {@link AutowireCapableBeanFactory#autowireBean(Object)}, {@link AutowireCapableBeanFactory#initializeBean(Object, String)}, {@link #autowire(Object)}
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
	 * @return
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
	 * @param targetClass type of fetchedEntities elements
	 */
	public static <targetClass> void prefetchLocalizedStrings(targetClass fetchedEntity, Class<?> targetClass) {
		if (fetchedEntity!=null) {
			List<targetClass> list = new ArrayList<>();
			list.add(fetchedEntity);
			prefetchLocalizedStringList(list, targetClass);
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
	public static <entityClass> void prefetchLocalizedStringList(List<entityClass> entities, Class<?> entityClass, String...attributes) {
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
	 * @param LocalizedValueMap
	 * @param locale the needed locale for the value, can be null for the current request locale
	 * @return the localized value, or the empty string if no value has been defined and no default locale has been configured
	 */
	public static String getLocalValue(Map<Locale, String> LocalizedValueMap, Locale locale) {
		if (locale==null) {
			locale = LocaleContextHolder.getLocale();
		}
		String result = LocalizedValueMap.get(locale);
		if (StringUtils.isEmpty(result) && defaultLocale!=null && !defaultLocale.equals(locale)) {
			result = LocalizedValueMap.get(defaultLocale);
		}
		return result==null?"":result;
	}

	/**
	 * Deletes a file without reporting any errors.
	 * @param file the file. It could also be an empty foder. Folders containing files are not deleted.
	 */
	public boolean deleteFileSilently(Path file) {
		try {
			return Files.deleteIfExists(file);
		} catch (Throwable e) {
			return false;
		}
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
		log.debug("No applicationContext injected in getBean() yet - returning null");
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
			log.debug("Executing shell command: {}", StringUtils.join(commandLine, " "));
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
	 * Be aware that args can not contain "Commons Configuration variables" because they clash with placeholders as defined below.
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
		// Need to use getProperty() to avoid interpolation on ${} arguments
		// List<String> args = config.getConfiguration().getList(String.class, shellCommandKey + "/arg", null);
		Object argsObject = config.getConfiguration().getProperty(shellCommandKey + "/arg");
		List<String> args = new ArrayList<>();
		if (argsObject!=null) {
			if (argsObject instanceof List) {
				args.addAll((Collection<? extends String>) argsObject);
			} else {
				args.add((String) argsObject);
			}
		}
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
	 * NOTE: the object doesn't have to be an @Entity
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
	 * @param classObject classe da usare per creare il clone quando il source è nascosto dentro a un HibernateProxy
	 * @return
	 */
	public static Object copyEntity(CloneableFiltered source, Class classObject) {
		return copyEntity(source, classObject, false);
	}

	public static Object copyEntity(CloneableFiltered source, Class classObject, boolean setFieldDirectly) {
		Map<CloneableFiltered, Object> alreadyCopiedMap = new HashMap<>();
		return copyEntity(source, classObject, setFieldDirectly, alreadyCopiedMap);
	}

	private static Object copyEntity(CloneableFiltered source, Class classObject, boolean setFieldDirectly, Map<CloneableFiltered, Object> alreadyCopiedMap) {
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
			Constructor constructor = sourceClass.getDeclaredConstructor(new Class[0]);
	        constructor.setAccessible(true);
			Object target = constructor.newInstance(new Object[0]);
			if(target instanceof org.hibernate.proxy.HibernateProxy && classObject!=null) {
				target = classObject.newInstance();
			}

			alreadyCopiedMap.put(source, target); // Needed to avoid infinite recursion if a value holds a reference to the parent
			copyFields(source, sourceClass, target, setFieldDirectly, alreadyCopiedMap);
			Class<?> superclass = sourceClass.getSuperclass();
			while (superclass!=null && superclass!=Object.class) {
				sourceClass = superclass;
				copyFields(source, sourceClass, target, setFieldDirectly, alreadyCopiedMap);
				superclass = sourceClass.getSuperclass();
			}
			return target;
		} catch (Exception e) {
			String msg = "Can't duplicate object '" + source + "'";
			log.error(msg + ": " + e);
			throw new YadaInternalException(msg, e);
		}
	}

	/**
	 * Copy a value either by getter or by field
	 * @param setFieldDirectly
	 * @param field
	 * @param getter
	 * @param setter
	 * @param source object containing the value to copy
	 * @param target object where to copy the value
	 * @param args
	 */
	private static void copyValue(boolean setFieldDirectly, Field field, Method getter, Method setter, Object source, Object target, Object... args) {
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

	private static void copyFields(CloneableFiltered source, Class<?> sourceClass, Object target, boolean setFieldDirectly, Map<CloneableFiltered, Object> alreadyCopiedMap) {
		log.debug("Copio oggetto {} di tipo {}", source, sourceClass);
		Field[] fields = sourceClass.getDeclaredFields();
		Field[] excludedFields = source.getExcludedFields();
		List<Field>filteredFields = excludedFields!=null? (List<Field>) Arrays.asList(excludedFields):new ArrayList<>();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			log.debug("Copying field {}", field.getName());
			field.setAccessible(true);
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
						copyValue(setFieldDirectly, field, getter, setter, source, target);
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
								copyValue(setFieldDirectly, field, getter, setter, source, target, targetCollection);
								// The getter should have returned a new empty instance.
								// We could
//								targetCollection = new ArrayList();
							}
							// Faccio la copia shallow di tutti gli elementi che non implementano CloneableDeep;
							// per questi faccio la copia deep.
							for (Object value : sourceCollection) {
								if (isType(value.getClass(), CloneableDeep.class)) {
									Object clonedValue = YadaUtil.copyEntity((CloneableFiltered) value, null, false, alreadyCopiedMap); // deep
									// For YadaAttachedFile objects, duplicate the file on disk too
									if (isType(value.getClass(), YadaAttachedFile.class)) {
										clonedValue = yadaFileManager.duplicateFiles((YadaAttachedFile) clonedValue);
									}
									targetCollection.add(clonedValue);
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
								copyValue(setFieldDirectly, field, getter, setter, source, target, targetMap);
//								setter.invoke(target, targetMap);
							}
							// Faccio la copia shallow di tutti gli elementi che non implementano CloneableDeep;
							// per questi faccio la copia deep.
							for (Object key : sourceMap.keySet()) {
								Object value = sourceMap.get(key);
								if (isType(value.getClass(), CloneableDeep.class)) {
									Object clonedValue = YadaUtil.copyEntity((CloneableFiltered) value, null, false, alreadyCopiedMap); // deep
									// For YadaAttachedFile objects, duplicate the file on disk too
									if (isType(value.getClass(), YadaAttachedFile.class)) {
										clonedValue = yadaFileManager.duplicateFiles((YadaAttachedFile) clonedValue);
									}
									targetMap.put(key, clonedValue);
								} else {
									targetMap.put(key, value); // shallow
								}
							}
//								targetMap.putAll(sourceMap);
						} else {
							// Non è una collection nè una mappa.
							if (isType(fieldType, CloneableDeep.class)) {
								// Siccome implementa CloneableDeep, lo duplico deep
								CloneableFiltered fieldValue = setFieldDirectly ? (CloneableFiltered) field.get(source) : (CloneableFiltered) getter.invoke(source);
								Object clonedValue = YadaUtil.copyEntity(fieldValue, null, setFieldDirectly, alreadyCopiedMap); // deep but detached
								// For YadaAttachedFile objects, duplicate the file on disk too
								if (isType(fieldType, YadaAttachedFile.class)) {
									clonedValue = yadaFileManager.duplicateFiles((YadaAttachedFile) clonedValue);
								}
								copyValue(setFieldDirectly, field, getter, setter, source, target, clonedValue);
//								setter.invoke(target, YadaUtil.copyEntity(fieldValue)); // deep but detached
							} else if (isType(fieldType, StringBuilder.class)) {
								// String builder/buffer is cloned otherwise changes to the original object would be reflected in the new one
								StringBuilder fieldValue = setFieldDirectly ? (StringBuilder) field.get(source) : (StringBuilder) getter.invoke(source);
								StringBuilder fieldClone = new StringBuilder(fieldValue.toString());
								copyProvidedValue(setFieldDirectly, field, getter, setter, fieldClone, target);
							} else if (isType(fieldType, StringBuffer.class)) {
								// String builder/buffer is cloned otherwise changes to the original object would be reflected in the new one
								StringBuffer fieldValue = setFieldDirectly ? (StringBuffer) field.get(source) : (StringBuffer) getter.invoke(source);
								StringBuffer fieldClone = new StringBuffer(fieldValue.toString());
								copyProvidedValue(setFieldDirectly, field, getter, setter, fieldClone, target);
							} else {
								// E' un oggetto normale, per cui copio il riferimento
								copyValue(setFieldDirectly, field, getter, setter, source, target);
//								setter.invoke(target, getter.invoke(source)); // shallow
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
	 * @param minutes
	 * @return
	 */
	public static Calendar addDaysClone(Calendar source, int days) {
		return addDays((Calendar) source.clone(), days);
	}

	/**
	 * Adds or removes the days. The original object is modified.
	 * @param calendar
	 * @param minutes
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
		// If the filename is a path, keep the last portion
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


}
