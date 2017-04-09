package com.biqasoft.microservice.communicator.interfaceimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/25/2016
 *         All Rights Reserved
 */
@SpringBootTest(classes = StartApplicationTest.class)
@WebAppConfiguration
@Test(suiteName = "microserviceCommunicationInterface")
@ActiveProfiles({"development", "test"})
public class ExternalResourceYahooTestTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private YahooCurrencyExchange yahooCurrencyExchange;

    private static final Logger logger = LoggerFactory.getLogger(ExternalResourceYahooTestTest.class);

    @Test
    public void testReturnExpression() throws Exception {
        double v = yahooCurrencyExchange.getExchangeRate("USD", "RUB").get("query").get("results").get("row").get("col1").asDouble();
        Double exchangeRateAsDouble = yahooCurrencyExchange.getExchangeRateAsDouble("USD", "RUB");
        Assert.assertEquals(v, exchangeRateAsDouble);
    }


}