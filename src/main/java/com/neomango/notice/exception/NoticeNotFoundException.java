package com.neomango.notice.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class NoticeNotFoundException extends BusinessException {

	public NoticeNotFoundException() {
		super(ErrorCode.NOTICE_NOT_FOUND);
	}
}
