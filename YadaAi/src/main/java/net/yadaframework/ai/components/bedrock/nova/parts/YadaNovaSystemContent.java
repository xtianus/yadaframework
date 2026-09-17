package net.yadaframework.ai.components.bedrock.nova.parts;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaSystemContent {
	private String text;

	public YadaNovaSystemContent() {}

	public YadaNovaSystemContent text(String text) {
		this.text = text;
		return this;
	}

	// Remove getters to keep autocompletion clean
//	public String getText() { return text; }
}
