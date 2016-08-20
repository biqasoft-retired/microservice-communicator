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
@Microservice("test-microservice") // test-microservice is id in service discovery
public interface MicroserviceUsersRepository {

    @MicroMapping(path = "/domain/users/mock/one", method = HttpMethod.GET)// HTTP GET - default, you can leave it
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
    default UserAccount returnDefaultValue(){ return new UserAccount("I'm default return Java 8 interface value"); }

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

 - `InvalidRequestException` this exceptions is throws if remote host set HTTP status 422 (Unprocessable Entity), 401, 403.
When we recieve such response, we immedialty throw exception, without trying new requests to another microservices
 - `InternalSeverErrorProcessingRequestException` if we have non 422, 401, 403 response code, can not find suitable microservice URL in service discovery predetermined number of time,
or can not retry request to microservices predetermined number of times(on error)

## Java 9
Spring 4.3 requires to add custom VM option to run `-addmods java.xml.bind`,
optionally you can use `-ea -XaddExports:java.base/jdk.internal.loader=ALL-UNNAMED  -addmods java.xml.bind`

### License
Copyright © 2016 [Nikita Bakaev](http://nbakaev.ru). Licensed under the Apache License.
