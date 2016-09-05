/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.MicroserviceRequestMaker;
import com.biqasoft.microservice.communicator.exceptions.InvalidStateException;
import com.biqasoft.microservice.communicator.http.HttpClientsHelpers;
import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroPathVar;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroPayloadVar;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.Microservice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Implement REST methods logic interface {@link Microservice} microservice requests
 *
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/15/2016
 *         All Rights Reserved
 */
@Component
public class MicroserviceInterfaceImpFactory {

    private static final Logger logger = LoggerFactory.getLogger(MicroserviceInterfaceImpFactory.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final JsonNodeFactory factory = JsonNodeFactory.instance;

    /**
     * Create microservice implementation
     *
     * @param interfaceToExtend interface, annotated {@link Microservice}
     * @return object implemented interface
     */
    public static Object create(Class<?> interfaceToExtend) {
        if (interfaceToExtend.isInterface()) {

            Annotation declaredAnnotation = interfaceToExtend.getDeclaredAnnotation(Microservice.class);
            if (declaredAnnotation == null) {
                throw new InvalidStateException(interfaceToExtend.toString() + " must be annotated with " + Microservice.class.toString() + " annotation");
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
                    boolean mergePayloadToObject = microserviceCall.mergePayloadToObject;

                    // java 8 default interface method
                    boolean haveDefaultValue = method.isDefault();

                    Object payload = null;
                    List<Parameter> parameters = Arrays.asList(method.getParameters());

                    // number of params in interface for bound to URL
                    int paramsForMappingUrl = 0;

                    // replace {} in annotated URL
                    for (Parameter parameter : parameters) {
                        if (!(parameter.getType().equals(String.class))) {
                            continue;
                        }

                        MicroPathVar param = AnnotationUtils.findAnnotation(parameter, MicroPathVar.class);
                        if (param == null || StringUtils.isEmpty(param.param())) {
                            continue;
                        }

                        if (!StringUtils.isEmpty(param.param())) {
                            String paramValue = (String) objects[parameters.indexOf(parameter)];
                            paramValue = URLEncoder.encode(paramValue, "UTF-8");

                            annotatedPath = annotatedPath.replace("{" + param.param() + "}", paramValue);
                            paramsForMappingUrl++;
                        }
                    }

                    // create(merge) json payload from many method arguments
                    if (mergePayloadToObject) {
                        ObjectNode rootNode = factory.objectNode();
                        payload = rootNode;

                        for (Parameter parameter : parameters) {

                            MicroPayloadVar param = AnnotationUtils.findAnnotation(parameter, MicroPayloadVar.class);
                            if (param == null) {
                                continue;
                            }

                            String jsonName;
                            String delimiter = ".";

                            if (!StringUtils.isEmpty(param.path())) {
                                jsonName = param.path();
                            } else {
                                // java 8 param reflection
                                if (!parameter.isNamePresent()) {
                                    throw new InvalidStateException("You try to use java 8 param name extract via reflection, but looks like, not compile javac with -parameters");
                                }
                                jsonName = parameter.getName();
                                delimiter = "_";
                            }

                            ObjectNode latestNode = rootNode;
                            if (jsonName.contains(delimiter)) {
                                String[] split = jsonName.split(delimiter.equals(".") ? "\\." : "_");
                                int i = 0;
                                for (String s : split) {
                                    if ((i + 1) == split.length) {
                                        break;
                                    }

                                    if (latestNode.path(s).isObject()) {
                                        latestNode = (ObjectNode) latestNode.path(s);
                                    } else {
                                        ObjectNode newNode = factory.objectNode();
                                        latestNode.set(s, newNode);
                                        latestNode = newNode;
                                    }
                                    i++;
                                }
                                jsonName = split[split.length - 1];
                            }

                            JsonNode node = objectMapper.convertValue(objects[parameters.indexOf(parameter)], JsonNode.class);
                            latestNode.set(jsonName, node);
                        }
                    }

                    // only POST and PUT can have payload
                    if ((httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) && payload == null) {
                        if (objects.length >= 1) {
                            // +1 - this is payload param
                            if ((paramsForMappingUrl + 1) != objects.length) {
                                throw new InvalidStateException("You must pass EXACTLY ONE payload to POST or PUT method");
                            }
                        } else {
                            // zero params
                            throw new InvalidStateException("You must pass EXACTLY ONE payload to POST or PUT method, have 0");
                        }
                        payload = objects[0];
                    }

                    MicroserviceRestTemplate restTemplate = HttpClientsHelpers.getRestTemplate(microserviceCall.tryToReconnect, microserviceCall.tryToReconnectTimes,
                            microserviceCall.sleepTimeBetweenTrying, microserviceName, annotatedPath, httpMethod);
                    Map<String, Object> param = null;

                    if (convertJsonToMap) {
                        param = new HashMap<>();
                        param.put("convertResponseToMap", true);
                    }

                    if (haveDefaultValue) {
                        if (param == null) {
                            param = new HashMap<>();
                        }

                        param.put("HAVE_DEFAULT_VALUE", true);
                        param.put(MicroserviceRequestMaker.DEFAULT_INTERFACE_PROXY_METHOD, method);
                        param.put(MicroserviceRequestMaker.INTERFACE_IMPLEMENTED, interfaceToExtend);
                        param.put(MicroserviceRequestMaker.METHOD_PARAMS, objects);
                    }

                    HttpHeaders httpHeaders = new HttpHeaders();
                    MicroserviceRequestMaker.beforeProcessRequest(restTemplate, httpHeaders);

                    if (microserviceReturnType.equals(CompletableFuture.class)) {
                        Object finalPayload = payload;
                        Map<String, Object> finalParam = param;
                        return CompletableFuture
                                .supplyAsync(() -> {
                                    Object result = MicroserviceRequestMaker.makeRequestToMicroservice(finalPayload, microserviceReturnType, restTemplate, returnGenericType, finalParam, httpHeaders);
                                    result = MicroserviceRequestMaker.onBeforeReturnResultProcessor(result, finalPayload, microserviceReturnType, restTemplate, returnGenericType, finalParam);
                                    return result;
                                });
                    } else {
                        Object result = MicroserviceRequestMaker.makeRequestToMicroservice(payload, microserviceReturnType, restTemplate, returnGenericType, param, httpHeaders);
                        result = MicroserviceRequestMaker.onBeforeReturnResultProcessor(result, payload, microserviceReturnType, restTemplate, returnGenericType, param);

                        logger.debug("End microservice method {} in {}", method.getName(), o.toString());
                        return result;
                    }

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
        boolean mergePayloadToObject = false;
        boolean tryToReconnect;
        int tryToReconnectTimes;
        int sleepTimeBetweenTrying;
    }

}
