package net.yadaframework.components;

import java.util.HashMap;

import org.springframework.stereotype.Component;

@Component
public class YadaJsonMapper extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	public YadaJsonMapper getObject(String name) {
		return (YadaJsonMapper) super.get(name);
	}

}
