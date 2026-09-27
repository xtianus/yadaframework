package net.yadaframework.ai.components.bedrock;

import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface YadaAiMessageInterface {

	String toJson() throws JsonProcessingException;

	String extractText(String responseBody);

	YadaAiMessageInterface maxTokens(int maxTokens);

	YadaAiMessageInterface temperature(double temperature);

	YadaAiMessageInterface topP(double topP);

	YadaAiMessageInterface system(String system);

	YadaAiMessageInterface addUserText(String text);

	YadaAiMessageInterface addUserImage(Path imagePath);

	YadaAiMessageInterface addAssistantText(String text);
}
