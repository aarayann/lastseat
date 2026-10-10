package com.example.bookingengine.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String what, Object id) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " " + id + " not found");
    }
}
