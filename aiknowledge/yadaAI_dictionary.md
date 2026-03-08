# YadaAI Code Dictionary

## `net.yadaframework.ai.components`

| Class | Description |
|---|---|
| [YadaAiUtil](yadaAI/YadaAiUtil.md) | Collects helper methods for localized prompts and Claude Bedrock invocation. Use when Yada code needs one-call AI request execution. |

## `net.yadaframework.ai.components.bedrock.claude`

| Class | Description |
|---|---|
| [YadaClaudeRequest](yadaAI/YadaClaudeRequest.md) | Builds an Anthropic Claude request for AWS Bedrock, including messages, system prompts, tools, and generation settings. Use before serializing and submitting a chat request. |

## `net.yadaframework.ai.components.bedrock.claude.parts`

| Class | Description |
|---|---|
| [YadaClaudeContentBlock](yadaAI/YadaClaudeContentBlock.md) | Builds the claude content block section of a Claude Bedrock request. Use while composing YadaClaudeRequest. |
| [YadaClaudeImageSource](yadaAI/YadaClaudeImageSource.md) | Builds the claude image source section of a Claude Bedrock request. Use while composing YadaClaudeRequest. |
| [YadaClaudeInputSchema](yadaAI/YadaClaudeInputSchema.md) | Builds the claude input schema section of a Claude Bedrock request. Use while composing YadaClaudeRequest. |
| [YadaClaudeMessage](yadaAI/YadaClaudeMessage.md) | Builds the claude message section of a Claude Bedrock request. Use while composing YadaClaudeRequest. |
| [YadaClaudeMetadata](yadaAI/YadaClaudeMetadata.md) | Builds the claude metadata section of a Claude Bedrock request. Use while composing YadaClaudeRequest. |
| [YadaClaudeSystemContent](yadaAI/YadaClaudeSystemContent.md) | Builds the claude system content section of a Claude Bedrock request. Use while composing YadaClaudeRequest. |
| [YadaClaudeTool](yadaAI/YadaClaudeTool.md) | Builds the claude tool section of a Claude Bedrock request. Use while composing YadaClaudeRequest. |
| [YadaClaudeToolChoice](yadaAI/YadaClaudeToolChoice.md) | Builds the claude tool choice section of a Claude Bedrock request. Use while composing YadaClaudeRequest. |
