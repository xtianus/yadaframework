package net.yadaframework.ai.components.bedrock.nova.parts;

import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaToolSpec {
	private String name;
	private String description;
	private YadaNovaInputSchema inputSchema;

	public YadaNovaToolSpec() {}

	public YadaNovaToolSpec name(String name) {
		this.name = name;
		return this;
	}

	public YadaNovaToolSpec description(String description) {
		this.description = description;
		return this;
	}

	public YadaNovaToolSpec inputSchema(YadaNovaInputSchema inputSchema) {
		this.inputSchema = inputSchema;
		return this;
	}

	public YadaNovaToolSpec inputSchema(Consumer<YadaNovaInputSchema> schemaBuilder) {
		YadaNovaInputSchema schema = new YadaNovaInputSchema();
		schemaBuilder.accept(schema);
		this.inputSchema = schema;
		return this;
	}

	// Remove getters to keep autocompletion clean
//	public String getName() { return name; }
//	public String getDescription() { return description; }
//	public YadaNovaInputSchema getInputSchema() { return inputSchema; }
}
