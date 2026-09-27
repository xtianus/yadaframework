package net.yadaframework.web;

import static org.junit.jupiter.api.Assertions.assertEquals;


import java.util.List;

import org.junit.jupiter.api.Test;

import net.yadaframework.web.YadaPageSort.Order;

/**
 * Verifies that sort parameters accumulate instead of replacing the ones already requested, and that
 * prependSort really puts them first.
 */
class YadaPageRequestSortTest {

	private List<String> propertiesOf(YadaPageRequest pageRequest) {
		return pageRequest.getPageSort().getOrders().stream().map(Order::getProperty).toList();
	}

	@Test
	void appendSortKeepsTheExistingOrdering() {
		YadaPageRequest pageRequest = YadaPageRequest.of(0, 10);
		pageRequest.appendSort("publishDate").desc();
		pageRequest.appendSort("id").desc();

		assertEquals(List.of("publishDate", "id"), propertiesOf(pageRequest));
		List<Order> orders = pageRequest.getPageSort().getOrders();
		assertEquals(YadaPageSort.KEYWORD_DESC, orders.get(0).getDirection());
		assertEquals(YadaPageSort.KEYWORD_DESC, orders.get(1).getDirection());
	}

	@Test
	void prependSortPutsTheNewParametersFirst() {
		YadaPageRequest pageRequest = YadaPageRequest.of(0, 10);
		pageRequest.appendSort("publishDate");
		pageRequest.prependSort("relevance");

		assertEquals(List.of("relevance", "publishDate"), propertiesOf(pageRequest));
	}

	@Test
	void severalParametersCanBeAddedInOneCall() {
		YadaPageRequest pageRequest = YadaPageRequest.of(0, 10);
		pageRequest.appendSort("publishDate,id").desc();

		assertEquals(List.of("publishDate", "id"), propertiesOf(pageRequest));
	}

	@Test
	void theExtraRowIsRequestedForTheHasMoreFlag() {
		YadaPageRequest pageRequest = YadaPageRequest.of(2, 12);
		assertEquals(24, pageRequest.getFirstResult()); // zero-based page index, not an offset
		assertEquals(13, pageRequest.getMaxResults()); // one extra row
	}
}
