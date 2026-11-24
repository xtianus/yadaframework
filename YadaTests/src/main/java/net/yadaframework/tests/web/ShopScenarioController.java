package net.yadaframework.tests.web;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.yadaframework.commerce.persistence.entity.YadaCart;
import net.yadaframework.commerce.persistence.entity.YadaOrder;
import net.yadaframework.components.YadaNotify;
import net.yadaframework.tests.persistence.entity.UserProfile;
import net.yadaframework.tests.persistence.repository.ShopDao;
import net.yadaframework.tests.persistence.repository.UserProfileDao;

/**
 * Commerce: article prices, cart and order persistence for the logged-in user
 */
@Controller
@RequestMapping("/test/user/shop")
public class ShopScenarioController {
	private final ShopDao shopDao;
	private final UserProfileDao userProfileDao;
	private final YadaNotify yadaNotify;

	public ShopScenarioController(ShopDao shopDao, UserProfileDao userProfileDao, YadaNotify yadaNotify) {
		this.shopDao = shopDao;
		this.userProfileDao = userProfileDao;
		this.yadaNotify = yadaNotify;
	}

	private UserProfile currentUser(Principal principal) {
		return userProfileDao.findUserProfileByUsername(principal.getName());
	}

	@GetMapping("")
	public String shop(Principal principal, Model model) {
		YadaCart cart = shopDao.findCart(currentUser(principal));
		model.addAttribute("articles", shopDao.findArticles());
		model.addAttribute("cart", cart);
		model.addAttribute("total", ShopDao.total(cart));
		model.addAttribute("orders", shopDao.findOrders(currentUser(principal)));
		return "/test/shop";
	}

	@PostMapping("/add/{articleId}")
	public String add(@PathVariable Long articleId, Principal principal) {
		shopDao.addToCart(currentUser(principal), articleId);
		return "redirect:/test/user/shop";
	}

	@PostMapping("/order")
	public String order(Principal principal, RedirectAttributes redirectAttributes) {
		YadaOrder order = shopDao.placeOrder(currentUser(principal));
		if (order != null) {
			yadaNotify.titleKey(redirectAttributes, "yts.shop.title").ok().messageKey("yts.shop.ordered", String.valueOf(order.getId())).add();
		}
		return "redirect:/test/user/shop";
	}
}
