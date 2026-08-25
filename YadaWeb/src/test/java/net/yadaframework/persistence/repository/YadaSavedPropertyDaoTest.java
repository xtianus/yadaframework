package net.yadaframework.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.exceptions.YadaInvalidValueException;
import net.yadaframework.persistence.YadaSavedPropertyCodec;
import net.yadaframework.persistence.YadaSavedPropertyCodecs;
import net.yadaframework.persistence.YadaSavedPropertyKey;
import net.yadaframework.persistence.YadaSavedPropertyTypeEnum;
import net.yadaframework.persistence.entity.YadaSavedProperty;

/**
 * Verifies typed saved-property DAO behavior at the JPA boundary.
 */
@ExtendWith(MockitoExtension.class)
class YadaSavedPropertyDaoTest {
	private static final String FIND_QUERY = "select savedProperty from YadaSavedProperty savedProperty where savedProperty.applicationName = :applicationName and savedProperty.name = :name";

	@Mock
	private EntityManager entityManager;

	@Mock
	private TypedQuery<YadaSavedProperty> query;

	@Mock
	private YadaSavedPropertyCodec<String> inconsistentCodec;

	@Captor
	private ArgumentCaptor<YadaSavedProperty> savedPropertyCaptor;

	@InjectMocks
	private YadaSavedPropertyDao savedPropertyDao;

	/**
	 * Configures the typed lookup query used by every persistence operation.
	 */
	@BeforeEach
	void setUpQuery() {
		lenient().when(entityManager.createQuery(FIND_QUERY, YadaSavedProperty.class)).thenReturn(query);
		lenient().when(query.setParameter("applicationName", "application")).thenReturn(query);
		lenient().when(query.setParameter("name", "property.name")).thenReturn(query);
	}

	/**
	 * Verifies that a missing row produces an empty optional after a scoped typed query.
	 */
	@Test
	void findValueReturnsEmptyWhenRowIsMissing() {
		when(query.getResultList()).thenReturn(List.of());

		Optional<Long> result = savedPropertyDao.findValue(longKey());

		assertTrue(result.isEmpty());
		verifyLookup();
	}

	/**
	 * Verifies that a declared default is returned when no row exists.
	 */
	@Test
	void getValueReturnsDeclaredDefaultWhenRowIsMissing() {
		when(query.getResultList()).thenReturn(List.of());
		YadaSavedPropertyKey<Long> key = YadaSavedPropertyKey.withDefault("application", "property.name", YadaSavedPropertyCodecs.longCodec(), 17L);

		assertEquals(17L, savedPropertyDao.getValue(key));
		verifyLookup();
	}

	/**
	 * Verifies that a missing required value reports its scope and name.
	 */
	@Test
	void getValueRejectsMissingRowWithoutDefault() {
		when(query.getResultList()).thenReturn(List.of());

		YadaInvalidUsageException exception = assertThrows(YadaInvalidUsageException.class, () -> savedPropertyDao.getValue(longKey()));

		assertContext(exception);
		verifyLookup();
	}

	/**
	 * Verifies decoding when the persisted and requested logical types match.
	 */
	@Test
	void findValueDecodesMatchingStoredType() {
		YadaSavedProperty savedProperty = savedProperty(YadaSavedPropertyTypeEnum.LONG, "42");
		when(query.getResultList()).thenReturn(List.of(savedProperty));

		assertEquals(Optional.of(42L), savedPropertyDao.findValue(longKey()));
		verifyLookup();
	}

	/**
	 * Verifies that logical type drift is rejected before decoding.
	 */
	@Test
	void findValueRejectsMismatchedStoredType() {
		YadaSavedProperty savedProperty = savedProperty(YadaSavedPropertyTypeEnum.BOOLEAN, "true");
		when(query.getResultList()).thenReturn(List.of(savedProperty));

		YadaInvalidValueException exception = assertThrows(YadaInvalidValueException.class, () -> savedPropertyDao.findValue(longKey()));

		assertContext(exception);
		verifyLookup();
	}

	/**
	 * Verifies that malformed stored text is wrapped with context and without its full payload.
	 */
	@Test
	void findValueWrapsMalformedStoredTextWithContext() {
		String malformedValue = "secret-malformed-value";
		YadaSavedProperty savedProperty = savedProperty(YadaSavedPropertyTypeEnum.LONG, malformedValue);
		when(query.getResultList()).thenReturn(List.of(savedProperty));

		YadaInvalidValueException exception = assertThrows(YadaInvalidValueException.class, () -> savedPropertyDao.findValue(longKey()));

		assertContext(exception);
		assertFalse(exception.getMessage().contains(malformedValue));
		assertTrue(exception.getCause() instanceof YadaInvalidValueException);
		verifyLookup();
	}

	/**
	 * Verifies that a codec result inconsistent with its declared Java type is treated as corruption.
	 */
	@Test
	void findValueRejectsDecodedValueWithWrongJavaType() {
		when(inconsistentCodec.getType()).thenReturn(YadaSavedPropertyTypeEnum.STRING);
		when(inconsistentCodec.getJavaType()).thenReturn(String.class);
		doReturn(123L).when(inconsistentCodec).deserialize("encoded");
		YadaSavedPropertyKey<String> key = YadaSavedPropertyKey.of("application", "property.name", inconsistentCodec);
		when(query.getResultList()).thenReturn(List.of(savedProperty(YadaSavedPropertyTypeEnum.STRING, "encoded")));

		YadaInvalidValueException exception = assertThrows(YadaInvalidValueException.class, () -> savedPropertyDao.findValue(key));

		assertContext(exception);
		verifyLookup();
	}

	/**
	 * Verifies that duplicate rows are surfaced as an internal invariant violation.
	 */
	@Test
	void findValueRejectsMultipleRows() {
		YadaSavedProperty first = savedProperty(YadaSavedPropertyTypeEnum.LONG, "1");
		YadaSavedProperty second = savedProperty(YadaSavedPropertyTypeEnum.LONG, "2");
		when(query.getResultList()).thenReturn(List.of(first, second));

		YadaInternalException exception = assertThrows(YadaInternalException.class, () -> savedPropertyDao.findValue(longKey()));

		assertContext(exception);
		verifyLookup();
	}

	/**
	 * Verifies that a missing row is persisted with normalized key metadata and canonical text.
	 */
	@Test
	void setValuePersistsNormalizedCanonicalRowWhenMissing() {
		when(entityManager.createQuery(FIND_QUERY, YadaSavedProperty.class)).thenReturn(query);
		when(query.setParameter("applicationName", "default")).thenReturn(query);
		when(query.setParameter("name", "property.name")).thenReturn(query);
		when(query.getResultList()).thenReturn(List.of());
		YadaSavedPropertyKey<Long> key = YadaSavedPropertyKey.of(" \t ", "property.name", YadaSavedPropertyCodecs.longCodec());

		savedPropertyDao.setValue(key, 42L);

		verify(entityManager).persist(savedPropertyCaptor.capture());
		YadaSavedProperty persisted = savedPropertyCaptor.getValue();
		assertEquals("default", persisted.getApplicationName());
		assertEquals("property.name", persisted.getName());
		assertSame(YadaSavedPropertyTypeEnum.LONG, persisted.getType());
		assertEquals("42", persisted.getValue());
		verify(entityManager, never()).merge(any());
		verify(entityManager).createQuery(FIND_QUERY, YadaSavedProperty.class);
		verify(query).setParameter("applicationName", "default");
		verify(query).setParameter("name", "property.name");
		verify(query).getResultList();
	}

	/**
	 * Verifies that updating a managed row changes only its canonical value.
	 */
	@Test
	void setValueUpdatesOnlyManagedRowValue() {
		YadaSavedProperty managedProperty = savedProperty(YadaSavedPropertyTypeEnum.LONG, "41");
		when(query.getResultList()).thenReturn(List.of(managedProperty));

		savedPropertyDao.setValue(longKey(), 42L);

		assertEquals("application", managedProperty.getApplicationName());
		assertEquals("property.name", managedProperty.getName());
		assertSame(YadaSavedPropertyTypeEnum.LONG, managedProperty.getType());
		assertEquals("42", managedProperty.getValue());
		assertEquals(0, managedProperty.getVersion());
		verify(entityManager, never()).persist(any());
		verify(entityManager, never()).merge(any());
		verifyLookup();
	}

	/**
	 * Verifies that writes cannot change an existing row's logical type.
	 */
	@Test
	void setValueRejectsKeyWithDifferentLogicalType() {
		YadaSavedProperty managedProperty = savedProperty(YadaSavedPropertyTypeEnum.BOOLEAN, "true");
		when(query.getResultList()).thenReturn(List.of(managedProperty));

		YadaInvalidValueException exception = assertThrows(YadaInvalidValueException.class, () -> savedPropertyDao.setValue(longKey(), 42L));

		assertContext(exception);
		assertEquals("true", managedProperty.getValue());
		assertSame(YadaSavedPropertyTypeEnum.BOOLEAN, managedProperty.getType());
		verify(entityManager, never()).persist(any());
		verify(entityManager, never()).merge(any());
		verifyLookup();
	}

	/**
	 * Verifies that null writes are rejected before reaching JPA.
	 */
	@Test
	void setValueRejectsNullValue() {
		YadaInvalidValueException exception = assertThrows(YadaInvalidValueException.class, () -> savedPropertyDao.setValue(longKey(), null));

		assertContext(exception);
		verifyNoInteractions(entityManager, query);
	}

	/**
	 * Verifies that deleting an existing managed row removes that row.
	 */
	@Test
	void deleteRemovesExistingManagedRow() {
		YadaSavedProperty managedProperty = savedProperty(YadaSavedPropertyTypeEnum.LONG, "42");
		when(query.getResultList()).thenReturn(List.of(managedProperty));

		assertTrue(savedPropertyDao.delete(longKey()));
		verify(entityManager).remove(managedProperty);
		verifyLookup();
	}

	/**
	 * Verifies that deleting a missing row reports that nothing was removed.
	 */
	@Test
	void deleteReturnsFalseWhenRowIsMissing() {
		when(query.getResultList()).thenReturn(List.of());

		assertFalse(savedPropertyDao.delete(longKey()));
		verify(entityManager, never()).remove(any());
		verifyLookup();
	}

	/**
	 * Creates the standard long-valued key used by DAO tests.
	 * @return the key
	 */
	private YadaSavedPropertyKey<Long> longKey() {
		return YadaSavedPropertyKey.of("application", "property.name", YadaSavedPropertyCodecs.longCodec());
	}

	/**
	 * Creates a stored row in the standard test scope.
	 * @param type the stored logical type
	 * @param value the stored canonical text
	 * @return the row
	 */
	private YadaSavedProperty savedProperty(YadaSavedPropertyTypeEnum type, String value) {
		return new YadaSavedProperty("application", "property.name", type, value);
	}

	/**
	 * Verifies the complete typed JPA lookup at the persistence boundary.
	 */
	private void verifyLookup() {
		verify(entityManager).createQuery(FIND_QUERY, YadaSavedProperty.class);
		verify(query).setParameter("applicationName", "application");
		verify(query).setParameter("name", "property.name");
		verify(query).getResultList();
	}

	/**
	 * Verifies that a failure message identifies the property without relying on its value.
	 * @param exception the failure to inspect
	 */
	private void assertContext(RuntimeException exception) {
		assertTrue(exception.getMessage().contains("application"));
		assertTrue(exception.getMessage().contains("property.name"));
	}
}
