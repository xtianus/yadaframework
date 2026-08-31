package net.yadaframework.ai.components.bedrock.nova.parts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaNovaInputSchema {
	private String type;
	private Map<String, Object> properties;
	private List<String> required;

	public YadaNovaInputSchema() {
		this.properties = new HashMap<>();
		this.required = new ArrayList<>();
	}

	public YadaNovaInputSchema type(String type) {
		this.type = type;
		return this;
	}

	public YadaNovaInputSchema properties(Map<String, Object> properties) {
		this.properties = properties;
		return this;
	}

	public YadaNovaInputSchema addProperty(String name, Object schema) {
		this.properties.put(name, schema);
		return this;
	}

	public YadaNovaInputSchema required(List<String> required) {
		this.required = required;
		return this;
	}

	public YadaNovaInputSchema addRequired(String field) {
		this.required.add(field);
		return this;
	}

	// Remove getters to keep autocompletion clean
//	public String getType() { return type; }
//	public Map<String, Object> getProperties() { return properties; }
//	public List<String> getRequired() { return required; }
}
