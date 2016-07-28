# Microservice communicator

Auto generate HTTP REST classes for interfaces.

## Status
[![Maven Central Widget]][Maven Central]  [![Download Bintray Widget]][Download Bintray] [![Javadoc Widget]][Javadoc]

[Maven Central]: https://mvnrepository.com/artifact/com.biqasoft/microservice-communicator
[Maven Central Widget]: https://img.shields.io/maven-central/v/com.biqasoft/microservice-communicator.svg
[Download Bintray]: https://bintray.com/biqasoft/maven/microservice-communicator/_latestVersion
[Download Bintray Widget]: https://api.bintray.com/packages/biqasoft/maven/microservice-communicator/images/download.svg
[Javadoc]: http://www.javadoc.io/doc/com.biqasoft/microservice-communicator
[Javadoc Widget]: https://javadoc-emblem.rhcloud.com/doc/com.biqasoft/microservice-communicator/badge.svg


## Requirements
 - Spring 4
 - [Spring Cloud](http://projects.spring.io/spring-cloud/)

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
    UserAccount returnSingleObjectWithPathParam(@MicroservicePathVariable(param = "s1") String s,
                                                @MicroservicePathVariable(param = "s2") String s2);

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
  <version>1.1.1-RELEASE</version>
</dependency>
```
 
## How works

Internally, library use spring bean `LoadBalancerClient` with default implementation of spring cloud `RibbonLoadBalancerClient`. So, you can use Consul, Zookeeper, Cloudfoundry.
 
if you have not configured, you can do it easily, for example for consul create configuration bean 

```java
@EnableDiscoveryClient
@Configuration
public class ServiceDiscoveryConfiguration {

    @Value("${spring.cloud.consul.host}")
    private String serverUrl;

    public
    @Bean
    ConsulClient consulClient() throws Exception {
        ConsulClient client = new ConsulClient( serverUrl );
        return client;
    }

}
```

## Demo
 - [microservice-communicator-demo-server](https://github.com/biqasoft/microservice-communicator-demo-server)
 
### License
Copyright © 2016 Nikita Bakaev. Licensed under the Apache License.
