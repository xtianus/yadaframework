package net.yadaframework.web.datatables.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.yadaframework.core.YadaFluentBase;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDTBreakpoint extends YadaFluentBase<YadaDTResponsive> {
	@JsonProperty private String name;
	@JsonProperty private String width;

	public YadaDTBreakpoint(YadaDTResponsive parent) {
		super(parent);
	}

	public YadaDTBreakpoint dtName(String name) {
		this.name = name;
		return this;
	}

	public YadaDTBreakpoint dtWidth(String width) {
		this.width = width;
		return this;
	}

}
