package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.interfaceimpl.demo.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

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
public class MicroserviceInterfaceImplementorFactoryTest extends AbstractTestNGSpringContextTests {

    static {
        System.setProperty("spring.cloud.consul.host", "192.168.127.131");
    }

    @Autowired
    private MicroserviceUsersRepositoryTest microserviceUsersRepositoryTest;

    @Test
    public void initSpringContextPure() throws Exception {
    }

    @Test(enabled = true)
    public void testCreate() throws Exception {
        Object o = MicroserviceInterfaceImpFactory.create(MicroserviceUsersRepositoryTest.class);
        Assert.assertNotNull(o, "MicroserviceInterfaceImplementorFactory object is NULL");

        if (o instanceof MicroserviceUsersRepositoryTest) {
            UserAccount userAccount = ((MicroserviceUsersRepositoryTest) o).returnSingleObject();

            Assert.assertNotNull(userAccount);
            Assert.assertNotNull(userAccount.getId());
        } else {
            Assert.fail( o.toString() + "Not implements interface");
        }
    }

    @Test(enabled = true, description = "assert that we create implementation of interface")
    public void testIsImplementInterface() throws Exception {
        Object o = MicroserviceInterfaceImpFactory.create(MicroserviceUsersRepositoryTest.class);

        if (!(o instanceof MicroserviceUsersRepositoryTest)) {
            Assert.fail("Not implements interface");
        }
    }

    @Test(enabled = true, invocationCount = 2)
    public void testReturnGenericList() throws Exception {
        List<UserAccount> allUsersInDomain = microserviceUsersRepositoryTest.returnGenericList();
        Assert.assertTrue(allUsersInDomain.size() > 2);
        Assert.assertNotNull(allUsersInDomain.get(1).getId());
    }

    @Test(enabled = true, invocationCount = 2)
    public void testПостроитьПолучитьМестоDomainГдеUsersГдеMock() throws Exception {
        List<UserAccount> allUsersInDomain = microserviceUsersRepositoryTest.построитьПолучитьМестоDomainГдеUsersГдеMock();
        Assert.assertTrue(allUsersInDomain.size() > 2);
        Assert.assertNotNull(allUsersInDomain.get(1).getId());
    }

    @Test(enabled = true, invocationCount = 2)
    public void testReturnGenericResponseEntity() throws Exception {
        ResponseEntity<UserAccount> allUsersInDomainMockOneResponseEntity = microserviceUsersRepositoryTest.returnGenericResponseEntity();
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity);
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity.getBody());
        Assert.assertNotNull(allUsersInDomainMockOneResponseEntity.getBody().getId());
    }

}