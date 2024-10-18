package net.yadaframework.web.datatables;

/**
 * Functional interface used internally
 */
@FunctionalInterface
public interface YadaDataTableConfigurer {
	void configure(YadaDataTable yadaDataTable);
}
