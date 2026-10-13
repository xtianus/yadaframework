package net.yadaframework.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.web.YadaDatatablesRequest;

/**
 * A database failure while reading a DataTables page keeps the "yadaError" contract but never sends exception text to the browser.
 */
class YadaDataTableErrorSafetyTest {

	@Entity
	public static class Row {
		private Long id;
		private String name;
	}

	@Test
	@SuppressWarnings("unchecked")
	void databaseFailureReturnsGenericYadaError() {
		String secret = "could not execute statement [select secret_column from internal_table] <script>alert(1)</script>";
		EntityManager em = mock(EntityManager.class);
		when(em.createQuery(anyString())).thenThrow(new IllegalStateException(secret));
		YadaDataTableDao dao = new YadaDataTableDao();
		ReflectionTestUtils.setField(dao, "em", em);
		ReflectionTestUtils.setField(dao, "yadaUtil", new YadaUtil());
		YadaDatatablesRequest request = new YadaDatatablesRequest();
		request.setDataTableId("rowTable");
		request.setLength(10);
		YadaDataTableQueryPolicy policy = YadaDataTableQueryPolicy.forEntity(Row.class).column("id").column("name").build();

		Map<String, Object> json = dao.getConvertedJsonPage(request, Row.class, Locale.ENGLISH, policy);

		assertNotNull(json.get("yadaError"), json.toString());
		assertNull(json.get("data"), json.toString());
		assertEquals("Server error", ((Map<String, String>) json.get("yadaError")).get("error"));
		assertFalse(json.toString().contains("secret_column"), json.toString());
		assertFalse(json.toString().contains("IllegalStateException"), json.toString());
		assertFalse(json.toString().contains("script"), json.toString());
	}
}
