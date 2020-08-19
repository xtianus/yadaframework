package net.yadaframework.web;

import java.util.Objects;

/**
 * A page for pageable content.
 * 
 */
public class YadaPageRequest {
	private int page = -1;
	private int size = 0;
	private boolean loadPrevious = false;
	
	/**
	 * Creates a new "invalid" YadaPageRequest, with {@code page=-1, size=0}.
	 * Has the same meaning of a "null" value. 
	 * Used by Spring when injecting method parameters in a @Controller
	 * with no request values to set.
	 * See {@link #isValid()}
	 */
	public YadaPageRequest() {
	}
	
	/**
	 * Check if this object has been created with actual values.
	 * @return true if this object has been initialized
	 */
	public boolean isValid() {
		return page>-1 && size>0;
	}

	/**
	 * Creates a new {@link YadaPageRequest}. Pages are zero indexed, thus providing 0 for {@code page} will return the first
	 * page.
	 * 
	 * @param page zero-based page index, must not be less than zero.
	 * @param size the size of the page to be returned, must not be less than one.
	 */
	public YadaPageRequest(int page, int size) {
		this(page, size, false);
	}
	
	/**
	 * Creates a new {@link YadaPageRequest}. Pages are zero indexed, thus providing 0 for {@code page} will return the first
	 * page.
	 * 
	 * @param page zero-based page index, must not be less than zero.
	 * @param size the size of the page to be returned, must not be less than one.
	 * @param loadPrevious true if all pages before this one must be fetched from database
	 */
	public YadaPageRequest(int page, int size, boolean loadPrevious) {
		if (page < 0) {
			throw new IllegalArgumentException("Page index must not be less than zero!");
		}
		if (size < 1) {
			throw new IllegalArgumentException("Page size must not be less than one!");
		}
		this.page = page;
		this.size = size;
		this.loadPrevious = loadPrevious;
	}

	/**
	 * Tell if data from previous pages should also be returned
	 * @param loadPrevious true to load data from previous pages (only applicabile when page>1)
	 */
	public void setLoadPrevious(boolean loadPrevious) {
		this.loadPrevious = loadPrevious;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.YadaPageRequest#next()
	 */
	public YadaPageRequest getNextPageRequest() {
		return new YadaPageRequest(page + 1, size);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.AbstractYadaPageRequest#previous()
	 */
	public YadaPageRequest getPreviousPageRequest() {
		return page == 0 ? this : new YadaPageRequest(page - 1, size);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.YadaPageRequest#first()
	 */
	public YadaPageRequest getFirstPageRequest() {
		return new YadaPageRequest(0, size);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Page request [page: %d, size %d]", page, size);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.YadaPageRequest#getPageSize()
	 */
	public int getSize() {
		return size;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.YadaPageRequest#getPageNumber()
	 */
	public int getPage() {
		return page;
	}

	/**
	 * Returns the position of the first element of this page in the database: {@code size*page}
	 * @return
	 */
	public int getOffset() {
		return page * size;
	}
	
	/**
	 * Returns the position of the first element to be loaded from the database: 
	 * {@code size*page} when loadPrevious is false, 0 otherwise
	 * @return
	 */
	public int getFirstResult() {
		return loadPrevious ? 0 : page * size;
	}
	
	/**
	 * Returns the amount of rows to fetch from the database. 
	 * It is equal to {@link #getSize()+1} when loadPrevious is false, otherwise it
	 * adds the count of all the previous pages to the value then adds 1.
	 * 1 is added to find out if there are more rows to fetch after this page.
	 * @return
	 */
	public int getMaxResults() {
		return loadPrevious ? getOffset() + size + 1: size + 1;
	}
	
	public boolean isFirst() {
		return page == 0;
	}

	public YadaPageRequest getPreviousOrFirstRequest() {
		return isFirst() ? getFirstPageRequest() : getPreviousPageRequest();
	}

	public boolean isLoadPrevious() {
		return loadPrevious;
	}

	@Override
	public int hashCode() {
		return Objects.hash(loadPrevious, page, size);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		YadaPageRequest other = (YadaPageRequest) obj;
		return loadPrevious == other.loadPrevious && page == other.page && size == other.size;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public void setSize(int size) {
		this.size = size;
	}	
}
