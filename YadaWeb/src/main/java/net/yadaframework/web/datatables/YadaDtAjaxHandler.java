package net.yadaframework.web.datatables;

/**
 * Functional interface for DataTable ajax handler method references.
 * Used with YadaDataTable.dtAjaxUrl(YadaDtAjaxHandler) to enable type-safe
 * method references like this::userProfileTablePage.
 * 
 * The referenced method should match the signature:
 * Map&lt;String,Object&gt; method(YadaDatatablesRequest request, Locale locale)
 */
@FunctionalInterface
public interface YadaDtAjaxHandler extends java.io.Serializable {
	java.util.Map<String, Object> handle(net.yadaframework.web.YadaDatatablesRequest request, java.util.Locale locale);
}
