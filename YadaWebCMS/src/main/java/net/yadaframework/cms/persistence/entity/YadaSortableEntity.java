package net.yadaframework.cms.persistence.entity;

import net.yadaframework.persistence.entity.YadaAttachedFile;

/**
 * Entities can be sorted visually (by showing a list of thumbnails) if they implement this interface.
 * Use with /yadacms/entitySorter
 *
 */
public interface YadaSortableEntity {

	/**
	 * Returns the representative image used for the sorting interface
	 */
	YadaAttachedFile getThumbnail();
	
	/**
	 * Returns a string representation of the element
	 * @return
	 */
	String getTitle();
	
}
