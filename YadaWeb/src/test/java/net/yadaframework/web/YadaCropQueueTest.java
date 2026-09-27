package net.yadaframework.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import net.yadaframework.components.YadaWebUtil;

class YadaCropQueueTest {

	@Test
	void addRedirectParameterEnhancesDestinationWithoutRedirectPrefix() throws Exception {
		YadaCropQueue cropQueue = new YadaCropQueue("redirect:/en/manager/cropPage", "redirect:/en/manager/novelties");
		YadaWebUtil yadaWebUtil = Mockito.mock(YadaWebUtil.class);
		setField(cropQueue, "yadaWebUtil", yadaWebUtil);
		when(yadaWebUtil.enhanceUrl(eq("/en/manager/novelties"), eq(null), eq("yadaAttachedFileId"), eq("42")))
			.thenReturn("/en/manager/novelties?yadaAttachedFileId=42");

		YadaCropQueue result = cropQueue.addRedirectParameter("yadaAttachedFileId", "42");

		assertSame(cropQueue, result);
		assertEquals("redirect:/en/manager/novelties?yadaAttachedFileId=42", cropQueue.getDestinationRedirect());
	}

	@Test
	void addRedirectParameterIgnoresNullName() throws Exception {
		YadaCropQueue cropQueue = new YadaCropQueue("redirect:/en/manager/cropPage", "redirect:/en/manager/novelties");
		YadaWebUtil yadaWebUtil = Mockito.mock(YadaWebUtil.class);
		setField(cropQueue, "yadaWebUtil", yadaWebUtil);

		YadaCropQueue result = cropQueue.addRedirectParameter(null, "42");

		assertSame(cropQueue, result);
		assertEquals("redirect:/en/manager/novelties", cropQueue.getDestinationRedirect());
		verifyNoInteractions(yadaWebUtil);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

}
