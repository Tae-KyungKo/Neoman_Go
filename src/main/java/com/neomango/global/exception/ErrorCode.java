package com.neomango.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "G001", "잘못된 요청입니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "G002", "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "G003", "권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "G004", "요청한 리소스를 찾을 수 없습니다."),
	DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "G005", "이미 존재하는 리소스입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G999", "서버 내부 오류가 발생했습니다."),

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),

	TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "팀을 찾을 수 없습니다."),
	TEAM_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "T002", "팀장 권한이 필요합니다."),
	ALREADY_TEAM_MEMBER(HttpStatus.CONFLICT, "T003", "이미 팀에 가입한 사용자입니다."),
	DUPLICATE_TEAM_MEMBER(HttpStatus.CONFLICT, "T004", "이미 존재하는 팀 멤버입니다."),
	TEAM_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "TA001", "팀 가입 신청을 찾을 수 없습니다."),
	DUPLICATE_PENDING_TEAM_APPLICATION(HttpStatus.CONFLICT, "TA002", "이미 처리 대기 중인 가입 신청이 있습니다."),
	INVALID_TEAM_APPLICATION_STATUS(HttpStatus.CONFLICT, "TA003", "처리할 수 없는 가입 신청 상태입니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}

