package net.yadaframework.tests.persistence.repository;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.cms.persistence.entity.YadaArticle;
import net.yadaframework.commerce.persistence.entity.YadaCommerceArticle;
import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.web.YadaPageRequest;
import net.yadaframework.web.YadaPageRows;

/**
 * Articles of the CMS scenarios, paged and sorted with the framework YadaPageRequest and YadaSql
 */
@Repository
@Transactional(readOnly = true)
public class ArticleDao {
	@PersistenceContext
	private EntityManager em;

	/**
	 * @param yadaPageRequest page, size and sort, e.g. "sort=sku,desc"
	 * @return one page of articles, with their localized names loaded
	 */
	public YadaPageRows<YadaArticle> find(YadaPageRequest yadaPageRequest) {
		List<YadaArticle> articles = YadaSql.instance().selectFrom("select a from YadaArticle a")
			.orderBy(yadaPageRequest)
			.query(em, YadaArticle.class)
			.setFirstResult(yadaPageRequest.getFirstResult())
			.setMaxResults(yadaPageRequest.getMaxResults())
			.getResultList();
		articles.forEach(article -> article.getName().size()); // Loaded here because views render outside the transaction
		return new YadaPageRows<>(articles, yadaPageRequest);
	}

	public YadaArticle find(Long id) {
		YadaArticle article = em.find(YadaArticle.class, id);
		if (article != null) {
			article.getName().size();
		}
		return article;
	}

	/**
	 * Creates a published article that can be sold
	 * @return the new article
	 */
	@Transactional(readOnly = false)
	public YadaCommerceArticle createCommerceArticle(String sku, Locale locale, String name, YadaMoney unitPrice) {
		YadaCommerceArticle article = new YadaCommerceArticle();
		article.setSku(sku);
		article.setLocalName(locale, name);
		article.setUnitPrice(unitPrice);
		article.setPublished(true);
		article.setAvailableQuantity(10);
		em.persist(article);
		return article;
	}

	@Transactional(readOnly = false)
	public void rename(Long id, Locale locale, String name) {
		YadaArticle article = em.find(YadaArticle.class, id);
		article.setLocalName(locale, name);
	}
}
