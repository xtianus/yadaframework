package net.yadaframework.ai.components.bedrock.claude.parts;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaClaudeToolChoice {
    private String type;
    private String name;
    
    public YadaClaudeToolChoice() {}
    
    public YadaClaudeToolChoice type(String type) {
        this.type = type;
        return this;
    }
    
    public YadaClaudeToolChoice name(String name) {
        this.name = name;
        return this;
    }
    
    // Remove getters to keep autocompletion clean
//    public String getType() { return type; }
//    public String getName() { return name; }
}
