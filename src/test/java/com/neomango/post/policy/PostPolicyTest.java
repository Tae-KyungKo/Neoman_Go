package com.neomango.post.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostPolicyTest {

	@Test
	void postLengthPolicyConstantsAreDefined() {
		assertThat(PostPolicy.TITLE_MIN_LENGTH).isEqualTo(1);
		assertThat(PostPolicy.TITLE_MAX_LENGTH).isEqualTo(100);
		assertThat(PostPolicy.CONTENT_MIN_LENGTH).isEqualTo(1);
		assertThat(PostPolicy.CONTENT_MAX_LENGTH).isEqualTo(5000);
	}
}
