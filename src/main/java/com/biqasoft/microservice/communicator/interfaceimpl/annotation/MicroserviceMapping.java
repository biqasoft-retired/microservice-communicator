/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl.annotation;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Add to annotation to interface method
 *
 * Payload for PUT and POST must be first argument
 *
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/15/2016
 *         All Rights Reserved
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Component
@Inherited
public @interface MicroserviceMapping {

    String path() default "/";
    HttpMethod method() default HttpMethod.GET;

    /**
     * see {@link com.biqasoft.microservice.communicator.http.HttpClientsHelpers#getRestTemplate(Boolean, int, int)}
     * @return
     */
    boolean tryToReconnect() default true;

    /**
     * see {@link com.biqasoft.microservice.communicator.http.HttpClientsHelpers#getRestTemplate(Boolean, int, int)}
     * @return
     */
    int tryToReconnectTimes() default 11;

    /**
     * see {@link com.biqasoft.microservice.communicator.http.HttpClientsHelpers#getRestTemplate(Boolean, int, int)}
     * @return
     */
    int sleepTimeBetweenTrying() default 1000;

//    MediaType contentType() default MediaType.APPLICATION_JSON;
//    String[] produces() default {};
//    String[] consumes() default {};

}
