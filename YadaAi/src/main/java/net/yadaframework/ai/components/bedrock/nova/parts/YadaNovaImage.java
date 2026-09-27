package net.yadaframework.ai.components.bedrock.nova.parts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonInclude;

import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.exceptions.YadaSystemException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaImage {
	private YadaNovaImageFormat format;
	private YadaNovaImageSource source;

	public YadaNovaImage() {}

	public YadaNovaImage format(YadaNovaImageFormat format) {
		this.format = format;
		return this;
	}

	public YadaNovaImage source(YadaNovaImageSource source) {
		this.source = source;
		return this;
	}

	public YadaNovaImage bytes(String base64ImageString) {
		this.source = new YadaNovaImageSource().bytes(base64ImageString);
		return this;
	}

	public YadaNovaImage data(Path imagePath) {
		String fileName = imagePath.getFileName().toString().toLowerCase();
		if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
			this.format = YadaNovaImageFormat.JPEG;
		} else if (fileName.endsWith(".png")) {
			this.format = YadaNovaImageFormat.PNG;
		} else if (fileName.endsWith(".gif")) {
			this.format = YadaNovaImageFormat.GIF;
		} else if (fileName.endsWith(".webp")) {
			this.format = YadaNovaImageFormat.WEBP;
		} else {
			throw new YadaInvalidUsageException(
				"Unsupported image format: " + fileName +
				". Supported formats: jpg, jpeg, png, gif, webp");
		}

		try {
			byte[] imageBytes = Files.readAllBytes(imagePath);
			this.source = new YadaNovaImageSource().bytes(Base64.getEncoder().encodeToString(imageBytes));
			return this;
		} catch (IOException e) {
			throw new YadaSystemException(e, "Failed to read image file: {}", imagePath);
		}
	}

	// Remove getters to keep autocompletion clean
//	public YadaNovaImageFormat getFormat() { return format; }
//	public YadaNovaImageSource getSource() { return source; }
}
