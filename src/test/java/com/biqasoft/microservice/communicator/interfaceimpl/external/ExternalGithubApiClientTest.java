package com.biqasoft.microservice.communicator.interfaceimpl.external;

import com.biqasoft.microservice.communicator.interfaceimpl.ExternalGithubApiClient;
import com.biqasoft.microservice.communicator.interfaceimpl.StartApplicationTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/25/2016
 *         All Rights Reserved
 */
@SpringBootTest(classes = StartApplicationTest.class)
@WebAppConfiguration
@Test(suiteName = "microserviceCommunicationInterface")
@ActiveProfiles({"development", "test"})
public class ExternalGithubApiClientTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private ExternalGithubApiClient externalGithubApiClient;

    private static final Logger logger = LoggerFactory.getLogger(ExternalGithubApiClientTest.class);

    @Test
    public void returnSingleObject() throws Exception {
        String githubUsername = "nbakaev";
        String githubUsernameId = "3147913";

        List<GithubRepo> repos = externalGithubApiClient.returnUserRepos(githubUsername);

        Assert.assertTrue(repos.size() > 0);
        Assert.assertEquals(repos.get(0).getOwner().getId(), githubUsernameId);
    }

}