package com.neomango.team.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class InvalidOwnerDelegationTargetException extends BusinessException {

	public InvalidOwnerDelegationTargetException() {
		super(ErrorCode.INVALID_OWNER_DELEGATION_TARGET);
	}
}
