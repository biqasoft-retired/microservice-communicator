package com.biqasoft.microservice.communicator.internal;

import com.biqasoft.microservice.communicator.exceptions.InternalSeverErrorProcessingRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.biqasoft.microservice.communicator.MicroserviceRequestMaker.DEFAULT_INTERFACE_PROXY_METHOD;
import static com.biqasoft.microservice.communicator.MicroserviceRequestMaker.INTERFACE_IMPLEMENTED;
import static com.biqasoft.microservice.communicator.MicroserviceRequestMaker.METHOD_PARAMS;

/**
 * Created by ya on 4/8/2017.
 */
@Service
public class DefaultReturnValueService {

    private final Map<String, Object> defaultObjectImplProxy = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(DefaultReturnValueService.class);

    /**
     * Get default java 8 interface return value
     *
     * @param params params
     * @return return from interface
     */
    public Object getDefaultValue(Map<String, Object> params) {
        Method method = (Method) params.get(DEFAULT_INTERFACE_PROXY_METHOD);
        Class<?> interfaceToExtend = (Class<?>) params.get(INTERFACE_IMPLEMENTED);
        Object[] methodParams = (Object[]) params.get(METHOD_PARAMS);

        Object defaultInterfaceProxy = defaultObjectImplProxy.computeIfAbsent(interfaceToExtend.toString(),
                x -> Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                        new Class[]{interfaceToExtend}, (Object proxy2, Method method2, Object[] arguments2) -> null));

        try {
            Method defaultJava8Method = interfaceToExtend.getMethod(method.getName(), method.getParameterTypes());
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            field.setAccessible(true);
            MethodHandles.Lookup lookup = (MethodHandles.Lookup) field.get(null);
            return lookup.unreflectSpecial(defaultJava8Method, defaultJava8Method.getDeclaringClass()).bindTo(defaultInterfaceProxy).invokeWithArguments(methodParams);
        } catch (Throwable throwable) {
            logger.error("Can not execute java 8 default interface method", throwable);
            throw new InternalSeverErrorProcessingRequestException("Internal error processing. Retry later");
        }
    }

}
