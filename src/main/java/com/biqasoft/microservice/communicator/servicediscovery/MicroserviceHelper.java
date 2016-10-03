/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.servicediscovery;

import com.biqasoft.microservice.communicator.exceptions.CannotResolveHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;

import java.net.URI;

/**
 * Created by Nikita Bakaev, ya@nbakaev.ru on 5/12/2016.
 * All Rights Reserved
 */
@Service
public class MicroserviceHelper {

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    private static final Logger logger = LoggerFactory.getLogger(MicroserviceHelper.class);

    private final int FAIL_AFTER_UNSUCCESS_TIMES = 4;
    private final int DEFAULT_SLEEP_TIME_BETWEEN_TRYING = 1200;

    /**
     * @param microserviceName  registered service name. For example gateway
     * @param pathToApiResource URl path such as /users/all
     * @param sleepMilliseconds      sleep time if we can not resolve hostname of microservice
     * @param tryToReconnect    if we can not get hostname of microservice - fail immediately or sleep and try to get
     * @param https    use http or https
     * @return URL to which make request
     * @throws CannotResolveHostException if can not get microservice name for microserviceName in service discovery
     */
    public URI getLoadBalancedURIByMicroservice(String microserviceName, String pathToApiResource, Integer sleepMilliseconds, Boolean tryToReconnect, boolean https) {
        ServiceInstance instance = null;

        boolean exitLoop = false;
        int triedTimes = 0;
        int sleepTimeBetweenTrying = sleepMilliseconds == null ? DEFAULT_SLEEP_TIME_BETWEEN_TRYING : sleepMilliseconds;

        while (!exitLoop) {
            instance = loadBalancerClient.choose(microserviceName);

            if (instance != null) {
                exitLoop = true;
            } else {

                if (tryToReconnect == null || !tryToReconnect) {
                    exitLoop = true;
                }

                if (triedTimes > FAIL_AFTER_UNSUCCESS_TIMES) {
                    throw new CannotResolveHostException("Can not resolve hostname for microservice name: " + microserviceName);
                }

                triedTimes++;

                try {
                    Thread.sleep(sleepTimeBetweenTrying);
                    logger.info("Can not resolve hostname for microservice name: " + microserviceName + " and times: " + triedTimes);

                } catch (InterruptedException e) {
                    exitLoop = true;
                    e.printStackTrace();
                }
                continue;
            }

        }

        String prefix;
        if (https){
            prefix = "https://%s:%s";
        }else{
            prefix = "http://%s:%s";
        }

        return URI.create(String.format(prefix, instance.getHost(), instance.getPort()) + pathToApiResource);
    }

}
