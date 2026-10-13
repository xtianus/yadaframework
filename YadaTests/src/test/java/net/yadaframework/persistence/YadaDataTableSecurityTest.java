package net.yadaframework.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.YadaDatatablesColumn;
import net.yadaframework.web.YadaDatatablesOrder;
import net.yadaframework.web.YadaDatatablesRequest;

/**
 * {@link YadaDataTableQueryPolicy} and {@link YadaDataTableDao}: browser values never reach the query or the reflective
 * output unless the server policy allows them, and tampered requests are rejected before any query.
 */
class YadaDataTableSecurityTest {

	@Entity
	public static class Owner {
		private Long id;
		private String name;
		private String password;
	}

	public enum Kind { RED, BLUE }

	@Embeddable
	public static class Size {
		private int width;
		@Override
		public String toString() {
			return "Size[width=" + width + "]";
		}
	}

	@Entity
	public static class Item {
		private Long id;
		private String name;
		private Map<Locale, String> title = new HashMap<>();
		private Owner owner;
		private List<Owner> owners;
		private List<String> tags;
		private Kind kind;
		private int count;
		private String password;
		private String tokenHash;
		private Map<String, String> labels = new HashMap<>();
		private Map<String, String> settings = new HashMap<>();
		private Size size;

		public String getComputed() {
			return "computed-" + name;
		}

		public Owner getMainOwner() {
			return owner;
		}

		/** Declared as Object, so only the runtime safeguard can see that it is an entity */
		public Object getAnything() {
			return owner;
		}

		/** Declared as Object, so only the runtime safeguard can see that it is an embeddable */
		public Object getSizeObject() {
			return size;
		}

		public String getDT_RowClass() {
			return "row-" + id;
		}
	}

	/** A subclass with an attribute that the declared entity class doesn't have */
	@Entity
	public static class SpecialItem extends Item {
		private String special;
	}

	private EntityManager em;
	private Query query;
	private YadaDataTableDao dao;
	private ArgumentCaptor<String> hqlCaptor;

	@BeforeEach
	void setUp() {
		em = mock(EntityManager.class);
		query = mock(Query.class);
		hqlCaptor = ArgumentCaptor.forClass(String.class);
		when(em.createQuery(hqlCaptor.capture())).thenReturn(query);
		when(query.getSingleResult()).thenReturn(1L);
		dao = new YadaDataTableDao();
		ReflectionTestUtils.setField(dao, "em", em);
		ReflectionTestUtils.setField(dao, "yadaUtil", new YadaUtil());
	}

	private static YadaDataTableQueryPolicy policy() {
		return YadaDataTableQueryPolicy.forEntity(Item.class)
			.column("id")
			.column("name", false, true)
			.column("title.en")
			.column("owner.name", true, false)
			.column("computed", false, false)
			.column("anything", false, false)
			.extraOutput("count")
			.build();
	}

	private static YadaDatatablesColumn column(String data, String name, boolean searchable, boolean orderable) {
		YadaDatatablesColumn column = new YadaDatatablesColumn();
		column.put("data", data);
		column.put("name", name);
		column.put("searchable", String.valueOf(searchable));
		column.put("orderable", String.valueOf(orderable));
		return column;
	}

	private static YadaDatatablesOrder order(String columnIndex, String direction) {
		YadaDatatablesOrder order = new YadaDatatablesOrder();
		order.put("column", columnIndex);
		order.put("dir", direction);
		return order;
	}

	/**
	 * The request that the browser sends for the policy table: selection column, id, name, title.en, owner.name, computed
	 */
	private static YadaDatatablesRequest request() {
		YadaDatatablesRequest request = new YadaDatatablesRequest();
		request.setDataTableId("itemTable");
		request.setLength(10);
		List<YadaDatatablesColumn> columns = new ArrayList<>();
		columns.add(column("", "", false, false));
		columns.add(column("id", "id", true, true));
		columns.add(column("name", "name", false, true));
		columns.add(column("title.en", "title.en", true, true));
		columns.add(column("owner.name", "owner.name", true, false));
		columns.add(column("function", "computed", false, false));
		columns.add(column("", YadaDataTableDao.COLUMN_COMMAND, false, false));
		request.setColumns(columns);
		List<YadaDatatablesOrder> orders = new ArrayList<>();
		orders.add(order("1", "asc"));
		request.setOrder(orders);
		return request;
	}

	private Map<String, Object> send(YadaDatatablesRequest request) {
		return dao.getConvertedJsonPage(request, Item.class, Locale.ENGLISH, policy());
	}

	private void assertRejectedBeforeQuery(Consumer<YadaDatatablesRequest> tampering) {
		YadaDatatablesRequest request = request();
		tampering.accept(request);
		Map<String, Object> json = send(request);
		assertNotNull(json.get("yadaError"), json.toString());
		assertNull(json.get("data"), json.toString());
		assertFalse(json.toString().contains("password"), json.toString());
		verifyNoInteractions(em);
	}

	private static Item item(long id, String name) {
		Item item = new Item();
		item.id = id;
		item.name = name;
		item.password = "$2a$10$secret-hash";
		item.tokenHash = "token-hash";
		item.title.put(Locale.ENGLISH, "Title " + name);
		item.owner = new Owner();
		item.owner.id = 7L;
		item.owner.name = "owner";
		item.owner.password = "$2a$10$owner-hash";
		item.count = 3;
		return item;
	}

	// Policy validation

	@ParameterizedTest
	@ValueSource(strings = { "password", "owner.password", "tokenHash", "owner", "owners", "mainOwner", "missing", "owner.missing",
		"name or 1=1", "name)", "id desc", "e.name", "title.en'", "", "settings.password", "labels.token", "size", "special" })
	void policyRefusesUnsafeOrInvalidPaths(String path) {
		assertThrows(YadaInvalidUsageException.class, () -> YadaDataTableQueryPolicy.forEntity(Item.class).column(path, false, false).build());
	}

	@Test
	void policyAcceptsEmbeddableAttributes() {
		assertNotNull(YadaDataTableQueryPolicy.forEntity(Item.class).column("size.width").build());
	}

	@Test
	void policyRefusesGettersForQueryableColumns() {
		assertThrows(YadaInvalidUsageException.class, () -> YadaDataTableQueryPolicy.forEntity(Item.class).column("computed").build());
		assertNotNull(YadaDataTableQueryPolicy.forEntity(Item.class).column("computed", false, false).build());
	}

	@Test
	void policyRefusesUnsafeExtraOutputs() {
		assertThrows(YadaInvalidUsageException.class, () -> YadaDataTableQueryPolicy.forEntity(Item.class).extraOutput("owner.password").build());
		assertThrows(YadaInvalidUsageException.class, () -> YadaDataTableQueryPolicy.forEntity(Item.class).extraOutput("owner").build());
	}

	@Test
	void policyAcceptsFieldsMapKeysCollectionsOfValuesAndEnums() {
		YadaDataTableQueryPolicy policy = YadaDataTableQueryPolicy.forEntity(Item.class).column("title.en").column("owner.name").column("tags").column("kind").column("count").build();
		assertEquals(List.of("title.en", "owner.name", "tags", "kind", "count"), List.copyOf(policy.getColumnPaths()));
		assertThrows(UnsupportedOperationException.class, () -> policy.getColumnPaths().clear());
	}

	@Test
	void policyMustMatchTheEntityClass() {
		YadaDataTableQueryPolicy ownerPolicy = YadaDataTableQueryPolicy.forEntity(Owner.class).column("name").build();
		assertThrows(YadaInvalidUsageException.class, () -> dao.getConvertedJsonPage(request(), Item.class, Locale.ENGLISH, ownerPolicy));
		assertThrows(YadaInvalidUsageException.class, () -> dao.getConvertedJsonPage(request(), Item.class, Locale.ENGLISH, null));
	}

	// Rejection before any query

	@Test
	void unknownColumnNameIsRejected() {
		assertRejectedBeforeQuery(request -> request.getColumns().get(2).put("name", "password"));
	}

	@Test
	void nestedCredentialColumnIsRejected() {
		assertRejectedBeforeQuery(request -> request.getColumns().get(4).put("name", "owner.password"));
	}

	@Test
	void columnDataIsCheckedWhenTheNameIsEmpty() {
		assertRejectedBeforeQuery(request -> request.getColumns().set(2, column("owner.password", "", false, false)));
	}

	@Test
	void validButUnconfiguredFieldIsRejected() {
		assertRejectedBeforeQuery(request -> request.getColumns().get(2).put("name", "count"));
	}

	@Test
	void hqlInAColumnNameIsRejected() {
		assertRejectedBeforeQuery(request -> request.getColumns().get(1).put("name", "id=e.id or e.password"));
	}

	@Test
	void undeclaredExtraJsonAttributeIsRejected() {
		assertRejectedBeforeQuery(request -> request.getExtraJsonAttributes().add("password"));
		assertRejectedBeforeQuery(request -> request.addExtraJsonAttribute("owner.password"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "asc, e.password", "desc nulls first", "ASC; drop", "", "random" })
	void orderDirectionOtherThanAscOrDescIsRejected(String direction) {
		assertRejectedBeforeQuery(request -> request.getOrder().set(0, order("1", direction)));
	}

	@Test
	void missingOrderDirectionIsRejected() {
		assertRejectedBeforeQuery(request -> request.getOrder().set(0, order("1", null)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "-1", "7", "99", "x", "1.0", "" })
	void orderIndexOutOfRangeIsRejected(String index) {
		assertRejectedBeforeQuery(request -> request.getOrder().set(0, order(index, "asc")));
	}

	@Test
	void orderOnAColumnWithoutDataIsRejected() {
		assertRejectedBeforeQuery(request -> request.getOrder().set(0, order("0", "asc")));
	}

	@Test
	void orderOnAColumnThatThePolicyDoesNotSortIsRejected() {
		// The browser claims that owner.name is orderable
		assertRejectedBeforeQuery(request -> {
			request.getColumns().get(4).put("orderable", "true");
			request.getOrder().set(0, order("4", "desc"));
		});
	}

	@Test
	void tooManyOrderEntriesAreRejected() {
		assertRejectedBeforeQuery(request -> {
			for (int i = 0; i < 10; i++) {
				request.getOrder().add(order("1", "asc"));
			}
		});
	}

	@Test
	void invalidTableIdIsRejected() {
		assertRejectedBeforeQuery(request -> request.setDataTableId("x\"><script>"));
	}

	// Query construction

	@Test
	void queryUsesOnlyPolicyPathsAndNormalizedDirections() {
		when(query.getResultList()).thenReturn(new ArrayList<>(List.of(item(1, "first"))));
		YadaDatatablesRequest request = request();
		request.getOrder().set(0, order("2", "DESC"));
		request.getSearch().put("value", "abc");
		// The browser claims that name is searchable
		request.getColumns().get(2).put("searchable", "true");
		Map<String, Object> json = send(request);
		assertNull(json.get("yadaError"), json.toString());
		String hql = hqlCaptor.getAllValues().get(0);
		assertTrue(hql.contains("e.name desc"), hql);
		assertFalse(hql.contains("e.name like"), "name is not searchable: " + hql);
		assertTrue(hql.contains("e_owner.name like :globalSearchString"), hql);
		assertFalse(hql.toLowerCase().contains("password"), hql);
	}

	@Test
	void mapKeysAreBoundAsParameters() {
		// Sorting on a joined column selects it too, so rows are arrays
		List<Object[]> rows = new ArrayList<>();
		rows.add(new Object[] { item(1, "first"), "Title first" });
		when(query.getResultList()).thenReturn(rows);
		YadaDatatablesRequest request = request();
		request.getOrder().set(0, order("3", "asc"));
		Map<String, Object> json = send(request);
		assertNull(json.get("yadaError"), json.toString());
		String hql = hqlCaptor.getAllValues().get(0);
		assertTrue(hql.contains("KEY(e_title)=:yadaKey_e_title_656e)"), hql);
		assertFalse(hql.contains("'en'"), hql);
		// Bound with the key type of the map (Locale), on the page query and on the filtered count query
		verify(query, org.mockito.Mockito.times(2)).setParameter("yadaKey_e_title_656e", Locale.ENGLISH);
	}

	@Test
	void similarMapKeysGetDistinctParameters() {
		YadaDataTableQueryPolicy policy = YadaDataTableQueryPolicy.forEntity(Item.class).column("labels.en-US").column("labels.en_US").build();
		when(query.getResultList()).thenReturn(new ArrayList<>());
		YadaDatatablesRequest request = new YadaDatatablesRequest();
		request.setDataTableId("itemTable");
		request.setLength(10);
		request.setColumns(new ArrayList<>(List.of(column("labels.en-US", "labels.en-US", true, true), column("labels.en_US", "labels.en_US", true, true))));
		request.getSearch().put("value", "abc");
		Map<String, Object> json = dao.getConvertedJsonPage(request, Item.class, Locale.ENGLISH, policy);
		assertNull(json.get("yadaError"), json.toString());
		String hql = hqlCaptor.getAllValues().get(0);
		assertTrue(hql.contains("KEY(e_labels)=:yadaKey_e_labels_656e2d5553)"), hql);
		assertTrue(hql.contains("KEY(e_labels)=:yadaKey_e_labels_656e5f5553)"), hql);
		verify(query, org.mockito.Mockito.atLeastOnce()).setParameter("yadaKey_e_labels_656e2d5553", "en-US");
		verify(query, org.mockito.Mockito.atLeastOnce()).setParameter("yadaKey_e_labels_656e5f5553", "en_US");
	}

	// Output

	@SuppressWarnings("unchecked")
	@Test
	void outputContainsOnlyPolicyValues() {
		when(query.getResultList()).thenReturn(new ArrayList<>(List.of(item(1, "first"))));
		Map<String, Object> json = send(request());
		List<Map<String, Object>> rows = (List<Map<String, Object>>) json.get("data");
		assertEquals(1, rows.size(), json.toString());
		Map<String, Object> row = rows.get(0);
		assertEquals("1", row.get("id"));
		assertEquals("first", row.get("name"));
		assertEquals("Title first", ((Map<String, Object>) row.get("title")).get("en"));
		assertEquals(Map.of("name", "owner"), row.get("owner"));
		assertEquals("computed-first", row.get("computed"));
		assertEquals("3", row.get("count"), "Extra outputs are always returned");
		assertEquals("itemTable_Item_1", row.get("DT_RowId"));
		assertEquals("row-1", row.get("DT_RowClass"));
		assertFalse(json.toString().contains("hash"), json.toString());
	}

	@SuppressWarnings("unchecked")
	@Test
	void entityValuesAreNeverReturned() {
		when(query.getResultList()).thenReturn(new ArrayList<>(List.of(item(1, "first"))));
		YadaDatatablesRequest request = request();
		request.getColumns().add(column("anything", "anything", false, false));
		Map<String, Object> json = send(request);
		Map<String, Object> row = ((List<Map<String, Object>>) json.get("data")).get(0);
		assertTrue(row.containsKey("anything"), row.toString());
		assertNull(row.get("anything"), "The toString() of an entity was returned: " + row);
	}

	@Test
	void requestedExtraThatIsDeclaredIsAccepted() {
		when(query.getResultList()).thenReturn(new ArrayList<>());
		YadaDatatablesRequest request = request();
		request.getExtraJsonAttributes().add("count");
		assertNull(send(request).get("yadaError"));
		verify(em, org.mockito.Mockito.atLeastOnce()).createQuery(anyString());
	}

	// Policies derived from a configured table

	@Test
	void explicitPolicyFailsFastOnNonPersistentQueryablePaths() {
		assertThrows(YadaInvalidUsageException.class, () -> YadaDataTableQueryPolicy.forEntity(Item.class).column("computed").build());
		assertThrows(YadaInvalidUsageException.class, () -> YadaDataTableQueryPolicy.forEntity(Item.class).column("special").build());
		assertThrows(YadaInvalidUsageException.class, () -> YadaDataTableQueryPolicy.forEntity(Item.class).column("special", false, false).build());
	}

	@Test
	void configuredTableMakesNonPersistentColumnsOutputOnly() {
		YadaDataTableQueryPolicy policy = YadaDataTableQueryPolicy.forConfiguredTable("itemTable", Item.class).column("id").column("computed").column("special").build();
		assertTrue(policy.isSearchable("id") && policy.isOrderable("id"));
		assertEquals(List.of("id", "computed", "special"), List.copyOf(policy.getColumnPaths()));
		assertFalse(policy.isSearchable("computed") || policy.isOrderable("computed"));
		assertFalse(policy.isSearchable("special") || policy.isOrderable("special"));
	}

	@Test
	void configuredTableStillRefusesUnsafePaths() {
		for (String path : new String[] { "password", "owner.password", "settings.password", "owner", "size", "mainOwner", "name or 1=1" }) {
			assertThrows(YadaInvalidUsageException.class, () -> YadaDataTableQueryPolicy.forConfiguredTable("itemTable", Item.class).column(path).build(), path);
		}
	}

	private YadaDatatablesRequest configuredRequest() {
		YadaDatatablesRequest request = new YadaDatatablesRequest();
		request.setDataTableId("itemTable");
		request.setLength(1);
		request.setStart(1);
		request.setColumns(new ArrayList<>(List.of(column("id", "id", true, true), column("computed", "computed", true, true), column("special", "special", true, true))));
		request.setOrder(new ArrayList<>(List.of(order("0", "asc"))));
		return request;
	}

	@SuppressWarnings("unchecked")
	@Test
	void configuredTableWithGetterAndSubclassColumnsPagesAndRejectsTheirSorting() {
		YadaDataTableQueryPolicy policy = YadaDataTableQueryPolicy.forConfiguredTable("itemTable", Item.class).column("id").column("computed").column("special").build();
		SpecialItem special = new SpecialItem();
		((Item) special).id = 2L;
		((Item) special).name = "second";
		special.special = "only-in-subclass";
		when(query.getResultList()).thenReturn(new ArrayList<>(List.of(special)));
		YadaDatatablesRequest request = configuredRequest();
		request.getSearch().put("value", "second");
		Map<String, Object> json = dao.getConvertedJsonPage(request, Item.class, Locale.ENGLISH, policy);
		assertNull(json.get("yadaError"), json.toString());
		Map<String, Object> row = ((List<Map<String, Object>>) json.get("data")).get(0);
		assertEquals("computed-second", row.get("computed"));
		assertEquals("only-in-subclass", row.get("special"));
		String hql = hqlCaptor.getAllValues().get(0);
		assertFalse(hql.contains("computed") || hql.contains("special"), "Output-only columns reached the query: " + hql);
		verify(query).setFirstResult(1);
		verify(query).setMaxResults(1);
		for (String index : new String[] { "1", "2" }) {
			YadaDatatablesRequest sorted = configuredRequest();
			sorted.getOrder().set(0, order(index, "asc"));
			org.mockito.Mockito.clearInvocations(em);
			Map<String, Object> rejected = dao.getConvertedJsonPage(sorted, Item.class, Locale.ENGLISH, policy);
			assertNotNull(rejected.get("yadaError"), rejected.toString());
			verifyNoInteractions(em);
		}
	}

	@Test
	void configuredTableLogsOneWarningPerOutputOnlyColumn() {
		ch.qos.logback.classic.Logger policyLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(YadaDataTableQueryPolicy.class);
		ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ch.qos.logback.core.read.ListAppender<>();
		appender.start();
		policyLogger.addAppender(appender);
		try {
			YadaDataTableQueryPolicy.forConfiguredTable("itemTable", Item.class).column("id").column("computed").column("special").build();
		} finally {
			policyLogger.detachAppender(appender);
		}
		List<String> warnings = appender.list.stream().filter(event -> event.getLevel() == ch.qos.logback.classic.Level.WARN).map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage).toList();
		assertEquals(2, warnings.size(), warnings.toString());
		assertTrue(warnings.get(0).contains("'itemTable' column 'computed'"), warnings.get(0));
		assertTrue(warnings.get(1).contains("'itemTable' column 'special'"), warnings.get(1));
	}

	@SuppressWarnings("unchecked")
	@Test
	void embeddableValuesAreNeverReturned() {
		Item item = item(1, "first");
		item.size = new Size();
		when(query.getResultList()).thenReturn(new ArrayList<>(List.of(item)));
		YadaDataTableQueryPolicy policy = YadaDataTableQueryPolicy.forEntity(Item.class).column("id").column("sizeObject", false, false).build();
		YadaDatatablesRequest request = new YadaDatatablesRequest();
		request.setDataTableId("itemTable");
		request.setLength(10);
		request.setColumns(new ArrayList<>(List.of(column("id", "id", true, true), column("sizeObject", "sizeObject", false, false))));
		Map<String, Object> json = dao.getConvertedJsonPage(request, Item.class, Locale.ENGLISH, policy);
		Map<String, Object> row = ((List<Map<String, Object>>) json.get("data")).get(0);
		assertTrue(row.containsKey("sizeObject"), row.toString());
		assertNull(row.get("sizeObject"), row.toString());
		assertFalse(json.toString().contains("Size["), json.toString());
	}
}
