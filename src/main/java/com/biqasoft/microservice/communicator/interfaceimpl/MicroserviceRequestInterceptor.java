/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.net.URI;
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
   default void beforeCreateHttpEntity(MicroserviceRestTemplate restTemplate, Class returnType, Class[] returnGenericType, HttpHeaders httpHeaders){};
   default void beforeRequest(MicroserviceRestTemplate restTemplate, URI uri){};
   default void afterRequest(MicroserviceRestTemplate restTemplate, HttpEntity<Object> request, ResponseEntity<byte[]> responseEntity, Class returnType, Class[] returnGenericType){};

   // before execution async request. executed yet in same thread as main request
   default void beforeProcessRequest(MicroserviceRestTemplate restTemplate, HttpHeaders httpHeaders){};


   default void onException(MicroserviceRestTemplate restTemplate, URI uri, Exception e){};

   /**
    * Executed when we parse, deserialize response from server, executed after MicroserviceRequestInterceptor#afterRequest method
    *
    * @param modifiedObject object that we want to return
    * @param originalObject original(default) object from internal request processing
    * @param payload request payload
    * @param returnType return type in interface
    * @param restTemplate request microserviceRestTemplate
    * @param returnGenericType return types generic info
    * @param params additional params
    * @return object that interface will return
    */
   default Object onBeforeReturnResult(Object modifiedObject, Object originalObject, Object payload, Class returnType,
                                       MicroserviceRestTemplate restTemplate, Class[] returnGenericType, Map<String, Object> params){return originalObject;}

}
