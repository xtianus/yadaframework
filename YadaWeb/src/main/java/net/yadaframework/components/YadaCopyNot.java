package net.yadaframework.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fields marked by this annotation are not copied at all when cloning with YadaUtil, and the value is not initialized (e.g. null or zero).
 * This is an evolution of {@link net.yadaframework.core.CloneableFiltered#getExcludedFields()}
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface YadaCopyNot {

}
