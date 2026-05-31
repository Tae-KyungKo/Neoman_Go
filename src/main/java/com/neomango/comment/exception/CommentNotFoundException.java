package com.neomango.comment.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class CommentNotFoundException extends BusinessException {

	public CommentNotFoundException() {
		super(ErrorCode.COMMENT_NOT_FOUND);
	}
}
