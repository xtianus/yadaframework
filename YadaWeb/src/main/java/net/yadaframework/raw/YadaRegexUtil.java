package net.yadaframework.raw;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Miscellaneous Regular Expression functions
 *
 */
public class YadaRegexUtil {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	private Map<String, Pattern> patternCache = new Hashtable<String, Pattern>(); // Not a HashMap

	/**
	 * Escapes dots in regular expressions, replacing them with \.
	 * @param source
	 * @return
	 */
	public String escapeDots(String source) {
		return source.replace(".", "\\.");
	}
	
	/**
	 * Return a compiled Pattern, either from the cache or new.
	 * @param pattern
	 * @return a Pattern or null id the pattern parameter is null
	 */
	public Pattern getOrCreatePattern(String pattern) {
		return getOrCreatePattern(pattern, null);
	}
	/**
	 * Return a compiled Pattern, either from the cache or new, with case-insensitive and dotall flags set.
	 * @param pattern
	 * @param patternCache a Hashtable to store compiled patterns. Can be null to use the default.
	 * @return a Pattern or null id the pattern parameter is null
	 */
	public Pattern getOrCreatePattern(String pattern, Map<String, Pattern> patternCache) {
		if (pattern==null) {
			return null;
		}
		if (patternCache==null) {
			patternCache = this.patternCache;
		}
		Pattern compiledPattern = patternCache.get(pattern);
		if (compiledPattern==null) { // Don't care about synchronizing as redundant puts won't harm (they'll eventually cease)
			compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			patternCache.put(pattern, compiledPattern);
			if (log.isDebugEnabled()) {
				log.debug("Pattern not found in cache: '{}' (added) - total size = {}", pattern, patternCache.size());
			}
		}
		return compiledPattern;
	}

	/**
	 * Return a matcher for the given source document and pattern. Compiled Pattern objects are cached.
	 * @param source the document that has to be matched against the pattern
	 * @param pattern the pattern to find in the document, or null for null result
	 * @return a new Matcher ready for use, or null if pattern is null
	 */
	public Matcher createMatcher(String source, String pattern) {
		return createMatcher(source, pattern, null);
	}
	
	/**
	 * Return a matcher for the given source document and pattern. Compiled Pattern objects are cached.
	 * @param source the document that has to be matched against the pattern
	 * @param pattern the pattern to find in the document, or null for null result
	 * @param patternCache the Hastable that holds compiled patterns, can be null to use the default cache
	 * @return a new Matcher ready for use, or null if pattern is null
	 */
	public Matcher createMatcher(String source, String pattern, Map<String, Pattern> patternCache) {
		if (patternCache==null) {
			patternCache = this.patternCache;
		}
		Pattern compiledPattern = getOrCreatePattern(pattern, patternCache);
		return compiledPattern==null?null:compiledPattern.matcher(source);
	}
	
	/**
	 * Performs a find-and-replace in a delimited area of the source, returning the source with replacements applied.
	 * @param source the text to be searched
	 * @param startPattern pattern that identifies the region start
	 * @param endPattern pattern that identifies the region end
	 * @param repeatRegion true if the region has to be searched many times in the source
	 * @param replacer the object performing the replacement within the region
	 * @return the source where the replacer has been applied within the delimited region
	 */
	public String replaceInRegion(String source, String startPattern, String endPattern, boolean repeatRegion, YadaRegexReplacer replacer) {
		StringBuffer result = new StringBuffer();
		Matcher regionMatcher = this.createMatcher(source, "(" + startPattern + ")" + "(.*?)" + "(" +  endPattern + ")");
		 while (regionMatcher.find()) {
			 try {
				 regionMatcher.appendReplacement(result, ""); // Append all characters before the end of the region startPattern
				 result.append(regionMatcher.group(1)); // Append the start pattern
				 String toBeReplaced = regionMatcher.group(2);
				 if (toBeReplaced.length()>0) {
					 result.append(replacer.apply(toBeReplaced)); // Can't put this in appendReplacement because it interprets $ and \
				 }
				 result.append(regionMatcher.group(3)); // Append the end pattern
				 if (!repeatRegion) {
					 break;
				 }
			} catch (Exception e) {
				log.error("Failed to replace in region (ignored)", e);
			}
		 }
		 regionMatcher.appendTail(result);
		 return result.toString();
	}
	
	/**
	 * Searches a region of text for a pattern that contains a capturing group, and returns the captured group.
	 * @param source the text to search
	 * @param startPattern a regular expression that identifies the start of the region, null to search from start.
	 *                     The matched text is excluded from search.
	 * @param endPattern a regular expression that identifies the end of the region, null to search to the end.
	 *                     The matched text is excluded from search.
	 * @param extractPattern a regular expression with a capturing group, null to return the whole region
	 * @return the text matched by the first capturing group of extractorPattern, or the empty string
	 */
	public String extractInRegion(String source, String startPattern, String endPattern, String extractPattern) {
		startPattern=(startPattern==null)?"\\A":startPattern; // null is beginning of document
		endPattern=(endPattern==null)?"\\z":endPattern; // null is end of document
		Matcher regionMatcher = this.createMatcher(source, startPattern + "(.*?)" + endPattern);
		 try {
			if (regionMatcher.find()) {
				 String region = regionMatcher.group(1); // "(.*?)"
				 if (extractPattern==null) {
					 return region;
				 }
				 Matcher extractor = this.createMatcher(region, extractPattern);
				 if (extractor.find()) {
					 return extractor.group(1);
				 }
			 }
		} catch (Exception e) {
			log.error("Failed to search in region (ignored)", e);
		}
		 return "";
	}

}
