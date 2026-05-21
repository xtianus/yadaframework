package net.yadaframework.ai.components.bedrock.nova.parts;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum YadaNovaRole {
	@JsonProperty("user")
	USER,
	@JsonProperty("assistant")
	ASSISTANT;
}
