package com.neomango.user.policy;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class UserPolicy {

	public static final int LOGIN_ID_MIN_LENGTH = 4;
	public static final int LOGIN_ID_MAX_LENGTH = 12;
	public static final String LOGIN_ID_PATTERN = "^[A-Za-z0-9가-힣]{4,12}$";
	public static final Pattern LOGIN_ID_REGEX = Pattern.compile(LOGIN_ID_PATTERN);

	public static final int NICKNAME_MIN_LENGTH = 2;
	public static final int NICKNAME_MAX_LENGTH = 12;

	public static final int PASSWORD_MIN_LENGTH = 8;
	public static final int PASSWORD_MAX_LENGTH = 16;
	public static final String PASSWORD_PATTERN = "^[A-Za-z0-9!@#$%^&*()_\\-+=\\[\\]{};:'\",.<>/?\\\\|`~]{8,16}$";
	public static final Pattern PASSWORD_REGEX = Pattern.compile(PASSWORD_PATTERN);

	private static final Set<String> RESERVED_NICKNAMES = Set.of("관리자", "운영자", "admin");

	private UserPolicy() {
	}

	public static boolean isReservedNickname(String nickname) {
		if (nickname == null) {
			return false;
		}

		String normalizedNickname = nickname.trim().toLowerCase(Locale.ROOT);
		return RESERVED_NICKNAMES.contains(normalizedNickname);
	}
}
