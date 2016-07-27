/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.exceptions;

@SuppressWarnings("serial")
public class InternalSeverErrorProcessingRequestException extends RuntimeException {

    public InternalSeverErrorProcessingRequestException(String message) {
        super(message);
    }

}