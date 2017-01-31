/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Add to annotation to interface
 *
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/15/2016
 *         All Rights Reserved
 */
@Retention(RetentionPolicy.RUNTIME)
@Component
@Inherited
@Target({ElementType.PARAMETER})
public @interface MicroPathVar {

    @AliasFor("param")
    String value() default "";

    @AliasFor("value")
    String param() default "";

    boolean encode() default true;

}
