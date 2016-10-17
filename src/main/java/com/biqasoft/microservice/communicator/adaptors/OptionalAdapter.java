package com.biqasoft.microservice.communicator.adaptors;

import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import com.biqasoft.microservice.communicator.interfaceimpl.MicroserviceRequestInterceptor;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Nikita on 10/17/2016.
 */
@Component
public class OptionalAdapter implements MicroserviceRequestInterceptor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object onBeforeReturnResult(Object modifiedObject, Object originalObject, Object payload, Class returnType, MicroserviceRestTemplate restTemplate, Class[] returnGenericType, Map<String, Object> params) {
        if (returnType.equals(Optional.class)) {

            if (originalObject == null) {
                return Optional.empty();
            }

            if (originalObject instanceof byte[]) {
                byte[] responseBody = (byte[]) originalObject;

                if (responseBody.length == 0) {
                    return Optional.empty();
                }

                Object object;

                try {
                    if (Collection.class.isAssignableFrom(returnGenericType[0])) {
                        JavaType type = objectMapper.getTypeFactory().constructCollectionType(returnGenericType[0], returnGenericType[1]);
                        object = objectMapper.readValue(responseBody, type);
                    } else {
                        object = objectMapper.readValue(responseBody, returnGenericType[0]);
                    }

                    return Optional.of(object);
                } catch (IOException e) {
                    return modifiedObject;
                }

            }
        }
        return modifiedObject;
    }
}
