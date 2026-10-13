package net.yadaframework.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.yadaframework.components.YadaDataTableFactory;
import net.yadaframework.components.YadaSecurityUtilStub;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.YadaDataTableDao;
import net.yadaframework.persistence.YadaDataTableQueryPolicy;
import net.yadaframework.web.datatables.proxy.YadaDataTableProxy;

/**
 * Verifies that servlet error dispatches preserve their original HTTP status, and that the default DataTables endpoint
 * checks page access and takes its query policy from the table configuration.
 */
class YadaControllerTest {

	@Entity
	public static class TableRow {
		private Long id;
		private String name;
		private String password;
	}

	@AfterEach
	void clearRequestContext() {
		RequestContextHolder.resetRequestAttributes();
	}

	/**
	 * Keeps a not-found dispatch at HTTP 404 while rendering the configured error page.
	 */
	@Test
	void yadaError_NotFoundDispatchPreserves404() {
		assertErrorStatusPreserved(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * Keeps an internal-error dispatch at HTTP 500 while rendering the configured error page.
	 */
	@Test
	void yadaError_InternalErrorDispatchPreserves500() {
		assertErrorStatusPreserved(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	/**
	 * Invokes the real error controller with one servlet error status and verifies the response.
	 *
	 * @param errorStatus original servlet error status
	 */
	private void assertErrorStatusPreserved(int errorStatus) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
		YadaConfiguration configuration = mock(YadaConfiguration.class);
		YadaWebUtil yadaWebUtil = mock(YadaWebUtil.class);
		YadaSecurityUtilStub yadaSecurityUtil = mock(YadaSecurityUtilStub.class);
		when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(errorStatus);
		when(request.getAttribute("jakarta.servlet.error.message")).thenReturn("");
		when(request.getAttribute("jakarta.servlet.error.request_uri")).thenReturn("/missing");
		when(configuration.getErrorPageForward()).thenReturn("/errorPage");
		when(yadaSecurityUtil.getUsername()).thenReturn("test-user");

		YadaController controller = new YadaController();
		ReflectionTestUtils.setField(controller, "config", configuration);
		ReflectionTestUtils.setField(controller, "yadaWebUtil", yadaWebUtil);
		ReflectionTestUtils.setField(controller, "yadaSecurityUtil", yadaSecurityUtil);

		String viewName = controller.yadaError(
			request,
			response,
			redirectAttributes,
			new ExtendedModelMap(),
			Locale.ENGLISH
		);

		assertEquals("forward:/errorPage", viewName);
		verify(response).resetBuffer();
		verify(response).setStatus(errorStatus);
	}

	/**
	 * Configures a table as a page controller does, inside a web request to the page
	 */
	private YadaDataTableProxy configuredTable() {
		MockHttpServletRequest pageRequest = new MockHttpServletRequest("GET", "/admin/rows");
		pageRequest.setServletPath("/admin/rows");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(pageRequest));
		YadaDataTableProxy table = new YadaDataTableProxy("rowTable", Locale.ENGLISH);
		table.dtEntityClass(TableRow.class)
			.dtStructureObj()
				.dtColumnObj("ID", "id").dtSearchableOff().back()
				.dtColumnObj("Name", "name").dtOrderableOff().back()
				.dtColumnCheckbox("select")
			.back();
		RequestContextHolder.resetRequestAttributes();
		return table;
	}

	private YadaController dataTableController(YadaDataTableProxy table, YadaDataTableDao dao, YadaSecurityUtilStub yadaSecurityUtil) {
		YadaDataTableFactory factory = mock(YadaDataTableFactory.class);
		when(factory.getSingleton("rowTable", Locale.ENGLISH)).thenReturn(table);
		YadaController controller = new YadaController();
		ReflectionTestUtils.setField(controller, "yadaDataTableFactory", factory);
		ReflectionTestUtils.setField(controller, "yadaDataTableDao", dao);
		ReflectionTestUtils.setField(controller, "yadaSecurityUtil", yadaSecurityUtil);
		return controller;
	}

	/**
	 * The DAO receives the policy derived from the table configuration, whatever the request contains.
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void yadaDataTableData_UsesThePolicyOfTheConfiguredTable() throws Exception {
		YadaDataTableProxy table = configuredTable();
		YadaDataTableDao dao = mock(YadaDataTableDao.class);
		YadaSecurityUtilStub yadaSecurityUtil = mock(YadaSecurityUtilStub.class);
		HttpServletRequest request = new MockHttpServletRequest("POST", "/yadaDataTableData");
		when(yadaSecurityUtil.checkUrlAccess(request, "/admin/rows")).thenReturn(true);
		when(dao.getConvertedJsonPage(any(), any(), any(), any())).thenReturn(Map.of("data", List.of()));
		YadaDatatablesRequest tableRequest = new YadaDatatablesRequest();
		tableRequest.setDataTableId("rowTable");
		tableRequest.getExtraJsonAttributes().add("password");

		dataTableController(table, dao, yadaSecurityUtil).yadaDataTableData(tableRequest, request, Locale.ENGLISH);

		ArgumentCaptor<YadaDataTableQueryPolicy> policyCaptor = ArgumentCaptor.forClass(YadaDataTableQueryPolicy.class);
		verify(dao).getConvertedJsonPage(eq(tableRequest), eq((Class) TableRow.class), eq(Locale.ENGLISH), policyCaptor.capture());
		YadaDataTableQueryPolicy policy = policyCaptor.getValue();
		assertEquals(TableRow.class, policy.getEntityClass());
		assertEquals(List.of("id", "name"), List.copyOf(policy.getColumnPaths()));
		assertFalse(policy.isSearchable("id"));
		assertTrue(policy.isOrderable("id"));
		assertTrue(policy.isSearchable("name"));
		assertFalse(policy.isOrderable("name"));
		assertTrue(policy.getExtraOutputPaths().isEmpty());
	}

	/**
	 * A user that can't open the page of the table never reaches the DAO.
	 */
	@Test
	void yadaDataTableData_DeniedPageNeverReachesTheDao() {
		YadaDataTableProxy table = configuredTable();
		YadaDataTableDao dao = mock(YadaDataTableDao.class);
		YadaSecurityUtilStub yadaSecurityUtil = mock(YadaSecurityUtilStub.class);
		HttpServletRequest request = new MockHttpServletRequest("POST", "/yadaDataTableData");
		when(yadaSecurityUtil.checkUrlAccess(request, "/admin/rows")).thenReturn(false);
		YadaDatatablesRequest tableRequest = new YadaDatatablesRequest();
		tableRequest.setDataTableId("rowTable");

		YadaController controller = dataTableController(table, dao, yadaSecurityUtil);
		assertThrows(AccessDeniedException.class, () -> controller.yadaDataTableData(tableRequest, request, Locale.ENGLISH));
		verifyNoInteractions(dao);
	}

	/**
	 * The table is serialized into the page: the policy must never be part of it, and a table without entity class
	 * (served by a custom endpoint) must still serialize.
	 */
	@Test
	void queryPolicyIsNotSerializedToThePage() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		YadaDataTableProxy configured = configuredTable();
		configured.getQueryPolicy();
		String json = objectMapper.writeValueAsString(configured);
		assertFalse(json.contains("queryPolicy"), json);
		assertTrue(json.contains("rowTable"), json);

		YadaDataTableProxy ajaxOnly = new YadaDataTableProxy("ajaxTable", Locale.ENGLISH);
		ajaxOnly.dtAjaxUrl("/custom/tablePage").dtStructureObj().dtColumnObj("Name", "name").back().back();
		String ajaxJson = objectMapper.writeValueAsString(ajaxOnly);
		assertFalse(ajaxJson.contains("queryPolicy"), ajaxJson);
		assertThrows(net.yadaframework.exceptions.YadaInvalidUsageException.class, ajaxOnly::getQueryPolicy);
	}
}
