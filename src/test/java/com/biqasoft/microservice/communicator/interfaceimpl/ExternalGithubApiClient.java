package com.biqasoft.microservice.communicator.interfaceimpl;


import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroMapping;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroPathVar;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.Microservice;
import com.biqasoft.microservice.communicator.interfaceimpl.external.GithubRepo;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/18/2016
 *         All Rights Reserved
 */
@Microservice("https://api.github.com/")
public interface ExternalGithubApiClient {

    @MicroMapping(path = "users/{user}/repos", method = HttpMethod.GET)
    List<GithubRepo> returnUserRepos(@MicroPathVar("user") String username);

}
