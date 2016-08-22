package com.biqasoft.microservice.communicator;

import com.biqasoft.microservice.communicator.exceptions.CannotResolveHostException;
import com.biqasoft.microservice.communicator.exceptions.InternalSeverErrorProcessingRequestException;
import com.biqasoft.microservice.communicator.exceptions.InvalidRequestException;
import com.biqasoft.microservice.communicator.exceptions.InvalidStateException;
import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import com.biqasoft.microservice.communicator.interfaceimpl.MicroserviceInterfaceImpFactory;
import com.biqasoft.microservice.communicator.interfaceimpl.MicroserviceRequestInterceptor;
import com.biqasoft.microservice.communicator.servicediscovery.MicroserviceHelper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Nikita on 21.08.2016.
 */
@Component
public class MicroserviceRequestMaker {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(MicroserviceRequestMaker.class);

    private static MicroserviceHelper microserviceHelper;
    public final static ThreadLocal<Map<String, String>> httpHeadersThreadLocal = new ThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> initialValue() {
            return new ConcurrentHashMap<>();
        }
    };

    private static boolean RETURN_NULL_ON_EMPTY_RESPONSE_BODY = true;

    // static field for ResponseEntity<>
    public static Field body = null;

    static {
        try {
            body = HttpEntity.class.getDeclaredField("body");
            body.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Autowired
    public void setMicroserviceHelper(MicroserviceHelper microserviceHelper) {
        MicroserviceRequestMaker.microserviceHelper = microserviceHelper;
    }

    private static List<MicroserviceRequestInterceptor> microserviceRequestInterceptors = null;

    @Autowired(required = false)
    public void setMicroserviceRequestInterceptors(List<MicroserviceRequestInterceptor> microserviceRequestInterceptors) {
        MicroserviceRequestMaker.microserviceRequestInterceptors = microserviceRequestInterceptors;
    }

    /**
     * @param payload           object that will be send in HTTP POST and PUT methods
     * @param returnType        java return type in interface. If generic - collection
     * @param httpMethod        HTTP method
     * @param returnGenericType null if return type is not generic
     * @return response from server depend on interface return method or null if remote server has not response body
     */
    public static Object makeRequestToMicroservice(Object payload, Class returnType, HttpMethod httpMethod,
                                                    MicroserviceRestTemplate restTemplate, Class[] returnGenericType, Map<String, Object> params) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();

            for (Map.Entry<String, String> entry : httpHeadersThreadLocal.get().entrySet()) {
                httpHeaders.add(entry.getKey(), entry.getValue());
            }

            // if payload not byte[] - use JSON as payload type
            if (!(payload instanceof byte[])) {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            }

            if (microserviceRequestInterceptors != null) {
                microserviceRequestInterceptors.forEach(x -> {
                    x.beforeCreateHttpEntity(restTemplate.getMicroserviceName(), restTemplate.getPathToApiResource(), httpMethod, returnType, returnGenericType, httpHeaders);
                });
            }

            HttpEntity<Object> request;
            if (payload == null) {
                request = new HttpEntity<>(httpHeaders);
            } else {
                request = new HttpEntity<>(payload, httpHeaders);
            }

            if (microserviceRequestInterceptors != null) {
                microserviceRequestInterceptors.forEach(x -> {
                    x.beforeRequest(restTemplate.getMicroserviceName(), restTemplate.getPathToApiResource(), httpMethod, request, returnType, returnGenericType);
                });
            }

            ResponseEntity<byte[]> responseEntity;
            try {
                // get all responses as byte[] and if we request object - deserialize then
                responseEntity = restTemplate.exchange(null, httpMethod, request, byte[].class);
            } catch (InvalidRequestException e) {

                if (returnType.equals(ResponseEntity.class)) {
                    if (e.getClientHttpResponse() != null) {
                        ClientHttpResponse clientHttpResponse = e.getClientHttpResponse();

                        if (clientHttpResponse.getBody() != null) {
                            String body = null;
                            Scanner s = new Scanner(clientHttpResponse.getBody()).useDelimiter("\\A");
                            body = s.hasNext() ? s.next() : "";

                            if (body == null) {
                                return ResponseEntity.status(clientHttpResponse.getRawStatusCode()).headers(clientHttpResponse.getHeaders());
                            } else {
                                return ResponseEntity.status(clientHttpResponse.getRawStatusCode()).headers(clientHttpResponse.getHeaders()).body(body);
                            }
                        }
                    }
                }
                throw e;
            }

            if (microserviceRequestInterceptors != null) {
                microserviceRequestInterceptors.forEach(x -> {
                    x.afterRequest(restTemplate.getMicroserviceName(), restTemplate.getPathToApiResource(), httpMethod, request, responseEntity, returnType, returnGenericType);
                });
            }

            logger.debug("Request to microservice {}", restTemplate.getLastURI().toString());

            // if we have void in interface as return - return void
            if (returnType.equals(Void.TYPE)) {
                return Void.TYPE;
            }

            if (returnType.equals(Optional.class)) {
                if (!responseEntity.hasBody()) {
                    return Optional.empty();
                }

                Object object;

                if (Collection.class.isAssignableFrom(returnGenericType[0])) {
                    JavaType type = objectMapper.getTypeFactory().constructCollectionType(returnGenericType[0], returnGenericType[1]);
                    object = objectMapper.readValue(responseEntity.getBody(), type);
                } else {
                    object = objectMapper.readValue(responseEntity.getBody(), returnGenericType[0]);
                }

                return Optional.of(object);
            }

            if (!responseEntity.hasBody() && RETURN_NULL_ON_EMPTY_RESPONSE_BODY && !returnType.equals(ResponseEntity.class)) {
                return null;
            }

            if (returnType.equals(CompletableFuture.class)) {
                if (returnGenericType.length == 0) {
                    return Void.TYPE;
                }

                if (Collection.class.isAssignableFrom(returnGenericType[0])) {
                    JavaType type = objectMapper.getTypeFactory().constructCollectionType(returnGenericType[0], returnGenericType[1]);
                    return objectMapper.readValue(responseEntity.getBody(), type);
                }
                return objectMapper.readValue(responseEntity.getBody(), returnGenericType[0]);
            }

            // if we request byte[] return immediately
            if (returnType.equals(byte[].class)) {
                return responseEntity.getBody();
            }

            if (returnGenericType == null) {
                return objectMapper.readValue(responseEntity.getBody(), returnType);
            } else {
                // return ResponseEntity<>
                if (returnType.equals(ResponseEntity.class)) {
                    if (!responseEntity.hasBody()) {
                        return responseEntity;
                    }
                    ReflectionUtils.setField(body, responseEntity, objectMapper.readValue(responseEntity.getBody(), returnGenericType[0]));
                    return responseEntity;
                }

                if (returnType.equals(Map.class) && returnGenericType.length == 2 && returnGenericType[0].equals(String.class) && returnGenericType[1].equals(Object.class)) {
                    if (params != null && Boolean.TRUE.equals(params.get("convertResponseToMap"))) {
                        JsonNode jsonNode = objectMapper.readTree(responseEntity.getBody());
                        Map<String, String> map = objectMapper.convertValue(jsonNode, Map.class);
                        return map;
                    }
                }

                // return List<>
                if (Collection.class.isAssignableFrom(returnType)) {
                    JavaType type = objectMapper.getTypeFactory().constructCollectionType(returnType, returnGenericType[0]);
                    return objectMapper.readValue(responseEntity.getBody(), type);
                }
            }

            throw new InvalidStateException("Internal error processing. Retry later"); // invalid @annotation

        } catch (Throwable e) {
            if (params != null) {
                Object defaultValue = params.get("DEFAULT_VALUE");
                if (defaultValue != null) {
                    return defaultValue;
                }
            }

            if (e instanceof InvalidRequestException) {
                throw (InvalidRequestException) e;
            }

            if (e instanceof CannotResolveHostException) {
                logger.error(e.getMessage(), e);
            }

            logger.error("Can not get bytes from microservice {} {}", httpMethod.toString(), restTemplate.getLastURI().toString(), e);
            throw new InternalSeverErrorProcessingRequestException("Internal error processing. Retry later");
        }
    }


    public List<MicroserviceRequestInterceptor> getMicroserviceRequestInterceptors() {
        if (microserviceRequestInterceptors == null) {
            microserviceRequestInterceptors = new ArrayList<>();
        }

        return microserviceRequestInterceptors;
    }

    public static boolean isReturnNullOnEmptyResponseBody() {
        return RETURN_NULL_ON_EMPTY_RESPONSE_BODY;
    }

    public static void setReturnNullOnEmptyResponseBody(boolean returnNullOnEmptyResponseBody) {
        MicroserviceRequestMaker.RETURN_NULL_ON_EMPTY_RESPONSE_BODY = returnNullOnEmptyResponseBody;
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static MicroserviceHelper getMicroserviceHelper() {
        return microserviceHelper;
    }


}
