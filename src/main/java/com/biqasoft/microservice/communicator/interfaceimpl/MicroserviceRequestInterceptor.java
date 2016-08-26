/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

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

   /**
    * Executed when we parse, deserialize response from server, executed after MicroserviceRequestInterceptor#afterRequest method
    *
    * @param modifiedObject object that we want to return
    * @param originalObject original(default) object from internal request processing
    * @param payload request payload
    * @param returnType return type in interface
    * @param httpMethod request http method
    * @param restTemplate request microserviceRestTemplate
    * @param returnGenericType return types generic info
    * @param params additional params
    * @return object that interface will return
    */
   default Object onBeforeReturnResult(Object modifiedObject, Object originalObject, Object payload, Class returnType, HttpMethod httpMethod,
                                       MicroserviceRestTemplate restTemplate, Class[] returnGenericType, Map<String, Object> params){return originalObject;}

}
