package net.yadaframework.ai.components.bedrock.claude.parts;

import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaClaudeTool {
    private String name;
    private String description;
    @JsonProperty("input_schema")
    private YadaClaudeInputSchema inputSchema;
    
    public YadaClaudeTool() {}
    
    public YadaClaudeTool name(String name) {
        this.name = name;
        return this;
    }
    
    public YadaClaudeTool description(String description) {
        this.description = description;
        return this;
    }
    
    public YadaClaudeTool inputSchema(YadaClaudeInputSchema inputSchema) {
        this.inputSchema = inputSchema;
        return this;
    }

    /**
     * Sets the input schema using a lambda expression or method reference.
     * This allows for a more concise syntax when building input schemas.
     *
     * @param schemaBuilder a Consumer that configures the InputSchema
     * @return this Tool instance for method chaining
     *
     * @example
     * <pre>
     * tool.inputSchema(schema -> schema
     *     .type("object")
     *     .addProperty("location", Map.of("type", "string"))
     *     .addRequired("location"));
     * </pre>
     */
    public YadaClaudeTool inputSchema(Consumer<YadaClaudeInputSchema> schemaBuilder) {
        YadaClaudeInputSchema schema = new YadaClaudeInputSchema();
        schemaBuilder.accept(schema);
        this.inputSchema = schema;
        return this;
    }
    
    // Remove getters to keep autocompletion clean
//    public String getName() { return name; }
//    public String getDescription() { return description; }
//    public YadaClaudeInputSchema getInputSchema() { return inputSchema; }
}
