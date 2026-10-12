package net.yadaframework.tests.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import net.yadaframework.cms.persistence.entity.YadaArticle;
import net.yadaframework.tests.persistence.repository.ArticleDao;

/**
 * CMS articles: localized names, pagination and sorting with YadaPageRequest, editing with a committed write
 */
class ArticlesE2eTest extends BrowserTestSupport {

	@Test
	void pagesFollowEachOther() {
		page.navigate("/test/articles");
		assertThat(page.getByTestId("articleSku")).hasText(new String[] { "SKU-A", "SKU-B" });
		assertThat(page.getByTestId("previousPage")).hasCount(0);
		page.getByTestId("nextPage").click();
		assertThat(page.getByTestId("articleSku")).hasText(new String[] { "SKU-C", "SKU-D" });
		page.getByTestId("nextPage").click();
		assertThat(page.getByTestId("articleSku")).hasText(new String[] { "SKU-E" });
		assertThat(page.getByTestId("nextPage")).hasCount(0);
		page.getByTestId("previousPage").click();
		assertThat(page.getByTestId("articleSku")).hasText(new String[] { "SKU-C", "SKU-D" });
	}

	@Test
	void sortOrderIsKeptAcrossPages() {
		// Two sort parameters are bound without splitting: see singleSortParameterWithDirection() for the single one
		page.navigate("/test/articles?sort=sku,desc&sort=id,asc");
		assertThat(page.getByTestId("articleSku")).hasText(new String[] { "SKU-E", "SKU-D" });
		assertThat(page.getByTestId("articleName")).hasText(new String[] { "Easel", "Desk" });
		page.getByTestId("nextPage").click();
		assertThat(page.getByTestId("articleSku")).hasText(new String[] { "SKU-C", "SKU-B" });
	}

	/**
	 * Spring binds a single "sort" request parameter to YadaPageRequest.setSort(List) by splitting it at commas,
	 * so "sku,desc" arrives as ["sku", "desc"]: YadaPageRequest joins the direction back to its property.
	 */
	@Test
	void singleSortParameterWithDirection() {
		page.navigate("/test/articles?sort=sku,desc");
		assertThat(page.getByTestId("articleSku")).hasText(new String[] { "SKU-E", "SKU-D" });
	}

	@Test
	void editedNameIsRenderedAndPersisted() {
		page.navigate("/test/articles");
		page.getByTestId("article").filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText("SKU-A")).getByRole(AriaRole.LINK, new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Edit")).click();
		assertThat(page.getByTestId("articleTitle")).hasText("Armchair");
		page.getByLabel("Name").fill("Lounge chair");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();
		assertThat(page.getByTestId("articleTitle")).hasText("Lounge chair");
		assertThat(page.locator("#yada-notification.show .modal-body")).containsText("Article renamed");
		ArticleDao articleDao = session.application().getBean(ArticleDao.class);
		YadaArticle stored = articleDao.find(1L);
		assertEquals("SKU-A", stored.getSku());
		assertEquals("Lounge chair", stored.getName().get(Locale.ENGLISH));
	}
}
