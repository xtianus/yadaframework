package net.yadaframework.ai.components.bedrock.claude.parts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaClaudeMetadata {
    @JsonProperty("user_id")
    private String userId;
    
    public YadaClaudeMetadata() {}
    
    public YadaClaudeMetadata userId(String userId) {
        this.userId = userId;
        return this;
    }
    
    // Remove getters to keep autocompletion clean
//    public String getUserId() { return userId; }
}
