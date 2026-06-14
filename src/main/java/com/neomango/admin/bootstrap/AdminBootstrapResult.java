package com.neomango.admin.bootstrap;

public record AdminBootstrapResult(
	boolean adminCreated,
	AdminBootstrapSkipReason skipReason
) {

	public static AdminBootstrapResult created() {
		return new AdminBootstrapResult(true, null);
	}

	public static AdminBootstrapResult skipped(AdminBootstrapSkipReason skipReason) {
		return new AdminBootstrapResult(false, skipReason);
	}
}
