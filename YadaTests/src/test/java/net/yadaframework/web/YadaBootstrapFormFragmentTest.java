package net.yadaframework.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.configuration2.XMLConfiguration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import net.yadaframework.components.YadaFormHelper;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaWebConfig;

class YadaBootstrapFormFragmentTest {

	@ParameterizedTest
	@CsvSource({"fileUpload, field, attachment, file", "fileUpload, body, attachment, file", "text, field, subject, text", "text, body, subject, text"})
	void bootstrapFiveFormFragmentsRenderBoundInputs(String fragment, String selector, String fieldName, String inputType) throws Exception {
		String template = """
			<form th:object="${example}" method="post" enctype="multipart/form-data">
				<div th:replace="~{${@yadaWebUtil.getFormFragment('%s')} :: %s(fieldName='%s', label='Example label', required=true)}"></div>
			</form>
			""".formatted(fragment, selector, fieldName);

		Document page = Jsoup.parse(render(template));

		assertEquals(1, page.select("form input.form-control[name=%s][type=%s][required]".formatted(fieldName, inputType)).size());
		assertEquals("Example label", page.select("form label").text());
	}

	@ParameterizedTest
	@MethodSource("fieldFragmentCases")
	void bootstrapFiveFieldFragmentsRenderTheSameAsBody(String fragment, String fragmentArguments, String controlSelector, int controlCount) throws Exception {
		String template = """
			<form th:object="${example}" method="post">
				<div th:replace="~{${@yadaWebUtil.getFormFragment('%s')} :: %s(%s)}"></div>
			</form>
			""";

		Document fieldPage = Jsoup.parse(render(template.formatted(fragment, "field", fragmentArguments)));
		Document bodyPage = Jsoup.parse(render(template.formatted(fragment, "body", fragmentArguments)));

		assertEquals(controlCount, fieldPage.select("form " + controlSelector).size());
		assertEquals(fieldPage.select("form").html(), bodyPage.select("form").html());
	}

	static Stream<Arguments> fieldFragmentCases() {
		return Stream.of(
			Arguments.of("boolean", "fieldName='enabled', label='Example label'", "input[type=checkbox][name=enabled]", 1),
			Arguments.of("checklist", "fieldName='tags', options=${ {'red','blue'} }, label='Example label'", "input[type=checkbox][name=tags]", 2),
			Arguments.of("enum", "fieldName='day', enumClass='java.time.DayOfWeek', localized=false, label='Example label'", "select[name=day]", 1),
			Arguments.of("list", "fieldName='choice', options=${ {'red','blue'} }, label='Example label'", "select[name=choice]", 1),
			Arguments.of("radio-values", "fieldName='choice', values=${ {'red','blue'} }, labelPrefix='form.label.', label='Example label'", "input[type=radio][name=choice]", 2),
			Arguments.of("range", "fieldName='amount', min=0, max=10, step=1, label='Example label'", "input[type=range][name=amount]", 1)
		);
	}

	private String render(String template) throws Exception {
		YadaConfiguration config = new YadaConfiguration() {};
		config.setConfiguration(new XMLConfiguration());
		YadaWebUtil yadaWebUtil = new YadaWebUtil();
		ReflectionTestUtils.setField(yadaWebUtil, "config", config);
		YadaWebConfig webConfig = new YadaWebConfig();
		ReflectionTestUtils.setField(webConfig, "config", config);

		MockServletContext servletContext = new MockServletContext();
		try (StaticWebApplicationContext applicationContext = new StaticWebApplicationContext()) {
			applicationContext.setServletContext(servletContext);
			applicationContext.getBeanFactory().registerSingleton("yadaWebUtil", yadaWebUtil);
			applicationContext.getBeanFactory().registerSingleton("yadaFormHelper", new YadaFormHelper());
			applicationContext.refresh();
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

			StringTemplateResolver page = new StringTemplateResolver() {
				@Override
				protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate, String name, Map<String, Object> attributes) {
					return new StringTemplateResource(template);
				}
			};
			page.setResolvablePatterns(Set.of("test-form"));
			SpringTemplateEngine engine = new SpringTemplateEngine();
			engine.addTemplateResolver(webConfig.yadaTemplateResolver());
			engine.addTemplateResolver(page);

			ThymeleafViewResolver resolver = new ThymeleafViewResolver();
			resolver.setApplicationContext(applicationContext);
			resolver.setTemplateEngine(engine);
			MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/test-form");
			MockHttpServletResponse response = new MockHttpServletResponse();
			RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
			try {
				resolver.resolveViewName("test-form", Locale.ENGLISH).render(Map.of("example", new ExampleForm()), request, response);
			} finally {
				RequestContextHolder.resetRequestAttributes();
			}
			return response.getContentAsString();
		}
	}

	public static class ExampleForm {
		public MultipartFile getAttachment() {
			return null;
		}

		public String getSubject() {
			return "A subject";
		}

		public boolean isEnabled() {
			return true;
		}

		public List<String> getTags() {
			return List.of("red");
		}

		public DayOfWeek getDay() {
			return DayOfWeek.MONDAY;
		}

		public String getChoice() {
			return "red";
		}

		public int getAmount() {
			return 5;
		}
	}
}
