# Microservice communicator

Auto generate HTTP REST classes for interfaces.

## Status
[![Travis Widget]][Travis] [![Coverage Status Widget]][Coverage Status] [![Maven Central Widget]][Maven Central]  [![Download Bintray Widget]][Download Bintray] [![Javadoc Widget]][Javadoc]

[Travis]: https://travis-ci.org/biqasoft/microservice-communicator
[Travis Widget]: https://travis-ci.org/biqasoft/microservice-communicator.svg?branch=master
[Coverage Status]: https://codecov.io/github/biqasoft/microservice-communicator?branch=master
[Coverage Status Widget]: https://img.shields.io/codecov/c/github/biqasoft/microservice-communicator/master.svg
[Maven Central]: https://mvnrepository.com/artifact/com.biqasoft/microservice-communicator
[Maven Central Widget]: https://img.shields.io/maven-central/v/com.biqasoft/microservice-communicator.svg
[Download Bintray]: https://bintray.com/biqasoft/maven/microservice-communicator/_latestVersion
[Download Bintray Widget]: https://api.bintray.com/packages/biqasoft/maven/microservice-communicator/images/download.svg
[Javadoc]: http://www.javadoc.io/doc/com.biqasoft/microservice-communicator
[Javadoc Widget]: https://javadoc-emblem.rhcloud.com/doc/com.biqasoft/microservice-communicator/badge.svg


## Requirements
 - Java 8
 - Spring 4
 - [Spring Cloud](http://projects.spring.io/spring-cloud/)

## Usage
 - Add `@EnableMicroserviceCommunicator` to any @Configuration class. Optionally set `basePackages` or `basePackages` from `@ComponentScan` will be used
 - Create interface, for example
 
```java
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
    
    @MicroserviceMapping(path = "/domain/users/mock/null")
    Optional<UserAccount> returnSingleOptionalEmptyObject();
    
}
```

 - Inject `MicroserviceUsersRepositoryTest` to any bean
```java
@Service
public class UsersRepository {

    @Autowired
    private MicroserviceUsersRepository microserviceUsersRepository;

    public void testReturnGenericList(){
        List<UserAccount> allUsers = microserviceUsersRepository.returnGenericList();
    }

}
```

## Maven dependency

To use this extension on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.biqasoft</groupId>
  <artifactId>microservice-communicator</artifactId>
  <version>1.2.10-RELEASE</version>
</dependency>
```
 
## Return type from interface can be:
 - [CompletableFuture<>](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) - async execute request
 - Any your Data object (DTO), will be deserialize with Jackson; supported return `List<SomeClass>`
 - [ResponseEntity<>](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/http/ResponseEntity.html) - spring MVC object, with headers, response code, response body
 - [JsonNode](https://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/JsonNode.html) - if you do not want to map response to some object
 - [Optional<>](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html)
 
## How it works

Internally, library use spring bean `LoadBalancerClient` with default implementation of spring cloud `RibbonLoadBalancerClient`. So, you can use Consul, Zookeeper, Cloudfoundry.
 
if you have not configured service discovery in Spring Cloud, you can do it easily, for example for consul create configuration bean 

```java
@EnableDiscoveryClient
@Configuration
public class ServiceDiscoveryConfiguration {

    @Value("${spring.cloud.consul.host}")
    private String serverUrl;

    @Bean
    public ConsulClient consulClient() throws Exception {
        ConsulClient client = new ConsulClient( serverUrl );
        return client;
    }

}
```

## Demo
 - [demo server, used for tests](https://github.com/biqasoft/microservice-communicator-demo-server)
 - [MicroserviceUsersRepositoryTest](https://github.com/biqasoft/microservice-communicator/blob/master/src/test/java/com/biqasoft/microservice/communicator/interfaceimpl/MicroserviceUsersRepositoryTest.java) - test interface usage

## Exceptions
If you have return type `ResponseEntity` you will never have exceptions from method. For example with `responseEntity.getStatusCode().is2xxSuccessful()`.
Also, you have access to headers and body with `responseEntity.getHeaders()` and `responseEntity.getBody()`

If you have return type some object (not ResponseEntity) following unchecked exceptions can be thrown:

 - `InvalidRequestException` this exceptions is throws if remote host set HTTP status 422 (Unprocessable Entity).
When we recieve such response, we immedialty throw exception, without trying new requests to another microservices
 - `InternalSeverErrorProcessingRequestException` if we have non 422 response code, can not find suitable microservice URL in service discovery predetermined number of time,
or can not retry request to microservices predetermined number of times(on error)

### License
Copyright © 2016 [Nikita Bakaev](http://nbakaev.ru). Licensed under the Apache License.
