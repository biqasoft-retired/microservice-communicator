package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.MicroserviceRequestMaker;
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

import java.util.*;
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

    @Autowired
    private MicroserviceRequestMaker microserviceRequestMaker;

    @Test
    public void testReturnGenericList() throws Exception {
        List<UserAccount> allUsersInDomain = microserviceUsersRepository.returnGenericList();
        Assert.assertTrue(allUsersInDomain.size() > 2);
        Assert.assertNotNull(allUsersInDomain.get(1).getId());
    }

    @Test
    public void testReturnSingleObjectWithPathParam() throws Exception {
        UserAccount account = microserviceUsersRepository.returnSingleObjectWithPathParam("users", "mock");
        Assert.assertNotNull(account);
        Assert.assertNotNull(account.getId());
    }

    @Test
    public void testReturnSingleOptionalObject() throws Exception {
        Optional<UserAccount> userAccount = microserviceUsersRepository.returnSingleOptionalObject();
        Assert.assertNotNull(userAccount);
        Assert.assertTrue(userAccount.isPresent() );
        Assert.assertNotNull(userAccount.get());
        Assert.assertNotNull(userAccount.get().getId());
    }

    @Test
    public void testReturnListOptionalObject() throws Exception {
        Optional<List<UserAccount>> userAccounts = microserviceUsersRepository.returnListOptionalObject();

        Assert.assertNotNull(userAccounts);
        Assert.assertTrue(userAccounts.isPresent() );
        Assert.assertNotNull(userAccounts.get());
        Assert.assertTrue(userAccounts.get().size() > 0);
        Assert.assertNotNull(userAccounts.get().get(0).getId());
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void testReturnSingleOptionalEmptyObject() throws Exception {
        Optional<UserAccount> userAccount = microserviceUsersRepository.returnSingleOptionalEmptyObject();

        Assert.assertNotNull(userAccount);
        Assert.assertFalse(userAccount.isPresent() );
        userAccount.get();
    }

    @Test
    public void testReturnCompletableFutureSingleObject() throws Exception {
        boolean[] asyncExecutionDetect = {false, false}; // [0] -> completeFuture, [1] -> execute statement after future request(async)

        CompletableFuture<UserAccount> completableFuture = microserviceUsersRepository.returnCompletableFutureSingleObject();
        completableFuture.thenAccept(x -> {
            asyncExecutionDetect[0] = true;
        });

        asyncExecutionDetect[1] = true; // execute some code after submitting future
        Assert.assertFalse(completableFuture.isDone());

        UserAccount userAccount = completableFuture.get();
        Thread.sleep(100);
        Assert.assertEquals(completableFuture.isDone(), true);

        Assert.assertTrue(asyncExecutionDetect[0]);
        Assert.assertTrue(asyncExecutionDetect[1]);

        Assert.assertNotNull(userAccount);
        Assert.assertNotNull(userAccount.getId());
    }

    @Test
    public void testReturnListCompletableFutureSingleObject() throws Exception {
        final boolean[] asyncExecutionDetect = {false, false}; // [0] -> completeFuture, [1] -> execute statement after future request(async)

        CompletableFuture<List<UserAccount>> completableFuture = microserviceUsersRepository.returnListCompletableFutureObjects();
        completableFuture.thenAccept(x -> {
            asyncExecutionDetect[0] = true;
        });

        asyncExecutionDetect[1] = true; // execute some code after submitting future
//        Assert.assertFalse(completableFuture.isDone());

        List<UserAccount> userAccounts = completableFuture.get();
        Assert.assertEquals(completableFuture.isDone(), true);

        Thread.sleep(100);

        Assert.assertTrue(asyncExecutionDetect[0]);
        Assert.assertTrue(asyncExecutionDetect[1]);

        Assert.assertNotNull(userAccounts);
        Assert.assertTrue(userAccounts.size() > 0);
    }

    @Test
    public void testПостроитьПолучитьМестоDomainГдеUsersГдеMock() throws Exception {
        List<UserAccount> allUsersInDomain = microserviceUsersRepository.построитьПолучитьМестоDomainГдеUsersГдеMock();
        Assert.assertTrue(allUsersInDomain.size() > 2);
        Assert.assertNotNull(allUsersInDomain.get(1).getId());
    }

    @Test
    public void testReturnNullBodyResponse() throws Exception {
        UserAccount account = microserviceUsersRepository.returnNullBodyResponse();
        Assert.assertNull(account);
    }

    @Test
    public void testReturnNonNullNullBodyResponseForResponseEntity() throws Exception {
        ResponseEntity<UserAccount> userAccountResponseEntity = microserviceUsersRepository.returnNonNullBodyResponse();
        Assert.assertNotNull(userAccountResponseEntity);
        Assert.assertNull(userAccountResponseEntity.getBody());
    }

    @Test
    public void testReturnGenericResponseEntity() throws Exception {
        ResponseEntity<UserAccount> allUsersInDomainMockOneResponseEntity = microserviceUsersRepository.returnGenericResponseEntity();
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity);
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity.getBody());
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity.getBody().getId());
    }

    @Test
    public void testReturnJson() throws Exception {
        JsonNode jsonNode = microserviceUsersRepository.returnJson();
        Assert.assertNotNull(jsonNode);
        Assert.assertEquals(jsonNode.path("address").path("state").asText(), "LA");
    }

    @Test
    public void testReturnJsonAsMap() throws Exception {
        Map<String, Object> stringObjectMapperMap = microserviceUsersRepository.returnResponseAsJsonMap();
        Assert.assertNotNull(stringObjectMapperMap);
        Assert.assertEquals(((LinkedHashMap) stringObjectMapperMap.get("address")).get("state"), "LA");
    }

    @Test
    public void testReturnInvalidResponseResponseEntity() throws Exception {
        ResponseEntity<UserAccount> userAccountResponseEntity = microserviceUsersRepository.returnInvalidResponse();
        Assert.assertEquals(userAccountResponseEntity.getStatusCode().value(), 422);
    }

    @Test
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

    @Test
    public void testReturnInvalidServerErrorResponseException() throws Exception {
        try {
            UserAccount account = microserviceUsersRepository.returnInvalidServerException();
        } catch (InternalSeverErrorProcessingRequestException e) {
            Assert.assertNotNull(e);
            return;
        }
        Assert.fail("Not get required exception");
    }

    @Test
    public void testReturnDefaultValue() throws Throwable {
        UserAccount userAccount = microserviceUsersRepository.returnDefaultValue();
        Assert.assertEquals(userAccount.getId(), "I'm default Java 8 interface");
    }

    @Test
    public void testReturnAuthenticatedUser() throws Exception {
        String username = "Nikita";
        String password = "super_secret_password";
        UserAccount account = microserviceUsersRepository.returnAuthenticatedUser(username, password);

        Assert.assertEquals(account.getFirstname(), username);
        Assert.assertEquals(account.getLastname(), password);
    }

    @Test
    public void testCreateJsonFromParam() throws Exception {
        String username = "Nikita";
        String password = "super_secret_password";
        String country = "LAAAAA";
        String city = "BOO";
        UserAccount account = microserviceUsersRepository.returnAuthenticatedUserComplexEcho(username, password, country, city);

        Assert.assertEquals(account.getUsername(), username);
        Assert.assertEquals(account.getPassword(), password);
        Assert.assertEquals(account.getAddress().getCountry(), country);
        Assert.assertEquals(account.getAddress().getCity(), city);
    }

    @Test
    public void testReturnPayloadFromName() throws Exception {
        String username = "Nikita";
        String password = "super_secret_password";
        String country = "LAAAAA";
        String city = "BOO";
        UserAccount account = microserviceUsersRepository.returnPayloadFromName(username, password, country, city);

        Assert.assertEquals(account.getUsername(), username);
        Assert.assertEquals(account.getPassword(), password);
        Assert.assertEquals(account.getAddress().getCountry(), country);
        Assert.assertEquals(account.getAddress().getCity(), city);
    }

    @Test
    public void testReturnInvalidServerExceptionEntity() throws Exception {
        try {
            ResponseEntity<UserAccount> userAccountResponseEntity = microserviceUsersRepository.returnInvalidServerExceptionEntity();
        } catch (InternalSeverErrorProcessingRequestException e) {
            Assert.assertNotNull(e);
            return;
        }
        Assert.fail("Not get required exception");
    }

    @Test
    public void testAfterRequestInterceptor() throws Exception {
        UserAccount userAccount = new UserAccount();
        userAccount.setId("Secret mocked id");

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] bytes = objectMapper.writeValueAsBytes(userAccount);

        MicroserviceRequestInterceptor microserviceRequestInterceptor = new MicroserviceRequestInterceptor() {
            @Override
            public void afterRequest(String microserviceName, String microservicePath, HttpMethod httpMethod, HttpEntity<Object> request, ResponseEntity<byte[]> responseEntity, Class returnType, Class[] returnGenericType) {
                ReflectionUtils.setField(MicroserviceRequestMaker.body, responseEntity, bytes);
            }
        };

        int beforeInterceptors = microserviceRequestMaker.getMicroserviceRequestInterceptors().size();
        microserviceRequestMaker.getMicroserviceRequestInterceptors().add(microserviceRequestInterceptor);

        UserAccount account = microserviceUsersRepository.returnSingleObject();
        Assert.assertNotNull(account);
        Assert.assertNotNull(account.getId());
        Assert.assertEquals(account.getId(), userAccount.getId());

        microserviceRequestMaker.getMicroserviceRequestInterceptors().remove(microserviceRequestInterceptor);
        Assert.assertEquals(microserviceRequestMaker.getMicroserviceRequestInterceptors().size(), beforeInterceptors, "Not deleted after request interceptor");
    }

}