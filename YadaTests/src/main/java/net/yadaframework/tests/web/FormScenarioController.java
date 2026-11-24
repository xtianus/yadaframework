package net.yadaframework.tests.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.yadaframework.components.YadaNotify;

/**
 * Server-side validation failure and success of a plain form
 */
@Controller
@RequestMapping("/test/form")
public class FormScenarioController {
	private final YadaNotify yadaNotify;

	public FormScenarioController(YadaNotify yadaNotify) {
		this.yadaNotify = yadaNotify;
	}

	@GetMapping("")
	public String form(@ModelAttribute ContactForm contactForm) {
		return "/test/form";
	}

	@PostMapping("")
	public String submit(@Validated @ModelAttribute ContactForm contactForm, BindingResult bindingResult, Model model) {
		if (bindingResult.hasErrors()) {
			return "/test/form";
		}
		model.addAttribute("saved", true);
		yadaNotify.titleKey(model, "yts.form.title").ok().messageKey("yts.form.saved", contactForm.getName()).add();
		return "/test/form";
	}
}
