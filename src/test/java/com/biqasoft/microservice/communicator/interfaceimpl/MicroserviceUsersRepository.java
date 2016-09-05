package com.biqasoft.microservice.communicator.interfaceimpl;


import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroMapping;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroPathVar;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroPayloadVar;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.Microservice;
import com.biqasoft.microservice.communicator.interfaceimpl.demo.UserAccount;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/18/2016
 *         All Rights Reserved
 */
@Microservice("test-microservice") // test-microservice is id in service discovery
public interface MicroserviceUsersRepository {

    // HTTP GET - default, you can leave it
    @MicroMapping(path = "/domain/users/mock/one", method = HttpMethod.GET)
    UserAccount returnSingleObject();

    @MicroMapping("/domain/users/mock/null")
    UserAccount returnNullBodyResponse();

    @MicroMapping("/domain/users/mock/null")
    ResponseEntity<UserAccount> returnNonNullBodyResponse();

    @MicroMapping("/domain/users/mock")
    List<UserAccount> returnGenericList();

    @MicroMapping("/domain/users/mock/one")
    ResponseEntity<UserAccount> returnGenericResponseEntity();

    // russian special language
    // equals to findAllUsersInDomainMock() - GET /domain/users/mock
    @MicroMapping
    List<UserAccount> построитьПолучитьМестоDomainГдеUsersГдеMock();

    // in tests url will be /domain/users/mock/one
    @MicroMapping("/domain/{s1}/{s2}/one")
    UserAccount returnSingleObjectWithPathParam(@MicroPathVar("s1") String s,
                                                @MicroPathVar("s2") String s2);

    @MicroMapping("/domain/users/mock/one")
    JsonNode returnJson();

    @MicroMapping(path = "/domain/users/mock/one", convertResponseToMap = true)
    Map<String, Object> returnResponseAsJsonMap();

    @MicroMapping("/domain/users/mock/send_invalid_request")
    ResponseEntity<UserAccount> returnInvalidResponse();

    @MicroMapping("/domain/users/mock/send_invalid_request")
    UserAccount returnInvalidResponseException();

    // this endpoint simulate server error
    // will be 3 attempts (by default) to try again with interval (default 1100ms)
    @MicroMapping("/domain/users/mock/generate_500_http_error")
    UserAccount returnInvalidServerException();

    @MicroMapping("/domain/users/mock/generate_500_http_error")
    ResponseEntity<UserAccount> returnInvalidServerExceptionEntity();

    @MicroMapping(path = "/domain/users/mock/authenticate", method = HttpMethod.POST)
    UserAccount returnAuthenticatedUser(@MicroPayloadVar("username") String username,
                                        @MicroPayloadVar("password") String password);

    // will be POST json
    // {
    //    "username": %username%,
    //    "password": %password%,
    //    "address" : {
    //                   "country": %country%,
    //                   "city": %city%
    //                }
    // }
    // %username% etc... will be replaced by java function param
    @MicroMapping(path = "/domain/users/mock/echo", method = HttpMethod.POST)
    UserAccount returnAuthenticatedUserComplexEcho(@MicroPayloadVar("username") String username,
                                                   @MicroPayloadVar("password") String password,
                                                   @MicroPayloadVar("address.country") String country,
                                                   @MicroPayloadVar("address.city") String city);

    // use java 8 with javac -parameters to add parameter name and use as json node name
    // instead of annotation value with dot(.) delimiter - in parameter name - is decimeter
    @MicroMapping(path = "/domain/users/mock/echo", method = HttpMethod.POST)
    UserAccount returnPayloadFromName(@MicroPayloadVar String username,
                                      @MicroPayloadVar String password,
                                      @MicroPayloadVar String address_country,
                                      @MicroPayloadVar String address_city);

    @MicroMapping("/domain/users/mock/one")
    CompletableFuture<UserAccount> returnCompletableFutureSingleObject();

    @MicroMapping("/domain/users/mock")
    CompletableFuture<List<UserAccount>> returnListCompletableFutureObjects();

    @MicroMapping("/domain/users/mock/one")
    Optional<UserAccount> returnSingleOptionalObject();

    @MicroMapping("/domain/users/mock")
    Optional<List<UserAccount>> returnListOptionalObject();

    @MicroMapping("/domain/users/mock/null")
    Optional<UserAccount> returnSingleOptionalEmptyObject();

    // default will be executed on error main request
    @MicroMapping("/domain/users/mock/generate_500_http_error")
    default UserAccount returnDefaultValue() {
        return new UserAccount("I'm default Java 8 interface");
    }

    // default will be executed on error main request
    @MicroMapping("/domain/users/mock/generate_500_http_error")
    default UserAccount returnDefaultValue(String name) {
        return new UserAccount(name);
    }

}
