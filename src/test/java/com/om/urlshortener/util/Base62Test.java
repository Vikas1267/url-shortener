package com.om.urlshortener.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class Base62Test {

	@ParameterizedTest
	@CsvSource({
			"0, 0",
			"61, Z",
			"62, 10"
	})
	void encodesBoundaryValues(long value, String expected) {
		assertThat(Base62.encode(value)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(longs = {0, 1, 61, 62, 3843, 3844, 100000, 9_876_543_210L, Long.MAX_VALUE})
	void roundTripsEncodedLongs(long value) {
		assertThat(Base62.decode(Base62.encode(value))).isEqualTo(value);
	}

	@Test
	void rejectsNegativeValues() {
		assertThatThrownBy(() -> Base62.encode(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("negative");
	}
}
