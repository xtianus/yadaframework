package net.yadaframework.web;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.yadaframework.exceptions.InternalException;

/**
 * Parser for https://publicsuffix.org list of domain names
 * @see https://github.com/whois-server-list/public-suffix-list for another implementation
 */
public class YadaPublicSuffix {
	// 5 livelli: da "com" a "s3.cn-north-1.amazonaws.com.cn"
	// TODO rewrite with YadaLookupTableFive
	private Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> topLevel = null;
	
	/**
	 * Given a value like "www.something.co.uk" returns "something.co.uk"
	 * @param hostAddress a host with or without port
	 * @return the part of the argument that identifies a server, or the original address for ipv4 and ipv6
	 */
	public String extractServerAddress(String hostAddress) {
		if (topLevel==null) {
			throw new InternalException("Must call parse(File list) before use");
		}
		if (hostAddress!=null) {
			int posColumn = hostAddress.lastIndexOf(':');
			int posSquare = hostAddress.lastIndexOf(']'); // for ipv6 like [1fff:0:a88:85a3::ac1f]:8001
			if (posColumn>posSquare) {
				hostAddress = hostAddress.substring(0,posColumn);
			}
			String[] parts = hostAddress.split("\\.");
			if (parts[0].matches("[0-9]+") || parts[0].equals(hostAddress)) {
				return hostAddress; // Numeric address = ipv4, no dot = ipv6
			}
			int cutPoint = hostAddress.length();
			Map level = topLevel;
			for (int i = parts.length-1; i>=1; i--) { // Mi fermo a 1 perchè è inutile testare l'ultimo segmento
				String segment = parts[i];
				cutPoint = cutPoint - segment.length() - 1;
				Map newLevel = (Map) level.get(segment);
				if (newLevel==null) {
					newLevel = (Map) level.get("*"); // Wildcard "*.yokohama.jp"
					if (newLevel!=null) {
						Object override = level.get("!"+segment); // Wildcard override "!city.yokohama.jp"
						if (override!=null) { // "city"
							newLevel=null;  
						}
					}
				}
				if (newLevel==null) {
					return hostAddress.substring(cutPoint+1);
				}
				level = newLevel;
			}
		}
		return hostAddress;
	}
	
	public void parse(File list) throws IOException {
		topLevel = new HashMap<String, Map<String, Map<String, Map<String, Map<String, String>>>>>();
		BufferedReader reader = new BufferedReader(new FileReader(list));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.length()>0 && !line.startsWith("//")) {
				addSection(line, topLevel);
			}
		}
		reader.close();
	}
	
	private void addSection(String path, Map containingMap) {
		int pos = path.lastIndexOf('.');
		if (pos>-1) {
			String sectionName = path.substring(pos+1); // "uk"
			String pathLeft = path.substring(0, pos); // "co"
			Map level = (Map) containingMap.get(sectionName);
			if (level==null) {
				level = new HashMap();
				containingMap.put(sectionName, level);
			}
			addSection(pathLeft, level);
			return;
		}
		containingMap.put(path, new HashMap());
	}
	
	public static void main(String[] args) throws IOException {
		YadaPublicSuffix yadaPublicSuffix = new YadaPublicSuffix();
		yadaPublicSuffix.parse(new File("C:\\srv\\fcmp\\public_suffix_list.dat"));
		System.out.println(yadaPublicSuffix.extractServerAddress("myserver.com")); // myserver.com
		System.out.println(yadaPublicSuffix.extractServerAddress("www.myserver.com")); // myserver.com
		System.out.println(yadaPublicSuffix.extractServerAddress("www.myserver.com.ac")); // myserver.com.ac
		System.out.println(yadaPublicSuffix.extractServerAddress("www.myserver.bunkyo.tokyo.jp")); // myserver.bunkyo.tokyo.jp
		System.out.println(yadaPublicSuffix.extractServerAddress("www.myserver.skipthis.yokohama.jp")); // myserver.skipthis.yokohama.jp
		System.out.println(yadaPublicSuffix.extractServerAddress("www.nottheserver.city.yokohama.jp")); // city.yokohama.jp
		System.out.println(yadaPublicSuffix.extractServerAddress("www.myserver.s3.cn-north-1.amazonaws.com.cn")); // myserver.s3.cn-north-1.amazonaws.com.cn
	}
	
}
