/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl.annotation;

import com.biqasoft.microservice.communicator.http.HttpClientsHelpers;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Add to annotation to interface method
 * <p>
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
public @interface MicroMapping {

    @AliasFor("path")
    String value() default "/";


    @AliasFor("value")
    String path() default "/";

    HttpMethod method() default HttpMethod.GET;

    /**
     * see {@link com.biqasoft.microservice.communicator.http.HttpClientsHelpers#getRestTemplate(Boolean, int, int, String, String, HttpMethod)} (Boolean, int, int, String, String)}
     *
     * @return true if try to reconnect to service on error
     */
    boolean tryToReconnect() default true;

    /**
     * see {@link com.biqasoft.microservice.communicator.http.HttpClientsHelpers#getRestTemplate(Boolean, int, int, String, String, HttpMethod)} (Boolean, int, int, String, String)}
     *
     * @return number of tries to reconnect
     */
    int tryToReconnectTimes() default 2;

    /**
     * see {@link HttpClientsHelpers#getRestTemplate(Boolean, int, int, String, String, HttpMethod)} ()} (Boolean, int, int, String, String)}
     * @return millisecond between trying
     */
    int sleepTimeBetweenTrying() default 1100;

    boolean convertResponseToMap() default false;

//    MediaType contentType() default MediaType.APPLICATION_JSON;
//    String[] produces() default {};
//    String[] consumes() default {};

}
