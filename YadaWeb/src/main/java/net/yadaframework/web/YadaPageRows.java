package net.yadaframework.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import net.yadaframework.exceptions.YadaInvalidUsageException;

public class YadaPageRows<T> implements Iterable<T> {
	private final List<T> rows = new ArrayList<T>();
	private final YadaPageRequest currentPageRequest;
	private Long outOfRows = null; // Total number of elements that would be returned without pagination
	private boolean hasMoreRows = false;
	
	/**
	 * @param rows the result
	 * @param currentPageRequest the page request that generated this result
	 * @param outOfRows count of the total number of rows that would be returned without pagination
	 */
	public YadaPageRows(List<T> rows, YadaPageRequest currentPageRequest, long outOfRows) {
		this(rows, currentPageRequest);
		this.outOfRows = outOfRows;
		// If we loaded all previous pages too, then rows.size() counts rows from all previous pages
		long totSoFar = currentPageRequest.getFirstResult() + rows.size();
		if (totSoFar > outOfRows) {
			throw new YadaInvalidUsageException("Found more rows that the maximum count: the value of outOfRows is too small");
		}
	}

	/**
	 * 
	 * @param rows the result
	 * @param currentPageRequest the page request that generated this result
	 */
	public YadaPageRows(List<T> rows, YadaPageRequest currentPageRequest) {
		this.rows.addAll(rows);
		this.currentPageRequest = currentPageRequest;
		// We have fetched one more row to check if there is more data after the current page, so we have to fix that
		this.hasMoreRows = rows.size()==currentPageRequest.getMaxResults();
		if (this.hasMoreRows) {
			// removing the last element that was fetched just to check for more data
			this.rows.remove(this.rows.size()-1);
		}
	}

	/**
	 * Returns the page data fetched from database. It also contains the rows of all previous pages if {@link YadaPageRequest#isLoadPrevious()} is true
	 * @return
	 */
	public List<T> getRows() {
		return rows;
	}

	/**
	 * 
	 * @return the number of rows fetched from database, can be less, equal or higher than the page size
	 */
	public int getRowNumber() {
		return rows.size();
	}
	
	/**
	 * 
	 * @return the page size
	 */
	public int getPageSize() {
		return currentPageRequest.getSize();
	}
	
	/**
	 * 
	 * @return the page number
	 */
	public int getPage() {
		return currentPageRequest.getPage();
	}
	
	/**
	 * 
	 * @return the next page number
	 */
	public int getNextPage() {
		return currentPageRequest.getNextPageRequest().getPage();
	}
	
	/**
	 * 
	 * @return the YadaPageRequest that generated this YadaPageContent
	 */
	public YadaPageRequest getYadaPageRequest() {
		return currentPageRequest;
	}

	/**
	 * 
	 * @return the total number of rows (count) that would be returned without pagination
	 */
	public long getOutOfRows() {
		if (outOfRows==null) {
			throw new YadaInvalidUsageException("outOfRows has not been initialized");
		}
		return outOfRows;
	}

	/**
	 * 
	 * @return true if there is more data to fetch from the database, false if this is the last available page
	 */
	public boolean hasMoreRows() {
		return hasMoreRows;
	}
	
	/**
	 * @return true if there are no elements
	 */
	public boolean isEmpty() {
		return rows.isEmpty();
	}

	/**
	 * @return true if this is the first page
	 */
	public boolean isFirst() {
		return currentPageRequest.isFirst();
	}

	/**
	 * @return true if this is the last page in the database
	 */
	public boolean isLast() {
		return !hasMoreRows;
	}

	@Override
	public Iterator<T> iterator() {
		return rows.iterator();
	}

	@Override
	public int hashCode() {
		return Objects.hash(rows, hasMoreRows, outOfRows, currentPageRequest);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("unchecked")
		YadaPageRows<T> other = (YadaPageRows<T>) obj;
		return Objects.equals(rows, other.rows) && hasMoreRows == other.hasMoreRows && outOfRows == other.outOfRows
				&& Objects.equals(currentPageRequest, other.currentPageRequest);
	}

	/**
	 * Returns the next page request, for use in the "load more" buttons
	 * @return
	 */
	public YadaPageRequest getNextPageRequest() {
		return currentPageRequest.getNextPageRequest();
	}


}
