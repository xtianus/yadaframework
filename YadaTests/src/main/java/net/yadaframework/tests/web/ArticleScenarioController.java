package net.yadaframework.tests.web;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.yadaframework.components.YadaNotify;
import net.yadaframework.tests.persistence.repository.ArticleDao;
import net.yadaframework.web.YadaPageRequest;

/**
 * CMS content: paged and sorted article list, localized name rendering and editing
 */
@Controller
@RequestMapping("/test/articles")
public class ArticleScenarioController {
	private static final int PAGE_SIZE = 2;

	private final ArticleDao articleDao;
	private final YadaNotify yadaNotify;

	public ArticleScenarioController(ArticleDao articleDao, YadaNotify yadaNotify) {
		this.articleDao = articleDao;
		this.yadaNotify = yadaNotify;
	}

	@GetMapping("")
	public String list(YadaPageRequest yadaPageRequest, Model model) {
		if (!yadaPageRequest.isValid()) {
			// First page: keep any requested sort
			yadaPageRequest.setPage(0);
			yadaPageRequest.setSize(PAGE_SIZE);
		}
		if (yadaPageRequest.getPageSort().getOrders().isEmpty()) {
			yadaPageRequest.appendSort("sku").asc();
		}
		model.addAttribute("articles", articleDao.find(yadaPageRequest));
		return "/test/articles";
	}

	@GetMapping("/{id}")
	public String edit(@PathVariable Long id, Model model) {
		model.addAttribute("article", articleDao.find(id));
		return "/test/articleEdit";
	}

	@PostMapping("/{id}")
	public String save(@PathVariable Long id, String name, Locale locale, RedirectAttributes redirectAttributes) {
		articleDao.rename(id, locale, name);
		yadaNotify.titleKey(redirectAttributes, "yts.articles.title").ok().messageKey("yts.articles.saved").add();
		return "redirect:/test/articles/" + id;
	}
}
