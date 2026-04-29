package net.yadaframework.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests bot and crawler user-agent detection.
 */
public class YadaUserAgentTest {

	@Test
	public void detectsSearchEngineCrawlers() {
		assertTrue(YadaUserAgent.isBot("Mozilla/5.0 (compatible; Googlebot/2.1; +https://www.google.com/bot.html)"));
		assertTrue(YadaUserAgent.isBot("Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm) Chrome/120.0 Safari/537.36"));
		assertTrue(YadaUserAgent.isBot("Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)"));
		assertTrue(YadaUserAgent.isBot("DuckDuckBot/1.1; (+http://duckduckgo.com/duckduckbot.html)"));
		assertTrue(YadaUserAgent.isBot("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15 (Applebot/0.1; +http://www.apple.com/go/applebot)"));
	}

	@Test
	public void detectsSeoAiAndSocialBots() {
		assertTrue(YadaUserAgent.isBot("Mozilla/5.0 (compatible; AhrefsBot/7.0; +http://ahrefs.com/robot/)"));
		assertTrue(YadaUserAgent.isBot("Mozilla/5.0 (compatible; SemrushBot/7~bl; +http://www.semrush.com/bot.html)"));
		assertTrue(YadaUserAgent.isBot("Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko); compatible; GPTBot/1.3; +https://openai.com/gptbot"));
		assertTrue(YadaUserAgent.isBot("Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko); compatible; ChatGPT-User/1.0; +https://openai.com/bot"));
		assertTrue(YadaUserAgent.isBot("ClaudeBot"));
		assertTrue(YadaUserAgent.isBot("facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"));
		assertTrue(YadaUserAgent.isBot("meta-externalagent/1.1"));
	}

	@Test
	public void detectsGenericAutomationTools() {
		assertTrue(YadaUserAgent.isBot("curl/8.4.0"));
		assertTrue(YadaUserAgent.isBot("Wget/1.21.4"));
		assertTrue(YadaUserAgent.isBot("python-requests/2.31.0"));
		assertTrue(YadaUserAgent.isBot("Mozilla/5.0 HeadlessChrome/120.0.0.0 Safari/537.36"));
	}

	@Test
	public void acceptsNormalBrowsersAndPhoneModelsContainingBot() {
		assertFalse(YadaUserAgent.isBot("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
		assertFalse(YadaUserAgent.isBot("Mozilla/5.0 (Linux; Android 10; CUBOT X30 Build/QP1A.190711.020) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"));
	}

	@Test
	public void treatsMissingUserAgentAsBot() {
		assertTrue(YadaUserAgent.isBot((String)null));
		assertTrue(YadaUserAgent.isBot(" "));
	}
}
