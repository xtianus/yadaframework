package net.yadaframework.ai.components.bedrock.claude.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaClaudeMessage {
    private YadaClaudeRole role = YadaClaudeRole.USER; // Default to USER
    private Object content; // Can be String or List<ContentBlock>
    
    public YadaClaudeMessage() {}
    
    public YadaClaudeMessage role(YadaClaudeRole role) {
        this.role = role;
        return this;
    }
    
    public YadaClaudeMessage roleUser() {
        this.role = YadaClaudeRole.USER;
        return this;
    }
    
    public YadaClaudeMessage roleAssistant() {
        this.role = YadaClaudeRole.ASSISTANT;
        return this;
    }
    
    public YadaClaudeMessage content(String content) {
        this.content = content;
        return this;
    }
    
    public YadaClaudeMessage content(List<YadaClaudeContentBlock> content) {
        this.content = content;
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public YadaClaudeMessage addContentBlock(YadaClaudeContentBlock contentBlock) {
        if (this.content == null) {
            this.content = new ArrayList<YadaClaudeContentBlock>();
        }
        if (this.content instanceof String) {
            throw new IllegalStateException("Cannot add ContentBlock when content is already set as String");
        }
        ((List<YadaClaudeContentBlock>) this.content).add(contentBlock);
        return this;
    }

    /**
     * Adds a content block using a lambda expression or method reference.
     * This allows for a more concise syntax when building content blocks.
     *
     * @param contentBlockBuilder a Consumer that configures the ContentBlock
     * @return this Message instance for method chaining
     *
     * @example
     * <pre>
     * message.addContentBlock(block -> block.text("Hello, Claude!"));
     *
     * message.addContentBlock(block -> block
     *     .source(new ImageSource().data(imagePath)));
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public YadaClaudeMessage addContentBlock(Consumer<YadaClaudeContentBlock> contentBlockBuilder) {
        YadaClaudeContentBlock contentBlock = new YadaClaudeContentBlock();
        contentBlockBuilder.accept(contentBlock);
        if (this.content == null) {
            this.content = new ArrayList<YadaClaudeContentBlock>();
        }
        if (this.content instanceof String) {
            throw new IllegalStateException("Cannot add ContentBlock when content is already set as String");
        }
        ((List<YadaClaudeContentBlock>) this.content).add(contentBlock);
        return this;
    }
    
    // Remove getters to keep autocompletion clean
//    public YadaClaudeRole getRole() { return role; }
//    public Object getContent() { return content; }
}
