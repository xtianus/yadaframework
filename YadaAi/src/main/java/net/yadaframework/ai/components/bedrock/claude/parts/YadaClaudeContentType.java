package net.yadaframework.ai.components.bedrock.claude.parts;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum YadaClaudeContentType {
    @JsonProperty("text")
    TEXT,
    @JsonProperty("image")
    IMAGE,
    @JsonProperty("tool_use")
    TOOL_USE,
    @JsonProperty("tool_result")
    TOOL_RESULT
}
