/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.interfaceimpl.servicediscovery;

import com.ecwid.consul.v1.ConsulClient;
import com.netflix.config.ConfigurationManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@EnableDiscoveryClient
@Configuration
public class ServiceDiscoveryConfiguration {

    @Value("${spring.cloud.consul.host}")
    private String serverUrl;

    public
    @Bean
    ConsulClient consulClient() throws Exception {
        ConsulClient client = new ConsulClient( serverUrl );
        return client;
    }

    @PostConstruct
    public void init(){
        ConfigurationManager.getConfigInstance().setProperty("ribbon.ServerListRefreshInterval", 1000);
    }

}


