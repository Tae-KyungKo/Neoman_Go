package com.neomango.global.exception;

import java.util.List;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ErrorResponse.of(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
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
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
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
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {
		return ResponseEntity
			.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
			.body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}

