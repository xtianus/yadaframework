package net.yadaframework.web;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

/**
 * Detects declared bots, crawlers, fetchers and common automation user agents.
 */
public final class YadaUserAgent {
	private static final Pattern GENERIC_BOT_PATTERN = Pattern.compile("(^|[^a-z0-9])(bot|crawler|spider|scraper|archiver|fetcher)([^a-z0-9]|$)");

	private static final String[] BOT_TOKENS = {
		"googlebot",
		"googleother",
		"google-inspectiontool",
		"google-cloudvertexbot",
		"adsbot-google",
		"apis-google",
		"mediapartners-google",
		"storebot-google",
		"feedfetcher-google",
		"bingbot",
		"adidxbot",
		"bingpreview",
		"msnbot",
		"yandex",
		"duckduckbot",
		"duckassistbot",
		"applebot",
		"applebot-extended",
		"baiduspider",
		"slurp",
		"yahoo! slurp",
		"seznambot",
		"sogou",
		"exabot",
		"qwantify",
		"mojeekbot",
		"petalbot",
		"ahrefsbot",
		"ahrefssiteaudit",
		"semrushbot",
		"mj12bot",
		"dotbot",
		"blexbot",
		"dataforseobot",
		"serpstatbot",
		"screaming frog seo spider",
		"sitebulb",
		"uptimerobot",
		"gptbot",
		"chatgpt-user",
		"oai-searchbot",
		"oai-adsbot",
		"claudebot",
		"claude-searchbot",
		"claude-user",
		"anthropic-ai",
		"claude-web",
		"perplexitybot",
		"perplexity-user",
		"bytespider",
		"amazonbot",
		"ccbot",
		"commoncrawl",
		"facebookexternalhit",
		"facebookbot",
		"facebot",
		"meta-externalagent",
		"meta-externalfetcher",
		"twitterbot",
		"linkedinbot",
		"slackbot",
		"discordbot",
		"telegrambot",
		"whatsapp",
		"pinterestbot",
		"ia_archiver",
		"ia-archiver",
		"archive.org_bot",
		"internetarchivebot",
		"wayback",
		"curl/",
		"wget/",
		"python-requests",
		"python-urllib",
		"java/",
		"okhttp",
		"apache-httpclient",
		"go-http-client",
		"libwww-perl",
		"scrapy",
		"headlesschrome",
		"phantomjs",
		"selenium",
		"playwright",
		"puppeteer"
	};

	private YadaUserAgent() {
		// Utility class.
	}

	/**
	 * Returns true when the request user-agent identifies a bot or crawler.
	 * @param request the current request, can be null
	 * @return true for bots and for missing requests
	 */
	public static boolean isBot(HttpServletRequest request) {
		return request==null || isBot(request.getHeader("User-Agent"));
	}

	/**
	 * Returns true when the user-agent identifies a bot, crawler, fetcher or common automation tool.
	 * @param userAgent the user-agent header value
	 * @return true for bots and for missing user-agent values
	 */
	public static boolean isBot(String userAgent) {
		String userAgentLower = StringUtils.lowerCase(StringUtils.trimToNull(userAgent));
		if (userAgentLower==null) {
			return true;
		}
		for (String botToken : BOT_TOKENS) {
			if (userAgentLower.contains(botToken)) {
				return true;
			}
		}
		return GENERIC_BOT_PATTERN.matcher(userAgentLower).find();
	}
}
