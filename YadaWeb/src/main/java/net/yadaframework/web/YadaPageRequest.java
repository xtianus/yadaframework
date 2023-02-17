package net.yadaframework.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import net.yadaframework.web.YadaPageSort.YadaPageSortApi;

/**
 * A page for pageable content.
 * A common use case is to implement web pagination in both directions.
 * It also solves the problem of the browser back button not loading previously loaded values, via the loadPrevious flag.
 */
public class YadaPageRequest {
	private int page = -1;
	private int size = 0;
	private boolean loadPrevious = false;
	private List<String> sort = new ArrayList<>(); // Request parameters
	private YadaPageSort parsedSort = null; // Parsed request sort parameters
	// Attributes for scrolling into position when using a bookmark
	private String yadaContainer; // id of the element that contains the rows and needs scrolling
	private int yadaScroll = 0; // Scroll amount
	private String paramPrefix = null; // prefix on page parameters for multiple pagination sections

	/**
	 * Add sort parameters after any existing ones
	 * @param paramNames comma-separated list of sort parameters
	 * @return
	 */
	public YadaPageSortApi appendSort(String paramNames) {
		parsedSort = new YadaPageSort();
		return parsedSort.addSortParameters(paramNames, false);
	}

	/**
	 * Add sort parameters before any existing ones
	 * @param paramNames comma-separated list of sort parameters
	 * @return
	 */
	public YadaPageSortApi prependSort(String paramNames) {
		parsedSort = new YadaPageSort();
		return parsedSort.addSortParameters(paramNames, false);
	}

	/**
	 * Creates a YadaPageRequest with the given page and size.
	 * Drop-in replacement for the equivalent Spring Data PageRequest.of() method
	 * @param page
	 * @param size
	 * @return
	 */
	public static YadaPageRequest of(int page, int size) {
		return new YadaPageRequest(page, size);
	}

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
		this(page, size, false, null);
	}

	/**
	 * Creates a new {@link YadaPageRequest}. Pages are zero indexed, thus providing 0 for {@code page} will return the first
	 * page.
	 *
	 * @param page zero-based page index, must not be less than zero.
	 * @param size the size of the page to be returned, must not be less than one.
	 * @param loadPrevious true if all pages before this one must be fetched from database
	 * @param paramPrefix prefix to pagination request parameters to use for multiple paginations on same page
	 */
	public YadaPageRequest(int page, int size, boolean loadPrevious, String paramPrefix) {
		if (page < 0) {
			throw new IllegalArgumentException("Page index must not be less than zero!");
		}
		if (size < 1) {
			throw new IllegalArgumentException("Page size must not be less than one!");
		}
		this.page = page;
		this.size = size;
		this.loadPrevious = loadPrevious;
		this.paramPrefix = paramPrefix;
	}

	/**
	 * Add a sort order to this request
	 * @param paramName the name to sort on, can be multiple comma-separated names
	 * @param desc true for descending order, otherwise false or null
	 * @param ignoreCase true to ignore case when sorting, otherwise false or null
	 * @return
	 * @deprecated use {@link #appendSort(String)} and {@link #prependSort(String)} instead
	 */
	@Deprecated
	public YadaPageRequest addSort(String paramName, Boolean desc, Boolean ignoreCase) {
		paramName = StringUtils.trimToNull(paramName);
		if (paramName==null) {
			throw new IllegalArgumentException("Sort parameter name not specified");
		}
		if (Boolean.TRUE.equals(desc)) {
			paramName += "," + YadaPageSort.KEYWORD_DESC;
		}
		if (Boolean.TRUE.equals(ignoreCase)) {
			paramName += "," + YadaPageSort.KEYWORD_IGNORECASE;
		}
		if (parsedSort==null) {
			parsedSort = new YadaPageSort();
		}
		parsedSort.add(paramName);
		return this;
	}

	/**
	 * @return the page sort options
	 */
	public YadaPageSort getPageSort() {
		if (parsedSort==null) {
			parsedSort = new YadaPageSort();
			for (String requestParam : sort) {
				parsedSort.add(requestParam);
			}
		}
		return parsedSort;
	}

	/**
	 * Set the page sort options
	 * @param pageSort
	 */
	public void setPageSort(YadaPageSort pageSort) {
		this.parsedSort = pageSort;
	}

	/**
	 * Tell if data from previous pages should also be returned
	 * @param loadPrevious true to load data from previous pages (only applicabile when page>1)
	 */
	public void setLoadPrevious(boolean loadPrevious) {
		this.loadPrevious = loadPrevious;
	}

	public YadaPageRequest getNextPageRequest() {
		return new YadaPageRequest(page + 1, size);
	}

	public YadaPageRequest getPreviousPageRequest() {
		return page == 0 ? this : new YadaPageRequest(page - 1, size);
	}

	public YadaPageRequest getFirstPageRequest() {
		return new YadaPageRequest(0, size);
	}

	@Override
	public String toString() {
		return String.format("Page request [page: %d, size: %d]", page, size);
	}

	/**
	 * @return the number of rows for this page
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @return The current page number starting at 0
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
	 * Returns the amount of rows to fetch from the database + 1.
	 * It is equal to {@link #getSize()+1} when loadPrevious is false, otherwise it
	 * adds the count of all the previous pages to the value then adds 1
	 * to find out if there are more rows to fetch after this page.
	 * Note: this method must only be used when the results are stored into a YadaPageRows object
	 * otherwise the page size will be one element bigger than expected.
	 * <p>If you are not going to store the result in YadaPageRows, use {@link #getSize()} instead</p>
	 * @return
	 * @see YadaPageRows
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

	/**
	 * Spring Data - compatible method to get the page size (number of rows)
	 * @return
	 * @see #getSize()
	 */
	public int getPageSize() {
		return page;
	}

	/**
	 * sort strings passed as request parameters - not to be used by the application
	 * @return
	 * @see #getPageSort()
	 */
	public List<String> getSort() {
		return sort;
	}

	/**
	 * sort strings passed as request parameters - not to be used by the application
	 * @param sort
	 */
	public void setSort(List<String> sort) {
		this.sort = sort;
	}

	public String getYadaContainer() {
		return yadaContainer;
	}

	public void setYadaContainer(String yadaContainer) {
		this.yadaContainer = yadaContainer;
	}

	public int getYadaScroll() {
		return yadaScroll;
	}

	public void setYadaScroll(int yadaScroll) {
		this.yadaScroll = yadaScroll;
	}

	public String getParamPrefix() {
		return paramPrefix;
	}

	public void setParamPrefix(String paramPrefix) {
		this.paramPrefix = paramPrefix;
	}

}
