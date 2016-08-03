/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl;

import com.biqasoft.microservice.communicator.interfaceimpl.annotation.EnableMicroserviceCommunicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/15/2016
 *         All Rights Reserved
 */
@Component
public class MicroserviceInterfaceImplBeanDefinition implements BeanDefinitionRegistryPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MicroserviceInterfaceImplBeanDefinition.class);

    private void scanPackage(String basePackage, BeanDefinitionRegistry beanDefinitionRegistry) {
        ClassPathScanningCandidateComponentMicroserviceInterfaceProvider provider = new ClassPathScanningCandidateComponentMicroserviceInterfaceProvider();
        Set<BeanDefinition> components = provider.findCandidateComponents(basePackage);
        for (BeanDefinition component : components) {
            String interfaceClassName = component.getBeanClassName();
            try {
                Object beanSignature = MicroserviceInterfaceImpFactory.create(Class.forName(interfaceClassName));

                String beanName = beanSignature.getClass().getName();
                GenericBeanDefinition definition = new GenericBeanDefinition();
                definition.setScope("singleton");
                definition.setBeanClass(beanSignature.getClass());

                beanDefinitionRegistry.registerBeanDefinition(beanName, definition);
                logger.debug("Find microservice interface {}", interfaceClassName);
            } catch (Exception e) {
                logger.error("Error init dynamic microservice {}", interfaceClassName, e);
            }
        }

    }

    private String[] tryToExtractComponentScanAnnotationFromConfigurationClasses(List<Class> classes) {
        for (Class aClass : classes) {
            Annotation annotation = AnnotationUtils.getAnnotation(aClass, ComponentScan.class);

            String[] basePackage = (String[]) AnnotationUtils.getValue(annotation, "basePackage");
            if (basePackage != null && basePackage.length > 0) {
                return basePackage;
            }

            basePackage = (String[]) AnnotationUtils.getValue(annotation, "value");
            if (basePackage != null && basePackage.length > 0) {
                return basePackage;
            }

        }
        return null;
    }

    private List<Class> getSpringConfigurationClassesByBeanNames(String[] strings, BeanDefinitionRegistry beanDefinitionRegistry) {
        List<Class> classes = new ArrayList<>();

        for (String beanDefinitionName : strings) {
            BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanDefinitionName);

            // this is a signature @Configuration bean
            if ("full".equals(beanDefinition.getAttribute("org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass"))) {
                logger.debug("Find configuration file {}", beanDefinitionName);
                try {
                    Class aClass = Class.forName(beanDefinition.getBeanClassName());
                    classes.add(aClass);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return classes;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        boolean findRequireAnnotation = false;
        boolean resolveBasePackage = false;
        List<Class> configurationClasses = getSpringConfigurationClassesByBeanNames(beanDefinitionRegistry.getBeanDefinitionNames(), beanDefinitionRegistry);

        for (Class aClass : configurationClasses) {
            Annotation declaredAnnotation = aClass.getDeclaredAnnotation(EnableMicroserviceCommunicator.class);
            if (declaredAnnotation != null && declaredAnnotation instanceof EnableMicroserviceCommunicator) {
                findRequireAnnotation = true;
                String[] basePackages = ((EnableMicroserviceCommunicator) declaredAnnotation).basePackages();

                if (basePackages != null && basePackages.length > 0) {
                    if (basePackages.length >= 1 && !basePackages[0].equals("")) {
                        for (String basePackage : basePackages) {
                            resolveBasePackage = true;
                            scanPackage(basePackage, beanDefinitionRegistry);
                        }
                    }
                } else {
                    logger.warn("Found @EnableMicroserviceCommunicator but not found basePackage attribute");
                }
            } else {
                // we have not EnableMicroserviceCommunicator annotation
            }
        }

        // if not found annotation attribute with @EnableMicroserviceCommunicator - try to find and use @ComponentScan annotation
        if (!resolveBasePackage) {
            String[] strings = tryToExtractComponentScanAnnotationFromConfigurationClasses(configurationClasses);
            if (strings != null) {
                for (String basePackage : strings) {
                    scanPackage(basePackage, beanDefinitionRegistry);
                }
            }
        }

        if (!findRequireAnnotation) {
            logger.info("Not find @EnableMicroserviceCommunicator annotation on configuration class. Skipping scanning interfaces");
        }

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

}
