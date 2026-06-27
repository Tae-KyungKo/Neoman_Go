package com.neomango.post.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class PostAccessDeniedException extends BusinessException {

	public PostAccessDeniedException() {
		super(ErrorCode.POST_ACCESS_DENIED);
	}
}
