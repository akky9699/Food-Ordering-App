package com.upgrad.FoodOrderingApp.api.exception;

import com.upgrad.FoodOrderingApp.api.model.ErrorResponse;
import com.upgrad.FoodOrderingApp.service.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import javax.xml.ws.Response;

/**
 * Description - ExceptionHandler for all the exceptions to be implemented.
 */

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(SignUpRestrictedException.class)
    public ResponseEntity<ErrorResponse> signUpRestrictedException(SignUpRestrictedException ex,
                                                                   WebRequest request) {
        return new ResponseEntity<ErrorResponse>(new ErrorResponse()
                .code(ex.getCode())
                .message(ex.getErrorMessage()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> authenticationFailedException(AuthenticationFailedException ex,
                                                                       WebRequest request) {
        return new ResponseEntity<ErrorResponse>(new ErrorResponse()
                .code(ex.getCode())
                .message(ex.getErrorMessage()),
                HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(AuthorizationFailedException.class)
    public ResponseEntity<ErrorResponse> authorizationFailedException(AuthorizationFailedException ex,
                                                                      WebRequest request){
        return new ResponseEntity<ErrorResponse>(new ErrorResponse()
                .code(ex.getCode())
                .message(ex.getErrorMessage()),
                HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler(UpdateCustomerException.class)
    public ResponseEntity<ErrorResponse> updateCustomerException(UpdateCustomerException ex,
                                                                 WebRequest request){
        return new ResponseEntity<ErrorResponse>(new ErrorResponse()
                .code(ex.getCode())
                .message(ex.getErrorMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

}
