package net.yadaframework.ai.components.bedrock.claude.parts;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum YadaClaudeImageSourceType {
    @JsonProperty("base64")
    BASE64,
    @JsonProperty("url")
    URL
}
