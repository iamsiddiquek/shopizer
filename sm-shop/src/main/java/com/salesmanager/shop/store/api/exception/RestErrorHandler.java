package com.salesmanager.shop.store.api.exception;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice({"com.salesmanager.shop.store.api"})
public class RestErrorHandler {
  
    private static final Logger log = LoggerFactory.getLogger(RestErrorHandler.class);

    @RequestMapping(produces = "application/json")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorEntity handleUnreadableMessage(HttpMessageNotReadableException exception) {
        log.error(exception.getMessage(), exception);
        Throwable rootCause = Optional.ofNullable(exception.getCause()).orElse(exception);
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return createErrorEntity("400", "Invalid request payload", rootCause.getMessage());
    }

    @RequestMapping(produces = "application/json")
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody ErrorEntity handleServiceException(Exception exception) {
        log.error(exception.getMessage(), exception);
        Throwable rootCause = Optional.ofNullable(exception.getCause()).orElse(exception);
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        ErrorEntity errorEntity = createErrorEntity("500", exception.getMessage(),
        		rootCause.getMessage());
        return errorEntity;
    }

    @RequestMapping(produces = "application/json")
    @ExceptionHandler(GenericRuntimeException.class)
    public ResponseEntity<ErrorEntity> handleGenericRuntimeException(GenericRuntimeException exception) {
        log.error(exception.getErrorMessage(), exception);
        Throwable rootCause = Optional.ofNullable(exception.getCause()).orElse(exception);
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (!StringUtils.isBlank(exception.getErrorCode())) {
            try {
                HttpStatus resolved = HttpStatus.resolve(Integer.parseInt(exception.getErrorCode()));
                if (resolved != null) {
                    status = resolved;
                }
            } catch (NumberFormatException ignored) {
                // fallback to BAD_REQUEST
            }
        }
        ErrorEntity errorEntity = createErrorEntity(
                exception.getErrorCode() != null ? exception.getErrorCode() : String.valueOf(status.value()),
                exception.getErrorMessage(),
                rootCause.getMessage());
        return ResponseEntity.status(status).body(errorEntity);
    }

    @RequestMapping(produces = "application/json")
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public @ResponseBody ErrorEntity handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.error(exception.getMessage(), exception);
        Throwable rootCause = Optional.ofNullable(exception.getCause()).orElse(exception);
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return createErrorEntity("409", "Data integrity violation", rootCause.getMessage());
    }


    /**
     * Generic exception serviceException handler
     */
    @RequestMapping(produces = "application/json")
    @ExceptionHandler(ServiceRuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorEntity handleServiceException(ServiceRuntimeException exception) {
        log.error(exception.getErrorMessage(), exception);
        Throwable rootCause = Optional.ofNullable(exception.getCause()).orElse(exception);
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        ErrorEntity errorEntity = createErrorEntity(exception.getErrorCode()!=null?exception.getErrorCode():"500", exception.getErrorMessage(),
        		rootCause.getMessage());
        return errorEntity;
    }

    @RequestMapping(produces = "application/json")
    @ExceptionHandler(ConversionRuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorEntity handleServiceException(ConversionRuntimeException exception) {
        log.error(exception.getErrorMessage(), exception);
        ErrorEntity errorEntity = createErrorEntity(exception.getErrorCode(), exception.getErrorMessage(),
            exception.getLocalizedMessage());
        return errorEntity;
    }

    @RequestMapping(produces = "application/json")
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public @ResponseBody ErrorEntity handleServiceException(ResourceNotFoundException exception) {
        log.error(exception.getErrorMessage(), exception);

        ErrorEntity errorEntity = createErrorEntity(exception.getErrorCode(), exception.getErrorMessage(),
                exception.getLocalizedMessage());
        return errorEntity;
    }
    
    @RequestMapping(produces = "application/json")
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public @ResponseBody ErrorEntity handleServiceException(UnauthorizedException exception) {
        log.error(exception.getErrorMessage(), exception);

        ErrorEntity errorEntity = createErrorEntity(exception.getErrorCode(), exception.getErrorMessage(),
                exception.getLocalizedMessage());
        return errorEntity;
    }

    @RequestMapping(produces = "application/json")
    @ExceptionHandler(RestApiException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorEntity handleRestApiException(RestApiException exception) {
        log.error(exception.getErrorMessage(), exception);

        ErrorEntity errorEntity = createErrorEntity(exception.getErrorCode(), exception.getErrorMessage(),
                exception.getLocalizedMessage());
        return errorEntity;
    }

    private ErrorEntity createErrorEntity(String errorCode, String message, String detailMessage) {
        ErrorEntity errorEntity = new ErrorEntity();
        Optional.ofNullable(errorCode)
                .ifPresent(errorEntity::setErrorCode);

        String resultMessage = (message != null && detailMessage !=null)  ? new StringBuilder().append(message).append(", ").append(detailMessage).toString() : detailMessage;
        if(StringUtils.isBlank(resultMessage)) {
        	resultMessage = message;
        }
        Optional.ofNullable(resultMessage)
                .ifPresent(errorEntity::setMessage);
        return errorEntity;
    }
}
