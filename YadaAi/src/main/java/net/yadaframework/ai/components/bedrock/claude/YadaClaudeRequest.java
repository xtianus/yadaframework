package net.yadaframework.ai.components.bedrock.claude;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.yadaframework.ai.components.bedrock.claude.parts.YadaClaudeMessage;
import net.yadaframework.ai.components.bedrock.claude.parts.YadaClaudeMetadata;
import net.yadaframework.ai.components.bedrock.claude.parts.YadaClaudeSystemContent;
import net.yadaframework.ai.components.bedrock.claude.parts.YadaClaudeTool;
import net.yadaframework.ai.components.bedrock.claude.parts.YadaClaudeToolChoice;

/**
 * Fluent builder for Claude (Anthropic) model requests on Amazon Bedrock with version "bedrock-2023-05-31".
 * 
 * @example
 * <pre>
 * String requestJson = new YadaClaudeRequest()
 *     .maxTokens(1024)
 *     .addMessage(msg -> msg
 *         .roleUser()
 *         .content("What is the capital of France?"))
 *     .toJson();
 * 
 * // With image content from a file
 * String requestJson = new YadaClaudeRequest()
 *     .maxTokens(1024)
 *     .addMessage(msg -> msg
 *         .roleUser()
 *         .addContentBlock(block -> block
 *             .text("What is in this image?"))
 *         .addContentBlock(block -> block
 *             .source(src -> src
 *                 .data(Paths.get("/path/to/image.png")))))
 *     .toJson();
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaClaudeRequest {
    
    @JsonProperty("anthropic_version")
    private String anthropicVersion = "bedrock-2023-05-31"; // Will change when changing the message schema
    @JsonProperty("max_tokens")
    private Integer maxTokens = 1024;
    private List<YadaClaudeMessage> messages;
    private Object system; // Can be String or List<YadaClaudeSystemContent>
    private Double temperature;
    @JsonProperty("top_p")
    private Double topP;
    @JsonProperty("top_k")
    private Integer topK;
    @JsonProperty("stop_sequences")
    private List<String> stopSequences;
    private Boolean stream;
    private YadaClaudeMetadata metadata;
    private List<YadaClaudeTool> tools;
    @JsonProperty("tool_choice")
    private YadaClaudeToolChoice toolChoice;
    
    // Configure the mapper to access private fields as we removed the getters to keep autocomplation clean
    private static final ObjectMapper objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    // Constants for validation
    private static final double MIN_TEMPERATURE = 0.0;
    private static final double MAX_TEMPERATURE = 1.0;
    private static final double MIN_TOP_P = 0.0;
    private static final double MAX_TOP_P = 1.0;
    private static final int MIN_TOP_K = 1;
    private static final int MIN_MAX_TOKENS = 1;
    
    public YadaClaudeRequest() {
        this.messages = new ArrayList<>();
    }
    
    public YadaClaudeRequest anthropicVersion(String anthropicVersion) {
        this.anthropicVersion = anthropicVersion;
        return this;
    }
    
    /**
     * Sets the maximum number of tokens to generate.
     *
     * @param maxTokens the maximum number of tokens (must be >= 1)
     * @return this ClaudeRequest instance for method chaining
     * @throws IllegalArgumentException if maxTokens is less than 1
     */
    public YadaClaudeRequest maxTokens(int maxTokens) {
        if (maxTokens < MIN_MAX_TOKENS) {
            throw new IllegalArgumentException(
                "maxTokens must be at least " + MIN_MAX_TOKENS + ", got: " + maxTokens);
        }
        this.maxTokens = maxTokens;
        return this;
    }
    
    public YadaClaudeRequest addMessage(YadaClaudeMessage message) {
        this.messages.add(message);
        return this;
    }

    /**
     * Adds a message using a lambda expression or method reference.
     * This allows for a more concise syntax when building messages.
     *
     * @param messageBuilder a Consumer that configures the Message
     * @return this ClaudeRequest instance for method chaining
     *
     * @example
     * <pre>
     * request.addMessage(msg -> msg.content("Hello, Claude!"));
     *
     * request.addMessage(msg -> msg
     *     .addContentBlock(new ContentBlock().text("Describe this image"))
     *     .addContentBlock(new ContentBlock().source(imageSource)));
     * </pre>
     */
    public YadaClaudeRequest addMessage(Consumer<YadaClaudeMessage> messageBuilder) {
        YadaClaudeMessage message = new YadaClaudeMessage();
        messageBuilder.accept(message);
        this.messages.add(message);
        return this;
    }

    public YadaClaudeRequest messages(List<YadaClaudeMessage> messages) {
        this.messages = messages;
        return this;
    }
    
    public YadaClaudeRequest system(String system) {
        this.system = system;
        return this;
    }
    
    public YadaClaudeRequest system(List<YadaClaudeSystemContent> system) {
        this.system = system;
        return this;
    }
    
    /**
     * Sets the temperature for response generation.
     * Controls randomness: lower values make output more focused and deterministic,
     * higher values make output more random and creative.
     *
     * @param temperature the temperature value (must be between 0.0 and 1.0 inclusive)
     * @return this ClaudeRequest instance for method chaining
     * @throws IllegalArgumentException if temperature is not in valid range
     */
    public YadaClaudeRequest temperature(double temperature) {
        if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
            throw new IllegalArgumentException(
                "temperature must be between " + MIN_TEMPERATURE + " and " + MAX_TEMPERATURE +
                ", got: " + temperature);
        }
        this.temperature = temperature;
        return this;
    }
    
    /**
     * Sets the top-p (nucleus sampling) parameter.
     * Only tokens with cumulative probability up to top_p are considered.
     *
     * @param topP the top-p value (must be between 0.0 and 1.0 inclusive)
     * @return this ClaudeRequest instance for method chaining
     * @throws IllegalArgumentException if topP is not in valid range
     */
    public YadaClaudeRequest topP(double topP) {
        if (topP < MIN_TOP_P || topP > MAX_TOP_P) {
            throw new IllegalArgumentException(
                "topP must be between " + MIN_TOP_P + " and " + MAX_TOP_P +
                ", got: " + topP);
        }
        this.topP = topP;
        return this;
    }
    
    /**
     * Sets the top-k parameter.
     * Only the top k most likely tokens are considered for sampling.
     *
     * @param topK the top-k value (must be >= 1)
     * @return this ClaudeRequest instance for method chaining
     * @throws IllegalArgumentException if topK is less than 1
     */
    public YadaClaudeRequest topK(int topK) {
        if (topK < MIN_TOP_K) {
            throw new IllegalArgumentException(
                "topK must be at least " + MIN_TOP_K + ", got: " + topK);
        }
        this.topK = topK;
        return this;
    }
    
    public YadaClaudeRequest stopSequences(List<String> stopSequences) {
        this.stopSequences = stopSequences;
        return this;
    }
    
    public YadaClaudeRequest addStopSequence(String stopSequence) {
        if (this.stopSequences == null) {
            this.stopSequences = new ArrayList<>();
        }
        this.stopSequences.add(stopSequence);
        return this;
    }
    
    public YadaClaudeRequest stream(boolean stream) {
        this.stream = stream;
        return this;
    }
    
    public YadaClaudeRequest metadata(YadaClaudeMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Sets the metadata using a lambda expression or method reference.
     * This allows for a more concise syntax when building metadata.
     *
     * @param metadataBuilder a Consumer that configures the Metadata
     * @return this ClaudeRequest instance for method chaining
     *
     * @example
     * <pre>
     * request.metadata(meta -> meta.userId("user123"));
     * </pre>
     */
    public YadaClaudeRequest metadata(Consumer<YadaClaudeMetadata> metadataBuilder) {
        YadaClaudeMetadata metadata = new YadaClaudeMetadata();
        metadataBuilder.accept(metadata);
        this.metadata = metadata;
        return this;
    }
    
    public YadaClaudeRequest tools(List<YadaClaudeTool> tools) {
        this.tools = tools;
        return this;
    }
    
    public YadaClaudeRequest addTool(YadaClaudeTool tool) {
        if (this.tools == null) {
            this.tools = new ArrayList<>();
        }
        this.tools.add(tool);
        return this;
    }

    /**
     * Adds a tool using a lambda expression or method reference.
     * This allows for a more concise syntax when building tools.
     *
     * @param toolBuilder a Consumer that configures the Tool
     * @return this ClaudeRequest instance for method chaining
     *
     * @example
     * <pre>
     * request.addTool(tool -> tool
     *     .name("get_weather")
     *     .description("Get the current weather")
     *     .inputSchema(new InputSchema().type("object")));
     * </pre>
     */
    public YadaClaudeRequest addTool(Consumer<YadaClaudeTool> toolBuilder) {
        YadaClaudeTool tool = new YadaClaudeTool();
        toolBuilder.accept(tool);
        if (this.tools == null) {
            this.tools = new ArrayList<>();
        }
        this.tools.add(tool);
        return this;
    }
    
    public YadaClaudeRequest toolChoice(YadaClaudeToolChoice toolChoice) {
        this.toolChoice = toolChoice;
        return this;
    }

    /**
     * Sets the tool choice using a lambda expression or method reference.
     * This allows for a more concise syntax when building tool choice.
     *
     * @param toolChoiceBuilder a Consumer that configures the ToolChoice
     * @return this ClaudeRequest instance for method chaining
     *
     * @example
     * <pre>
     * request.toolChoice(choice -> choice
     *     .type("tool")
     *     .name("get_weather"));
     * </pre>
     */
    public YadaClaudeRequest toolChoice(Consumer<YadaClaudeToolChoice> toolChoiceBuilder) {
        YadaClaudeToolChoice toolChoice = new YadaClaudeToolChoice();
        toolChoiceBuilder.accept(toolChoice);
        this.toolChoice = toolChoice;
        return this;
    }
    
    /**
     * Validates the request and converts it to JSON string.
     * Validates that all required fields are set and within valid ranges.
     *
     * @return JSON string representation of this request
     * @throws JsonProcessingException if JSON serialization fails
     * @throws IllegalStateException if required fields are missing or invalid
     */
    public String toJson() throws JsonProcessingException {
        validate();
        return objectMapper.writeValueAsString(this);
    }

    /**
     * Validates that all required fields are set and within valid ranges.
     *
     * @throws IllegalStateException if validation fails
     */
    private void validate() {
        // Validate required fields
        if (maxTokens == null) {
            throw new IllegalStateException("maxTokens is required but not set");
        }

        if (messages == null || messages.isEmpty()) {
            throw new IllegalStateException("messages list is required and must not be empty");
        }

        // Validate that maxTokens is within valid range (in case it was set via reflection or deserialization)
        if (maxTokens < MIN_MAX_TOKENS) {
            throw new IllegalStateException(
                "maxTokens must be at least " + MIN_MAX_TOKENS + ", got: " + maxTokens);
        }

        // Validate temperature if set
        if (temperature != null && (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE)) {
            throw new IllegalStateException(
                "temperature must be between " + MIN_TEMPERATURE + " and " + MAX_TEMPERATURE +
                ", got: " + temperature);
        }

        // Validate topP if set
        if (topP != null && (topP < MIN_TOP_P || topP > MAX_TOP_P)) {
            throw new IllegalStateException(
                "topP must be between " + MIN_TOP_P + " and " + MAX_TOP_P +
                ", got: " + topP);
        }

        // Validate topK if set
        if (topK != null && topK < MIN_TOP_K) {
            throw new IllegalStateException(
                "topK must be at least " + MIN_TOP_K + ", got: " + topK);
        }
    }

    // No getters to keep autocomplation clean
//    // Getters
//    public String getAnthropicVersion() { return anthropicVersion; }
//    public Integer getMaxTokens() { return maxTokens; }
//    public List<YadaClaudeMessage> getMessages() { return messages; }
//    public Object getSystem() { return system; }
//    public Double getTemperature() { return temperature; }
//    public Double getTopP() { return topP; }
//    public Integer getTopK() { return topK; }
//    public List<String> getStopSequences() { return stopSequences; }
//    public Boolean getStream() { return stream; }
//    public YadaClaudeMetadata getMetadata() { return metadata; }
//    public List<YadaClaudeTool> getTools() { return tools; }
//    public YadaClaudeToolChoice getToolChoice() { return toolChoice; }
    
}
