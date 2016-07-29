/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.exceptions.InternalSeverErrorProcessingRequestException;
import com.biqasoft.microservice.communicator.exceptions.InvalidStateException;
import com.biqasoft.microservice.communicator.http.HttpClientsHelpers;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroservicePathVariable;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceRequest;
import com.biqasoft.microservice.communicator.servicediscovery.MicroserviceHelper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
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

    private static RestTemplate getRestTemplate(Boolean tryToReconnect, int tryToReconnectTimes, int sleepTimeBetweenTrying) {
        return HttpClientsHelpers.getRestTemplate(tryToReconnect, tryToReconnectTimes, sleepTimeBetweenTrying);
    }

    /**
     * @param payload           object that will be send in HTTP POST and PUT methods
     * @param storesUri         http URI
     * @param returnType        java return type in interface. If generic - collection
     * @param httpMethod        HTTP method
     * @param returnGenericType null if return type is not generic
     * @return response from server depend on interface return method or null if remote server has not response body
     */
    private static Object makeRequestToMicroservice(Object payload, URI storesUri, Class returnType, HttpMethod httpMethod, RestTemplate restTemplate, Class returnGenericType) {
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

            // get all responses as byte[] and if we request object - deserialize then
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(storesUri, httpMethod, request, byte[].class);

            if (microserviceRequestInterceptors != null) {
                microserviceRequestInterceptors.forEach(x -> {
                    x.afterRequest(storesUri, httpMethod, request, responseEntity, returnType, returnGenericType);
                });
            }

            logger.debug("Request to microservice {}", storesUri.toString());

            // if we have void in interface as return - return void
            if (returnType.equals(Void.TYPE)) {
                return Void.TYPE;
            }

            if (!responseEntity.hasBody() && returnNullOnEmptyResponseBody && !returnType.equals(ResponseEntity.class)){
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
                    if (!responseEntity.hasBody()){
                        return responseEntity;
                    }
                    ReflectionUtils.setField(body, responseEntity, objectMapper.readValue(responseEntity.getBody(), returnGenericType));
                    return responseEntity;
                }

                // return List<>
                if (Collection.class.isAssignableFrom(returnType)) {
                    JavaType type = objectMapper.getTypeFactory().constructCollectionType(returnType, returnGenericType);
                    return objectMapper.readValue(responseEntity.getBody(), type);
                }
            }

            throw new InvalidStateException("Internal error processing. Retry later");

        } catch (Exception e) {
            logger.error("Can not get bytes from microservice", e);
            throw new InternalSeverErrorProcessingRequestException("Internal error processing. Retry later");
        }
    }

    private static URI getMicroserviceURI(String microserviceName, String microserviceMapping) {
        return microserviceHelper.getLoadBalancedURIByMicroservice(microserviceName, microserviceMapping);
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
                    Class returnGenericType = microserviceCall.returnGenericType;
                    Class<?> microserviceReturnType = microserviceCall.microserviceReturnType;
                    String microserviceName = microserviceCall.microserviceName;

                    Object payload = null;

                    // number of params in interface for bound to URL
                    int paramsForMappingUrl = 0;

                    // replace {} in annotated URL
                    for (Object o1 : method.getParameters()) {
                        if  (!(((Parameter) o1).getType().equals(String.class))  ) {
                            continue;
                        }

                        Parameter parameter = (Parameter) o1;
                        Field field = parameter.getClass().getDeclaredField("index");
                        field.setAccessible(true);
                        Integer field1 = (Integer) ReflectionUtils.getField(field, parameter);

                        MicroservicePathVariable param = parameter.getDeclaredAnnotation((MicroservicePathVariable.class));
                        if (param == null || StringUtils.isEmpty(param.param())){
                            continue;
                        }

                        if (!StringUtils.isEmpty(param.param())) {
                            annotatedPath = annotatedPath.replace("{" + param.param() + "}", (String) Arrays.asList(objects).get(field1));
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

                    URI uri = getMicroserviceURI(microserviceName, annotatedPath);
                    RestTemplate restTemplate = getRestTemplate(microserviceCall.tryToReconnect, microserviceCall.tryToReconnectTimes, microserviceCall.sleepTimeBetweenTrying);
                    Object result = makeRequestToMicroservice(payload, uri, microserviceReturnType, httpMethod, restTemplate, returnGenericType);

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
        Class returnGenericType = null;
        HttpMethod httpMethod = null;
        String annotatedPath = null;
        String microserviceName = null;

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
}
