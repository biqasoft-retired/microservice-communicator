/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.http;

import com.biqasoft.microservice.communicator.servicediscovery.MicroserviceLoadBalancer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Class to store Spring context bean
 *
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/29/2016
 *         All Rights Reserved
 */
@Service
public class SpringContextAware {

    private static MicroserviceLoadBalancer microserviceLoadBalancer;

    public static MicroserviceLoadBalancer getMicroserviceLoadBalancer() {
        return microserviceLoadBalancer;
    }

    @Autowired
    public void setMicroserviceHelper(MicroserviceLoadBalancer microserviceLoadBalancer) {
        SpringContextAware.microserviceLoadBalancer = microserviceLoadBalancer;
    }
}
