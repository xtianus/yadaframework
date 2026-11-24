package net.yadaframework.tests.web.errors;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Endpoints that fail with a hostile exception message, before and after the response is committed.
 */
@Controller
public class ErrorScenarioController {
	public static final String HOSTILE = "<script>alert('boom')</script>secret-sql-detail";

	/** Fails before anything is sent: the normal error path applies */
	@RequestMapping("/test/errors/uncommitted")
	public String uncommitted() {
		throw new IllegalStateException(HOSTILE);
	}

	/** Sends part of the page, commits the response and then fails */
	@RequestMapping("/test/errors/committed")
	public void committed(HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=UTF-8");
		response.getWriter().write("<html><body><p>partial page</p>");
		response.flushBuffer();
		throw new IllegalStateException(HOSTILE);
	}
}
