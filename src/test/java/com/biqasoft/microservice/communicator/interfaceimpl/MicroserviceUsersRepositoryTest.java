package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.exceptions.InternalSeverErrorProcessingRequestException;
import com.biqasoft.microservice.communicator.exceptions.InvalidRequestException;
import com.biqasoft.microservice.communicator.interfaceimpl.demo.UserAccount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/25/2016
 *         All Rights Reserved
 */
@SpringBootTest(classes = StartApplicationTest.class)
@WebAppConfiguration
@Test(suiteName = "microserviceCommunicationInterface")
@ActiveProfiles({"development", "test"})
public class MicroserviceUsersRepositoryTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private MicroserviceUsersRepository microserviceUsersRepository;

    @Autowired
    private MicroserviceInterfaceImpFactory microserviceInterfaceImpFactory;

    @Test(enabled = true, invocationCount = 1)
    public void testReturnGenericList() throws Exception {
        List<UserAccount> allUsersInDomain = microserviceUsersRepository.returnGenericList();
        Assert.assertTrue(allUsersInDomain.size() > 2);
        Assert.assertNotNull(allUsersInDomain.get(1).getId());
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnSingleObjectWithPathParam() throws Exception {
        UserAccount account = microserviceUsersRepository.returnSingleObjectWithPathParam("users", "mock");
        Assert.assertNotNull(account);
        Assert.assertNotNull(account.getId());
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnCompletableFutureSingleObject() throws Exception {
        final boolean[] asyncExecutionDetect = {false, false}; // [0] -> completeFuture, [1] -> execute statement after future request(async)

        CompletableFuture<UserAccount> completableFuture = microserviceUsersRepository.returnCompletableFutureSingleObject();
        completableFuture.thenAccept(x -> {
            asyncExecutionDetect[0] = true;
        });

        asyncExecutionDetect[1] = true; // execute some code after submitting future
        Assert.assertFalse(completableFuture.isDone());

        UserAccount userAccount = completableFuture.get();
        Assert.assertEquals(completableFuture.isDone(), true);

        Assert.assertTrue(asyncExecutionDetect[0]);
        Assert.assertTrue(asyncExecutionDetect[1]);

        Assert.assertNotNull(userAccount);
        Assert.assertNotNull(userAccount.getId());
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnListCompletableFutureSingleObject() throws Exception {
        final boolean[] asyncExecutionDetect = {false, false}; // [0] -> completeFuture, [1] -> execute statement after future request(async)

        CompletableFuture<List<UserAccount>> completableFuture = microserviceUsersRepository.returnListCompletableFutureObjects();
        completableFuture.thenAccept(x -> {
            asyncExecutionDetect[0] = true;
        });

        asyncExecutionDetect[1] = true; // execute some code after submitting future
        Assert.assertFalse(completableFuture.isDone());

        List<UserAccount> userAccounts = completableFuture.get();
        Assert.assertEquals(completableFuture.isDone(), true);

        int processedTimes = 0;
        if (!asyncExecutionDetect[0]) {
            while (processedTimes < 5) {
                logger.warn("Waiting execute accept on CompletableFuture times " + processedTimes);
                if (asyncExecutionDetect[0]){
                    break;
                }
                Thread.sleep(1500);
                processedTimes++;
            }
        }

        Assert.assertTrue(asyncExecutionDetect[0]);
        Assert.assertTrue(asyncExecutionDetect[1]);

        Assert.assertNotNull(userAccounts);
        Assert.assertTrue(userAccounts.size() > 0);
    }

    @Test(enabled = true, invocationCount = 1)
    public void testПостроитьПолучитьМестоDomainГдеUsersГдеMock() throws Exception {
        List<UserAccount> allUsersInDomain = microserviceUsersRepository.построитьПолучитьМестоDomainГдеUsersГдеMock();
        Assert.assertTrue(allUsersInDomain.size() > 2);
        Assert.assertNotNull(allUsersInDomain.get(1).getId());
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnNullBodyResponse() throws Exception {
        UserAccount account = microserviceUsersRepository.returnNullBodyResponse();
        Assert.assertNull(account);
    }

    @Test(enabled = false, invocationCount = 1)
    public void testReturnNonNullNullBodyResponse() throws Exception {
        MicroserviceInterfaceImpFactory.setReturnNullOnEmptyResponseBody(false);
        UserAccount account = microserviceUsersRepository.returnNullBodyResponse();
        Assert.assertNull(account);
        MicroserviceInterfaceImpFactory.setReturnNullOnEmptyResponseBody(true);
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnNonNullNullBodyResponseForResponseEntity() throws Exception {
        ResponseEntity<UserAccount> userAccountResponseEntity = microserviceUsersRepository.returnNonNullBodyResponse();
        Assert.assertNotNull(userAccountResponseEntity);
        Assert.assertNull(userAccountResponseEntity.getBody());
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnGenericResponseEntity() throws Exception {
        ResponseEntity<UserAccount> allUsersInDomainMockOneResponseEntity = microserviceUsersRepository.returnGenericResponseEntity();
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity);
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity.getBody());
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity.getBody().getId());
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnJson() throws Exception {
        JsonNode jsonNode = microserviceUsersRepository.returnJson();
        Assert.assertNotNull(jsonNode);
        Assert.assertEquals(jsonNode.path("address").path("state").asText(), "LA");
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnJsonAsMap() throws Exception {
        Map<String, Object> stringObjectMapperMap = microserviceUsersRepository.returnResponseAsJsonMap();
        Assert.assertNotNull(stringObjectMapperMap);
        Assert.assertEquals(((LinkedHashMap) stringObjectMapperMap.get("address")).get("state"), "LA");
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnInvalidResponseResponseEntity() throws Exception {
        ResponseEntity<UserAccount> userAccountResponseEntity = microserviceUsersRepository.returnInvalidResponse();
        Assert.assertEquals(userAccountResponseEntity.getStatusCode().value(), 422);
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnInvalidResponseException() throws Exception {
        try {
            UserAccount account = microserviceUsersRepository.returnInvalidResponseException();
        } catch (InvalidRequestException e) {
            Assert.assertNotNull(e.getClientHttpResponse());
            Assert.assertEquals(e.getClientHttpResponse().getRawStatusCode(), 422);
            return;
        }
        Assert.fail("Not get required exception");
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnInvalidServerErrorResponseException() throws Exception {
        try {
            UserAccount account = microserviceUsersRepository.returnInvalidServerException();
        } catch (InternalSeverErrorProcessingRequestException e) {
            Assert.assertNotNull(e);
            return;
        }
        Assert.fail("Not get required exception");
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnAuthenticatedUser() throws Exception {
        String username = "Nikita";
        String password = "super_secret_password";
        UserAccount account = microserviceUsersRepository.returnAuthenticatedUser(username, password);

        Assert.assertEquals(account.getFirstname(), username);
        Assert.assertEquals(account.getLastname(), password);
    }

    @Test(enabled = true, invocationCount = 1)
    public void testEcho() throws Exception {
        String username = "Nikita";
        String password = "super_secret_password";
        String country = "LAAAAA";
        UserAccount account = microserviceUsersRepository.returnAuthenticatedUserComplexEcho(username, password, country);

        Assert.assertEquals(account.getUsername(), username);
        Assert.assertEquals(account.getPassword(), password);
        Assert.assertEquals(account.getAddress().getCountry(), country);
    }

    @Test(enabled = true, invocationCount = 1)
    public void testReturnInvalidServerExceptionEntity() throws Exception {
        try {
            ResponseEntity<UserAccount> userAccountResponseEntity = microserviceUsersRepository.returnInvalidServerExceptionEntity();
        } catch (InternalSeverErrorProcessingRequestException e) {
            Assert.assertNotNull(e);
            return;
        }
        Assert.fail("Not get required exception");
    }

    @Test(enabled = true, invocationCount = 1)
    public void testAfterRequestInterceptor() throws Exception {
        UserAccount userAccount = new UserAccount();
        userAccount.setId("Secret mocked id");

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] bytes = objectMapper.writeValueAsBytes(userAccount);

        MicroserviceRequestInterceptor microserviceRequestInterceptor = new MicroserviceRequestInterceptor() {
            @Override
            public void afterRequest(URI storesUri, HttpMethod httpMethod, HttpEntity<Object> request, ResponseEntity<byte[]> responseEntity, Class returnType, Class[] returnGenericType) {
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