package net.yadaframework.ai.components.bedrock.nova.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaMessage {
	private YadaNovaRole role = YadaNovaRole.USER;
	private List<YadaNovaContentBlock> content;

	public YadaNovaMessage() {
		this.content = new ArrayList<>();
	}

	public YadaNovaMessage role(YadaNovaRole role) {
		this.role = role;
		return this;
	}

	public YadaNovaMessage roleUser() {
		this.role = YadaNovaRole.USER;
		return this;
	}

	public YadaNovaMessage roleAssistant() {
		this.role = YadaNovaRole.ASSISTANT;
		return this;
	}

	public YadaNovaMessage content(List<YadaNovaContentBlock> content) {
		this.content = content;
		return this;
	}

	public YadaNovaMessage addContentBlock(YadaNovaContentBlock contentBlock) {
		this.content.add(contentBlock);
		return this;
	}

	public YadaNovaMessage addContentBlock(Consumer<YadaNovaContentBlock> contentBlockBuilder) {
		YadaNovaContentBlock contentBlock = new YadaNovaContentBlock();
		contentBlockBuilder.accept(contentBlock);
		this.content.add(contentBlock);
		return this;
	}

	// Remove getters to keep autocompletion clean
//	public YadaNovaRole getRole() { return role; }
//	public List<YadaNovaContentBlock> getContent() { return content; }
}
