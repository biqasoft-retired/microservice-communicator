package com.biqasoft.microservice.communicator.adaptors;

import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import com.biqasoft.microservice.communicator.interfaceimpl.MicroserviceRequestInterceptor;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
                    if (params != null) {
                        Object returnExpression = params.get("RETURN_EXPRESSION");
                        if (!StringUtils.isEmpty(returnExpression) && returnExpression instanceof String) {
                            Object latestNode = returnJsonPartly(returnType, responseBody, (String) returnExpression);
                            if (latestNode != null) return latestNode;
                        }
                    }

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

    private Object returnJsonPartly(Class returnType, byte[] responseBody, String returnExpression) throws java.io.IOException {
        JsonNode latestNode = objectMapper.readValue(responseBody, JsonNode.class);
        if (returnExpression.contains(".")) {
            String[] split = returnExpression.split("\\.");
            for (String s : split) {
                if (!latestNode.path(s).isNull()) {
                    latestNode = latestNode.path(s);
                }
            }
            if (String.class.equals(returnType)) {
                return latestNode.asText();
            } else if (Double.class.equals(returnType)) {
                return latestNode.asDouble();
            } else if (Boolean.class.equals(returnType)) {
                return latestNode.asBoolean();
            } else if (Integer.class.equals(returnType)) {
                return latestNode.asInt();
            } else if (Long.class.equals(returnType)) {
                return latestNode.asLong();
            }
            return objectMapper.readValue(latestNode.asText(), returnType);
        }
        return null;
    }
}
