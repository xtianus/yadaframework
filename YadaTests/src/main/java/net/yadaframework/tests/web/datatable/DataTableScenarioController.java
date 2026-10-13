package net.yadaframework.tests.web.datatable;

import java.util.Locale;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import net.yadaframework.components.YadaDataTableFactory;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.YadaDataTableDao;
import net.yadaframework.persistence.YadaDataTableQueryPolicy;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.commerce.persistence.entity.YadaCommerceArticle;
import net.yadaframework.tests.persistence.entity.UserProfile;
import net.yadaframework.web.YadaDatatablesRequest;
import net.yadaframework.web.datatables.YadaDataTable;

/**
 * DataTables over user profiles, whose credentials hold password hashes: a configured table served by the framework
 * /yadaDataTableData endpoint and a table served by a custom endpoint shaped like the YadaExamples userProfileTablePage.
 * Both live under /test/admin, so only administrators can open the page and call the custom endpoint.
 */
@Controller
@RequestMapping("/test/admin/datatable")
public class DataTableScenarioController {
	/** Id of the table served by /yadaDataTableData */
	public static final String CONFIGURED_TABLE_ID = "ytsUserTable";
	/** Id of the table served by {@link #userTablePage(YadaDatatablesRequest, Locale)} */
	public static final String CUSTOM_TABLE_ID = "ytsCustomUserTable";
	/** Id of a table configured on the YadaUserProfile superclass, with the subclass-only "nickname" column */
	public static final String SUPERCLASS_TABLE_ID = "ytsProfileTable";

	/**
	 * What the custom endpoint allows, written by hand as a custom endpoint must do: the same columns as the table
	 */
	private static final YadaDataTableQueryPolicy CUSTOM_TABLE_POLICY = YadaDataTableQueryPolicy.forEntity(UserProfile.class)
		.column("id")
		.column("nickname", false, true)
		.column("userCredentials.username")
		.column("userCredentials.lastSuccessfulLogin", true, false)
		.build();

	/**
	 * Articles have localized names (a Map with Locale keys): the key in "name.en" is bound as a Locale parameter
	 */
	private static final YadaDataTableQueryPolicy ARTICLE_TABLE_POLICY = YadaDataTableQueryPolicy.forEntity(YadaCommerceArticle.class)
		.column("id")
		.column("sku")
		.column("name.en")
		.build();

	private final YadaDataTableFactory yadaDataTableFactory;
	private final YadaDataTableDao yadaDataTableDao;
	private final YadaConfiguration config;

	public DataTableScenarioController(YadaDataTableFactory yadaDataTableFactory, YadaDataTableDao yadaDataTableDao, YadaConfiguration config) {
		this.yadaDataTableFactory = yadaDataTableFactory;
		this.yadaDataTableDao = yadaDataTableDao;
		this.config = config;
	}

	/**
	 * The page that configures both tables. The configured table inherits the access rule of this page.
	 */
	@GetMapping("/users")
	public String users(Model model, Locale locale) {
		YadaDataTable configuredTable = yadaDataTableFactory.getSingleton(CONFIGURED_TABLE_ID, locale, table -> {
			table
				.dtEntityClass(UserProfile.class)
				.dtStructureObj()
					.dtColumnObj("ID", "id").back()
					.dtColumnObj("Nickname", "nickname").dtSearchableOff().back()
					.dtColumnObj("Email", "userCredentials.username").dtOrderAsc().back()
					.dtColumnObj("Last login", "userCredentials.lastSuccessfulLogin").dtOrderableOff().back()
					.dtColumnObj("Enabled", "userCredentials.enabled").dtOrderableOff().back()
					.dtColumnCheckbox("Select")
				.back();
		});
		YadaDataTable customTable = yadaDataTableFactory.getSingleton(CUSTOM_TABLE_ID, locale, table -> {
			table
				.dtEntityClass(UserProfile.class)
				.dtAjaxUrl(this::userTablePage)
				.dtStructureObj()
					.dtColumnObj("ID", "id").back()
					.dtColumnObj("Nickname", "nickname").dtSearchableOff().back()
					.dtColumnObj("Email", "userCredentials.username").dtOrderAsc().back()
					.dtColumnObj("Last login", "userCredentials.lastSuccessfulLogin").dtOrderableOff().back()
					.dtColumnCheckbox("Select")
				.back();
		});
		// "nickname" is only a field of the UserProfile subclass: the derived policy makes it output-only
		yadaDataTableFactory.getSingleton(SUPERCLASS_TABLE_ID, locale, table -> {
			table
				.dtEntityClass(YadaUserProfile.class)
				.dtStructureObj()
					.dtColumnObj("ID", "id").back()
					.dtColumnObj("Email", "userCredentials.username").dtOrderAsc().back()
					.dtColumnObj("Nickname", "nickname").back()
				.back();
		});
		model.addAttribute("configuredTable", configuredTable);
		model.addAttribute("customTable", customTable);
		return "/test/datatable/users";
	}

	/**
	 * Custom data endpoint with a trusted row filter on the roles chosen in the page form, like the example application.
	 */
	@RequestMapping(value = "/userTablePage", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> userTablePage(YadaDatatablesRequest yadaDatatablesRequest, Locale locale) {
		YadaSql yadaSql = yadaDatatablesRequest.getYadaSql();
		String roleKey = yadaDatatablesRequest.getExtraParam().get("rolekey");
		if (roleKey != null) {
			Integer roleId = config.getRoleId(roleKey);
			yadaSql.where(":roleId member of e.userCredentials.roles").and();
			yadaSql.setParameter("roleId", roleId == null ? -1 : roleId);
		}
		return yadaDataTableDao.getConvertedJsonPage(yadaDatatablesRequest, UserProfile.class, locale, CUSTOM_TABLE_POLICY);
	}

	/**
	 * Custom data endpoint over the seeded articles, sorted and searched on a localized name
	 */
	@RequestMapping(value = "/articleTablePage", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> articleTablePage(YadaDatatablesRequest yadaDatatablesRequest, Locale locale) {
		return yadaDataTableDao.getConvertedJsonPage(yadaDatatablesRequest, YadaCommerceArticle.class, locale, ARTICLE_TABLE_POLICY);
	}
}
