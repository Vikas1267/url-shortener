package com.om.urlshortener.util;

public final class Base62 {

	private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private Base62() {
	}

	public static String encode(long num) {
		if (num < 0) {
			throw new IllegalArgumentException("Base62 cannot encode negative numbers");
		}
		if (num == 0) {
			return "0";
		}

		StringBuilder sb = new StringBuilder();
		while (num > 0) {
			sb.append(ALPHABET.charAt((int) (num % 62)));
			num /= 62;
		}
		return sb.reverse().toString();
	}

	public static long decode(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Base62 value is required");
		}

		long result = 0;
		for (int i = 0; i < value.length(); i++) {
			int digit = ALPHABET.indexOf(value.charAt(i));
			if (digit < 0) {
				throw new IllegalArgumentException("Invalid Base62 character: " + value.charAt(i));
			}
			result = Math.addExact(Math.multiplyExact(result, 62), digit);
		}
		return result;
	}
}
