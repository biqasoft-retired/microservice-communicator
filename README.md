# Microservice communicator

Auto generate HTTP REST classes for interfaces. Currently only for spring cloud

## Requirements
 - Spring 4
 - Spring Cloud
 - Consul

## Usage
 - Add `@EnableMicroserviceCommunicator` to any @Configuration class. Optionally set `basePackages` or `basePackages` from `@ComponentScan` will be used
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

    // in tests url will be /domain/users/mock/one
    @MicroserviceMapping(path = "/domain/{s1}/{s2}/one", method = HttpMethod.GET)
    UserAccount returnSingleObjectWithPathParam(@MicroservicePathVariable(param = "s1") String s, @MicroservicePathVariable(param = "s2") String s2);

}
```

 - Inject `MicroserviceUsersRepositoryTest` to any bean
```java
@Service
public class UsersRepository {

    @Autowired
    private MicroserviceUsersRepository microserviceUsersRepository;

    public void testReturnGenericList(){
        List<UserAccount> allUsers = microserviceUsersRepository.getAllUsers();
    }

}
```
 
### License
Copyright © 2016 Nikita Bakaev. Licensed under the Apache License.
