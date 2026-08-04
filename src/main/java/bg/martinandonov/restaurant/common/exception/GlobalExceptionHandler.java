package bg.martinandonov.restaurant.common.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleResourceNotFound(
			ResourceNotFoundException exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ApiError> handleBusinessRule(
			BusinessRuleException exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(
			Exception exception,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request);
	}

	private ResponseEntity<ApiError> buildResponse(
			HttpStatus status,
			String message,
			HttpServletRequest request) {
		ApiError body = new ApiError(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}