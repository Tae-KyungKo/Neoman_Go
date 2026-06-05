package com.neomango.notification.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class NotificationNotFoundException extends BusinessException {

	public NotificationNotFoundException() {
		super(ErrorCode.NOTIFICATION_NOT_FOUND);
	}
}
