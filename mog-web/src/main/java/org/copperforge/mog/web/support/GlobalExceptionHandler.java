package org.copperforge.mog.web.support;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MogApiClientException.class)
    public ModelAndView handleMogApi(MogApiClientException ex) {
        log.warn("MOG API client failure while serving UI request", ex);
        ModelAndView mav = new ModelAndView("ui/error");
        mav.setStatus(ex.getStatusCode() != null ? ex.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorTitle", "MOG API call failed");
        mav.addObject("errorMessage", ex.getMessage());
        mav.addObject("errorDetails", ex.getResponseBody());
        return mav;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleValidation(MethodArgumentNotValidException ex) {
        ModelAndView mav = new ModelAndView("ui/error");
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation failure while serving UI request: {}", message.isBlank() ? ex.getMessage() : message);
        mav.setStatus(HttpStatus.BAD_REQUEST);
        mav.addObject("errorTitle", "Validation error");
        mav.addObject("errorMessage", message.isBlank() ? "Check your input and try again" : message);
        mav.addObject("errorDetails", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNotFound(NoResourceFoundException ex) {
        log.debug("UI resource not found: {}", ex.getResourcePath());
        ModelAndView mav = new ModelAndView("ui/error");
        mav.setStatus(HttpStatus.NOT_FOUND);
        mav.addObject("errorTitle", "Page not found");
        mav.addObject("errorMessage", "The requested page or resource does not exist.");
        mav.addObject("errorDetails", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex) {
        log.error("Unhandled UI exception", ex);
        ModelAndView mav = new ModelAndView("ui/error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorTitle", "Something went wrong");
        mav.addObject("errorMessage", ex.getMessage());
        mav.addObject("errorDetails", ex.toString());
        return mav;
    }
}
