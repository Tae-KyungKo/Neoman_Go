package com.neomango.team.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class ApplicationAlreadyProcessedException extends BusinessException {

	public ApplicationAlreadyProcessedException() {
		super(ErrorCode.INVALID_TEAM_APPLICATION_STATUS);
	}
}
