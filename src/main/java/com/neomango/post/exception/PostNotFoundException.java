package com.neomango.post.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class PostNotFoundException extends BusinessException {

	public PostNotFoundException() {
		super(ErrorCode.POST_NOT_FOUND);
	}
}
