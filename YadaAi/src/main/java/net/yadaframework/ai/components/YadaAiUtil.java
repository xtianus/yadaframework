package net.yadaframework.ai.components;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import net.yadaframework.ai.YadaAiConfigurable;
import net.yadaframework.ai.components.bedrock.YadaAiMessageInterface;
import net.yadaframework.ai.components.bedrock.claude.YadaClaudeRequest;
import net.yadaframework.ai.components.bedrock.nova.YadaNovaRequest;
import net.yadaframework.core.YadaConfiguration;
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
	
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	@PostConstruct
	private void validateConfiguration() {
		if (config == null) {
			throw new YadaInternalException("YadaAiConfigurable configuration not found. Ensure your application configuration (subclass of YadaConfiguration) implements YadaAiConfigurable.");
		}
	}

	/**
	 * Invokes the configured Bedrock AI model expecting a json map as result, where the keys are the ISO2 language codes and the values are the localized strings.
	 * @param message a request that will result in a json map. Output example:
	 * <pre>
	 * {
	 * "en": "The image shows a cat.",
	 * "fr": "L'image montre un chat."
	 * }
	 * </pre>
	 * @return a map of Locale to localized strings. Can be empty. 
	 * The Locale will have the country component when configured in the application configuration.
	 * @throws YadaSystemException if the invocation or the conversion fails
	 */
	public Map<Locale,String> getLocalizedMap(YadaAiMessageInterface message) {
		try {
			String jsonWithMarkdown = invokeModel(message);
			String jsonString = cleanJson(jsonWithMarkdown);
			return parseJsonLocaleMap(jsonString);
		} catch (JsonProcessingException e) {
			throw new YadaSystemException("Failed to parse alt text JSON", e);
		}
	}

	/**
	 * Invoke the configured Bedrock AI model and return the response as a string without any postprocessing
	 * @param message the request for the configured Bedrock AI model
	 * @return the response from the configured Bedrock AI model as a string
	 * @throws YadaSystemException if the invocation fails
	 */
	public String invokeModel(YadaAiMessageInterface message) {
        try {
            String jsonPayload = message.toJson();
            InvokeModelRequest request = InvokeModelRequest.builder()
				.modelId(config.getBedrockModelId())
				.body(SdkBytes.fromString(jsonPayload, StandardCharsets.UTF_8))
				.build();
            
            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request); // bedrock-runtime invoke API
            
            String responseBody = response.body().asString(StandardCharsets.UTF_8);
            return message.extractText(responseBody);
        } catch (Exception e) {
            log.error("Error invoking Bedrock AI model", e);
            throw new YadaSystemException("Failed to invoke Bedrock AI model", e);
        }
	}

	public YadaAiMessageInterface createMessage() {
		String modelId = config.getBedrockModelId();
		if (modelId.contains("anthropic")) {
			return new YadaClaudeRequest();
		}
		if (modelId.contains("nova")) {
			return new YadaNovaRequest();
		}
		throw new YadaInternalException("Unsupported Bedrock model: " + modelId);
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
	 * @return the map of Locale to localized strings, can be empty but not null
	 */
	private Map<Locale, String> parseJsonLocaleMap(String jsonString) throws JsonProcessingException {
		Map<Locale, String> result = new HashMap<>();
		@SuppressWarnings("unchecked")
		Map<String, String> jsonMap = objectMapper.readValue(jsonString, Map.class);
		List<Locale> configuredLocales = ((YadaConfiguration)config).getLocales();
		for (Map.Entry<String, String> entry : jsonMap.entrySet()) {
			String languageFromJson = entry.getKey();
			// Find the configured locale that matches this language
			Locale matchingLocale = configuredLocales.stream()
				.filter(locale -> locale.getLanguage().equals(languageFromJson))
				.findFirst()
				.orElse(Locale.forLanguageTag(languageFromJson));
			result.put(matchingLocale, entry.getValue());
		}
		return result;
	}	
}
