package net.yadaframework.ai.components.bedrock.nova.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaToolConfig {
	private List<YadaNovaTool> tools;
	private Object toolChoice;

	public YadaNovaToolConfig() {
		this.tools = new ArrayList<>();
	}

	public YadaNovaToolConfig tools(List<YadaNovaTool> tools) {
		this.tools = tools;
		return this;
	}

	public YadaNovaToolConfig addTool(YadaNovaTool tool) {
		this.tools.add(tool);
		return this;
	}

	public YadaNovaToolConfig addTool(Consumer<YadaNovaTool> toolBuilder) {
		YadaNovaTool tool = new YadaNovaTool();
		toolBuilder.accept(tool);
		this.tools.add(tool);
		return this;
	}

	public YadaNovaToolConfig toolChoice(Object toolChoice) {
		this.toolChoice = toolChoice;
		return this;
	}

	// Remove getters to keep autocompletion clean
//	public List<YadaNovaTool> getTools() { return tools; }
//	public Object getToolChoice() { return toolChoice; }
}
