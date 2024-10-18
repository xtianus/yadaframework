package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

public class YadaDTOptionSearchBuilder extends YadaFluentBase<YadaDTColumns> {
	private Integer defaultCondition;
	private Object orthogonal;
	
	public YadaDTOptionSearchBuilder(YadaDTColumns parent) {
		super(parent);
	}
	
    public YadaDTOptionSearchBuilder defaultCondition(Integer defaultCondition) {
        this.defaultCondition = defaultCondition;
        return this;
    }
    
    public YadaDTOptionSearchBuilder orthogonal(Object orthogonal) {
    	this.orthogonal = orthogonal;
    	return this;
    }

	public Integer getDefaultCondition() {
		return defaultCondition;
	}

	public Object getOrthogonal() {
		return orthogonal;
	}

}
