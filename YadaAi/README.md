# YadaAi Module

This module provides AI integration capabilities for the Yada Framework, with a focus on AWS Bedrock support for Claude models. It offers a fluent API for building Claude requests and utilities for processing AI responses.

## Features

- **Fluent API** for building Claude model requests with lambda expressions
- **Automatic AWS Bedrock client configuration** via Spring
- **YadaAiUtil component** with utility methods for common AI tasks
- **Image handling** with automatic base64 encoding and media type detection
- **Localized response parsing** for multilingual applications
- **Configurable AI parameters** (temperature, max tokens, top-p, top-k, etc.)
- **Lazy initialization** of AWS resources

## Getting Started

### 1. Add Dependency

Add the YadaAi dependency to your `build.gradle`:

```gradle
implementation 'net.yadaframework:yadaai:x.x.x'
```
When using the sources directly, add the following instead:

```gradle
implementation project(':YadaAi')
```

and add the YadaAi project to your `settings.gradle`:

```gradle
include 'YadaAi'
project(':YadaAi').projectDir = "../../yadaframework/YadaAi" as File
```

### 2. Implement Configuration Interface

Make your configuration class implement `YadaAiConfigurable`:

```java
import net.yadaframework.ai.YadaAiConfigurable;
import net.yadaframework.core.YadaConfiguration;

public class MyAppConfiguration extends YadaConfiguration implements YadaAiConfigurable {
    // AI configuration methods are automatically available through default interface methods
}
```

### 3. Add Configuration Values

Add the following to your `configuration.xml`:

```xml
<config>
    <yadaAi>
        <caption>
            <prompt>Generate descriptive alt text for this image in the following languages: English, Italian, French</prompt>
        </caption>
        <bedrock>
            <accessKeyId>${bedrock_accessKeyId}</accessKeyId>
            <secretAccessKey>${bedrock_secretAccessKey}</secretAccessKey>
            <region>eu-central-1</region>
            <modelId>global.anthropic.claude-sonnet-4-5-20250929-v1:0</modelId>
            <maxTokens>1000</maxTokens>
            <temperature>0.7</temperature>
        </bedrock>
    </yadaAi>
</config>
```

The access keys should be added to the local `security.properties` file in each environment.

## Usage Examples

**Note:** The `BedrockRuntimeClient` bean is automatically created by `YadaAiConfig` as a lazy bean. You don't need to create it manually.

### Basic Text Request

```java
@Autowired
private YadaAiUtil yadaAiUtil;

@Autowired
private MyAppConfiguration config;

public void simpleTextQuery() {
    YadaClaudeRequest request = new YadaClaudeRequest()
        .maxTokens(config.getBedrockMaxTokens())
        .temperature(config.getBedrockTemperature())
        .addMessage(msg -> msg
            .roleUser()
            .content("What is the capital of France?"));
    
    String response = yadaAiUtil.invokeClaudeModel(request);
    System.out.println(response);
}
```

### Image Analysis with Text

```java
public void analyzeImage(Path imagePath) {
    YadaClaudeRequest request = new YadaClaudeRequest()
        .maxTokens(1024)
        .addMessage(msg -> msg
            .roleUser()
            .addContentBlock(block -> block.source(src -> src.data(imagePath)))
            .addContentBlock(block -> block.text("What is in this image?")));
    
    String description = yadaAiUtil.invokeClaudeModel(request);
}
```

### Advanced Request with Multiple Parameters

```java
public void advancedRequest() {
    YadaClaudeRequest request = new YadaClaudeRequest()
        .maxTokens(2048)
        .temperature(0.5)
        .topP(0.9)
        .topK(50)
        .addStopSequence("\n\n")
        .system("You are a helpful assistant specialized in technical documentation.")
        .addMessage(msg -> msg
            .roleUser()
            .content("Explain dependency injection"))
        .metadata(meta -> meta.userId("user-123"));
    
    String response = yadaAiUtil.invokeClaudeModel(request);
}
```

### Using Multiple Messages (Conversation)

```java
public void conversationExample() {
    YadaClaudeRequest request = new YadaClaudeRequest()
        .maxTokens(1024)
        .addMessage(msg -> msg
            .roleUser()
            .content("What is Spring Framework?"))
        .addMessage(msg -> msg
            .roleAssistant()
            .content("Spring Framework is a comprehensive framework for Java development..."))
        .addMessage(msg -> msg
            .roleUser()
            .content("Can you explain dependency injection in Spring?"));
    
    String response = yadaAiUtil.invokeClaudeModel(request);
}
```

## API Reference

### YadaAiUtil Component

The main utility component for AI operations:

#### Methods

- **`String invokeClaudeModel(YadaClaudeRequest request)`**
  - Invokes the Claude model and returns the response as a string
  - Throws `YadaSystemException` if invocation fails

- **`Map<Locale, String> getLocalizedMap(YadaClaudeRequest request)`**
  - Invokes Claude expecting a JSON map response where keys are ISO-2 language codes
  - Example response: `{"en": "Hello", "it": "Ciao", "fr": "Bonjour"}`
  - Automatically cleans markdown code blocks from the response
  - Returns a `Map<Locale, String>` with parsed locales
  - Throws `YadaSystemException` if parsing fails

### YadaClaudeRequest Fluent API

Builder class for constructing Claude API requests:

#### Core Methods

- **`maxTokens(int maxTokens)`** - Sets maximum tokens to generate (required, minimum 1)
- **`temperature(double temperature)`** - Controls randomness (0.0-1.0, lower = more deterministic)
- **`topP(double topP)`** - Nucleus sampling parameter (0.0-1.0)
- **`topK(int topK)`** - Top-k sampling parameter (minimum 1)
- **`system(String system)`** - Sets system prompt as string
- **`system(List<YadaClaudeSystemContent> system)`** - Sets system prompt as structured content

#### Message Building

- **`addMessage(Consumer<YadaClaudeMessage> messageBuilder)`** - Adds a message using lambda
- **`addMessage(YadaClaudeMessage message)`** - Adds a pre-built message

#### Additional Options

- **`addStopSequence(String sequence)`** - Adds a stop sequence
- **`stream(boolean stream)`** - Enables streaming responses
- **`metadata(Consumer<YadaClaudeMetadata> metadataBuilder)`** - Adds metadata
- **`addTool(Consumer<YadaClaudeTool> toolBuilder)`** - Adds a tool definition
- **`toolChoice(Consumer<YadaClaudeToolChoice> toolChoiceBuilder)`** - Sets tool choice preferences

#### Conversion

- **`String toJson()`** - Validates and converts the request to JSON
  - Throws `JsonProcessingException` if serialization fails
  - Throws `IllegalStateException` if validation fails

### YadaClaudeMessage

Message builder with fluent API:

- **`roleUser()`** - Sets role to USER (default)
- **`roleAssistant()`** - Sets role to ASSISTANT
- **`content(String text)`** - Sets simple text content
- **`addContentBlock(Consumer<YadaClaudeContentBlock> builder)`** - Adds structured content (text or image)

### YadaClaudeContentBlock

Content block builder:

- **`text(String text)`** - Creates text content
- **`source(Consumer<YadaClaudeImageSource> sourceBuilder)`** - Creates image content

### YadaClaudeImageSource

Image source builder with automatic handling:

- **`data(Path imagePath)`**
  - Reads image from path and encodes as base64
  - Automatically detects media type from file extension
  - Supported formats: jpg, jpeg, png, gif, webp
  - Throws `YadaInvalidUsageException` for unsupported formats
  - Throws `YadaSystemException` if file cannot be read

- **`data(String base64ImageString)`**
  - Sets pre-encoded base64 image data
  - You must manually call `mediaType()` when using this method

- **`mediaType(YadaClaudeMediaType mediaType)`**
  - Manually sets media type (only needed with string base64 data)

### YadaAiConfigurable Interface Methods

All methods have default implementations that read from Apache Commons Configuration:

- **`String getAiCaptionPrompt()`** - Returns AI caption prompt from `config/yadaAi/caption/prompt`
- **`String getBedrockAccessKeyId()`** - Returns AWS access key from `config/yadaAi/bedrock/accessKeyId`
- **`String getBedrockSecretAccessKey()`** - Returns AWS secret key from `config/yadaAi/bedrock/secretAccessKey`
- **`Region getBedrockRegion()`** - Returns AWS region from `config/yadaAi/bedrock/region`
- **`String getBedrockModelId()`** - Returns Claude model ID from `config/yadaAi/bedrock/modelId`
- **`int getBedrockMaxTokens()`** - Returns max tokens from `config/yadaAi/bedrock/maxTokens` (default: 1000)
- **`double getBedrockTemperature()`** - Returns temperature from `config/yadaAi/bedrock/temperature` (default: 0.7)

All methods throw `YadaConfigurationException` if required values are missing.

## Architecture

### Conditional Import Mechanism

YadaAi uses a conditional import mechanism to avoid classpath errors when the module is not included:

1. **`YadaAiConfigImportSelector`** in YadaWeb checks if `YadaAiConfig` class exists on the classpath
2. If found, it imports `YadaAiConfig` which enables the AI module
3. If not found, the import is skipped silently

This is implemented using Spring's `ImportSelector` interface:

```java
@Import(YadaAiConfigImportSelector.class)
public class YadaAppConfig {
    // ...
}
```

The selector uses `Class.forName()` to detect YadaAi availability without causing compilation errors.

### Component Scanning

YadaAi automatically scans for components in:
- `net.yadaframework.ai.components`

This is configured in `YadaAiConfig` which is conditionally imported by `YadaAiConfigImportSelector`.

### Lazy Initialization

The `BedrockRuntimeClient` bean is created lazily by `YadaAiConfig`:
- Only instantiated when first accessed (e.g., when `YadaAiUtil` uses it)
- Avoids unnecessary AWS connection overhead at application startup
- Useful if AI features are optional in your application

### Validation

The module performs validation at two levels:

1. **Startup validation** (`@PostConstruct` in `YadaAiConfig` and `YadaAiUtil`)
   - Ensures `YadaAiConfigurable` is implemented
   - Provides clear error messages if configuration is missing

2. **Request validation** (in `YadaClaudeRequest.toJson()`)
   - Validates required fields are present
   - Checks parameter ranges (temperature, topP, topK, maxTokens)
   - Prevents invalid requests from being sent to AWS

## Module is Optional

If your project doesn't need AI features:

1. **Don't add** the YadaAi dependency to your build.gradle
2. **Don't implement** `YadaAiConfigurable` in your configuration class
3. Your application will compile and run normally without AI support

The pluggable architecture ensures that YadaAi is completely optional and won't cause compilation errors when absent from the classpath.

## Error Handling

The module throws specific exceptions for different error scenarios:

- **`YadaInternalException`** - Configuration not properly set up (missing `YadaAiConfigurable` implementation)
- **`YadaConfigurationException`** - Required configuration values missing
- **`YadaSystemException`** - Runtime errors (AWS invocation failures, file I/O errors, JSON parsing errors)
- **`YadaInvalidUsageException`** - Invalid API usage (unsupported image format, invalid parameters)
- **`IllegalStateException`** - Request validation failures
- **`IllegalArgumentException`** - Invalid parameter values during request building
