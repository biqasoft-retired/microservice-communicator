package com.biqasoft.microservice.communicator.interfaceimpl;


import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceMapping;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceRequest;
import com.biqasoft.microservice.communicator.interfaceimpl.demo.UserAccount;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/18/2016
 *         All Rights Reserved
 */
@MicroserviceRequest(microservice = "users")
public interface MicroserviceUsersRepositoryTest {

    @MicroserviceMapping(path = "/domain/users/mock/one", method = HttpMethod.GET)
    UserAccount returnSingleObject();

    @MicroserviceMapping(path = "/domain/users/mock", method = HttpMethod.GET)
    List<UserAccount> returnGenericList();

    @MicroserviceMapping(path = "/domain/users/mock/one", method = HttpMethod.GET)
    ResponseEntity<UserAccount> returnGenericResponseEntity();

    // russian special language
    // equals to findAllUsersInDomainMock() - GET /domain/users/mock
    @MicroserviceMapping
    List<UserAccount> построитьПолучитьМестоDomainГдеUsersГдеMock();

}
