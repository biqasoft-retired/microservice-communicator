package com.biqasoft.microservice.communicator.adaptors;

import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import com.biqasoft.microservice.communicator.interfaceimpl.MicroserviceRequestInterceptor;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Nikita on 10/17/2016.
 */
@Component
public class CompletableFutureAdapter implements MicroserviceRequestInterceptor {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(CompletableFutureAdapter.class);

    @Override
    public Object onBeforeReturnResult(Object modifiedObject, Object originalObject, Object payload, Class returnType, MicroserviceRestTemplate restTemplate, Class[] returnGenericType, Map<String, Object> params) {
        if (originalObject instanceof byte[]){
            byte[] responseBody = (byte[]) originalObject;

            if (returnType.equals(CompletableFuture.class)) {
                if (returnGenericType.length == 0) {
                    return Void.TYPE;
                }

                try {
                    if (Collection.class.isAssignableFrom(returnGenericType[0])) {
                        JavaType type = objectMapper.getTypeFactory().constructCollectionType(returnGenericType[0], returnGenericType[1]);
                        return objectMapper.readValue(responseBody, type);
                    }
                    return objectMapper.readValue(responseBody, returnGenericType[0]);
                } catch (IOException e) {
                    return modifiedObject;
                }
            }
        }
        return modifiedObject;
    }
}
