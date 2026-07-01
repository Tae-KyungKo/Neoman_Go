package com.neomango.admin.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin-bootstrap")
public record AdminBootstrapProperties(
	boolean enabled,
	String loginId,
	String email,
	String password,
	String nickname
) {
}
