/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.exceptions;

import org.springframework.http.client.ClientHttpResponse;

/**
 * This is exception to just quick response user in API
 * that request is invalid.
 * Do not prints exception to stack trace
 */
@SuppressWarnings("serial")
public class InvalidRequestLocalizedException extends RuntimeException {

    private ClientHttpResponse clientHttpResponse = null;

    public InvalidRequestLocalizedException(ClientHttpResponse clientHttpResponse) {
        this.clientHttpResponse = clientHttpResponse;
    }


    public InvalidRequestLocalizedException() {
    }

    public InvalidRequestLocalizedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidRequestLocalizedException(String message) {
        super(message);
    }


    public ClientHttpResponse getClientHttpResponse() {
        return clientHttpResponse;
    }

    public void setClientHttpResponse(ClientHttpResponse clientHttpResponse) {
        this.clientHttpResponse = clientHttpResponse;
    }
}