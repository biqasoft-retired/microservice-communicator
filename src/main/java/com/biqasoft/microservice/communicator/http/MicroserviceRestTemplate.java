/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.http;

import com.biqasoft.microservice.communicator.exceptions.InternalSeverErrorProcessingRequestException;
import com.biqasoft.microservice.communicator.exceptions.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.client.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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

    private URI url;
    private HttpMethod method;
    private RequestCallback requestCallback;
    private ClientHttpRequest clientHttpRequest;
    private ResponseExtractor responseExtractor;

    private static final Logger logger = LoggerFactory.getLogger(MicroserviceRestTemplate.class);

    private final Boolean tryToReconnect;
    private int triedTimes = 0;

    // number of times to reconnect
    private final int tryToReconnectTimes;
    private final int sleepTimeBetweenTrying;

    private String microserviceName;
    private String pathToApiResource;

    private final static int DEFAULT_TRY_TO_RECONNECT_TIMES = 11;
    private final static int DEFAULT_SLEEP_TIME_BETWEEN_TRYING = 1000;

    private int invalidRequestStatusCode = 422;

    public MicroserviceRestTemplate(String microserviceName, String pathToApiResource ) throws URISyntaxException {
        super(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
        this.tryToReconnect = true;
        this.tryToReconnectTimes = DEFAULT_TRY_TO_RECONNECT_TIMES;
        this.sleepTimeBetweenTrying = DEFAULT_SLEEP_TIME_BETWEEN_TRYING;
        this.microserviceName = microserviceName;
        this.pathToApiResource = pathToApiResource;
        this.url = SpringInjectorHelper.getMicroserviceHelper().getLoadBalancedURIByMicroservice(microserviceName, pathToApiResource, sleepTimeBetweenTrying, tryToReconnect);
    }

    /**
     * {@link RestTemplate} that tried to reconnect or error
     *
     * @param tryToReconnect true if try to retry failed request to microsrevice
     * @param tryToReconnectTimes number of times to try to reconnect
     * @param sleepTimeBetweenTrying sleep in millias to try to reconnect between failed requests
     */
    public MicroserviceRestTemplate(Boolean tryToReconnect, int tryToReconnectTimes, int sleepTimeBetweenTrying, String microserviceName, String pathToApiResource ) throws URISyntaxException {
        super(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
        this.tryToReconnect = tryToReconnect;
        this.tryToReconnectTimes = tryToReconnectTimes;
        this.sleepTimeBetweenTrying = sleepTimeBetweenTrying;
        this.microserviceName = microserviceName;
        this.pathToApiResource = pathToApiResource;
        this.url = SpringInjectorHelper.getMicroserviceHelper().getLoadBalancedURIByMicroservice(microserviceName, pathToApiResource, sleepTimeBetweenTrying, tryToReconnect);
    }

    private URI getLoadBalanceUrlForMe(){
        return SpringInjectorHelper.getMicroserviceHelper().getLoadBalancedURIByMicroservice(microserviceName, pathToApiResource, sleepTimeBetweenTrying, tryToReconnect);
    }

    @Override
    protected <T> T doExecute(URI urlFake, HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {
        this.responseExtractor = responseExtractor;
        this.method = method;

        Assert.notNull(url, "'url' must not be null");
        Assert.notNull(method, "'method' must not be null");
        ClientHttpResponse response = null;
        try {
            ClientHttpRequest request = createRequest(url, method);
            if (requestCallback != null) {
                requestCallback.doWithRequest(request);
            }
            response = request.execute();
            handleResponse(url, method, response);
            if (responseExtractor != null) {
                return responseExtractor.extractData(response);
            } else {
                return null;
            }
            // note that we can have IOException and HttpServerErrorException
        } catch (Exception ex) {
            logger.error("I/O error on {} request for {} {}", method.name(), this.url.toString(), ex.getMessage(), ex.getCause());
            return doExecuteOnError(method, requestCallback, responseExtractor);
        } finally {
            if (response != null) {
                response.close();
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
                logger.error("Failed request {} {} tried={}", method.toString(), url.toString(), triedTimes);
                throw new InternalSeverErrorProcessingRequestException("Failed request, tried=" + triedTimes);
            }

            try {
                // try to make request to another another microservice
                url = getLoadBalanceUrlForMe();
                request = createRequest(url, method);

                if (requestCallback != null) {
                    requestCallback.doWithRequest(request);
                }
                response = request.execute();

                if (response != null && response.getStatusCode() != null && response.getStatusCode().value() == invalidRequestStatusCode){
                    throw new InvalidRequestException(response);
                }

                handleResponse(url, method, response);
                if (responseExtractor != null) {
                    return responseExtractor.extractData(response); // success result
                } else {
                    return null;
                }

            } catch (IOException | RestClientException e) {
                try {
                    logger.info("Can not make http request {} {} {} times={}", request.getMethod().toString(), request.getURI().toString(), response.getStatusText(), triedTimes);
                } catch (Exception e1) {
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


    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    /**
     *
     * @return null if no request was be done
     */
    public HttpMethod getMethod() {
        return method;
    }

    public RequestCallback getRequestCallback() {
        return requestCallback;
    }

    public void setRequestCallback(RequestCallback requestCallback) {
        this.requestCallback = requestCallback;
    }

    public ClientHttpRequest getClientHttpRequest() {
        return clientHttpRequest;
    }

    public void setClientHttpRequest(ClientHttpRequest clientHttpRequest) {
        this.clientHttpRequest = clientHttpRequest;
    }

    public ResponseExtractor getResponseExtractor() {
        return responseExtractor;
    }

    public void setResponseExtractor(ResponseExtractor responseExtractor) {
        this.responseExtractor = responseExtractor;
    }
}
