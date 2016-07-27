/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceMapping;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceRequest;
import com.strobel.reflection.Type;
import com.strobel.reflection.TypeBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

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
    private final static Map<Integer, MicroserviceInterfaceImpFactory.CachedMicroserviceCall> cachedMicroserviceCallMap = new ConcurrentHashMap<>();

    /**
     * Get cached microservice information about REST endpoint
     * Used to avoid use a lot of reflection every method call
     *
     * @param method
     * @param o
     * @return
     */
    static MicroserviceInterfaceImpFactory.CachedMicroserviceCall processMicroserviceSignature(Method method, Object o) {
        MicroserviceInterfaceImpFactory.CachedMicroserviceCall cachedMicroserviceCall = cachedMicroserviceCallMap.get(method.hashCode());

        if (cachedMicroserviceCall != null) {
            return cachedMicroserviceCall;
        }

        return computeMicroserviceSignature(method, o);
    }

    /**
     * We have not cached info about this method (REST endpoint)
     * So, calculate
     *
     * @param method
     * @param o
     * @return
     */
    private static MicroserviceInterfaceImpFactory.CachedMicroserviceCall computeMicroserviceSignature(Method method, Object o) {
        logger.info("Create microservice impl of method {}", method.getName());

        MicroserviceInterfaceImpFactory.CachedMicroserviceCall cachedMicroserviceCall = new MicroserviceInterfaceImpFactory.CachedMicroserviceCall();
        MicroserviceInterfaceImpFactory.SpecialLanguage specialLanguage = SpecialLanguageNotation.isProcessSpecialLanguageNotation(method);

        MicroserviceMapping microserviceMapping = method.getDeclaredAnnotation(MicroserviceMapping.class);
        Class returnGenericType = null;
        Class<?> microserviceReturnType = null;
        String microserviceName = null;

        try {
            Class aClass = Class.forName(MicroserviceBPP.isMicroserviceAnnotation(o).getTypeName());
            Annotation declaredAnnotation = aClass.getDeclaredAnnotation(MicroserviceRequest.class);
            microserviceName = (String) AnnotationUtils.getValue(declaredAnnotation, "microservice");

            microserviceReturnType = method.getReturnType();

            // get generic type...
            try {
                Type<?> returnType = Type.of(aClass).getMethod(method.getName()).getReturnType();

                if (returnType.isGenericType() && returnType.getGenericTypeParameters().size() > 0) {
                    Method allGenericsMethod = returnType.getClass().getDeclaredMethod("getTypeBindings");
                    allGenericsMethod.setAccessible(true);

                    TypeBindings getTypeBindings = (TypeBindings) allGenericsMethod.invoke(returnType);
                    returnGenericType = Class.forName(getTypeBindings.getBoundType(0).getTypeName());
                }
            } catch (Exception e) {
                returnGenericType = null;
                logger.error("can not get generic info ", e);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (specialLanguage == null) {
            cachedMicroserviceCall.annotatedPath = microserviceMapping.path();
            cachedMicroserviceCall.httpMethod = microserviceMapping.method();
            cachedMicroserviceCall.tryToReconnect = microserviceMapping.tryToReconnect();
            cachedMicroserviceCall.tryToReconnectTimes = microserviceMapping.tryToReconnectTimes();
            cachedMicroserviceCall.sleepTimeBetweenTrying = microserviceMapping.sleepTimeBetweenTrying();
        } else {
            SpecialLanguageNotation.processSpecialLanguageNotation(cachedMicroserviceCall, method, o, specialLanguage);
        }

        cachedMicroserviceCall.microserviceName = microserviceName;
        cachedMicroserviceCall.microserviceReturnType = microserviceReturnType;
        cachedMicroserviceCall.returnGenericType = returnGenericType;

        cachedMicroserviceCallMap.put(method.hashCode(), cachedMicroserviceCall);
        return cachedMicroserviceCall;
    }

}
