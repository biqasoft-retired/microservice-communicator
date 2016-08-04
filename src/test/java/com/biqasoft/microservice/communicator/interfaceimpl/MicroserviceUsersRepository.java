package com.biqasoft.microservice.communicator.interfaceimpl;


import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceMapping;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroservicePathVariable;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroservicePayloadVariable;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceRequest;
import com.biqasoft.microservice.communicator.interfaceimpl.demo.UserAccount;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/18/2016
 *         All Rights Reserved
 */
@MicroserviceRequest(microservice = "test-microservice") // test-microservice is id in service discovery
public interface MicroserviceUsersRepository {

    @MicroserviceMapping(path = "/domain/users/mock/one", method = HttpMethod.GET)// HTTP GET - default, you can leave it
    UserAccount returnSingleObject();

    @MicroserviceMapping(path = "/domain/users/mock/null")
    UserAccount returnNullBodyResponse();

    @MicroserviceMapping(path = "/domain/users/mock/null")
    ResponseEntity<UserAccount> returnNonNullBodyResponse();

    @MicroserviceMapping(path = "/domain/users/mock")
    List<UserAccount> returnGenericList();

    @MicroserviceMapping(path = "/domain/users/mock/one")
    ResponseEntity<UserAccount> returnGenericResponseEntity();

    // russian special language
    // equals to findAllUsersInDomainMock() - GET /domain/users/mock
    @MicroserviceMapping
    List<UserAccount> построитьПолучитьМестоDomainГдеUsersГдеMock();

    // in tests url will be /domain/users/mock/one
    @MicroserviceMapping(path = "/domain/{s1}/{s2}/one")
    UserAccount returnSingleObjectWithPathParam(@MicroservicePathVariable(param = "s1") String s,
                                                @MicroservicePathVariable(param = "s2") String s2);

    @MicroserviceMapping(path = "/domain/users/mock/one")
    JsonNode returnJson();

    @MicroserviceMapping(path = "/domain/users/mock/one", convertResponseToMap = true)
    Map<String, Object> returnResponseAsJsonMap();

    @MicroserviceMapping(path = "/domain/users/mock/send_invalid_request")
    ResponseEntity<UserAccount> returnInvalidResponse();

    @MicroserviceMapping(path = "/domain/users/mock/send_invalid_request")
    UserAccount returnInvalidResponseException();

    // will be 3 attempts (by default) to try again with interval (default 1100ms)
    @MicroserviceMapping(path = "/domain/users/mock/simulate_that_server_is_busy_and_can_not_process_current_request")
    UserAccount returnInvalidServerException();

    @MicroserviceMapping(path = "/domain/users/mock/simulate_that_server_is_busy_and_can_not_process_current_request")
    ResponseEntity<UserAccount> returnInvalidServerExceptionEntity();

    @MicroserviceMapping(path = "/domain/users/mock/authenticate", method = HttpMethod.POST, mergePayloadToObject = true)
    UserAccount returnAuthenticatedUser(@MicroservicePayloadVariable(path = "username") String username,
                                        @MicroservicePayloadVariable(path = "password") String password);

    // will be POST json
    // {
    //    "username": %username%,
    //    "password": %password%,
    //    "address" : {
    //                   "country": %addressCountry%,
    //                   "city": %city%
    //                }
    // }
    // %username% etc... will be replaced by java function param
    @MicroserviceMapping(path = "/domain/users/mock/echo", method = HttpMethod.POST, mergePayloadToObject = true)
    UserAccount returnAuthenticatedUserComplexEcho(@MicroservicePayloadVariable(path = "username") String username,
                                                   @MicroservicePayloadVariable(path = "password") String password,
                                                   @MicroservicePayloadVariable(path = "address.country") String addressCountry,
                                                   @MicroservicePayloadVariable(path = "address.city") String city);

    @MicroserviceMapping(path = "/domain/users/mock/one")
    CompletableFuture<UserAccount> returnCompletableFutureSingleObject();

    @MicroserviceMapping(path = "/domain/users/mock")
    CompletableFuture<List<UserAccount>> returnListCompletableFutureObjects();

}
