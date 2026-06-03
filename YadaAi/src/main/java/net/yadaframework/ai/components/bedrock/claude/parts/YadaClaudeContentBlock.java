package net.yadaframework.ai.components.bedrock.claude.parts;

import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaClaudeContentBlock {
    private YadaClaudeContentType type;
    private String text;
    private YadaClaudeImageSource source;
    private String id;
    private String name;
    private Map<String, Object> input;
    @JsonProperty("tool_use_id")
    private String toolUseId;
    @JsonProperty("is_error")
    private Boolean isError;
    
    public YadaClaudeContentBlock() {}
    
    public YadaClaudeContentBlock type(YadaClaudeContentType type) {
        this.type = type;
        return this;
    }
    
    public YadaClaudeContentBlock text(String text) {
        this.type = YadaClaudeContentType.TEXT;
        this.text = text;
        return this;
    }
    
    public YadaClaudeContentBlock source(YadaClaudeImageSource source) {
        this.type = YadaClaudeContentType.IMAGE;
        this.source = source;
        return this;
    }

    /**
     * Sets the image source using a lambda expression or method reference.
     * This allows for a more concise syntax when building image sources.
     *
     * @param sourceBuilder a Consumer that configures the ImageSource
     * @return this ContentBlock instance for method chaining
     *
     * @example
     * <pre>
     * contentBlock.source(src -> src
     *     .data(imagePath)
     *     .mediaType(MediaType.IMAGE_PNG));
     * </pre>
     */
    public YadaClaudeContentBlock source(Consumer<YadaClaudeImageSource> sourceBuilder) {
        YadaClaudeImageSource imageSource = new YadaClaudeImageSource();
        sourceBuilder.accept(imageSource);
        this.type = YadaClaudeContentType.IMAGE;
        this.source = imageSource;
        return this;
    }
    
    public YadaClaudeContentBlock id(String id) {
        this.id = id;
        return this;
    }
    
    public YadaClaudeContentBlock name(String name) {
        this.name = name;
        return this;
    }
    
    public YadaClaudeContentBlock input(Map<String, Object> input) {
        this.input = input;
        return this;
    }
    
    public YadaClaudeContentBlock toolUseId(String toolUseId) {
        this.toolUseId = toolUseId;
        return this;
    }
    
    public YadaClaudeContentBlock isError(boolean isError) {
        this.isError = isError;
        return this;
    }
    
    // Remove getters to keep autocompletion clean
//    public YadaClaudeContentType getType() { return type; }
//    public String getText() { return text; }
//    public YadaClaudeImageSource getSource() { return source; }
//    public String getId() { return id; }
//    public String getName() { return name; }
//    public Map<String, Object> getInput() { return input; }
//    public String getToolUseId() { return toolUseId; }
//    public Boolean getIsError() { return isError; }
}
