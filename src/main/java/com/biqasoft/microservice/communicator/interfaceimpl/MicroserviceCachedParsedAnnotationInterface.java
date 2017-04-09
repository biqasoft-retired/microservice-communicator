/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroMapping;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroPayloadVar;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.Microservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/25/2016
 *         All Rights Reserved
 */
public class MicroserviceCachedParsedAnnotationInterface {

    private static final Logger logger = LoggerFactory.getLogger(MicroserviceCachedParsedAnnotationInterface.class);

    // key - method#hashCode
    private final static Map<Integer, MicroserviceInterface.CachedMicroserviceCall> cachedMicroserviceCallMap = new ConcurrentHashMap<>();

    /**
     * Get cached microservice information about REST endpoint
     * Used to avoid use a lot of reflection every method call
     */
    static MicroserviceInterface.CachedMicroserviceCall processMicroserviceSignature(Method method, Object o) {
        MicroserviceInterface.CachedMicroserviceCall cachedMicroserviceCall = cachedMicroserviceCallMap.get(method.hashCode());

        if (cachedMicroserviceCall != null) {
            return cachedMicroserviceCall;
        }

        return computeMicroserviceSignature(method, o);
    }


    /**
     * We have not cached info about this method (REST endpoint)
     * So, calculate
     */
    private synchronized static MicroserviceInterface.CachedMicroserviceCall computeMicroserviceSignature(Method method, Object o) {
        logger.debug("Create microservice impl of method {}", method.getName());

        MicroserviceInterface.CachedMicroserviceCall cachedMicroserviceCall = new MicroserviceInterface.CachedMicroserviceCall();
        SpecialLanguageNotation.SpecialLanguage specialLanguage = SpecialLanguageNotation.isProcessSpecialLanguageNotation(method);

        MicroMapping microMapping = AnnotationUtils.findAnnotation(method, MicroMapping.class);
        Class[] returnGenericType;
        Class<?> microserviceReturnType;
        String microserviceName;
        String basePath;
        boolean https;

        Annotation declaredAnnotation = null;

        Class<?>[] allInterfaces = ClassUtils.getAllInterfaces(o);
        for (Class c : allInterfaces) {
            declaredAnnotation = AnnotationUtils.findAnnotation(c, Microservice.class);
            if (declaredAnnotation != null) {
                break;
            }
        }

        microserviceName = (String) AnnotationUtils.getValue(declaredAnnotation, "microservice");
        basePath = (String) AnnotationUtils.getValue(declaredAnnotation, "basePath");
        https = (boolean) AnnotationUtils.getValue(declaredAnnotation, "https");
        microserviceReturnType = method.getReturnType();

        // get generic type...
        try {
            returnGenericType = processGenericReturnType(method);
        } catch (Exception e) {
            returnGenericType = null;
            logger.error("can not get generic info ", e);
        }

        if (specialLanguage == null) {
            cachedMicroserviceCall.annotatedPath = microMapping.path();
            cachedMicroserviceCall.httpMethod = microMapping.method();
            cachedMicroserviceCall.tryToReconnect = microMapping.tryToReconnect();
            cachedMicroserviceCall.tryToReconnectTimes = microMapping.tryToReconnectTimes();
            cachedMicroserviceCall.sleepTimeBetweenTrying = microMapping.sleepTimeBetweenTrying();
            cachedMicroserviceCall.convertResponseToMap = microMapping.convertResponseToMap();
            cachedMicroserviceCall.returnExpression = microMapping.returnExpression();

            // we have [][]
            if (method.getParameterAnnotations().length > 0) {
                for (Annotation[] annotations : method.getParameterAnnotations()) {
                    for (Annotation annotation : annotations) {
                        if (annotation.annotationType().equals(MicroPayloadVar.class)) {
                            cachedMicroserviceCall.mergePayloadToObject = true;
                            break;
                        }
                    }
                }
            }

        } else {
            SpecialLanguageNotation.processSpecialLanguageNotation(cachedMicroserviceCall, method, o, specialLanguage);
        }

        cachedMicroserviceCall.https = https;
        cachedMicroserviceCall.microserviceName = microserviceName;
        cachedMicroserviceCall.microserviceReturnType = microserviceReturnType;
        cachedMicroserviceCall.returnGenericType = returnGenericType;
        cachedMicroserviceCall.basePath = basePath;

        cachedMicroserviceCallMap.put(method.hashCode(), cachedMicroserviceCall);
        return cachedMicroserviceCall;
    }

    /**
     *
     * @param method method from which get generics
     *
     * @return all method generics in one array;
     * normally if we want to support generic in generic, this method must be recursive,
     * but this method is used for return type of interface and this limitation is by design
     */
    private static Class[] processGenericReturnType(Method method) {
        Class[] returnGenericType = null;

        ResolvableType resolvableType = ResolvableType.forMethodReturnType(method);
        ResolvableType[] generics = resolvableType.getGenerics();

        int num = 0;
        if (generics.length > 0) {
            returnGenericType = new Class[generics.length];
            for (ResolvableType generic : generics) {
                num++;
                ResolvableType[] generics1 = generic.getGenerics();
                if (generics1.length > 0) {
                    num++;
                }
            }
        }

        if (generics.length > 0) {
            returnGenericType = new Class[num];
            for (int i = 0; i < generics.length; i++) {
                returnGenericType[i] = generics[i].getRawClass();
                ResolvableType[] generics1 = generics[i].getGenerics();
                if (generics1.length > 0) {
                    returnGenericType[i + 1] = generics1[0].getRawClass();
                }
            }
        }
        return returnGenericType;
    }

}
