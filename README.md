# Spring cloud microservice communicator

## Usage
 - Add `@EnableMicroserviceCommunicator` to anu configuration class. Optionally set `basePackages` or `basePackages` from `@ComponentScan` will be used
 - Create interface, for example
 
```java
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
```

 - Inject `MicroserviceUsersRepositoryTest` to any bean
  
### License
Copyright © 2016 Nikita Bakaev. Licensed under the Apache License.
