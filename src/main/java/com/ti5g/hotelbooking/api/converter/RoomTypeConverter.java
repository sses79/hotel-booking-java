package com.ti5g.hotelbooking.api.converter;

import java.util.Locale;

import com.ti5g.hotelbooking.domain.RoomType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class RoomTypeConverter implements Converter<String, RoomType> {

	@Override
	public RoomType convert(String source) {
		if (source.isBlank()) {
			return null;
		}

		return RoomType.valueOf(source.trim().toUpperCase(Locale.ROOT));
	}
}
