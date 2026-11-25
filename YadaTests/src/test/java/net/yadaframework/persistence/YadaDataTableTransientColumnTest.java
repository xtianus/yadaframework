package net.yadaframework.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Transient;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.entity.YadaJob;
import net.yadaframework.web.YadaDatatablesColumn;
import net.yadaframework.web.YadaDatatablesRequest;

class YadaDataTableTransientColumnTest {

	private abstract static class JobRow extends YadaJob {
		@Embedded
		private Details details;

		@Transient
		private Details temporaryDetails;
	}

	@Embeddable
	private static class Details {
		private String name;
		private transient String displayName;
	}

	@ParameterizedTest
	@ValueSource(strings = {"recovered", "details.displayName", "temporaryDetails.name"})
	void explicitPolicyRejectsTransientQueryPaths(String path) {
		assertThrows(YadaInvalidUsageException.class,
			() -> YadaDataTableQueryPolicy.forEntity(JobRow.class).column(path).build());
	}

	@ParameterizedTest
	@ValueSource(strings = {"recovered", "details.displayName", "temporaryDetails.name"})
	void configuredPolicyKeepsTransientColumnsForOutputOnly(String path) {
		YadaDataTableQueryPolicy policy = YadaDataTableQueryPolicy.forConfiguredTable("jobs", JobRow.class)
			.column(path).build();
		YadaDatatablesColumn column = new YadaDatatablesColumn();
		column.put("data", path);
		column.put("searchable", "true");
		YadaDatatablesRequest request = new YadaDatatablesRequest();
		request.setColumns(List.of(column));
		request.getSearch().put("value", "text");

		YadaDataTableQueryPolicy.Resolved resolved = policy.resolve(request);

		assertEquals(List.of(path), resolved.outputPaths());
		assertEquals(List.of(), resolved.searchPaths());
		assertFalse(policy.isOrderable(path));
	}

	@ParameterizedTest
	@ValueSource(strings = {"recovered", "details.displayName", "temporaryDetails.name"})
	void explicitPolicyAllowsTransientOutputColumns(String path) {
		YadaDataTableQueryPolicy policy = YadaDataTableQueryPolicy.forEntity(JobRow.class)
			.column(path, false, false).build();

		assertEquals(List.of(path), List.copyOf(policy.getColumnPaths()));
	}

	@ParameterizedTest
	@ValueSource(strings = {"temporaryDetails", "temporaryDetails.secret"})
	void configuredPolicyRefusesUnsafeTransientOutput(String path) {
		assertThrows(YadaInvalidUsageException.class,
			() -> YadaDataTableQueryPolicy.forConfiguredTable("jobs", JobRow.class).column(path).build());
	}
}
