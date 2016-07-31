package com.biqasoft.microservice.communicator.interfaceimpl;


import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceMapping;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroservicePathVariable;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceRequest;
import com.biqasoft.microservice.communicator.interfaceimpl.demo.UserAccount;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/18/2016
 *         All Rights Reserved
 */
@MicroserviceRequest(microservice = "test-microservice")
public interface MicroserviceUsersRepository {

    @MicroserviceMapping(path = "/domain/users/mock/one", method = HttpMethod.GET)
    UserAccount returnSingleObject();

    @MicroserviceMapping(path = "/domain/users/mock/null", method = HttpMethod.GET)
    UserAccount returnNullBodyResponse();

    @MicroserviceMapping(path = "/domain/users/mock/null", method = HttpMethod.GET)
    ResponseEntity<UserAccount> returnNonNullBodyResponse();

    @MicroserviceMapping(path = "/domain/users/mock", method = HttpMethod.GET)
    List<UserAccount> returnGenericList();

    @MicroserviceMapping(path = "/domain/users/mock/one", method = HttpMethod.GET)
    ResponseEntity<UserAccount> returnGenericResponseEntity();

    // russian special language
    // equals to findAllUsersInDomainMock() - GET /domain/users/mock
    @MicroserviceMapping
    List<UserAccount> построитьПолучитьМестоDomainГдеUsersГдеMock();

    // in tests url will be /domain/users/mock/one
    @MicroserviceMapping(path = "/domain/{s1}/{s2}/one", method = HttpMethod.GET)
    UserAccount returnSingleObjectWithPathParam(@MicroservicePathVariable(param = "s1") String s, @MicroservicePathVariable(param = "s2") String s2);

    @MicroserviceMapping(path = "/domain/users/mock/one", method = HttpMethod.GET)
    JsonNode returnJson();

    @MicroserviceMapping(path = "/domain/users/mock/one", method = HttpMethod.GET, convertResponseToMap = true)
    Map<String, Object> returnResponseAsJsonMap();

    @MicroserviceMapping(path = "/domain/users/mock/send_invalid_request", method = HttpMethod.GET)
    ResponseEntity<UserAccount> returnInvalidResponse();

    @MicroserviceMapping(path = "/domain/users/mock/send_invalid_request", method = HttpMethod.GET)
    UserAccount returnInvalidResponseException();

    @MicroserviceMapping(path = "/domain/users/mock/simulate_that_server_is_busy_and_can_not_process_current_request", method = HttpMethod.GET)
    UserAccount returnInvalidServerException();

    @MicroserviceMapping(path = "/domain/users/mock/simulate_that_server_is_busy_and_can_not_process_current_request", method = HttpMethod.GET)
    ResponseEntity<UserAccount> returnInvalidServerExceptionEntity();

}
