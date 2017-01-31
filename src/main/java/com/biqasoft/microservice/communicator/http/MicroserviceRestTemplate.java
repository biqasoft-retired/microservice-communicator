/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.http;

import com.biqasoft.microservice.communicator.MicroserviceRequestMaker;
import com.biqasoft.microservice.communicator.exceptions.InternalSeverErrorProcessingRequestException;
import com.biqasoft.microservice.communicator.exceptions.InvalidRequestException;
import com.biqasoft.microservice.communicator.interfaceimpl.MicroserviceRequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is template which retry request on error
 * Create this object per every request.
 * Not thread safe
 *
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 6/5/2016
 *         All Rights Reserved
 */
public class MicroserviceRestTemplate extends RestTemplate {

    private URI lastURI = null;

    private final HttpMethod method;
    private static final Logger logger = LoggerFactory.getLogger(MicroserviceRestTemplate.class);

    private final Boolean tryToReconnect;
    private int triedTimes = 0;

    private boolean https;

    // number of times to reconnect
    private final int tryToReconnectTimes;
    private final int sleepTimeBetweenTrying;

    private final String microserviceName;
    private final String pathToApiResource;

    private final static Set<Integer> defaultInvalidRequestStatusCode;

    private static List<HttpMessageConverter<?>> messageConverters;
    private static HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory;
    static {
        // init default bad response codes
        defaultInvalidRequestStatusCode = new HashSet<>();
        defaultInvalidRequestStatusCode.add(422); // unprocessable entity
        defaultInvalidRequestStatusCode.add(401); // unauthorized
        defaultInvalidRequestStatusCode.add(403); // access denied

        // default converters
        httpComponentsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        messageConverters = new ArrayList<>();
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());
    }

    private static ResponseErrorHandler responseErrorHandler = new ResponseErrorHandler();

    /**
     * {@link RestTemplate} that tried to reconnect or error
     *
     * @param tryToReconnect         true if try to retry failed request to microsrevice
     * @param tryToReconnectTimes    number of times to try to reconnect
     * @param microserviceName microservice name
     * @param pathToApiResource pathToApiResource
     * @param httpMethod http method
     * @param sleepTimeBetweenTrying sleep in millias to try to reconnect between failed requests
     * @param https    use http or https
     * @throws URISyntaxException exception
     */
    public MicroserviceRestTemplate(Boolean tryToReconnect, int tryToReconnectTimes, int sleepTimeBetweenTrying, String microserviceName, String pathToApiResource, HttpMethod httpMethod, boolean https) throws URISyntaxException {
        super(messageConverters);
        Assert.notNull(httpMethod, "'method' must not be null");
        this.setRequestFactory(httpComponentsClientHttpRequestFactory);
        this.tryToReconnect = tryToReconnect;
        this.tryToReconnectTimes = tryToReconnectTimes;
        this.sleepTimeBetweenTrying = sleepTimeBetweenTrying;
        this.microserviceName = microserviceName;
        this.pathToApiResource = pathToApiResource;
        this.method = httpMethod;
        this.https = https;
        this.setErrorHandler(responseErrorHandler);
        microserviceRequestInterceptors = MicroserviceRequestMaker.getMicroserviceRequestInterceptorsStaticInternal();
    }

    private URI getLoadBalanceUrlForMe() {
        // allow to use just as http rest client
        if (microserviceName.startsWith("http://") || microserviceName.startsWith("https://")){
            return URI.create(microserviceName + pathToApiResource);
        }

        // use load-balancer
        return SpringContextAware.getMicroserviceHelper().getLoadBalancedURIByMicroservice(microserviceName, pathToApiResource, sleepTimeBetweenTrying, tryToReconnect, https);
    }

    private static List<MicroserviceRequestInterceptor> microserviceRequestInterceptors = null;

    private static void onException(MicroserviceRestTemplate restTemplate, URI uri, Exception httpHeaders){
        if (microserviceRequestInterceptors != null) {
            microserviceRequestInterceptors.forEach(x -> {
                x.onException(restTemplate,uri, httpHeaders);
            });
        }
    }

    @Override
    protected <T> T doExecute(URI urlFake, HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {
        lastURI = getLoadBalanceUrlForMe();
        Assert.notNull(lastURI, "'url' must not be null");

        if (microserviceRequestInterceptors != null) {
            microserviceRequestInterceptors.forEach(x -> {
                x.beforeRequest(this, lastURI);
            });
        }

        ClientHttpResponse response = null;
        try {
            ClientHttpRequest request = createRequest(lastURI, method);
            if (requestCallback != null) {
                requestCallback.doWithRequest(request);
            }
            response = request.execute();
            handleResponse(lastURI, method, response);
            if (responseExtractor != null) {
                return responseExtractor.extractData(response);
            } else {
                return null;
            }
            // note that we can have IOException and HttpServerErrorException
        } catch (IOException | RestClientException ex) {
            try {
                onException(this, lastURI, ex);
                processInvalidRequest(response);
            } catch (IOException e) {
                logger.error("I/O error on {} request for {} {}", method.name(), lastURI.toString(), ex.getMessage(), ex.getCause());

//                TODO: we should close stream after or leak but we want somewhere in catch exception
                if (response != null) {
                    response.close();
                }
            }
            return doExecuteOnError(method, requestCallback, responseExtractor);
        } finally {

        }
    }

    private void processInvalidRequest(ClientHttpResponse response) throws IOException {
        if (response != null && response.getStatusCode() != null) {
            if (defaultInvalidRequestStatusCode.contains(response.getRawStatusCode())) {
                throw new InvalidRequestException(response);
            }
        }
    }

    /**
     * Try to execute more HTTP request if {@link #doExecute(URI, HttpMethod, RequestCallback, ResponseExtractor)} request is failed with exception
     */
    private <T> T doExecuteOnError(HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {
        boolean exitLoop = false;
        ClientHttpResponse response = null;
        ClientHttpRequest request = null;

        while (!exitLoop) {
            triedTimes++;

            // flag to exit from loop
            if (tryToReconnect == null || !tryToReconnect) {
                exitLoop = true;
            }

            // retry times limit
            if ((triedTimes > tryToReconnectTimes) && tryToReconnect) {
                logger.error("Failed request {} {} tried={}", method.toString(), this.lastURI.toString(), triedTimes);
                throw new InternalSeverErrorProcessingRequestException("Failed request, tried=" + triedTimes);
            }

            try {
                // try to make request to another another microservice
                lastURI = getLoadBalanceUrlForMe();
                Assert.notNull(lastURI, "'url' must not be null");
                request = createRequest(lastURI, method);

                if (microserviceRequestInterceptors != null) {
                    for (MicroserviceRequestInterceptor x : microserviceRequestInterceptors){
                        x.beforeRequest(this, lastURI);
                    }
                }

                if (requestCallback != null) {
                    requestCallback.doWithRequest(request);
                }
                response = request.execute();
                processInvalidRequest(response);

                handleResponse(lastURI, method, response);
                if (responseExtractor != null) {
                    return responseExtractor.extractData(response); // success result
                } else {
                    return null;
                }

            } catch (IOException | RestClientException e) {
                onException(this, lastURI, e);
                try {
                    logger.info("Can not make http request {} {} {} times={}", request.getMethod().toString(), request.getURI().toString(), response.getStatusText(), triedTimes);
                } catch (Exception e1) {
                }finally {
                    if (response != null) {
                        response.close();
                    }
                }
            }

            try {
                Thread.sleep(sleepTimeBetweenTrying);
            } catch (InterruptedException e) {
                exitLoop = true;
            }
        }
        throw new InternalSeverErrorProcessingRequestException("Failed request");
    }

    /**
     * @return null if no request was be done
     */
    public HttpMethod getMethod() {
        return method;
    }

    public String getMicroserviceName() {
        return microserviceName;
    }

    public String getPathToApiResource() {
        return pathToApiResource;
    }

    public URI getLastURI() {
        return lastURI;
    }

}
