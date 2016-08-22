/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.exceptions.InvalidStateException;
import org.springframework.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/25/2016
 *         All Rights Reserved
 */
public class SpecialLanguageNotation {

    static SpecialLanguage isProcessSpecialLanguageNotation(Method method) {

        if (method.getName().startsWith("buildMe")) {
            return SpecialLanguage.EN;
        }

        if (method.getName().startsWith("построить")) {
            return SpecialLanguage.RU;
        }
        return null;
    }

    static void processSpecialLanguageNotation(MicroserviceInterfaceImpFactory.CachedMicroserviceCall cachedMicroserviceCall, Method method, Object o, SpecialLanguage specialLanguage) {
        String methodName = method.getName();

        // russian language
        if (specialLanguage == SpecialLanguage.RU) {
            List<String> split = new ArrayList<>(Arrays.asList(methodName.split("(?<!(^|[А-Я]))(?=[А-Я])|(?<!^)(?=[А-Я][а-я])")));

            // remove useSpecialLanguage signature
            split.remove(0);

            String httpMethod = split.get(0);

            // HTTP REST methods
            if (httpMethod.startsWith("Получить")) {
                cachedMicroserviceCall.httpMethod = HttpMethod.GET;
            } else if (httpMethod.startsWith("Удалить")) {
                cachedMicroserviceCall.httpMethod = HttpMethod.DELETE;
            } else if (httpMethod.startsWith("Положить")) {
                cachedMicroserviceCall.httpMethod = HttpMethod.PUT;
            } else if (httpMethod.startsWith("Отправить")) {
                cachedMicroserviceCall.httpMethod = HttpMethod.POST;
            }
            split.remove(0);

            if (cachedMicroserviceCall.httpMethod == null) {
                throw new InvalidStateException("NULL httpMethod");
            }

            boolean onPath = false;

            // process language
            for (String s : split) {
                if (s.startsWith("Место")) {
                    onPath = true;
                    cachedMicroserviceCall.annotatedPath = "/" + s.replace("Место", "").toLowerCase();
                }

                if (onPath) {
                    if (s.contains("Где")) {
                        cachedMicroserviceCall.annotatedPath += s.replace("Где", "/").toLowerCase();
                    }
                }

            }

        } else if (specialLanguage == SpecialLanguage.EN) {
            throw new UnsupportedOperationException("English is not supported yet");
//                String[] split = methodName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
        }
    }

    enum SpecialLanguage {
        EN,
        RU
    }

}
