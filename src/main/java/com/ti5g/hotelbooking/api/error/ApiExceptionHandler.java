package com.ti5g.hotelbooking.api.error;

import com.ti5g.hotelbooking.service.availability.HotelNotFoundException;
import com.ti5g.hotelbooking.service.availability.InvalidAvailabilityRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidAvailabilityRequestException.class)
	ProblemDetail handleInvalidAvailabilityRequest(
			InvalidAvailabilityRequestException exception) {
		return problem(
				HttpStatus.BAD_REQUEST,
				"Invalid availability request",
				exception.getMessage());
	}

	@ExceptionHandler(HotelNotFoundException.class)
	ProblemDetail handleHotelNotFound(HotelNotFoundException exception) {
		return problem(
				HttpStatus.NOT_FOUND,
				"Hotel not found",
				exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		return problem(
				HttpStatus.BAD_REQUEST,
				"Invalid request parameter",
				"Invalid value for parameter '%s'.".formatted(exception.getName()));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception) {
		return problem(
				HttpStatus.BAD_REQUEST,
				"Missing request parameter",
				"Required parameter '%s' is missing.".formatted(exception.getParameterName()));
	}

	private static ProblemDetail problem(
			HttpStatus status,
			String title,
			String detail) {
		var problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
