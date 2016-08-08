/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Implement spring bean to intercept all microservice requests
 *
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/26/2016
 *         All Rights Reserved
 */
public interface MicroserviceRequestInterceptor {

   // you can modify http request headers here
   default void beforeCreateHttpEntity(String microserviceName, String microservicePath, HttpMethod httpMethod, Class returnType, Class[] returnGenericType, HttpHeaders httpHeaders){};
   default void beforeRequest(String microserviceName, String microservicePath, HttpMethod httpMethod, HttpEntity<Object> request, Class returnType, Class[] returnGenericType){};
   default void afterRequest(String microserviceName, String microservicePath, HttpMethod httpMethod, HttpEntity<Object> request, ResponseEntity<byte[]> responseEntity, Class returnType, Class[] returnGenericType){};

}
