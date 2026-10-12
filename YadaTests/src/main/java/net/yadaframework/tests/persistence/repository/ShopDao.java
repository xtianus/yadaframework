package net.yadaframework.tests.persistence.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.commerce.persistence.entity.YadaCart;
import net.yadaframework.commerce.persistence.entity.YadaCartItem;
import net.yadaframework.commerce.persistence.entity.YadaCommerceArticle;
import net.yadaframework.commerce.persistence.entity.YadaOrder;
import net.yadaframework.commerce.persistence.entity.YadaOrderItem;
import net.yadaframework.commerce.persistence.entity.YadaOrderStatus;
import net.yadaframework.commerce.persistence.repository.YadaOrderDao;
import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.repository.YadaPersistentEnumDao;
import net.yadaframework.security.persistence.entity.YadaUserProfile;

/**
 * Cart and order persistence of the commerce scenarios
 */
@Repository
@Transactional(readOnly = true)
public class ShopDao {
	@PersistenceContext
	private EntityManager em;

	private final YadaOrderDao yadaOrderDao;
	private final YadaPersistentEnumDao yadaPersistentEnumDao;

	public ShopDao(YadaOrderDao yadaOrderDao, YadaPersistentEnumDao yadaPersistentEnumDao) {
		this.yadaOrderDao = yadaOrderDao;
		this.yadaPersistentEnumDao = yadaPersistentEnumDao;
	}

	public List<YadaCommerceArticle> findArticles() {
		List<YadaCommerceArticle> articles = em.createQuery("select a from YadaCommerceArticle a order by a.sku", YadaCommerceArticle.class).getResultList();
		articles.forEach(article -> article.getName().size());
		return articles;
	}

	/**
	 * @return the cart of the user with its items and their articles loaded, or null
	 */
	public YadaCart findCart(YadaUserProfile owner) {
		List<YadaCart> carts = em.createQuery("select c from YadaCart c where c.owner = :owner", YadaCart.class).setParameter("owner", owner).getResultList();
		if (carts.isEmpty()) {
			return null;
		}
		YadaCart cart = carts.get(0);
		cart.getCartItems().forEach(item -> item.getArticle().getName().size());
		return cart;
	}

	/**
	 * Adds one unit of an article to the cart of the user, creating the cart when missing
	 */
	@Transactional(readOnly = false)
	public void addToCart(YadaUserProfile owner, Long articleId) {
		YadaCommerceArticle article = em.find(YadaCommerceArticle.class, articleId);
		List<YadaCart> carts = em.createQuery("select c from YadaCart c where c.owner = :owner", YadaCart.class).setParameter("owner", owner).getResultList();
		YadaCart cart;
		if (carts.isEmpty()) {
			cart = new YadaCart();
			cart.setOwner(em.getReference(YadaUserProfile.class, owner.getId()));
			cart.setCreationTimestamp(new Date());
			cart.setCartItems(new ArrayList<>());
			em.persist(cart);
		} else {
			cart = carts.get(0);
		}
		for (YadaCartItem item : cart.getCartItems()) {
			if (item.getArticle().getId().equals(articleId)) {
				item.setQuantity(item.getQuantity() + 1);
				return;
			}
		}
		YadaCartItem item = new YadaCartItem();
		item.setArticle(article);
		item.setCart(cart);
		item.setQuantity(1);
		cart.getCartItems().add(item);
	}

	/**
	 * @return the sum of quantity times unit price of the cart items
	 */
	public static YadaMoney total(YadaCart cart) {
		YadaMoney total = YadaMoney.ZERO;
		if (cart != null) {
			for (YadaCartItem item : cart.getCartItems()) {
				total = total.getSum(item.getArticle().getUnitPrice().getMultiply(item.getQuantity()));
			}
		}
		return total;
	}

	/**
	 * Turns the cart of the user into an unpaid order and empties the cart
	 * @return the new order, or null when the cart is empty
	 */
	@Transactional(readOnly = false)
	public YadaOrder placeOrder(YadaUserProfile owner) {
		YadaCart cart = findCart(owner);
		if (cart == null || cart.getCartItems().isEmpty()) {
			return null;
		}
		YadaOrder order = new YadaOrder();
		order.setOwner(em.getReference(YadaUserProfile.class, owner.getId()));
		order.setOrderStatus(yadaPersistentEnumDao.find(YadaOrderStatus.UNPAID));
		order.setCurrency("EUR");
		order.setTotalPrice(total(cart));
		order.setOrderItems(new ArrayList<>());
		for (YadaCartItem cartItem : cart.getCartItems()) {
			YadaOrderItem orderItem = new YadaOrderItem();
			orderItem.setOrder(order);
			orderItem.setArticleCode(cartItem.getArticle().getSku());
			orderItem.setQuantity(cartItem.getQuantity());
			orderItem.setUnitPrice(cartItem.getArticle().getUnitPrice());
			order.getOrderItems().add(orderItem);
		}
		YadaOrder saved = yadaOrderDao.save(order);
		em.remove(em.contains(cart) ? cart : em.merge(cart));
		return saved;
	}

	/**
	 * @return the orders of the user, newest first
	 */
	public List<YadaOrder> findOrders(YadaUserProfile owner) {
		List<YadaOrder> orders = em.createQuery("select o from YadaOrder o where o.owner = :owner order by o.id desc", YadaOrder.class).setParameter("owner", owner).getResultList();
		orders.forEach(order -> {
			order.getOrderItems().size();
			order.getOrderStatus().getLangToText().size();
		});
		return orders;
	}
}
