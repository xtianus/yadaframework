package net.yadaframework.ai.components;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.yadaframework.ai.YadaAiConfigurable;
import net.yadaframework.ai.components.bedrock.YadaAiMessageInterface;
import net.yadaframework.ai.components.bedrock.claude.YadaClaudeRequest;
import net.yadaframework.ai.components.bedrock.nova.YadaNovaRequest;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaSystemException;
import net.yadaframework.exceptions.YadaInvalidServiceResponseException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@Component
public class YadaAiUtil {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired(required = false)
	private YadaAiConfigurable config; // Will be null if the application configuration doesn't implement YadaAiConfigurable
	
	@Autowired(required = false)
	private BedrockRuntimeClient bedrockRuntimeClient;

	private String bedrockAccessKeyId;
	private String bedrockSecretAccessKey;
	private Region bedrockRegion;
	private String bedrockModelId;
	private List<Locale> configuredLocales = Collections.emptyList();
	
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	private void validateConfiguration() {
		if (config == null && bedrockModelId == null) {
			throw new YadaInternalException("Yada AI configuration not found. Configure YadaAiUtil directly or make your application configuration implement YadaAiConfigurable.");
		}
	}

	/**
	 * Sets the Yada AI configuration used by annotation-based Spring applications.
	 * @param config the application AI configuration
	 */
	public void setConfig(YadaAiConfigurable config) {
		this.config = config;
	}

	/**
	 * Sets a preconfigured Bedrock runtime client.
	 * @param bedrockRuntimeClient the client to use for model invocations
	 */
	public void setBedrockRuntimeClient(BedrockRuntimeClient bedrockRuntimeClient) {
		this.bedrockRuntimeClient = bedrockRuntimeClient;
	}

	/**
	 * Sets the AWS access key ID for direct XML/manual configuration.
	 * @param bedrockAccessKeyId the AWS access key ID
	 */
	public void setBedrockAccessKeyId(String bedrockAccessKeyId) {
		this.bedrockAccessKeyId = bedrockAccessKeyId;
	}

	/**
	 * Sets the AWS secret access key for direct XML/manual configuration.
	 * @param bedrockSecretAccessKey the AWS secret access key
	 */
	public void setBedrockSecretAccessKey(String bedrockSecretAccessKey) {
		this.bedrockSecretAccessKey = bedrockSecretAccessKey;
	}

	/**
	 * Sets the AWS Bedrock region for direct XML/manual configuration.
	 * @param bedrockRegion the AWS region
	 */
	public void setBedrockRegion(Region bedrockRegion) {
		this.bedrockRegion = bedrockRegion;
	}

	/**
	 * Sets the AWS Bedrock region for direct XML/manual configuration.
	 * @param bedrockRegion the AWS region ID
	 */
	public void setBedrockRegion(String bedrockRegion) {
		this.bedrockRegion = bedrockRegion==null?null:Region.of(bedrockRegion);
	}

	/**
	 * Sets the Bedrock model ID for direct XML/manual configuration.
	 * @param bedrockModelId the Bedrock model ID
	 */
	public void setBedrockModelId(String bedrockModelId) {
		this.bedrockModelId = bedrockModelId;
	}

	/**
	 * Sets the application locales used when parsing localized AI responses.
	 * @param configuredLocales the locales configured by the application
	 */
	public void setConfiguredLocales(List<Locale> configuredLocales) {
		this.configuredLocales = configuredLocales==null?Collections.emptyList():configuredLocales;
	}

	/**
	 * Returns the configured Bedrock model ID.
	 * @return the Bedrock model ID
	 */
	private String getBedrockModelId() {
		return config==null?bedrockModelId:config.getBedrockModelId();
	}

	/**
	 * Returns the Bedrock runtime client, creating it lazily for direct XML/manual configuration.
	 * @return the Bedrock runtime client
	 */
	private BedrockRuntimeClient getBedrockRuntimeClient() {
		if (bedrockRuntimeClient == null) {
			if (config != null) {
				bedrockAccessKeyId = config.getBedrockAccessKeyId();
				bedrockSecretAccessKey = config.getBedrockSecretAccessKey();
				bedrockRegion = config.getBedrockRegion();
			}
			if (bedrockAccessKeyId == null || bedrockSecretAccessKey == null || bedrockRegion == null) {
				throw new YadaInternalException("Bedrock credentials and region must be configured before invoking a model.");
			}
			AwsBasicCredentials awsCreds = AwsBasicCredentials.create(bedrockAccessKeyId, bedrockSecretAccessKey);
			bedrockRuntimeClient = BedrockRuntimeClient.builder()
				.region(bedrockRegion)
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds))
				.build();
		}
		return bedrockRuntimeClient;
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
	 * @throws YadaSystemException if the model invocation fails
	 * @throws YadaInvalidServiceResponseException if the response can't be parsed into a non-empty json map
	 */
	public Map<Locale,String> getLocalizedMap(YadaAiMessageInterface message) {
		String rawResponse = invokeModel(message);
		String jsonString = extractJson(rawResponse);
		try {
			Map<Locale, String> result = parseJsonLocaleMap(jsonString);
			if (result.isEmpty()) {
				throw new YadaInvalidServiceResponseException("Empty json map in AI response: {}", rawResponse);
			}
			return result;
		} catch (JsonProcessingException e) {
			throw new YadaInvalidServiceResponseException(e, "Failed to parse json map from AI response: {}", rawResponse);
		}
	}

	/**
	 * Invoke the configured Bedrock AI model and return the response as a string without any postprocessing
	 * @param message the request for the configured Bedrock AI model
	 * @return the response from the configured Bedrock AI model as a string
	 * @throws YadaSystemException if the invocation fails
	 */
	public String invokeModel(YadaAiMessageInterface message) {
		validateConfiguration();
		try {
			String jsonPayload = message.toJson();
			InvokeModelRequest request = InvokeModelRequest.builder()
				.modelId(getBedrockModelId())
				.body(SdkBytes.fromString(jsonPayload, StandardCharsets.UTF_8))
				.build();
			
			InvokeModelResponse response = getBedrockRuntimeClient().invokeModel(request); // bedrock-runtime invoke API
			
			String responseBody = response.body().asString(StandardCharsets.UTF_8);
			return message.extractText(responseBody);
		} catch (Exception e) {
			log.error("Error invoking Bedrock AI model", e);
			throw new YadaSystemException("Failed to invoke Bedrock AI model", e);
		}
	}

	public YadaAiMessageInterface createMessage() {
		validateConfiguration();
		String modelId = getBedrockModelId();
		if (modelId.contains("anthropic")) {
			return new YadaClaudeRequest();
		}
		if (modelId.contains("nova")) {
			return new YadaNovaRequest();
		}
		throw new YadaInternalException("Unsupported Bedrock model: " + modelId);
	}

	// Matches a fenced ```json ... ``` (or bare ``` ... ```) block anywhere in the text, capturing the json object
	private static final Pattern FENCED_JSON_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);

	/**
	 * Extract the json object from a model response that may wrap it in markdown code fences and/or surround it with
	 * explanatory prose. A fenced json block found anywhere in the text is preferred; otherwise the substring from the
	 * first '{' to the last '}' is used. Any leading or trailing text found around the json is logged at info level.
	 * @param rawResponse the raw model response, possibly containing prose, markdown fences and the json object
	 * @return the extracted json string, or the trimmed input when no json delimiters are found
	 */
	private String extractJson(String rawResponse) {
		String trimmed = rawResponse.trim();
		// Prefer a fenced json block found anywhere in the text
		Matcher fencedMatcher = FENCED_JSON_PATTERN.matcher(trimmed);
		if (fencedMatcher.find()) {
			String json = fencedMatcher.group(1).trim();
			logExtraneousText(trimmed.substring(0, fencedMatcher.start()), trimmed.substring(fencedMatcher.end()));
			return json;
		}
		// Fallback: substring from the first '{' to the last '}'
		int firstBrace = trimmed.indexOf('{');
		int lastBrace = trimmed.lastIndexOf('}');
		if (firstBrace >= 0 && lastBrace > firstBrace) {
			String json = trimmed.substring(firstBrace, lastBrace + 1).trim();
			logExtraneousText(trimmed.substring(0, firstBrace), trimmed.substring(lastBrace + 1));
			return json;
		}
		// No json delimiters found: return as-is and let the parser report the problem
		return trimmed;
	}

	/**
	 * Log, at info level, any non-blank text found before or after the extracted json
	 * @param leading the text preceding the json
	 * @param trailing the text following the json
	 */
	private void logExtraneousText(String leading, String trailing) {
		String leadingText = leading.trim();
		String trailingText = trailing.trim();
		if (!leadingText.isEmpty()) {
			log.info("Leading text found before json in AI response: {}", leadingText);
		}
		if (!trailingText.isEmpty()) {
			log.info("Trailing text found after json in AI response: {}", trailingText);
		}
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
		List<Locale> configuredLocales = config==null?this.configuredLocales:config.getConfiguredLocales();
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
