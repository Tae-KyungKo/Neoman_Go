package com.neomango.comment.exception;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;

public class CommentAccessDeniedException extends BusinessException {

	public CommentAccessDeniedException() {
		super(ErrorCode.COMMENT_ACCESS_DENIED);
	}
}
