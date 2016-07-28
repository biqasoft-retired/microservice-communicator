/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Add annotation to interface
 *
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/15/2016
 *         All Rights Reserved
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
@Inherited
public @interface MicroserviceRequest {

    String microservice() default "";

}
