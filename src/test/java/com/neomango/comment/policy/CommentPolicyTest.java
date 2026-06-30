package com.neomango.comment.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommentPolicyTest {

	@Test
	void commentLengthPolicyConstantsAreDefined() {
		assertThat(CommentPolicy.CONTENT_MIN_LENGTH).isEqualTo(1);
		assertThat(CommentPolicy.CONTENT_MAX_LENGTH).isEqualTo(1000);
	}
}
