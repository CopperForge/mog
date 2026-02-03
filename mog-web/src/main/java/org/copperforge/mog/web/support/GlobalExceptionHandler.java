package org.copperforge.mog.web.support;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MogApiClientException.class)
    public ModelAndView handleMogApi(MogApiClientException ex) {
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
        mav.setStatus(HttpStatus.BAD_REQUEST);
        mav.addObject("errorTitle", "Validation error");
        mav.addObject("errorMessage", message.isBlank() ? "Check your input and try again" : message);
        mav.addObject("errorDetails", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex) {
        ModelAndView mav = new ModelAndView("ui/error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorTitle", "Something went wrong");
        mav.addObject("errorMessage", ex.getMessage());
        mav.addObject("errorDetails", ex.toString());
        return mav;
    }
}
