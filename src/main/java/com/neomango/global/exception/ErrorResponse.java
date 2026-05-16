package com.neomango.global.exception;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
	int status,
	String code,
	String message,
	List<FieldError> errors
) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(
			errorCode.getHttpStatus().value(),
			errorCode.getCode(),
			errorCode.getMessage(),
			null
		);
	}

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
		return new ErrorResponse(
			errorCode.getHttpStatus().value(),
			errorCode.getCode(),
			errorCode.getMessage(),
			errors
		);
	}

	public record FieldError(
		String field,
		String message
	) {
	}
}
