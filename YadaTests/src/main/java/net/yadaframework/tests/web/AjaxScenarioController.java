package net.yadaframework.tests.web;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.yadaframework.components.YadaNotify;

/**
 * Ajax fragment replacement and notification modal, through yada.ajax.js and YadaNotify.
 */
@Controller
@RequestMapping("/test/ajax")
public class AjaxScenarioController {
	private final AtomicInteger refreshCount = new AtomicInteger();
	private final YadaNotify yadaNotify;

	public AjaxScenarioController(YadaNotify yadaNotify) {
		this.yadaNotify = yadaNotify;
	}

	@GetMapping("")
	public String page(Model model) {
		model.addAttribute("refreshCount", 0);
		return "/test/ajax";
	}

	/**
	 * Returns just the fragment that replaces the target element on the page
	 */
	@GetMapping("/fragment")
	public String fragment(Model model) {
		model.addAttribute("refreshCount", refreshCount.incrementAndGet());
		return "/test/ajax :: #ajaxTarget";
	}

	@GetMapping("/notify")
	public String notify(Model model) {
		return yadaNotify.titleKey(model, "yts.ajax.notify.title").ok().messageKey("yts.ajax.notify.message").add();
	}
}
