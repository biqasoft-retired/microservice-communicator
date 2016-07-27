/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.interfaceimpl.annotation.EnableMicroserviceCommunicator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@ComponentScan(value = "com.biqasoft")
@Configuration
@EnableAutoConfiguration
@EnableMicroserviceCommunicator
public class StartApplicationTest {

    public static void main(String[] args) {
        SpringApplication.run(StartApplicationTest.class, args);
    }
}
