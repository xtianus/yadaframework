package net.yadaframework.ai.components;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.annotation.PostConstruct;
import net.yadaframework.ai.YadaAiConfigurable;
import net.yadaframework.ai.components.bedrock.claude.YadaClaudeRequest;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaSystemException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@Component
public class YadaAiUtil {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired(required = false) // Use false to have a chance of printing a meaningful error message in PostConstruct
	private YadaAiConfigurable config; // Will be null if the application configuration doesn't implement YadaAiConfigurable
	
	@Autowired private BedrockRuntimeClient bedrockRuntimeClient;
	
	private static final Gson gson = new Gson(); // Reusable Gson instance. Gson is thread-safe.
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	@PostConstruct
	private void validateConfiguration() {
		if (config == null) {
			throw new YadaInternalException("YadaAiConfigurable configuration not found. Ensure your application configuration (subclass of YadaConfiguration) implements YadaAiConfigurable.");
		}
	}

	/**
	 * Invokes the Claude model expecting a json map as result, where the keys are the ISO2 language codes and the values are the localized strings.
	 * @param claudeRequest a request for Claude that will result in a json map. Output example:
	 * <pre>
	 * {
	 * "en": "The image shows a cat.",
	 * "fr": "L'image montre un chat."
	 * }
	 * </pre>
	 * @return a map of Locale to localized strings. The Locale usually does not contain any other component other than the language, unless the prompt returns it.
	 * @throws YadaSystemException if the invocation or the conversion fails
	 */
	public Map<Locale,String> getLocalizedMap(YadaClaudeRequest claudeRequest) {
		try {
		String jsonWithMarkdown = invokeClaudeModel(claudeRequest);
		String jsonString = cleanJson(jsonWithMarkdown);
		return parseJsonLocaleMap(jsonString);
		} catch (JsonProcessingException e) {
			throw new YadaSystemException("Failed to parse alt text JSON", e);
		}
	}

	/**
	 * Invoke the Claude model and return the response as a string without any postprocessing
	 * @param claudeRequest the request for Claude
	 * @return the response from Claude as a string
	 * @throws YadaSystemException if the invocation fails
	 */
	public String invokeClaudeModel(YadaClaudeRequest claudeRequest) {
        try {
            String jsonPayload = claudeRequest.toJson();
            InvokeModelRequest request = InvokeModelRequest.builder()
				.modelId(config.getBedrockModelId())
				.body(SdkBytes.fromString(jsonPayload, StandardCharsets.UTF_8))
				.build();
            
            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);
            
            String responseBody = response.body().asString(StandardCharsets.UTF_8);
            JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
            // Extract text from response
            if (responseJson.has("content")) {
                return responseJson.getAsJsonArray("content")
					.get(0)
					.getAsJsonObject()
					.get("text")
					.getAsString();
            }
            return responseBody;
        } catch (Exception e) {
            log.error("Error invoking Claude model", e);
            throw new YadaSystemException("Failed to invoke Claude model", e);
        }
	}

	/**
	 * Remove markdown code block markers if present
	 * @param jsonWithMarkdown the json string with possible markdown code block markers
	 * @return the json string without markdown code block markers
	 */
	private String cleanJson(String jsonWithMarkdown) {
		String cleanJson = jsonWithMarkdown.trim();
		if (cleanJson.startsWith("```")) {
			// Remove opening ```json or ``` and closing ```
			cleanJson = cleanJson.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("```\\s*$", "").trim();
		}
		return cleanJson;
	}

	/**
	 * Parse the json string into a map of Locale to localized strings
	 * @param jsonString the json string that contains the map of Locale to localized strings
	 * @return the map of Locale to localized strings
	 */
	private Map<Locale, String> parseJsonLocaleMap(String jsonString) throws JsonProcessingException {
		Map<Locale, String> result = new HashMap<>();
		@SuppressWarnings("unchecked")
		Map<String, String> jsonMap = objectMapper.readValue(jsonString, Map.class);
		for (Map.Entry<String, String> entry : jsonMap.entrySet()) {
			Locale locale = Locale.forLanguageTag(entry.getKey());
			result.put(locale, entry.getValue());
		}
		return result;
	}	
}
