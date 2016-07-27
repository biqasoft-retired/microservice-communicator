/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl.configs;

import java.util.UUID;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/20/2016
 *         All Rights Reserved
 */
public class LoggerConfigHelper {

    public final static String REQUEST_ID_LOGGER = "RequestId";

    // Collision ??? Use mongodb object id
    public static String generateRequestId(){
        return UUID.randomUUID().toString();
    }

}
