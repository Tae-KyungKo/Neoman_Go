package com.neomango.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	TEAM_OWNER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "T005", "팀 소유자를 찾을 수 없습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "G001", "잘못된 요청입니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "G002", "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "G003", "권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "G004", "요청한 리소스를 찾을 수 없습니다."),
	DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "G005", "이미 존재하는 리소스입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G999", "서버 내부 오류가 발생했습니다."),

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),

	TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "존재하지 않는 팀입니다."),
	TEAM_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "T002", "팀장 권한이 필요합니다."),
	ALREADY_TEAM_MEMBER(HttpStatus.CONFLICT, "T003", "이미 소속되었습니다."),
	DUPLICATE_TEAM_MEMBER(HttpStatus.CONFLICT, "T004", "이미 존재하는 팀 멤버입니다."),
	TEAM_CLOSED(HttpStatus.CONFLICT, "T006", "마감된 팀입니다."),
	ALREADY_CATEGORY_TEAM_MEMBER(HttpStatus.CONFLICT, "T007", "이미 해당 카테고리의 팀에 소속되었습니다."),
	TEAM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "T008", "해당 팀의 멤버가 아닙니다."),
	CANNOT_KICK_SELF(HttpStatus.CONFLICT, "T009", "자기 자신을 강퇴할 수 없습니다."),
	CANNOT_LEAVE_OWNER_WITHOUT_DELEGATION(
		HttpStatus.CONFLICT,
		"T010",
		"현재 탈퇴하려는 팀의 주장입니다. 다음 주장을 선택하고 탈퇴하여 주십시오."
	),
	INVALID_OWNER_DELEGATION_TARGET(HttpStatus.CONFLICT, "T011", "위임 대상은 같은 팀의 활성 멤버여야 합니다."),
	CANNOT_KICK_OWNER(HttpStatus.CONFLICT, "T012", "팀 주장은 강퇴할 수 없습니다."),
	TEAM_OWNER_INVARIANT_VIOLATED(HttpStatus.CONFLICT, "T013", "활성 팀 주장은 반드시 한 명이어야 합니다."),
	TEAM_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "TA001", "존재하지 않는 가입 신청입니다."),
	DUPLICATE_PENDING_TEAM_APPLICATION(HttpStatus.CONFLICT, "TA002", "이미 가입 신청한 팀입니다."),
	INVALID_TEAM_APPLICATION_STATUS(HttpStatus.CONFLICT, "TA003", "처리할 수 없는 가입 신청 상태입니다."),
	TEAM_APPLICATION_CANCEL_FORBIDDEN(HttpStatus.FORBIDDEN, "TA004", "본인의 가입 신청만 취소할 수 있습니다."),
	ONLY_PENDING_TEAM_APPLICATION_CANCELABLE(HttpStatus.CONFLICT, "TA005", "대기 중인 가입 신청만 취소할 수 있습니다."),
	TEAM_APPLICATION_LIST_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "TA006", "팀 주장만 가입 신청 목록을 조회할 수 있습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
