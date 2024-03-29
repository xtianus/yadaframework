package net.yadaframework.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.exceptions.YadaInvalidValueException;

/**
 * Database "order by" definition. Can have multiple columns (=properties) each one with a specific sort order and case sensitivity.
 * Use together with YadaPageRequest
 * @see YadaPageRequest
 */
public class YadaPageSort {
	public final static String KEYWORD_ASC="asc";
	public final static String KEYWORD_DESC="desc";
	public final static String KEYWORD_IGNORECASE="ignorecase";

	private List<Order> orders = new ArrayList<>();

	public class Order {
		String property = null;
		String direction = ""; // default
		boolean ignorecase = false;

		public String getProperty() {
			return property;
		}
		public void setProperty(String property) {
			this.property = property;
		}
		public String getDirection() {
			return direction;
		}
		public void setDirection(String direction) {
			this.direction = direction;
		}
		public boolean isIgnorecase() {
			return ignorecase;
		}
		public void setIgnorecase(boolean ignorecase) {
			this.ignorecase = ignorecase;
		}
	}

	/**
	 * Public API to set sort parameters on page requests
	 * @since 0.7.0
	 */
	public class YadaPageSortApi {
		private List<Order> lastAddedOrders = new ArrayList<>();

		/**
		 * Set 'asc' sort direction on the previously specified sort parameters
		 * @return
		 */
		public YadaPageSortApi asc() {
			for (Order order : lastAddedOrders) {
				if (order.direction=="") {
					order.setDirection(KEYWORD_ASC);
				} else {
					throw new YadaInvalidUsageException("Sort direction already set to {} on '{}'", order.direction, order.property);
				}
			}
			return this;
		}

		/**
		 * Set 'desc' sort direction on the previously specified sort parameters
		 * @return
		 */
		public YadaPageSortApi desc() {
			for (Order order : lastAddedOrders) {
				if (order.direction=="") {
					order.setDirection(KEYWORD_DESC);
				} else {
					throw new YadaInvalidUsageException("Sort direction already set to {} on '{}'", order.direction, order.property);
				}
			}
			return this;
		}

		/**
		 * Set ignorecase sort on the previously specified sort parameters
		 * @return
		 */
		public YadaPageSortApi ignorecase() {
			for (Order order : lastAddedOrders) {
				order.setIgnorecase(true);
			}
			return this;
		}
	}

	/**
	 * Spring Data compatible API to retrieve sort parameters.
	 * @return
	 */
	public Iterator<Order> iterator() {
		return this.orders.iterator();
	}

	/**
	 * @return all the sort parameters
	 */
	public List<Order> getOrders() {
		return orders;
	}

	/**
	 * Package-protected method used by YadaPageRequest for the new sort API
	 * @param sortParameters
	 * @param prepend
	 * @since 0.7.0
	 * @return
	 */
	// Package visibility
	YadaPageSortApi addSortParameters(String sortParameters, Boolean prepend) {
		sortParameters = StringUtils.trimToNull(sortParameters);
		if (sortParameters==null) {
			throw new IllegalArgumentException("Sort parameter name not specified");
		}
		YadaPageSortApi yadaPageSortApi = new YadaPageSortApi();
		String[] parts;
		if (sortParameters.contains("%2C")) {
			// There could be the case where the comma is double-encoded in the request, so when decoded it becomes "%2C" instead of ",".
			parts = sortParameters.split("%2C");
		} else {
			parts = sortParameters.split(",");
		}
		for (String part : parts) {
			Order currentOrder = new Order();
			yadaPageSortApi.lastAddedOrders.add(currentOrder);
			currentOrder.property = part; // column or property name
		}
		if (prepend) {
			orders.addAll(0, yadaPageSortApi.lastAddedOrders);
		} else {
			orders.addAll(yadaPageSortApi.lastAddedOrders);
		}
		return yadaPageSortApi;
	}

	/**
	 * Parses the sort string received in Request. Can be called multiple times.
	 * The sort string syntax is the same as for Spring Data: <pre>property(,property)(,ASC|DESC)(,IgnoreCase)</pre>
	 * Users should use {@link YadaPageRequest#appendSort(String)} and YadaPageRequest#prependSort(String)
	 * @param requestParam example: <pre>"firstname,lastname,desc,ignorecase"</pre>
	 * @see <a href="https://docs.spring.io/spring-data/commons/docs/2.5.0/reference/html/#core.web.basic.paging-and-sorting">Spring Data</a>
	 * @see YadaPageRequest#appendSort(String)
	 * @see YadaPageRequest#prependSort(String)
	 */
	// Package visibility
	void add(String requestParam) {
		List<Order> result = new ArrayList<YadaPageSort.Order>();
		String[] parts;
		if (requestParam.contains("%2C")) {
			// There could be the case where the comma is double-encoded in the request, so when decoded it becomes "%2C" instead of ",".
			parts = requestParam.split("%2C");
		} else {
			parts = requestParam.split(",");
		}
		boolean noMoreCol = false;
		Order currentOrder = null;
		for (String part : parts) {
			if (KEYWORD_IGNORECASE.equalsIgnoreCase(part)) {
				if (currentOrder==null) {
					throw new YadaInvalidValueException("No sort column specified in '{}'", requestParam);
				}
				currentOrder.ignorecase = true;
				noMoreCol = true;
			} else if (KEYWORD_ASC.equalsIgnoreCase(part) || KEYWORD_DESC.equalsIgnoreCase(part)) {
				if (currentOrder==null) {
					throw new YadaInvalidValueException("No sort column specified in '{}'", requestParam);
				}
				if (currentOrder.direction.isEmpty()) {
					currentOrder.direction = part;
					noMoreCol = true;
				} else {
					throw new YadaInvalidValueException("Direction '{}' used after it was set to '{}' already", part, currentOrder.direction);
				}
			} else {
				if (noMoreCol) {
					throw new YadaInvalidValueException("Column name '{}' found after direction or case keywords", part);
				}
				currentOrder = new Order();
				result.add(currentOrder);
				currentOrder.property = part; // column or property name
			}
		}
		orders.addAll(result);
	}

	/**
	 * Adds the given sort order to the list.
	 * @param order
	 */
	public void add(Order order) {
		this.orders.add(order);
	}

}
