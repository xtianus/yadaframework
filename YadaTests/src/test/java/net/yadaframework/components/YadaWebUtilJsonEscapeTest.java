package net.yadaframework.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link YadaWebUtil#escapeJsonString(String)} writes values that stay inside a JSON string and contain no markup.
 */
class YadaWebUtilJsonEscapeTest {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void escapedValuesParseBackUnchanged() throws Exception {
		for (String value : new String[] {"/x?a=1&b=2#top", "/x?<script>alert(1)</script>", "/x?a=\",\"newTab\":\"true", "back\\slash", "it's", "line\nbreak sep\u0000nul", "café"}) {
			String escaped = YadaWebUtil.escapeJsonString(value);
			assertEquals(value, objectMapper.readTree("\"" + escaped + "\"").asText(), escaped);
			for (String unsafe : new String[] {"<", ">", "&", "'", "\n", " "}) {
				assertFalse(escaped.contains(unsafe), escaped);
			}
		}
	}

	@Test
	void nullIsEmpty() {
		assertEquals("", YadaWebUtil.escapeJsonString(null));
		assertEquals("/plain/path", YadaWebUtil.escapeJsonString("/plain/path"));
	}
}
