package com.biqasoft.microservice.communicator.adaptors;

import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import com.biqasoft.microservice.communicator.interfaceimpl.MicroserviceRequestInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by Nikita on 10/17/2016.
 */
@Component
public class StringAdapter implements MicroserviceRequestInterceptor {

    @Override
    public Object onBeforeReturnResult(Object modifiedObject, Object originalObject, Object payload, Class returnType, MicroserviceRestTemplate restTemplate, Class[] returnGenericType, Map<String, Object> params) {
        if (originalObject instanceof String) {
            byte[] responseBody = (byte[]) originalObject;
            return new String(responseBody);
        }
        return modifiedObject;
    }
}
