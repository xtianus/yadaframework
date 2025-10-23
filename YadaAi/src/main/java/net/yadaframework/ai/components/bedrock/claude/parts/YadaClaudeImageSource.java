package net.yadaframework.ai.components.bedrock.claude.parts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.exceptions.YadaSystemException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaClaudeImageSource {
    private YadaClaudeImageSourceType type;
    @JsonProperty("media_type")
    private YadaClaudeMediaType mediaType;
    private String data;
    private String url;
    
    public YadaClaudeImageSource() {}
    
    public YadaClaudeImageSource type(YadaClaudeImageSourceType type) {
        this.type = type;
        return this;
    }
    
    public YadaClaudeImageSource mediaType(YadaClaudeMediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }
    
    /**
     * Sets the image data as a base64-encoded string.
     * Automatically sets the type to BASE64.
     * 
     * @param base64ImageString the base64-encoded image data
     * @return this ImageSource instance for method chaining
     * @apiNote You must manually set the mediaType using {@link #mediaType(YadaClaudeMediaType)} 
     *          when using this method, as it cannot be auto-detected from a string.
     */
    public YadaClaudeImageSource data(String base64ImageString) {
        this.type = YadaClaudeImageSourceType.BASE64;
        this.data = base64ImageString;
        return this;
    }
    
    /**
     * Sets the image data by reading from a file path and encoding it as base64.
     * Automatically sets both the type to BASE64 and the mediaType based on the file extension.
     * 
     * @param imagePath the path to the image file
     * @return this ImageSource instance for method chaining
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if the file extension is not supported
     * @apiNote Supported file extensions: .jpg, .jpeg, .png, .gif, .webp
     *          The mediaType is automatically detected and set, so you don't need to call 
     *          {@link #mediaType(YadaClaudeMediaType)} when using this method.
     */
    public YadaClaudeImageSource data(Path imagePath) {
        this.type = YadaClaudeImageSourceType.BASE64;
        
        // Detect media type from file extension
        String fileName = imagePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            this.mediaType = YadaClaudeMediaType.IMAGE_JPEG;
        } else if (fileName.endsWith(".png")) {
            this.mediaType = YadaClaudeMediaType.IMAGE_PNG;
        } else if (fileName.endsWith(".gif")) {
            this.mediaType = YadaClaudeMediaType.IMAGE_GIF;
        } else if (fileName.endsWith(".webp")) {
            this.mediaType = YadaClaudeMediaType.IMAGE_WEBP;
        } else {
            throw new YadaInvalidUsageException(
                "Unsupported image format: " + fileName + 
                ". Supported formats: jpg, jpeg, png, gif, webp");
        }
        
		try {
			byte[] imageBytes = Files.readAllBytes(imagePath);
			this.data = Base64.getEncoder().encodeToString(imageBytes);
			return this;
		} catch (IOException e) {
			throw new YadaSystemException(e, "Failed to read image file: {}", imagePath);
		}
    }
    
    public YadaClaudeImageSource url(String url) {
        this.url = url;
        return this;
    }
    
    // Remove getters to keep autocompletion clean
//    public YadaClaudeImageSourceType getType() { return type; }
//    public YadaClaudeMediaType getMediaType() { return mediaType; }
//    public String getData() { return data; }
//    public String getUrl() { return url; }
}
