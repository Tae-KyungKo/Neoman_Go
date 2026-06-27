package com.neomango.team.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class CannotKickOwnerException extends BusinessException {

	public CannotKickOwnerException() {
		super(ErrorCode.CANNOT_KICK_OWNER);
	}
}
