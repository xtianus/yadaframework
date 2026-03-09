package net.yadaframework.ai.components.bedrock.claude.parts;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum YadaClaudeMediaType {
    @JsonProperty("image/jpeg")
    IMAGE_JPEG,
    @JsonProperty("image/png")
    IMAGE_PNG,
    @JsonProperty("image/gif")
    IMAGE_GIF,
    @JsonProperty("image/webp")
    IMAGE_WEBP
}
