package net.yadaframework.core;


/**
 * Marker interface that tells that if this object is inside a collection,
 * it will be recursively cloned before being added to the cloned collection of the parent.
 * When not present, the original object reference will be added to the cloned collection,
 * so that the object instances will be shared between original and cloned parent.
 * It has no effect when an object is NOT inside a collection.
 * @see ClonableFiltered
 */
public interface CloneableDeep extends CloneableFiltered {
}
