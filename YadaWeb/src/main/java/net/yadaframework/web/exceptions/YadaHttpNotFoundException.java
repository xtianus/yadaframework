package net.yadaframework.web.exceptions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Throw this exception to generate a 404 HTTP return code.
 * The exception text is localized with yada.error.http.404
 */
@ResponseStatus(value=HttpStatus.NOT_FOUND)  // 404
public class YadaHttpNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -357253456084830193L;
}
