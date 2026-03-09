package net.yadaframework.ai;

import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;

import net.yadaframework.exceptions.YadaConfigurationException;
import software.amazon.awssdk.regions.Region;

/**
 * Interface for AI-related configuration methods.
 * Application configuration classes can implement this interface to gain access to AI configuration
 * when the YadaAi module is in the classpath.
 * All methods are default implementations that read from the Apache Commons Configuration.
 */
public interface YadaAiConfigurable {

	/**
	 * Provides access to the underlying Apache Commons Configuration.
	 * This method must be implemented by the configuration class.
	 * @return the configuration object
	 */
	ImmutableHierarchicalConfiguration getConfiguration();

	/**
	 * @return the AWS access key ID for Bedrock
	 */
	default String getBedrockAccessKeyId() {
		String key = "config/yadaAi/bedrock/accessKeyId";
		String accessKeyId = getConfiguration().getString(key, null);
		if (accessKeyId==null) {
			throw new YadaConfigurationException("AWS access key ID for Bedrock not found in configuration at key {}", key);
		}
		return accessKeyId;
	}

	/**
	 * @return the AWS secret access key for Bedrock
	 */
	default String getBedrockSecretAccessKey() {
		String key = "config/yadaAi/bedrock/secretAccessKey";
		String secretAccessKey = getConfiguration().getString(key, null);
		if (secretAccessKey==null) {
			throw new YadaConfigurationException("AWS secret access key for Bedrock not found in configuration at key {}", key);
		}
		return secretAccessKey;
	}

	/**
	 * @return the AWS region for Bedrock
	 */
	default Region getBedrockRegion() {
		String key = "config/yadaAi/bedrock/region";
		String region = getConfiguration().getString(key, null);
		if (region==null) {
			throw new YadaConfigurationException("AWS region for Bedrock not found in configuration at key {}", key);
		}
		return Region.of(region);
	}

	/**
	 * @return the Claude model ID for Bedrock
	 */
	default String getBedrockModelId() {
		String key = "config/yadaAi/bedrock/modelId";
		String modelId = getConfiguration().getString(key, null);
		if (modelId==null) {
			throw new YadaConfigurationException("Claude model ID for Bedrock not found in configuration at key {}", key);
		}
		return modelId;
	}

	/**
	 * @return the maximum number of tokens for Claude responses
	 */
	default int getBedrockMaxTokens() {
		return getConfiguration().getInt("config/yadaAi/bedrock/maxTokens", 1000);
	}

	/**
	 * @return the temperature setting for Claude (controls randomness)
	 */
	default double getBedrockTemperature() {
		return getConfiguration().getDouble("config/yadaAi/bedrock/temperature", 0.7);
	}
}
