package net.yadaframework.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

class YadaSqlTest {
	@Test
	void bindsLocaleUsedOnlyInJoinCondition() {
		YadaSql sql = YadaSql.instance().selectFrom("select e from Item e")
			.join("left join e.labels label on key(label) = :locale")
			.setParameter("locale", "en_US");
		EntityManager em = mock(EntityManager.class);
		Query query = mock(Query.class);
		when(em.createQuery(sql.sql())).thenReturn(query);

		sql.query(em);

		verify(query).setParameter("locale", "en_US");
	}

	@Test
	void filteredCountRemovesTheWholeProjectionWhenAnAliasContainsFrom() {
		YadaSql sql = YadaSql.instance().selectFrom("select distinct e, fromStatus_langToText from Item e")
			.join("left join e.fromStatus fromStatus")
			.join("left join fromStatus.langToText fromStatus_langToText")
			.where("key(fromStatus_langToText) = :language")
			.orderBy("fromStatus_langToText asc");

		assertEquals("select count(*) from Item e left join e.fromStatus fromStatus left join fromStatus.langToText fromStatus_langToText where key(fromStatus_langToText) = :language",
			sql.toCount().sql());
	}
}
