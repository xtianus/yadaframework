package net.yadaframework.ai.components.bedrock.nova.parts;

import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaTool {
	private YadaNovaToolSpec toolSpec;

	public YadaNovaTool() {}

	public YadaNovaTool toolSpec(YadaNovaToolSpec toolSpec) {
		this.toolSpec = toolSpec;
		return this;
	}

	public YadaNovaTool toolSpec(Consumer<YadaNovaToolSpec> toolSpecBuilder) {
		YadaNovaToolSpec toolSpec = new YadaNovaToolSpec();
		toolSpecBuilder.accept(toolSpec);
		this.toolSpec = toolSpec;
		return this;
	}

	// Remove getters to keep autocompletion clean
//	public YadaNovaToolSpec getToolSpec() { return toolSpec; }
}
