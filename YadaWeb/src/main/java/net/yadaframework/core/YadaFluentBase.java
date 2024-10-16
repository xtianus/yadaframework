package net.yadaframework.core;

/**
 * Base class for fluent interface implementations that allows nesting
 * @param <T>
 */
public abstract class YadaFluentBase<T> {
	protected final T parent;

    public YadaFluentBase(T parent) {
        this.parent = parent;
    }

    // Method to return to parent for fluent chaining
    public T back() {
        return parent;
    }
}
