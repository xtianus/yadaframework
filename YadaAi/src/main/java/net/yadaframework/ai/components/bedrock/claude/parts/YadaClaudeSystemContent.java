package net.yadaframework.ai.components.bedrock.claude.parts;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaClaudeSystemContent {
    private String text;
    
    public YadaClaudeSystemContent() {}
    
    public YadaClaudeSystemContent text(String text) {
        this.text = text;
        return this;
    }
    
    // Remove getters to keep autocompletion clean
//    public String getText() { return text; }
}
