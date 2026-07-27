package com.ti5g.hotelbooking.api.converter;

import com.ti5g.hotelbooking.domain.RoomType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomTypeConverterTests {

	private final RoomTypeConverter converter = new RoomTypeConverter();

	@Test
	void roomTypeIsParsedWithoutCaseOrWhitespaceSensitivity() {
		assertThat(converter.convert(" double ")).isEqualTo(RoomType.DOUBLE);
		assertThat(converter.convert("DELUXE")).isEqualTo(RoomType.DELUXE);
	}

	@Test
	void blankRoomTypeMeansNoFilter() {
		assertThat(converter.convert(" ")).isNull();
	}

	@Test
	void unknownRoomTypeIsRejected() {
		assertThatThrownBy(() -> converter.convert("penthouse"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
