package net.yadaframework.ai.components.bedrock.claude.parts;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum YadaClaudeRole {
    @JsonProperty("user")
    USER,
    @JsonProperty("assistant")
    ASSISTANT;
    
    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
