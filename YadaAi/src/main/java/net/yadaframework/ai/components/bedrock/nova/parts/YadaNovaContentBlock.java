package net.yadaframework.ai.components.bedrock.nova.parts;

import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaContentBlock {
	private String text;
	private YadaNovaImage image;
	private Object toolUse;
	private Object toolResult;

	public YadaNovaContentBlock() {}

	public YadaNovaContentBlock text(String text) {
		this.text = text;
		return this;
	}

	public YadaNovaContentBlock image(YadaNovaImage image) {
		this.image = image;
		return this;
	}

	public YadaNovaContentBlock image(Consumer<YadaNovaImage> imageBuilder) {
		YadaNovaImage image = new YadaNovaImage();
		imageBuilder.accept(image);
		this.image = image;
		return this;
	}

	public YadaNovaContentBlock toolUse(Object toolUse) {
		this.toolUse = toolUse;
		return this;
	}

	public YadaNovaContentBlock toolResult(Object toolResult) {
		this.toolResult = toolResult;
		return this;
	}

	// Remove getters to keep autocompletion clean
//	public String getText() { return text; }
//	public YadaNovaImage getImage() { return image; }
//	public Object getToolUse() { return toolUse; }
//	public Object getToolResult() { return toolResult; }
}
