package com.neomango.team.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class TeamOwnerInvariantViolationException extends BusinessException {

	public TeamOwnerInvariantViolationException() {
		super(ErrorCode.TEAM_OWNER_INVARIANT_VIOLATED);
	}
}
