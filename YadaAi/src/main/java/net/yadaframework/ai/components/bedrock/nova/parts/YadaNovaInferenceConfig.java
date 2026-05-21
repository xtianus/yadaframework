package net.yadaframework.ai.components.bedrock.nova.parts;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaInferenceConfig {
	private Integer maxTokens;
	private Double temperature;
	private Double topP;
	private Integer topK;
	private List<String> stopSequences;

	public YadaNovaInferenceConfig() {}

	public YadaNovaInferenceConfig maxTokens(int maxTokens) {
		this.maxTokens = maxTokens;
		return this;
	}

	public YadaNovaInferenceConfig temperature(double temperature) {
		this.temperature = temperature;
		return this;
	}

	public YadaNovaInferenceConfig topP(double topP) {
		this.topP = topP;
		return this;
	}

	public YadaNovaInferenceConfig topK(int topK) {
		this.topK = topK;
		return this;
	}

	public YadaNovaInferenceConfig stopSequences(List<String> stopSequences) {
		this.stopSequences = stopSequences;
		return this;
	}

	public YadaNovaInferenceConfig addStopSequence(String stopSequence) {
		if (this.stopSequences == null) {
			this.stopSequences = new ArrayList<>();
		}
		this.stopSequences.add(stopSequence);
		return this;
	}

	// Remove getters to keep autocompletion clean
//	public Integer getMaxTokens() { return maxTokens; }
//	public Double getTemperature() { return temperature; }
//	public Double getTopP() { return topP; }
//	public Integer getTopK() { return topK; }
//	public List<String> getStopSequences() { return stopSequences; }
}
