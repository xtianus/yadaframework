package net.yadaframework.ai.components.bedrock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.yadaframework.ai.components.bedrock.claude.YadaClaudeRequest;
import net.yadaframework.ai.components.bedrock.nova.YadaNovaRequest;

class YadaAiRequestJsonTest {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final String SYSTEM_PROMPT = "You are a precise assistant.";
	private static final String USER_TEXT = "Describe the accessibility implications.";

	@Test
	void claudeRequest_serializesToExpectedShape() throws Exception {
		YadaClaudeRequest request = new YadaClaudeRequest()
			.system(SYSTEM_PROMPT)
			.addUserText(USER_TEXT)
			.maxTokens(1024)
			.temperature(0.25);

		Map<String, Object> payload = readJson(request.toJson());

		assertEquals("bedrock-2023-05-31", payload.get("anthropic_version"));
		assertEquals(1024, payload.get("max_tokens"));
		assertEquals(SYSTEM_PROMPT, payload.get("system"));
		assertEquals(0.25, (Double) payload.get("temperature"), 0.001);

		List<?> messages = (List<?>) payload.get("messages");
		Map<?, ?> firstMessage = (Map<?, ?>) messages.get(0);
		assertEquals("user", firstMessage.get("role"));
		assertInstanceOf(String.class, firstMessage.get("content"));
		assertEquals(USER_TEXT, firstMessage.get("content"));
		// Shape expected by global.anthropic.claude-haiku-4-5-20251001-v1:0.
	}

	@Test
	void novaRequest_serializesToExpectedShape() throws Exception {
		YadaNovaRequest request = new YadaNovaRequest()
			.system(SYSTEM_PROMPT)
			.addUserText(USER_TEXT)
			.maxTokens(1024)
			.temperature(0.25);

		Map<String, Object> payload = readJson(request.toJson());

		assertEquals("messages-v1", payload.get("schemaVersion"));
		assertFalse(payload.containsKey("max_new_tokens"));
		assertFalse(payload.containsKey("top_p"));
		assertFalse(payload.containsKey("anthropic_version"));

		List<?> messages = (List<?>) payload.get("messages");
		Map<?, ?> firstMessage = (Map<?, ?>) messages.get(0);
		assertEquals("user", firstMessage.get("role"));
		List<?> content = (List<?>) firstMessage.get("content");
		assertEquals(USER_TEXT, ((Map<?, ?>) content.get(0)).get("text"));

		List<?> system = (List<?>) payload.get("system");
		assertEquals(SYSTEM_PROMPT, ((Map<?, ?>) system.get(0)).get("text"));

		Map<?, ?> inferenceConfig = (Map<?, ?>) payload.get("inferenceConfig");
		assertEquals(1024, inferenceConfig.get("maxTokens"));
		assertEquals(0.25, (Double) inferenceConfig.get("temperature"), 0.001);
		// Shape expected by global.amazon.nova-2-lite-v1:0.
	}

	@Test
	void extractText_parsesClaudeResponse() {
		String responseBody = """
			{
			  "content": [
			    {"type": "text", "text": "first "},
			    {"type": "text", "text": "second"}
			  ]
			}
			""";

		assertEquals("first second", new YadaClaudeRequest().extractText(responseBody));
	}

	@Test
	void extractText_parsesNovaResponse() {
		String responseBody = """
			{
			  "output": {
			    "message": {
			      "content": [
			        {"text": "first "},
			        {"text": "second"}
			      ]
			    }
			  }
			}
			""";

		assertEquals("first second", new YadaNovaRequest().extractText(responseBody));
	}

	@Test
	void claudeRequest_keepsUserImageAndTextInSameMessage(@TempDir Path tempDir) throws Exception {
		Path imagePath = writeImage(tempDir);
		YadaClaudeRequest request = new YadaClaudeRequest()
			.addUserImage(imagePath)
			.addUserText(USER_TEXT);

		Map<String, Object> payload = readJson(request.toJson());
		List<?> messages = (List<?>) payload.get("messages");
		assertEquals(1, messages.size());

		Map<?, ?> firstMessage = (Map<?, ?>) messages.get(0);
		assertEquals("user", firstMessage.get("role"));
		List<?> content = (List<?>) firstMessage.get("content");
		assertEquals(2, content.size());

		Map<?, ?> imageBlock = (Map<?, ?>) content.get(0);
		assertEquals("image", imageBlock.get("type"));
		Map<?, ?> source = (Map<?, ?>) imageBlock.get("source");
		assertEquals("image/png", source.get("media_type"));
		assertEquals("AQID", source.get("data"));

		Map<?, ?> textBlock = (Map<?, ?>) content.get(1);
		assertEquals("text", textBlock.get("type"));
		assertEquals(USER_TEXT, textBlock.get("text"));
	}

	@Test
	void novaRequest_keepsUserImageAndTextInSameMessage(@TempDir Path tempDir) throws Exception {
		Path imagePath = writeImage(tempDir);
		YadaNovaRequest request = new YadaNovaRequest()
			.addUserImage(imagePath)
			.addUserText(USER_TEXT);

		Map<String, Object> payload = readJson(request.toJson());
		List<?> messages = (List<?>) payload.get("messages");
		assertEquals(1, messages.size());

		Map<?, ?> firstMessage = (Map<?, ?>) messages.get(0);
		assertEquals("user", firstMessage.get("role"));
		List<?> content = (List<?>) firstMessage.get("content");
		assertEquals(2, content.size());

		Map<?, ?> imageBlock = (Map<?, ?>) content.get(0);
		Map<?, ?> image = (Map<?, ?>) imageBlock.get("image");
		assertEquals("png", image.get("format"));
		Map<?, ?> source = (Map<?, ?>) image.get("source");
		assertEquals("AQID", source.get("bytes"));

		Map<?, ?> textBlock = (Map<?, ?>) content.get(1);
		assertEquals(USER_TEXT, textBlock.get("text"));
	}

	private Map<String, Object> readJson(String json) throws Exception {
		return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
	}

	private Path writeImage(Path tempDir) throws Exception {
		Path imagePath = tempDir.resolve("sample.png");
		Files.write(imagePath, new byte[] { 1, 2, 3 });
		return imagePath;
	}
}
