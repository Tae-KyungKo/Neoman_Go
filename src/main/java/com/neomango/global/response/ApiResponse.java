package com.neomango.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
	boolean success,
	String code,
	String message,
	T data
) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, "SUCCESS", "요청이 성공했습니다.", data);
	}

	public static ApiResponse<Void> successWithoutData() {
		return new ApiResponse<>(true, "SUCCESS", "요청이 성공했습니다.", null);
	}

	public static ApiResponse<Void> fail(String code, String message) {
		return new ApiResponse<>(false, code, message, null);
	}
}

