package net.yadaframework.example.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.web.YadaPageRequest;
import net.yadaframework.web.YadaPageRows;

/**
 * Demonstrates pagination with preserved filter history.
 * Uses an in-memory dataset so no database is needed.
 */
@Controller
public class PaginationController {
	private static final int PAGE_SIZE = 4;

	@Autowired private YadaWebUtil yadaWebUtil;

	/** Simple item with a catalog and a category for demonstrating filters. */
	public record Item(int id, String name, String catalog, String category) {}

	private static final List<Item> ALL_ITEMS = new ArrayList<>();
	static {
		String[] catalogs = {"Indoor", "Outdoor"};
		String[] categories = {"Lamp", "Table", "Chair", "Shelf"};
		int id = 1;
		for (String catalog : catalogs) {
			for (String category : categories) {
				for (int i = 1; i <= 20; i++) {
					ALL_ITEMS.add(new Item(id++, catalog + " " + category + " " + i, catalog, category));
				}
			}
		}
		// 24 items: 2 catalogs × 4 categories × 3 per group
	}

	/**
	 * Paginated and filtered item list. Handles both initial page load and ajax "load more" requests.
	 */
	@RequestMapping("/pagination")
	public String pagination(@RequestParam(required = false) String catalog, @RequestParam(required = false) List<String> category, YadaPageRequest yadaPageRequest, HttpServletRequest request, Model model) {
		if (!yadaPageRequest.isValid()) {
			yadaPageRequest = new YadaPageRequest(0, PAGE_SIZE);
		}
		// Filter
		List<Item> filtered = ALL_ITEMS.stream()
			.filter(item -> catalog == null || catalog.isEmpty() || item.catalog().equals(catalog))
			.filter(item -> category == null || category.isEmpty() || category.contains(item.category()))
			.toList();
		// Paginate: fetch maxResults (pageSize+1) so YadaPageRows can detect the last page
		int first = Math.min(yadaPageRequest.getFirstResult(), filtered.size());
		int end = Math.min(first + yadaPageRequest.getMaxResults(), filtered.size());
		YadaPageRows<Item> result = new YadaPageRows<>(filtered.subList(first, end), yadaPageRequest);
		model.addAttribute("items", result);
		model.addAttribute("catalog", catalog);
		model.addAttribute("categories", category);
		model.addAttribute("allCatalogs", new String[]{"Indoor", "Outdoor"});
		model.addAttribute("allCategories", new String[]{"Lamp", "Table", "Chair", "Shelf"});
		model.addAttribute("yadaContainer", yadaPageRequest.getYadaContainer());
		model.addAttribute("yadaScroll", yadaPageRequest.getYadaScroll());
		if (yadaWebUtil.isAjaxRequest(request)) {
			return "/pagination :: itemList";
		}
		return "/pagination";
	}

	/**
	 * Simple detail page for testing back button navigation.
	 */
	@RequestMapping("/pagination/detail")
	public String detail(@RequestParam int id, Model model) {
		ALL_ITEMS.stream()
			.filter(item -> item.id() == id)
			.findFirst()
			.ifPresent(item -> model.addAttribute("item", item));
		return "/paginationDetail";
	}
}
