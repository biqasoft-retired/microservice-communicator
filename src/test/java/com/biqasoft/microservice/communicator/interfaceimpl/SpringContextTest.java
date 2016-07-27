package com.biqasoft.microservice.communicator.interfaceimpl;

import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testng.annotations.Test;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/25/2016
 *         All Rights Reserved
 */
@SpringApplicationConfiguration(classes = StartApplicationTest.class)
@WebAppConfiguration
@Test(suiteName = "microserviceCommunicationInterface")
@ActiveProfiles({"development", "test"})
public class SpringContextTest extends AbstractTestNGSpringContextTests {

    static {
        System.setProperty("spring.cloud.consul.host", "192.168.127.131");
    }

    @Test
    public void initSpringContextPure() throws Exception {
    }

}