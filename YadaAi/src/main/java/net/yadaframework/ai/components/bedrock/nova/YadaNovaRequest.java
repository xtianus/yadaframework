package net.yadaframework.ai.components.bedrock.nova;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.yadaframework.ai.components.bedrock.YadaAiMessageInterface;
import net.yadaframework.ai.components.bedrock.nova.parts.YadaNovaInferenceConfig;
import net.yadaframework.ai.components.bedrock.nova.parts.YadaNovaMessage;
import net.yadaframework.ai.components.bedrock.nova.parts.YadaNovaSystemContent;
import net.yadaframework.ai.components.bedrock.nova.parts.YadaNovaTool;
import net.yadaframework.ai.components.bedrock.nova.parts.YadaNovaToolConfig;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaRequest implements YadaAiMessageInterface {

	private String schemaVersion = "messages-v1";
	private List<YadaNovaMessage> messages;
	private List<YadaNovaSystemContent> system;
	private YadaNovaInferenceConfig inferenceConfig;
	private YadaNovaToolConfig toolConfig;
	@JsonIgnore
	private YadaNovaMessage currentUserContentBlockMessage;

	private static final ObjectMapper objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
	private static final Gson gson = new Gson();

	private static final double MIN_TEMPERATURE = 0.0;
	private static final double MAX_TEMPERATURE = 1.0;
	private static final double MIN_TOP_P = 0.0;
	private static final double MAX_TOP_P = 1.0;
	private static final int MIN_TOP_K = 1;
	private static final int MIN_MAX_TOKENS = 1;

	public YadaNovaRequest() {
		this.messages = new ArrayList<>();
	}

	public YadaNovaRequest schemaVersion(String schemaVersion) {
		this.schemaVersion = schemaVersion;
		return this;
	}

	public YadaNovaRequest messages(List<YadaNovaMessage> messages) {
		this.currentUserContentBlockMessage = null;
		this.messages = messages;
		return this;
	}

	public YadaNovaRequest addMessage(YadaNovaMessage message) {
		this.currentUserContentBlockMessage = null;
		this.messages.add(message);
		return this;
	}

	public YadaNovaRequest addMessage(Consumer<YadaNovaMessage> messageBuilder) {
		YadaNovaMessage message = new YadaNovaMessage();
		messageBuilder.accept(message);
		this.currentUserContentBlockMessage = null;
		this.messages.add(message);
		return this;
	}

	@Override
	public YadaNovaRequest system(String system) {
		this.system = new ArrayList<>();
		this.system.add(new YadaNovaSystemContent().text(system));
		return this;
	}

	public YadaNovaRequest system(List<YadaNovaSystemContent> system) {
		this.system = system;
		return this;
	}

	public YadaNovaRequest inferenceConfig(YadaNovaInferenceConfig inferenceConfig) {
		this.inferenceConfig = inferenceConfig;
		return this;
	}

	public YadaNovaRequest inferenceConfig(Consumer<YadaNovaInferenceConfig> inferenceConfigBuilder) {
		YadaNovaInferenceConfig inferenceConfig = new YadaNovaInferenceConfig();
		inferenceConfigBuilder.accept(inferenceConfig);
		this.inferenceConfig = inferenceConfig;
		return this;
	}

	@Override
	public YadaNovaRequest maxTokens(int maxTokens) {
		if (maxTokens < MIN_MAX_TOKENS) {
			throw new IllegalArgumentException(
				"maxTokens must be at least " + MIN_MAX_TOKENS + ", got: " + maxTokens);
		}
		ensureInferenceConfig().maxTokens(maxTokens);
		return this;
	}

	@Override
	public YadaNovaRequest temperature(double temperature) {
		if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
			throw new IllegalArgumentException(
				"temperature must be between " + MIN_TEMPERATURE + " and " + MAX_TEMPERATURE +
				", got: " + temperature);
		}
		ensureInferenceConfig().temperature(temperature);
		return this;
	}

	@Override
	public YadaNovaRequest topP(double topP) {
		if (topP < MIN_TOP_P || topP > MAX_TOP_P) {
			throw new IllegalArgumentException(
				"topP must be between " + MIN_TOP_P + " and " + MAX_TOP_P +
				", got: " + topP);
		}
		ensureInferenceConfig().topP(topP);
		return this;
	}

	public YadaNovaRequest topK(int topK) {
		if (topK < MIN_TOP_K) {
			throw new IllegalArgumentException(
				"topK must be at least " + MIN_TOP_K + ", got: " + topK);
		}
		ensureInferenceConfig().topK(topK);
		return this;
	}

	public YadaNovaRequest stopSequences(List<String> stopSequences) {
		ensureInferenceConfig().stopSequences(stopSequences);
		return this;
	}

	public YadaNovaRequest addStopSequence(String stopSequence) {
		ensureInferenceConfig().addStopSequence(stopSequence);
		return this;
	}

	public YadaNovaRequest toolConfig(YadaNovaToolConfig toolConfig) {
		this.toolConfig = toolConfig;
		return this;
	}

	public YadaNovaRequest toolConfig(Consumer<YadaNovaToolConfig> toolConfigBuilder) {
		YadaNovaToolConfig toolConfig = new YadaNovaToolConfig();
		toolConfigBuilder.accept(toolConfig);
		this.toolConfig = toolConfig;
		return this;
	}

	public YadaNovaRequest addTool(YadaNovaTool tool) {
		ensureToolConfig().addTool(tool);
		return this;
	}

	public YadaNovaRequest addTool(Consumer<YadaNovaTool> toolBuilder) {
		ensureToolConfig().addTool(toolBuilder);
		return this;
	}

	public YadaNovaRequest toolChoice(Object toolChoice) {
		ensureToolConfig().toolChoice(toolChoice);
		return this;
	}

	@Override
	public YadaNovaRequest addUserText(String text) {
		if (this.currentUserContentBlockMessage != null) {
			this.currentUserContentBlockMessage.addContentBlock(block -> block.text(text));
			this.currentUserContentBlockMessage = null;
			return this;
		}
		return addMessage(msg -> msg
			.roleUser()
			.addContentBlock(block -> block.text(text)));
	}

	@Override
	public YadaNovaRequest addUserImage(Path imagePath) {
		ensureCurrentUserContentBlockMessage()
			.addContentBlock(block -> block.image(image -> image.data(imagePath)));
		return this;
	}

	@Override
	public YadaNovaRequest addAssistantText(String text) {
		this.currentUserContentBlockMessage = null;
		return addMessage(msg -> msg
			.roleAssistant()
			.addContentBlock(block -> block.text(text)));
	}

	@Override
	public String toJson() throws JsonProcessingException {
		validate();
		return objectMapper.writeValueAsString(this);
	}

	@Override
	public String extractText(String responseBody) {
		JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
		if (!responseJson.has("output") || !responseJson.get("output").isJsonObject()) {
			return responseBody;
		}
		JsonObject output = responseJson.getAsJsonObject("output");
		if (!output.has("message") || !output.get("message").isJsonObject()) {
			return responseBody;
		}
		JsonObject message = output.getAsJsonObject("message");
		if (!message.has("content") || !message.get("content").isJsonArray()) {
			return responseBody;
		}

		JsonArray content = message.getAsJsonArray("content");
		StringBuilder text = new StringBuilder();
		for (JsonElement element : content) {
			if (element.isJsonObject()) {
				JsonObject block = element.getAsJsonObject();
				if (block.has("text") && !block.get("text").isJsonNull()) {
					text.append(block.get("text").getAsString());
				}
			}
		}
		return text.length() > 0 ? text.toString() : responseBody;
	}

	private YadaNovaInferenceConfig ensureInferenceConfig() {
		if (this.inferenceConfig == null) {
			this.inferenceConfig = new YadaNovaInferenceConfig();
		}
		return this.inferenceConfig;
	}

	private YadaNovaToolConfig ensureToolConfig() {
		if (this.toolConfig == null) {
			this.toolConfig = new YadaNovaToolConfig();
		}
		return this.toolConfig;
	}

	private void validate() {
		if (messages == null || messages.isEmpty()) {
			throw new IllegalStateException("messages list is required and must not be empty");
		}
	}

	private YadaNovaMessage ensureCurrentUserContentBlockMessage() {
		if (this.currentUserContentBlockMessage == null) {
			this.currentUserContentBlockMessage = new YadaNovaMessage().roleUser();
			this.messages.add(this.currentUserContentBlockMessage);
		}
		return this.currentUserContentBlockMessage;
	}

	// Remove getters to keep autocompletion clean
//	public String getSchemaVersion() { return schemaVersion; }
//	public List<YadaNovaMessage> getMessages() { return messages; }
//	public List<YadaNovaSystemContent> getSystem() { return system; }
//	public YadaNovaInferenceConfig getInferenceConfig() { return inferenceConfig; }
//	public YadaNovaToolConfig getToolConfig() { return toolConfig; }
}
