/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl.annotation;


import java.lang.annotation.*;

/**
 * Enable {@link com.biqasoft.microservice.communicator.interfaceimpl.MicroserviceInterfaceImpFactory}
 *
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/17/2016
 *         All Rights Reserved
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface EnableMicroserviceCommunicator {

    String[] basePackages() default {""};

}
