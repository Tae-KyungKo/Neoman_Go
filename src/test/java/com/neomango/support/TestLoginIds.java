package com.neomango.support;

import java.util.concurrent.atomic.AtomicLong;

public final class TestLoginIds {

	private static final AtomicLong SEQUENCE = new AtomicLong(1);

	private TestLoginIds() {
	}

	public static String next() {
		return "test" + String.format("%08d", SEQUENCE.getAndIncrement());
	}
}
