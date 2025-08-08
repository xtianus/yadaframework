package net.yadaframework.web.datatables;

public class YadaDataTableHelper {
	
	static String resolveAjaxUrlFromHandlerRef(YadaDtAjaxHandler handlerRef) {
		if (handlerRef == null) {
			return null;
		}
		java.lang.reflect.Method method = resolveMethodFromLambda(handlerRef);
		String url = resolveRequestMappingUrl(method);
		if (url == null || url.isEmpty()) {
			throw new IllegalArgumentException("Could not resolve @RequestMapping path for method: " + method);
		}
		return url;
	}

	private static java.lang.reflect.Method resolveMethodFromLambda(java.io.Serializable lambda) {
		try {
			java.lang.reflect.Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
			writeReplace.setAccessible(true);
			Object ser = writeReplace.invoke(lambda);
			java.lang.invoke.SerializedLambda sl = (java.lang.invoke.SerializedLambda) ser;
			String implClassName = sl.getImplClass().replace('/', '.');
			String implMethodName = sl.getImplMethodName();
			ClassLoader cl = lambda.getClass().getClassLoader();
			Class<?> implClass = Class.forName(implClassName, false, cl);
			// Prefer method with two params (request, locale)
			for (java.lang.reflect.Method m : implClass.getDeclaredMethods()) {
				if (m.getName().equals(implMethodName) && m.getParameterCount() == 2) {
					return m;
				}
			}
			// Fallback by name only
			for (java.lang.reflect.Method m : implClass.getDeclaredMethods()) {
				if (m.getName().equals(implMethodName)) {
					return m;
				}
			}
			throw new NoSuchMethodException(implClassName + "#" + implMethodName);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to resolve method from lambda", e);
		}
	}

	private static String resolveRequestMappingUrl(java.lang.reflect.Method method) {
		String methodPath = extractPathFromMappingAnnotations(method);
		if (methodPath == null) {
			return null;
		}
		String classPath = extractPathFromMappingAnnotations(method.getDeclaringClass());
		return joinPaths(classPath, methodPath);
	}

	private static String extractPathFromMappingAnnotations(java.lang.reflect.AnnotatedElement element) {
		org.springframework.web.bind.annotation.RequestMapping rm =
				element.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
		String path = firstNonEmpty(rm == null ? null : rm.value());
		if (path == null) {
			path = firstNonEmpty(rm == null ? null : rm.path());
		}
		if (path != null) {
			return path;
		}
		if (element instanceof java.lang.reflect.Method) {
			java.lang.reflect.Method m = (java.lang.reflect.Method) element;
			org.springframework.web.bind.annotation.GetMapping get = m.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
			if (get != null) {
				path = firstNonEmpty(get.value());
				if (path == null) path = firstNonEmpty(get.path());
				if (path != null) return path;
			}
			org.springframework.web.bind.annotation.PostMapping post = m.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
			if (post != null) {
				path = firstNonEmpty(post.value());
				if (path == null) path = firstNonEmpty(post.path());
				if (path != null) return path;
			}
			org.springframework.web.bind.annotation.PutMapping put = m.getAnnotation(org.springframework.web.bind.annotation.PutMapping.class);
			if (put != null) {
				path = firstNonEmpty(put.value());
				if (path == null) path = firstNonEmpty(put.path());
				if (path != null) return path;
			}
			org.springframework.web.bind.annotation.DeleteMapping del = m.getAnnotation(org.springframework.web.bind.annotation.DeleteMapping.class);
			if (del != null) {
				path = firstNonEmpty(del.value());
				if (path == null) path = firstNonEmpty(del.path());
				if (path != null) return path;
			}
			org.springframework.web.bind.annotation.PatchMapping patch = m.getAnnotation(org.springframework.web.bind.annotation.PatchMapping.class);
			if (patch != null) {
				path = firstNonEmpty(patch.value());
				if (path == null) path = firstNonEmpty(patch.path());
				if (path != null) return path;
			}
		}
		return null;
	}

	private static String joinPaths(String base, String path) {
		if (path == null || path.isEmpty()) return path;
		String p = path.startsWith("/") ? path : "/" + path;
		if (base == null || base.isEmpty()) return p;
		String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
		return (b.startsWith("/") ? b : "/" + b) + p;
	}

	private static String firstNonEmpty(String[] arr) {
		if (arr == null) return null;
		for (String s : arr) {
			if (s != null && !s.isEmpty()) return s;
		}
		return null;
	}
}
