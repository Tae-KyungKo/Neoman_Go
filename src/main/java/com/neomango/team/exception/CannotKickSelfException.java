package com.neomango.team.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class CannotKickSelfException extends BusinessException {

	public CannotKickSelfException() {
		super(ErrorCode.CANNOT_KICK_SELF);
	}
}
