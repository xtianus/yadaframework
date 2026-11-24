package net.yadaframework.example.web.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.yadaframework.example.support.ExampleWebTestBase;

/**
 * The user table endpoint of the dashboard answers only with the columns of its server-side policy: tampered column
 * paths, order directions and indexes and extra json attributes are rejected and never disclose credentials.
 */
class UserProfileTableSecurityTest extends ExampleWebTestBase {
	private static final String ENDPOINT = "/dashboard/user/userProfileTablePage";
	private static final ObjectMapper JSON = new ObjectMapper();
	/** The data paths of the "userTable" columns, after the selection checkbox at index 0 */
	private static final String[] PATHS = { "id", "userCredentials.enabled", "nickname", "userCredentials.username", "userCredentials.lastSuccessfulLogin" };

	private MockHttpSession session;
	private String csrf;
	private String supervisorPasswordHash;

	@BeforeEach
	void loginAsSupervisor() throws Exception {
		session = login(SUPERVISOR_EMAIL, SUPERVISOR_PASSWORD);
		csrf = csrfToken(session);
		supervisorPasswordHash = findUser(SUPERVISOR_EMAIL).getUserCredentials().getPassword();
	}

	/**
	 * The parameters that DataTables sends for the userTable: selection column, the data columns and the command column
	 */
	private static final class TableRequest {
		final List<String[]> columns = new ArrayList<>(); // data, name, searchable, orderable
		String orderColumn = "4";
		String orderDir = "asc";
		final List<String> extraJsonAttributes = new ArrayList<>();

		TableRequest() {
			columns.add(new String[] { "", "", "false", "false" });
			for (String path : PATHS) {
				columns.add(new String[] { path, path, "true", "true" });
			}
			columns.add(new String[] { "", "_yadaCommandColumn", "false", "false" });
		}

		MockHttpServletRequestBuilder apply(MockHttpServletRequestBuilder builder) {
			builder.param("draw", "1").param("start", "0").param("length", "10").param("search[value]", "").param("search[regex]", "false").param("dataTableId", "userTable");
			for (int i = 0; i < columns.size(); i++) {
				builder.param("columns[" + i + "][data]", columns.get(i)[0]).param("columns[" + i + "][name]", columns.get(i)[1])
					.param("columns[" + i + "][searchable]", columns.get(i)[2]).param("columns[" + i + "][orderable]", columns.get(i)[3]);
			}
			builder.param("order[0][column]", orderColumn).param("order[0][dir]", orderDir);
			for (int i = 0; i < extraJsonAttributes.size(); i++) {
				builder.param("extraJsonAttributes[" + i + "]", extraJsonAttributes.get(i));
			}
			return builder;
		}
	}

	private String send(TableRequest tableRequest) throws Exception {
		MockHttpServletResponse response = mockMvc.perform(tableRequest.apply(post(ENDPOINT).session(session).param("_csrf", csrf).header("X-Requested-With", "XMLHttpRequest"))).andReturn().getResponse();
		assertEquals(200, response.getStatus(), response.getContentAsString());
		return response.getContentAsString();
	}

	private void assertRejected(Consumer<TableRequest> tampering) throws Exception {
		TableRequest tableRequest = new TableRequest();
		tampering.accept(tableRequest);
		String body = send(tableRequest);
		assertFalse(body.contains(supervisorPasswordHash), "Password hash disclosed: " + body);
		assertFalse(body.contains("password"), body);
		JsonNode json = JSON.readTree(body);
		assertNotNull(json.get("yadaError"), "Not rejected: " + body);
		assertNull(json.get("data"), body);
	}

	@Test
	void tableReturnsTheConfiguredColumnsWithoutCredentials() throws Exception {
		String body = send(new TableRequest());
		JsonNode json = JSON.readTree(body);
		assertNull(json.get("yadaError"), body);
		JsonNode rows = json.get("data");
		assertEquals(4, rows.size(), body);
		assertEquals(ADMIN_EMAIL, rows.get(0).get("userCredentials").get("username").asText(), body);
		assertFalse(rows.get(0).get("userCredentials").has("password"), body);
		assertFalse(body.contains(supervisorPasswordHash), body);
	}

	@Test
	void extraJsonAttributeForThePasswordIsRejected() throws Exception {
		assertRejected(request -> request.extraJsonAttributes.add("userCredentials.password"));
	}

	@Test
	void columnNameForThePasswordIsRejected() throws Exception {
		assertRejected(request -> request.columns.get(3)[1] = "userCredentials.password");
	}

	@Test
	void columnDataForThePasswordIsRejected() throws Exception {
		assertRejected(request -> request.columns.set(3, new String[] { "userCredentials.password", "", "true", "true" }));
	}

	@Test
	void relatedEntityColumnIsRejected() throws Exception {
		assertRejected(request -> request.columns.set(3, new String[] { "userCredentials", "userCredentials", "true", "true" }));
	}

	@Test
	void orderDirectionWithHqlIsRejected() throws Exception {
		assertRejected(request -> request.orderDir = "asc, e.userCredentials.password");
	}

	@Test
	void outOfRangeOrderIndexIsRejected() throws Exception {
		assertRejected(request -> request.orderColumn = "99");
		assertRejected(request -> request.orderColumn = "-1");
	}

	@Test
	void legacyTableColumnsAreAccepted() throws Exception {
		// yada.datatablesLegacy.js: hidden control column, selection column, data columns, command column, legacy=true
		TableRequest tableRequest = new TableRequest();
		tableRequest.columns.add(0, new String[] { "", "", "false", "false" });
		tableRequest.columns.get(1)[1] = "_yadaSelectionColumn";
		tableRequest.orderColumn = "2";
		tableRequest.orderDir = "desc";
		MockHttpServletResponse response = mockMvc.perform(tableRequest.apply(post(ENDPOINT).session(session).param("_csrf", csrf).param("legacy", "true"))).andReturn().getResponse();
		JsonNode json = JSON.readTree(response.getContentAsString());
		assertNull(json.get("yadaError"), response.getContentAsString());
		assertEquals(4, json.get("data").size());
		assertFalse(response.getContentAsString().contains(supervisorPasswordHash));
	}
}
