package net.yadaframework.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fields marked by this annotation are not recursively copied when cloning with YadaUtil, but copied by value,
 * unless their value has been cloned in an ancestor object, in which case the cloned version will be used as a value.<br>
 * <br>
 * For example, if ProjectA contains ModuleA that has a project field referring to ProjectA with this annotation, then when
 * cloning ModuleA the new ModuleB will keep a reference to ProjectA (shallow copy); when
 * cloning ProjectA (the parent of ModuleA) the new ProjectB will contain a ModuleB with a reference to ProjectB (not
 * to ProjectA because a clone of it has been made already).<br>
 * <br>
 * If this annotation is not used, it makes no difference for the latter case because ModuleB would get a reference to ProjectB
 * that has already been cloned (there's a check to prevent infinite loops). 
 * In the former case though, when cloning just ModuleA (and not its parent ProjectA) the cloned
 * module ModuleB would get a reference to a clone of ProjectA and this probably not what you mean as you're not cloning the project. 
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface YadaCopyShallow {

}
