package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.interfaceimpl.demo.UserAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.ReflectionUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.List;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/25/2016
 *         All Rights Reserved
 */
@SpringApplicationConfiguration(classes = StartApplicationTest.class)
@WebAppConfiguration
@Test(suiteName = "microserviceCommunicationInterface")
@ActiveProfiles({"development", "test"})
public class MicroserviceUsersRepositoryTest extends AbstractTestNGSpringContextTests {

    static {
        System.setProperty("spring.cloud.consul.host", "192.168.127.131");
    }

    @Autowired
    private MicroserviceUsersRepository microserviceUsersRepository;

    @Autowired
    private MicroserviceInterfaceImpFactory microserviceInterfaceImpFactory;

    @Test
    public void initSpringContextPure() throws Exception {
    }

    @Test(enabled = true, invocationCount = 2)
    public void testReturnGenericList() throws Exception {
        List<UserAccount> allUsersInDomain = microserviceUsersRepository.returnGenericList();
        Assert.assertTrue(allUsersInDomain.size() > 2);
        Assert.assertNotNull(allUsersInDomain.get(1).getId());
    }

    @Test(enabled = true, invocationCount = 2)
    public void testПостроитьПолучитьМестоDomainГдеUsersГдеMock() throws Exception {
        List<UserAccount> allUsersInDomain = microserviceUsersRepository.построитьПолучитьМестоDomainГдеUsersГдеMock();
        Assert.assertTrue(allUsersInDomain.size() > 2);
        Assert.assertNotNull(allUsersInDomain.get(1).getId());
    }

    @Test(enabled = true, invocationCount = 2)
    public void testReturnGenericResponseEntity() throws Exception {
        ResponseEntity<UserAccount> allUsersInDomainMockOneResponseEntity = microserviceUsersRepository.returnGenericResponseEntity();
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity);
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity.getBody());
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity.getBody().getId());
    }

    @Test(enabled = true, invocationCount = 2)
    public void testAfterRequestInterceptor() throws Exception {
        UserAccount userAccount = new UserAccount();
        userAccount.setId("Secret mocked id");

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] bytes = objectMapper.writeValueAsBytes(userAccount);

        MicroserviceRequestInterceptor microserviceRequestInterceptor = new MicroserviceRequestInterceptor() {
            @Override
            public void afterRequest(URI storesUri, HttpMethod httpMethod, HttpEntity<Object> request, ResponseEntity<byte[]> responseEntity, Class returnType, Class returnGenericType) {
                ReflectionUtils.setField(MicroserviceInterfaceImpFactory.body, responseEntity, bytes);
            }
        };

        int beforeInterceptors = microserviceInterfaceImpFactory.getMicroserviceRequestInterceptors().size();
        microserviceInterfaceImpFactory.getMicroserviceRequestInterceptors().add(microserviceRequestInterceptor);

        UserAccount account = microserviceUsersRepository.returnSingleObject();
        Assert.assertNotNull(account);
        Assert.assertNotNull(account.getId());
        Assert.assertEquals(account.getId(), userAccount.getId());

        microserviceInterfaceImpFactory.getMicroserviceRequestInterceptors().remove(microserviceRequestInterceptor);
        Assert.assertEquals(microserviceInterfaceImpFactory.getMicroserviceRequestInterceptors().size(), beforeInterceptors, "Not deleted after request interceptor");
    }

}