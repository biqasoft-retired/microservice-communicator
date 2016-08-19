/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.interfaceimpl.annotation.Microservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 6/7/2016
 *         All Rights Reserved
 */
@Component
public class MicroserviceBPP implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MicroserviceBPP.class);

    /**
     * Does our object instance implement interface that have annotated with {@link Microservice}
     * @param o object to check
     * @return null if bean is not created from {@link Microservice} or {@link Type} if created from interface
     */
    public static Type isMicroserviceAnnotation(Object o) {
        try {
            for (AnnotatedType typeObj : o.getClass().getAnnotatedInterfaces()) {
                Type type = typeObj.getType();

                Field field = type.getClass().getDeclaredField("annotationData");
                field.setAccessible(true);
                Object o1 = field.get(type);

                Field field2 = o1.getClass().getDeclaredField("annotations");
                field2.setAccessible(true);
                Object o2 = field2.get(o1);

                if (o2 instanceof LinkedHashMap) {
                    if (((LinkedHashMap) o2).containsKey(Microservice.class)) {
                        return type;
                    }
                }
            }

        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Type type = isMicroserviceAnnotation(bean);

        if (type != null) {
            try {
                String interfaceName = ((Class) type).getName();
                bean = MicroserviceInterfaceImpFactory.create(Class.forName(interfaceName));
                logger.info("Find new microservice bean {}", interfaceName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}