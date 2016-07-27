/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.exceptions;

/**
 * This is exception to just quick response user in API
 * that request is invalid.
 * Do not prints exception to stack trace
 */
@SuppressWarnings("serial")
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException() {
    }

    public InvalidRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidRequestException(String message) {
        super(message);
    }

}