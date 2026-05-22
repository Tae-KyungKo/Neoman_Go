package com.neomango.team.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class DuplicateTeamMemberException extends BusinessException {

	public DuplicateTeamMemberException() {
		super(ErrorCode.DUPLICATE_TEAM_MEMBER);
	}
}
