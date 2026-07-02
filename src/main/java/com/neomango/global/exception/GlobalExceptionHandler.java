package com.neomango.global.exception;

import java.util.List;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(
		BusinessException exception,
		HttpServletRequest request
	) {
		ErrorCode errorCode = exception.getErrorCode();
		if (isSseRequest(request)) {
			return ResponseEntity
				.status(errorCode.getHttpStatus())
				.build();
		}

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ErrorResponse.of(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
		MethodArgumentNotValidException exception,
		HttpServletRequest request
	) {
		if (isSseRequest(request)) {
			return ResponseEntity
				.badRequest()
				.build();
		}

		List<ErrorResponse.FieldError> errors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
			.toList();

		return ResponseEntity
			.badRequest()
			.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, errors));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(
		ConstraintViolationException exception,
		HttpServletRequest request
	) {
		if (isSseRequest(request)) {
			return ResponseEntity
				.badRequest()
				.build();
		}

		List<ErrorResponse.FieldError> errors = exception.getConstraintViolations()
			.stream()
			.map(violation -> new ErrorResponse.FieldError(
				violation.getPropertyPath().toString(),
				violation.getMessage()
			))
			.toList();

		return ResponseEntity
			.badRequest()
			.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, errors));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
		if (isSseRequest(request)) {
			return ResponseEntity
				.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
				.build();
		}

		return ResponseEntity
			.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
			.body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	private boolean isSseRequest(HttpServletRequest request) {
		String accept = request.getHeader(HttpHeaders.ACCEPT);
		if (accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
			return true;
		}

		return "/api/notifications/stream".equals(request.getRequestURI());
	}
}

