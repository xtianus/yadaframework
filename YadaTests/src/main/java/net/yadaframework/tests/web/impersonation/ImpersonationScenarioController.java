package net.yadaframework.tests.web.impersonation;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Browser navigations that change state through POST: yada.impersonate and a legacy yada.dataTableCrud button with
 * ajax false. The target only shows what it received, so the browser test can check the method and the parameters.
 */
@Controller
@RequestMapping("/test/impersonation")
public class ImpersonationScenarioController {

	@GetMapping("")
	public String page() {
		return "/test/impersonation/page";
	}

	/**
	 * Accepts any method, to show which one the browser used
	 */
	@RequestMapping("/start")
	public String start(String id, String currentLocation, HttpServletRequest request, Model model) {
		model.addAttribute("method", request.getMethod());
		model.addAttribute("id", id);
		model.addAttribute("currentLocation", currentLocation);
		return "/test/impersonation/received";
	}

	/**
	 * One row for the legacy table, in the server-side DataTables format
	 */
	@PostMapping(value = "/tableData", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> tableData(Integer draw) {
		return Map.of("draw", draw == null ? 1 : draw, "recordsTotal", 1, "recordsFiltered", 1, "data", List.of(Map.of("DT_RowId", "row#7", "name", "Row seven")));
	}
}
