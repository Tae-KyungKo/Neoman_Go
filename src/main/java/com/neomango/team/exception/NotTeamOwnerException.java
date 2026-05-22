package com.neomango.team.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class NotTeamOwnerException extends BusinessException {

	public NotTeamOwnerException() {
		super(ErrorCode.TEAM_OWNER_REQUIRED);
	}
}
