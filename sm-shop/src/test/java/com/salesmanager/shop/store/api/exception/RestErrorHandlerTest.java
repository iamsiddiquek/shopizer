package com.salesmanager.shop.store.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

class RestErrorHandlerTest {

	private final RestErrorHandler handler = new RestErrorHandler();

	@Test
	void handleUnreadableMessageReturnsBadRequestError() {
		HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
				"JSON parse error",
				new IllegalArgumentException("invalid json"),
				mock(HttpInputMessage.class));

		ErrorEntity error = handler.handleUnreadableMessage(exception);

		assertNotNull(error);
		assertEquals("400", error.getErrorCode());
		assertTrue(error.getMessage().contains("Invalid request payload"));
		assertTrue(error.getMessage().contains("invalid json"));
	}

	@Test
	void handleGenericRuntimeExceptionMapsHttpStatusFromNumericCode() {
		GenericRuntimeException exception = new GenericRuntimeException(
				"409",
				"Duplicate resource",
				new IllegalStateException("already exists"));

		ResponseEntity<ErrorEntity> response = handler.handleGenericRuntimeException(exception);

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("409", response.getBody().getErrorCode());
		assertTrue(response.getBody().getMessage().contains("Duplicate resource"));
		assertTrue(response.getBody().getMessage().contains("already exists"));
	}

	@Test
	void handleGenericRuntimeExceptionFallsBackToBadRequestForInvalidCode() {
		GenericRuntimeException exception = new GenericRuntimeException(
				"not-a-status",
				"Bad value",
				new IllegalArgumentException("invalid value"));

		ResponseEntity<ErrorEntity> response = handler.handleGenericRuntimeException(exception);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("not-a-status", response.getBody().getErrorCode());
		assertTrue(response.getBody().getMessage().contains("Bad value"));
	}

	@Test
	void handleDataIntegrityViolationReturnsConflict() {
		DataIntegrityViolationException exception =
				new DataIntegrityViolationException("constraint",
						new IllegalStateException("unique index violation"));

		ErrorEntity error = handler.handleDataIntegrityViolation(exception);

		assertNotNull(error);
		assertEquals("409", error.getErrorCode());
		assertTrue(error.getMessage().contains("Data integrity violation"));
		assertTrue(error.getMessage().contains("unique index violation"));
	}

	@Test
	void handleServiceRuntimeExceptionUsesRootCauseMessage() {
		Throwable root = new IllegalArgumentException("root cause");
		Throwable middle = new RuntimeException("middle", root);
		ServiceRuntimeException exception = new ServiceRuntimeException("service failure", middle);

		ErrorEntity error = handler.handleServiceException(exception);

		assertNotNull(error);
		assertEquals("500", error.getErrorCode());
		assertTrue(error.getMessage().contains("service failure"));
		assertTrue(error.getMessage().contains("root cause"));
	}

	@Test
	void handleSpecificApiExceptionsReturnExpectedCodes() {
		ErrorEntity notFound = handler.handleServiceException(new ResourceNotFoundException("Missing product"));
		ErrorEntity unauthorized = handler.handleServiceException(new UnauthorizedException("No access"));
		ErrorEntity restApi = handler.handleRestApiException(new RestApiException("400", "Invalid request"));
		ErrorEntity conversion = handler.handleServiceException(new ConversionRuntimeException("422", "Bad conversion"));

		assertEquals("404", notFound.getErrorCode());
		assertEquals("401", unauthorized.getErrorCode());
		assertEquals("400", restApi.getErrorCode());
		assertEquals("422", conversion.getErrorCode());
	}
}
