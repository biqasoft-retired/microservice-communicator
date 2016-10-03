/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.http;

import org.springframework.http.HttpMethod;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;

/**
 * Created by Nikita Bakaev, ya@nbakaev.ru on 5/24/2016.
 * All Rights Reserved
 */
public class HttpClientsHelpers {

    /**
     * @param tryToReconnect         true if try to retry failed request to microsrevice
     * @param tryToReconnectTimes    number of times to try to reconnect
     * @param microserviceName microservice name
     * @param pathToApiResource pathToApiResource
     * @param httpMethod http method
     * @param sleepTimeBetweenTrying sleep in millias to try to reconnect between failed requests
     * @param httpMethod    httpMethod
     * @param https    use http or https
     *
     * RestTemplate client can be not thread safe (depend on class implementation)
     * So, in secured to create a new instance to control result of each request
     *
     * @return new default spring HTTP restTemplate
     * @throws URISyntaxException exception
     */
    public static MicroserviceRestTemplate getRestTemplate(Boolean tryToReconnect, int tryToReconnectTimes, int sleepTimeBetweenTrying, String microserviceName, String pathToApiResource,
                                                           HttpMethod httpMethod, boolean https) throws URISyntaxException {
        return new MicroserviceRestTemplate(tryToReconnect, tryToReconnectTimes, sleepTimeBetweenTrying, microserviceName, pathToApiResource, httpMethod, https);
    }

    public static RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        return restTemplate;
    }

}

