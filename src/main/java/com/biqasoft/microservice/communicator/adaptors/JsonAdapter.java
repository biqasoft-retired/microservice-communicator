package com.biqasoft.microservice.communicator.adaptors;

import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import com.biqasoft.microservice.communicator.interfaceimpl.MicroserviceRequestInterceptor;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * process return type for return type json objects
 * Created by Nikita on 10/17/2016.
 */
@Component
public class JsonAdapter implements MicroserviceRequestInterceptor {

    private final ObjectMapper objectMapper;

    public JsonAdapter(@Qualifier("defaultObjectMapperConfiguration") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object onBeforeReturnResult(Object modifiedObject, Object originalObject, Object payload, Class returnType, MicroserviceRestTemplate restTemplate, Class[] returnGenericType, Map<String, Object> params) {
        if (originalObject instanceof byte[]) {
            byte[] responseBody = (byte[]) originalObject;

            try {
                if (returnGenericType == null) {
                    return objectMapper.readValue(responseBody, returnType);
                } else {
                    if (returnType.equals(Map.class) && returnGenericType.length == 2 && returnGenericType[0].equals(String.class) && returnGenericType[1].equals(Object.class)) {
                        if (params != null && Boolean.TRUE.equals(params.get("convertResponseToMap"))) {
                            JsonNode jsonNode = objectMapper.readTree(responseBody);
                            return objectMapper.convertValue(jsonNode, Map.class);
                        }
                    }

                    // return List<>
                    if (Collection.class.isAssignableFrom(returnType)) {
                        JavaType type = objectMapper.getTypeFactory().constructCollectionType(returnType, returnGenericType[0]);
                        return objectMapper.readValue(responseBody, type);
                    }
                }
            } catch (Exception e) {
                return modifiedObject;
            }

        }
        return modifiedObject;
    }
}
