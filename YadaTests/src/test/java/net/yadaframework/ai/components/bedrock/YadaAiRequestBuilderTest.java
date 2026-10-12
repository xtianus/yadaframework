package net.yadaframework.ai.components.bedrock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.yadaframework.ai.components.bedrock.claude.YadaClaudeRequest;
import net.yadaframework.ai.components.bedrock.nova.YadaNovaRequest;

/**
 * Request building of the Bedrock models: conversation turns, tools, optional fields and parameter validation.
 * Only local data is used: no AWS client or credentials.
 */
class YadaAiRequestBuilderTest {
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static Map<String, Object> readJson(String json) throws Exception {
		return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
	}

	@Test
	void claudeConversationKeepsTurnOrderAndRoles() throws Exception {
		YadaClaudeRequest request = new YadaClaudeRequest()
			.addUserText("First question")
			.addAssistantText("First answer")
			.addUserText("Second question")
			.maxTokens(10);
		List<?> messages = (List<?>) readJson(request.toJson()).get("messages");
		assertEquals(3, messages.size());
		assertEquals(List.of("user", "assistant", "user"), messages.stream().map(message -> ((Map<?, ?>) message).get("role")).toList());
		assertEquals("Second question", ((Map<?, ?>) messages.get(2)).get("content"));
	}

	@Test
	void claudeToolsAndToolChoiceUseTheApiFieldNames() throws Exception {
		YadaClaudeRequest request = new YadaClaudeRequest()
			.addUserText("What's the weather?")
			.maxTokens(100)
			.addTool(tool -> tool
				.name("get_weather")
				.description("Current weather of a city")
				.inputSchema(schema -> schema.type("object").addProperty("city", Map.of("type", "string")).addRequired("city")))
			.toolChoice(choice -> choice.type("tool").name("get_weather"))
			.addStopSequence("END")
			.topK(5);
		Map<String, Object> payload = readJson(request.toJson());
		Map<?, ?> tool = (Map<?, ?>) ((List<?>) payload.get("tools")).get(0);
		assertEquals("get_weather", tool.get("name"));
		Map<?, ?> inputSchema = (Map<?, ?>) tool.get("input_schema");
		assertEquals("object", inputSchema.get("type"));
		assertEquals(List.of("city"), inputSchema.get("required"));
		assertEquals(Map.of("type", "tool", "name", "get_weather"), payload.get("tool_choice"));
		assertEquals(List.of("END"), payload.get("stop_sequences"));
		assertEquals(5, payload.get("top_k"));
	}

	@Test
	void claudeOmitsUnsetOptionalFields() throws Exception {
		Map<String, Object> payload = readJson(new YadaClaudeRequest().addUserText("Hi").maxTokens(5).toJson());
		for (String optional : List.of("system", "temperature", "top_p", "top_k", "stop_sequences", "tools", "tool_choice", "stream", "metadata")) {
			assertFalse(payload.containsKey(optional), optional + " should not be serialized when unset");
		}
	}

	@Test
	void claudeRejectsOutOfRangeParameters() {
		YadaClaudeRequest request = new YadaClaudeRequest();
		assertThrows(IllegalArgumentException.class, () -> request.temperature(1.5));
		assertThrows(IllegalArgumentException.class, () -> request.temperature(-0.1));
		assertThrows(IllegalArgumentException.class, () -> request.topP(2));
		assertThrows(IllegalArgumentException.class, () -> request.topK(0));
		assertThrows(IllegalArgumentException.class, () -> request.maxTokens(0));
		// Boundaries are accepted
		request.temperature(0).temperature(1).topP(1).topK(1).maxTokens(1);
	}

	@Test
	void novaRejectsOutOfRangeParameters() {
		YadaNovaRequest request = new YadaNovaRequest();
		assertThrows(IllegalArgumentException.class, () -> request.temperature(1.5));
		assertThrows(IllegalArgumentException.class, () -> request.topP(-1));
		assertThrows(IllegalArgumentException.class, () -> request.maxTokens(0));
	}

	@Test
	void novaPutsInferenceParametersInTheirOwnObject() throws Exception {
		Map<String, Object> payload = readJson(new YadaNovaRequest().addUserText("Hi").maxTokens(50).temperature(0.5).topP(0.9).toJson());
		Map<?, ?> inferenceConfig = (Map<?, ?>) payload.get("inferenceConfig");
		assertTrue(inferenceConfig != null, "No inferenceConfig in " + payload);
		assertEquals(50, inferenceConfig.get("maxTokens"));
		assertEquals(0.5, ((Number) inferenceConfig.get("temperature")).doubleValue(), 0.0001);
		assertEquals(0.9, ((Number) inferenceConfig.get("topP")).doubleValue(), 0.0001);
		assertFalse(payload.containsKey("temperature"), "Nova parameters belong to inferenceConfig");
	}
}
