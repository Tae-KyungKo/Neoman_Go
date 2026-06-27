package com.neomango.team.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class CannotLeaveOwnerWithoutDelegationException extends BusinessException {

	public CannotLeaveOwnerWithoutDelegationException() {
		super(ErrorCode.CANNOT_LEAVE_OWNER_WITHOUT_DELEGATION);
	}
}
