package net.yadaframework.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import jakarta.annotation.PostConstruct;

import net.yadaframework.exceptions.YadaInternalException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
@ComponentScan(basePackages = { "net.yadaframework.ai.components" })
public class YadaAiConfig {

	@Autowired(required = false)
	private YadaAiConfigurable config; // Will be null if the application configuration doesn't implement YadaAiConfigurable

	@PostConstruct
	public void validateConfiguration() {
		if (config == null) {
			throw new YadaInternalException("YadaAiConfigurable configuration not found. Ensure your application configuration implements YadaAiConfigurable.");
		}
	}

	/**
	 * Creates a lazy-initialized BedrockRuntimeClient bean.
	 * The client is only instantiated when first accessed (e.g., when AiUtil uses it),
	 * not at application startup. This avoids unnecessary AWS connection overhead
	 * if AI features are not used.
	 * 
	 * @return configured BedrockRuntimeClient instance
	 */
	@Bean
	@Lazy
	public BedrockRuntimeClient bedrockRuntimeClient() {
		String accessKeyId = config.getBedrockAccessKeyId();
		String secretAccessKey = config.getBedrockSecretAccessKey();
		AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
		
		return BedrockRuntimeClient.builder()
			.region(config.getBedrockRegion())
			.credentialsProvider(StaticCredentialsProvider.create(awsCreds))
			.build();
	}

}
