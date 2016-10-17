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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/15/2016
 *         All Rights Reserved
 */
@Component
public class MicroserviceInterfaceImplBeanDefinition implements BeanDefinitionRegistryPostProcessor, Condition {

    private static final Logger logger = LoggerFactory.getLogger(MicroserviceInterfaceImplBeanDefinition.class);
    private volatile boolean enabled;
    private final Map<String, Object> objectMap = new HashMap<>();

    private void scanPackage(String basePackage) {
        ClassPathScanningCandidateComponentMicroserviceInterfaceProvider provider = new ClassPathScanningCandidateComponentMicroserviceInterfaceProvider();
        Set<BeanDefinition> components = provider.findCandidateComponents(basePackage);
        for (BeanDefinition component : components) {
            String interfaceClassName = component.getBeanClassName();
            try {
                Class interfaceClass = Class.forName(interfaceClassName);
                Object beanSignature = MicroserviceInterface.create(interfaceClass);
                objectMap.put(  interfaceClass.getName() , beanSignature);
                logger.info("Find microservice interface {}", interfaceClassName);
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
                    break;
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
            if (declaredAnnotation != null) {
                findRequireAnnotation = true;
                String[] basePackages = ((EnableMicroserviceCommunicator) declaredAnnotation).basePackages();

                if (basePackages.length > 0) {
                    if (basePackages.length >= 1 && !basePackages[0].equals("")) {
                        for (String basePackage : basePackages) {
                            resolveBasePackage = true;
                            scanPackage(basePackage);
                        }
                    }
                } else {
                    logger.warn("Found @EnableMicroserviceCommunicator but not found basePackage attribute");
                }
                break;
            } else {
                // we have not EnableMicroserviceCommunicator annotation
            }
        }

        // if not found annotation attribute with @EnableMicroserviceCommunicator - try to find and use @ComponentScan annotation
        if (!resolveBasePackage & findRequireAnnotation) {
            String[] strings = tryToExtractComponentScanAnnotationFromConfigurationClasses(configurationClasses);
            if (strings != null) {
                for (String basePackage : strings) {
                    scanPackage(basePackage);
                }
            }
        }

        if (!findRequireAnnotation) {
            logger.info("Not find @EnableMicroserviceCommunicator annotation on configuration class. Skipping scanning interfaces");
        }
        enabled = findRequireAnnotation;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        for (Map.Entry<String, Object> stringObjectEntry : objectMap.entrySet()) {
            configurableListableBeanFactory.registerSingleton(stringObjectEntry.getKey(), stringObjectEntry.getValue());
        }
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return this.enabled;
    }
}
