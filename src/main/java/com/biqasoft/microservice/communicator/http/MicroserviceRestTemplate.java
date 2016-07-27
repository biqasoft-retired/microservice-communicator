/*
 * Copyright (c) 2016. com.biqasoft
 */

package com.biqasoft.microservice.communicator.http;

import com.biqasoft.microservice.communicator.exceptions.InternalSeverErrorProcessingRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * This is template which retry request on error
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

    private final static int DEFAULT_FAIL_AFTER_UNSUCCESS_TIMES = 11;
    private final static int DEFAULT_SLEEP_TIME_BETWEEN_TRYING = 1000;
    private static final Logger logger = LoggerFactory.getLogger(MicroserviceRestTemplate.class);

    private final Boolean tryToReconnect;
    private int triedTimes = 0;

    // number of times to reconnect
    private final int tryToReconnectTimes;
    private final int sleepTimeBetweenTrying;

    /**
     * {@link RestTemplate} that tried to reconnect or error
     */
    public MicroserviceRestTemplate() {
        super(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
        this.tryToReconnect = true;
        this.tryToReconnectTimes = DEFAULT_FAIL_AFTER_UNSUCCESS_TIMES;
        this.sleepTimeBetweenTrying = DEFAULT_SLEEP_TIME_BETWEEN_TRYING;
    }

    public MicroserviceRestTemplate(Boolean tryToReconnect) {
        super(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
        this.tryToReconnect = tryToReconnect;
        this.tryToReconnectTimes = DEFAULT_FAIL_AFTER_UNSUCCESS_TIMES;
        this.sleepTimeBetweenTrying = DEFAULT_SLEEP_TIME_BETWEEN_TRYING;
    }

    /**
     *
     * @param tryToReconnect true if try to retry failed request to microsrevice
     * @param tryToReconnectTimes number of times to try to reconnect
     * @param sleepTimeBetweenTrying sleep in millias to try to reconnect between failed requests
     */
    public MicroserviceRestTemplate(Boolean tryToReconnect, int tryToReconnectTimes, int sleepTimeBetweenTrying) {
        super(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
        this.tryToReconnect = tryToReconnect;
        this.tryToReconnectTimes = tryToReconnectTimes;
        this.sleepTimeBetweenTrying = sleepTimeBetweenTrying;
    }

    @Override
    protected <T> T doExecute(URI url, HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {
        this.responseExtractor = responseExtractor;
        this.url = url;
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
            logger.error("I/O error on {} request for {} {}", method.name(), url.toString(), ex.getMessage(), ex.getCause());
            return doExecuteOnError(url, method, requestCallback, responseExtractor);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Try to execute more HTTP request if {@link #doExecute(URI, HttpMethod, RequestCallback, ResponseExtractor)} request is failed with exception
     */
    private <T> T doExecuteOnError(URI url, HttpMethod method, RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) throws RestClientException {
        boolean exitLoop = false;
        ClientHttpResponse response = null;
        ClientHttpRequest request = null;

        while (!exitLoop) {
            triedTimes++;

            if (tryToReconnect == null || !tryToReconnect) {
                exitLoop = true;
            }

            if (triedTimes > tryToReconnectTimes) {
                throw new InternalSeverErrorProcessingRequestException("Exit: Can not make http request, tried=" + triedTimes);
            }

            try {
                request = createRequest(url, method);

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

            } catch (Exception e) {
                try {
                    logger.info("Can not make http request {} {} time={}", request.getURI().toString(), response.getStatusText(), triedTimes);
                } catch (Exception e1) {
                }
            }

            try {
                Thread.sleep(sleepTimeBetweenTrying);
            } catch (InterruptedException e) {
                exitLoop = true;
            }
        }
        throw new InternalSeverErrorProcessingRequestException("Can not make http request");
    }


    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
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
