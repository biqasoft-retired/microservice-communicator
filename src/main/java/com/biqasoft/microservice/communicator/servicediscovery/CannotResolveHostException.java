/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.servicediscovery;

/**
 * Created by Nikita Bakaev, ya@nbakaev.ru on 5/12/2016.
 * All Rights Reserved
 */
public class CannotResolveHostException extends RuntimeException {

    public CannotResolveHostException() {
    }

    public CannotResolveHostException(String message) {
        super(message);
    }

    public CannotResolveHostException(String message, Throwable cause) {
        super(message, cause);
    }

    public CannotResolveHostException(Throwable cause) {
        super(cause);
    }

    public CannotResolveHostException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
