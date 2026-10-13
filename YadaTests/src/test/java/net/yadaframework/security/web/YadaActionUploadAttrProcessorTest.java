package net.yadaframework.security.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.yadaframework.components.YadaWebUtil;

/**
 * yada:actionUpload adds the CSRF hidden field only to actions that can't leave the origin of the page
 */
class YadaActionUploadAttrProcessorTest {
	private final YadaWebUtil yadaWebUtil = new YadaWebUtil();

	@ParameterizedTest
	@ValueSource(strings = {
		"/upload",
		"/upload?x=2",
		"/myContext/en/upload?x=2&y=3#done",
	})
	void localActionsGetTheToken(String action) {
		assertTrue(YadaActionUploadAttrProcessor.isSameOrigin(action, yadaWebUtil), action);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"//evil.example/upload",
		"\\\\evil.example/upload",
		"/\\evil.example/upload",
		"\\/evil.example/upload",
		"/\t/evil.example/upload",
		"/\r/evil.example/upload",
		"/\n/evil.example/upload",
		"\t//evil.example/upload",
		"/%5Cevil.example/upload",
		"/%5cevil.example/upload",
		"/%2F/evil.example/upload",
		"/%252F%252Fevil.example/upload",
		"/upload\u0000",
		"/up​load",
		"https://evil.example/upload",
		"http://localhost/upload",
		"javascript:alert(1)",
		"upload",
		"",
	})
	void actionsThatCouldLeaveTheOriginGetNoToken(String action) {
		assertFalse(YadaActionUploadAttrProcessor.isSameOrigin(action, yadaWebUtil), action);
	}

	@Test
	void anAbsoluteUrlOfTheSameHostGetsNoToken() {
		// Not resolved against the request: absolute urls never get the token, use @{/path} for local actions
		assertFalse(YadaActionUploadAttrProcessor.isSameOrigin("http://127.0.0.1:8080/upload", yadaWebUtil));
	}
}
