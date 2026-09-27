package net.yadaframework.ai.components.bedrock.nova.parts;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaImageSource {
	private String bytes;

	public YadaNovaImageSource() {}

	public YadaNovaImageSource bytes(String bytes) {
		this.bytes = bytes;
		return this;
	}

	// Remove getters to keep autocompletion clean
//	public String getBytes() { return bytes; }
}
