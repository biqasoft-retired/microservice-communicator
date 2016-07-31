/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.exceptions.InternalSeverErrorProcessingRequestException;
import com.biqasoft.microservice.communicator.exceptions.InvalidRequestException;
import com.biqasoft.microservice.communicator.exceptions.InvalidStateException;
import com.biqasoft.microservice.communicator.http.HttpClientsHelpers;
import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroservicePathVariable;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceRequest;
import com.biqasoft.microservice.communicator.servicediscovery.MicroserviceHelper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implement REST methods logic interface {@link MicroserviceRequest} microservice requests
 *
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/15/2016
 *         All Rights Reserved
 */
@Component
public class MicroserviceInterfaceImpFactory {

    private static final Logger logger = LoggerFactory.getLogger(MicroserviceInterfaceImpFactory.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static MicroserviceHelper microserviceHelper;

    public final static ThreadLocal<Map<String, String>> httpHeadersThreadLocal = new ThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> initialValue() {
            return new ConcurrentHashMap<>();
        }
    };

    private static boolean returnNullOnEmptyResponseBody = true;

    // static field for ResponseEntity<>
    static Field body = null;

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
        MicroserviceInterfaceImpFactory.microserviceHelper = microserviceHelper;
    }

    private static List<MicroserviceRequestInterceptor> microserviceRequestInterceptors = null;

    @Autowired(required = false)
    public void setMicroserviceRequestInterceptors(List<MicroserviceRequestInterceptor> microserviceRequestInterceptors) {
        MicroserviceInterfaceImpFactory.microserviceRequestInterceptors = microserviceRequestInterceptors;
    }

    /**
     * @param payload           object that will be send in HTTP POST and PUT methods
     * @param returnType        java return type in interface. If generic - collection
     * @param httpMethod        HTTP method
     * @param returnGenericType null if return type is not generic
     * @return response from server depend on interface return method or null if remote server has not response body
     */
    private static Object makeRequestToMicroservice(Object payload, Class returnType, HttpMethod httpMethod,
                                                    MicroserviceRestTemplate restTemplate, Class[] returnGenericType, Map<String, Object> params) {
        URI storesUri = restTemplate.getUrl();
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
                    x.beforeCreateHttpEntity(storesUri, httpMethod, returnType, returnGenericType, httpHeaders);
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
                    x.beforeRequest(storesUri, httpMethod, request, returnType, returnGenericType);
                });
            }

            ResponseEntity<byte[]> responseEntity = null;
            try {
                // get all responses as byte[] and if we request object - deserialize then
                responseEntity = restTemplate.exchange(storesUri, httpMethod, request, byte[].class);
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
                ResponseEntity<byte[]> finalResponseEntity = responseEntity;
                microserviceRequestInterceptors.forEach(x -> {
                    x.afterRequest(storesUri, httpMethod, request, finalResponseEntity, returnType, returnGenericType);
                });
            }

            logger.debug("Request to microservice {}", storesUri.toString());

            // if we have void in interface as return - return void
            if (returnType.equals(Void.TYPE)) {
                return Void.TYPE;
            }

            if (!responseEntity.hasBody() && returnNullOnEmptyResponseBody && !returnType.equals(ResponseEntity.class)) {
                return null;
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

            throw new InvalidStateException("Internal error processing. Retry later");

        } catch (IOException e) {
            logger.error("Can not get bytes from microservice {} {}", httpMethod.toString(), storesUri.toString(), e);
            throw new InternalSeverErrorProcessingRequestException("Internal error processing. Retry later");
        }
    }

    /**
     * Create microservice implementation
     *
     * @param interfaceToExtend interface, annotated {@link MicroserviceRequest}
     * @return object implemented interface
     */
    public static Object create(Class<?> interfaceToExtend) {
        if (interfaceToExtend.isInterface()) {

            Annotation declaredAnnotation = interfaceToExtend.getDeclaredAnnotation(MicroserviceRequest.class);
            if (declaredAnnotation == null) {
                throw new InvalidStateException(interfaceToExtend.toString() + "  must be annotated with " + MicroserviceRequest.class.toString() + " annotation");
            }

            List<Class<?>> extendInterfaces = new ArrayList<>();
            extendInterfaces.add(interfaceToExtend); // new microservice class - will implement microservice interface
            extendInterfaces.addAll(Arrays.asList(interfaceToExtend.getInterfaces())); // implement interface that current interface class implement

            Object extendedInterface = Enhancer.create(UserMicroserviceRequestSuperService.class, extendInterfaces.toArray(new Class[extendInterfaces.size()]), new MethodInterceptor() {
                @Override
                public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

                    // do not proxy toString and hashCode - invoke super class methods
                    if (method.getName().equals("toString") || method.getName().equals("hashCode")) {
                        return methodProxy.invokeSuper(o, objects);
                    }
                    logger.debug("Start microservice method {} in {}", method.getName(), o.toString());

                    CachedMicroserviceCall microserviceCall = MicroserviceCachedParsedAnnotationInterface.processMicroserviceSignature(method, o);

                    String annotatedPath = microserviceCall.annotatedPath;
                    HttpMethod httpMethod = microserviceCall.httpMethod;
                    Class[] returnGenericType = microserviceCall.returnGenericType;
                    Class<?> microserviceReturnType = microserviceCall.microserviceReturnType;
                    String microserviceName = microserviceCall.microserviceName;
                    boolean convertJsonToMap = microserviceCall.convertResponseToMap;

                    Object payload = null;

                    // number of params in interface for bound to URL
                    int paramsForMappingUrl = 0;

                    // replace {} in annotated URL
                    for (Object o1 : method.getParameters()) {
                        if (!(((Parameter) o1).getType().equals(String.class))) {
                            continue;
                        }

                        Parameter parameter = (Parameter) o1;
                        Field field = parameter.getClass().getDeclaredField("index");
                        field.setAccessible(true);
                        Integer field1 = (Integer) ReflectionUtils.getField(field, parameter);

                        MicroservicePathVariable param = parameter.getDeclaredAnnotation((MicroservicePathVariable.class));
                        if (param == null || StringUtils.isEmpty(param.param())) {
                            continue;
                        }

                        if (!StringUtils.isEmpty(param.param())) {
                            String paramValue = (String) Arrays.asList(objects).get(field1);
                            paramValue = URLEncoder.encode(paramValue, "UTF-8");

                            annotatedPath = annotatedPath.replace("{" + param.param() + "}", paramValue);
                            paramsForMappingUrl++;
                        }
                    }

                    // only POST and PUT can have payload
                    if (httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
                        if (objects.length >= 1) {
                            // +1 - this is payload param
                            if ((paramsForMappingUrl + 1) != objects.length) {
                                throw new InvalidStateException("You must pass EXACTLY ONE payload to POST or PUT method");
                            }
                        } else {
                            // zero params
                            throw new InvalidStateException("You must pass EXACTLY ONE payload to POST or PUT method");
                        }
                        payload = objects[0];
                    }

                    MicroserviceRestTemplate restTemplate = HttpClientsHelpers.getRestTemplate(microserviceCall.tryToReconnect, microserviceCall.tryToReconnectTimes,
                            microserviceCall.sleepTimeBetweenTrying, microserviceName, annotatedPath);
                    Map<String, Object> param = null;

                    if (convertJsonToMap) {
                        param = new HashMap<>();
                        param.put("convertResponseToMap", true);
                    }

                    Object result = makeRequestToMicroservice(payload, microserviceReturnType, httpMethod, restTemplate, returnGenericType, param);

                    logger.debug("End microservice method {} in {}", method.getName(), o.toString());
                    return result;
                }
            });
            return extendedInterface;
        } else {
            logger.error("Interface expected {}", interfaceToExtend.getName());
        }
        return null;
    }

    static class CachedMicroserviceCall {
        Class<?> microserviceReturnType = null;
        Class[] returnGenericType = null;
        HttpMethod httpMethod = null;
        String annotatedPath = null;
        String microserviceName = null;

        boolean convertResponseToMap = false;
        boolean tryToReconnect;
        int tryToReconnectTimes;
        int sleepTimeBetweenTrying;
    }

    enum SpecialLanguage {
        EN,
        RU
    }

    public MicroserviceInterfaceImpFactory() {
    }

    public List<MicroserviceRequestInterceptor> getMicroserviceRequestInterceptors() {
        if (microserviceRequestInterceptors == null) {
            microserviceRequestInterceptors = new ArrayList<>();
        }

        return microserviceRequestInterceptors;
    }

    public static boolean isReturnNullOnEmptyResponseBody() {
        return returnNullOnEmptyResponseBody;
    }

    public static void setReturnNullOnEmptyResponseBody(boolean returnNullOnEmptyResponseBody) {
        MicroserviceInterfaceImpFactory.returnNullOnEmptyResponseBody = returnNullOnEmptyResponseBody;
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static MicroserviceHelper getMicroserviceHelper() {
        return microserviceHelper;
    }
}
