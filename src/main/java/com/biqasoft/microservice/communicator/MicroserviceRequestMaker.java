package com.biqasoft.microservice.communicator;

import com.biqasoft.microservice.communicator.exceptions.CannotResolveHostException;
import com.biqasoft.microservice.communicator.exceptions.InternalSeverErrorProcessingRequestException;
import com.biqasoft.microservice.communicator.exceptions.InvalidRequestException;
import com.biqasoft.microservice.communicator.http.MicroserviceRestTemplate;
import com.biqasoft.microservice.communicator.interfaceimpl.MicroserviceRequestInterceptor;
import com.biqasoft.microservice.communicator.servicediscovery.MicroserviceHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Nikita on 21.08.2016.
 */
@Component
public class MicroserviceRequestMaker {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(MicroserviceRequestMaker.class);
    public static final String DEFAULT_INTERFACE_PROXY_METHOD = "DEFAULT_INTERFACE_PROXY_METHOD";
    public static final String INTERFACE_IMPLEMENTED = "INTERFACE_IMPLEMENTED";
    public static final String METHOD_PARAMS = "METHOD_PARAMS";

    private static MicroserviceHelper microserviceHelper;

    private static boolean RETURN_NULL_ON_EMPTY_RESPONSE_BODY = true;

    // static field for ResponseEntity<>
    public static Field body = null;

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
        MicroserviceRequestMaker.microserviceHelper = microserviceHelper;
    }

    private static List<MicroserviceRequestInterceptor> microserviceRequestInterceptors = null;

    @Autowired(required = false)
    public void setMicroserviceRequestInterceptors(List<MicroserviceRequestInterceptor> microserviceRequestInterceptors) {
        MicroserviceRequestMaker.microserviceRequestInterceptors = microserviceRequestInterceptors;
    }

    public static void beforeProcessRequest(MicroserviceRestTemplate restTemplate, HttpHeaders httpHeaders) {
        if (microserviceRequestInterceptors != null) {
            microserviceRequestInterceptors.forEach(x -> {
                x.beforeProcessRequest(restTemplate, httpHeaders);
            });
        }
    }

    /**
     * Allow modify request before return from interface
     *
     * @param returnObjectOriginal original(default) object from internal request processing
     * @param payload              request payload
     * @param returnType           return type in interface
     * @param restTemplate         request microserviceRestTemplate
     * @param returnGenericType    return types generic info
     * @param params               additional params
     * @return object that we want to return. object that interface will return
     */
    public static Object onBeforeReturnResultProcessor(Object returnObjectOriginal, Object payload, Class returnType,
                                                       MicroserviceRestTemplate restTemplate, Class[] returnGenericType, Map<String, Object> params) {
        if (microserviceRequestInterceptors == null) {
            return returnObjectOriginal;
        }

        Object returnObject = returnObjectOriginal;
        for (MicroserviceRequestInterceptor microserviceRequestInterceptor : microserviceRequestInterceptors) {
            returnObject = microserviceRequestInterceptor.onBeforeReturnResult(returnObject, returnObjectOriginal,
                    payload, returnType, restTemplate, returnGenericType, params);
        }
        return returnObject;
    }

    /**
     * @param requestTemplate      rest template
     * @param payload           object that will be send in HTTP POST and PUT methods
     * @param returnType        java return type in interface. If generic - collection
     * @param returnGenericType null if return type is not generic
     * @param httpHeaders       http headers
     * @return response from server depend on interface return method or null if remote server has not response body
     */
    public static Object makeRequestToMicroservice(Object payload, Class returnType, MicroserviceRestTemplate requestTemplate, Class[] returnGenericType, Map<String, Object> params,
                                                   HttpHeaders httpHeaders) {
        HttpMethod httpMethod = requestTemplate.getMethod();

        try {
            // if payload not byte[] - use JSON as payload type
            if (!(payload instanceof byte[])) {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            }

            if (microserviceRequestInterceptors != null) {
                microserviceRequestInterceptors.forEach(x -> {
                    x.beforeCreateHttpEntity(requestTemplate, returnType, returnGenericType, httpHeaders);
                });
            }

            HttpEntity<Object> request;
            if (payload == null) {
                request = new HttpEntity<>(httpHeaders);
            } else {
                request = new HttpEntity<>(payload, httpHeaders);
            }

            ResponseEntity<byte[]> responseEntity;
            try {
                // get all responses as byte[] and if we request object - deserialize then
                responseEntity = requestTemplate.exchange(null, requestTemplate.getMethod(), request, byte[].class);
            } catch (InvalidRequestException e) {

                if (returnType.equals(ResponseEntity.class)) {
                    if (e.getClientHttpResponse() != null) {
                        ClientHttpResponse clientHttpResponse = e.getClientHttpResponse();

                        if (clientHttpResponse.getBody() != null) {
                            String body;
                            Scanner s = new Scanner(clientHttpResponse.getBody()).useDelimiter("\\A");
                            body = s.hasNext() ? s.next() : "";

                            if (body == null) {
                                return ResponseEntity.status(clientHttpResponse.getRawStatusCode()).headers(clientHttpResponse.getHeaders());
                            } else {
                                return ResponseEntity.status(clientHttpResponse.getRawStatusCode()).headers(clientHttpResponse.getHeaders()).body(body);
                            }
                        }
                    }
                }
                throw e;
            }

            if (microserviceRequestInterceptors != null) {
                microserviceRequestInterceptors.forEach(x -> {
                    x.afterRequest(requestTemplate, request, responseEntity, returnType, returnGenericType);
                });
            }

            logger.debug("Request to microservice {}", requestTemplate.getLastURI().toString());

            // if we have void in interface as return - return void
            if (returnType.equals(Void.TYPE)) {
                return Void.TYPE;
            }

            // if we request byte[] return immediately
            if (returnType.equals(byte[].class)) {
                return responseEntity.getBody();
            }

            // return ResponseEntity<>
            if (returnType.equals(ResponseEntity.class)) {
                if (!responseEntity.hasBody()) {
                    return responseEntity;
                }
                ReflectionUtils.setField(body, responseEntity, objectMapper.readValue(responseEntity.getBody(), returnGenericType[0]));
                return responseEntity;
            }


            Object o = MicroserviceRequestMaker.onBeforeReturnResultProcessor(responseEntity.getBody(), payload, returnType, requestTemplate, returnGenericType, params);

            if (o == null & !responseEntity.hasBody() && RETURN_NULL_ON_EMPTY_RESPONSE_BODY && !returnType.equals(ResponseEntity.class)) {
                return null;
            }

            return o;

        } catch (Throwable e) {
            if (params != null) {
                Object defaultValue = params.get("HAVE_DEFAULT_VALUE");
                if (defaultValue == Boolean.TRUE) {
                    return getDefaultValue(params);
                }
            }

            if (e instanceof InvalidRequestException) {
                throw (InvalidRequestException) e;
            }

            if (e instanceof CannotResolveHostException) {
                logger.error(e.getMessage(), e);
            }

            logger.error("Can not get bytes from microservice {} {}", httpMethod.toString(), requestTemplate.getLastURI() == null ? "NULL_URL" : requestTemplate.getLastURI().toString(), e);
            throw new InternalSeverErrorProcessingRequestException("Internal error processing. Retry later");
        }
    }

    /**
     * Get default java 8 interface return value
     *
     * @param params params
     * @return return from interface
     */
    private static Object getDefaultValue(Map<String, Object> params) {
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

    private static Map<String, Object> defaultObjectImplProxy = new ConcurrentHashMap<>();


    public List<MicroserviceRequestInterceptor> getMicroserviceRequestInterceptors() {
        if (microserviceRequestInterceptors == null) {
            microserviceRequestInterceptors = new ArrayList<>();
        }

        return microserviceRequestInterceptors;
    }

    public static List<MicroserviceRequestInterceptor> getMicroserviceRequestInterceptorsStaticInternal() {
        if (microserviceRequestInterceptors == null) {
            microserviceRequestInterceptors = new ArrayList<>();
        }

        return microserviceRequestInterceptors;
    }

    public static boolean isReturnNullOnEmptyResponseBody() {
        return RETURN_NULL_ON_EMPTY_RESPONSE_BODY;
    }

    public static void setReturnNullOnEmptyResponseBody(boolean returnNullOnEmptyResponseBody) {
        MicroserviceRequestMaker.RETURN_NULL_ON_EMPTY_RESPONSE_BODY = returnNullOnEmptyResponseBody;
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static MicroserviceHelper getMicroserviceHelper() {
        return microserviceHelper;
    }


}
