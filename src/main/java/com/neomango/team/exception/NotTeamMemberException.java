package com.neomango.team.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class NotTeamMemberException extends BusinessException {

	public NotTeamMemberException() {
		super(ErrorCode.TEAM_MEMBER_NOT_FOUND);
	}
}
