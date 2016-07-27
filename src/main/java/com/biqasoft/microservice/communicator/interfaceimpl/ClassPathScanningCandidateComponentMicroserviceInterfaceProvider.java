/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroserviceRequest;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Find all interfaces that extends {@link MicroserviceRequest} in requested basePackage
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/17/2016
 *         All Rights Reserved
 */
public class ClassPathScanningCandidateComponentMicroserviceInterfaceProvider extends ClassPathScanningCandidateComponentProvider {

    public ClassPathScanningCandidateComponentMicroserviceInterfaceProvider() {
        super(false);
        addIncludeFilter(new AnnotationTypeFilter(MicroserviceRequest.class, false));
    }

    /**
     * Search for interface
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface();
    }
}
