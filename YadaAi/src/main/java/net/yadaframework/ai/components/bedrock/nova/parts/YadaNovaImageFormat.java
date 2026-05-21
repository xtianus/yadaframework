package net.yadaframework.ai.components.bedrock.nova.parts;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum YadaNovaImageFormat {
	@JsonProperty("jpeg")
	JPEG,
	@JsonProperty("png")
	PNG,
	@JsonProperty("gif")
	GIF,
	@JsonProperty("webp")
	WEBP
}
