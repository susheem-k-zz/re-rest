package com.core.rerest.service;

import com.core.rerest.enums.RestResponseResolutionStrategy;
import com.core.rerest.models.ActiveRestRequestsContainer;
import com.core.rerest.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@SuppressWarnings("ALL")
public class StatefulRestTemplate {

    private final ReentrantLock lock = new ReentrantLock(true);

    private final RestTemplate restTemplate;

    private final ActiveRestRequestsContainer requestsContainer;

    @Autowired
    public StatefulRestTemplate(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.requestsContainer = new ActiveRestRequestsContainer();
    }

    public <T> ResponseEntity<T> doGet(final String url, final MultiValueMap<String, String> queryParams,
                                       final MultiValueMap<String, String> headers, final Class<T> responseType)
            throws ExecutionException, InterruptedException {
        final URI uri = UriComponentsBuilder.fromHttpUrl(url).queryParams(queryParams).build().toUri();
        final RequestEntity<Object> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, uri);
        final Pair<RestResponseResolutionStrategy, CompletableFuture<ResponseEntity<?>>>
                resolutionStrategyAndResponseHandlePair =
                acquireLockAndUpsertTheRequestToContainerAndGetResolutionStrategy(requestEntity);
        return handleResolutionStrategyAndGetResponse(requestEntity, resolutionStrategyAndResponseHandlePair,
                                                      responseType);
    }

    private <T> ResponseEntity<T> handleResolutionStrategyAndGetResponse(final RequestEntity<Object> requestEntity,
                                                                         final Pair<RestResponseResolutionStrategy, CompletableFuture<ResponseEntity<?>>> resolutionStrategyAndResponseHandlePair,
                                                                         final Class<T> clazz)
            throws ExecutionException, InterruptedException {
        if (resolutionStrategyAndResponseHandlePair.getKey() ==
            RestResponseResolutionStrategy.PROCEED_WITH_INVOCATION) {
            return doProceedAndNotifyObservers(requestEntity, clazz);
        }
        return (ResponseEntity<T>) resolutionStrategyAndResponseHandlePair.getValue().get();
    }

    private <T> ResponseEntity<T> doProceedAndNotifyObservers(final RequestEntity<Object> requestEntity,
                                                              final Class<T> clazz) {
        final ResponseEntity<T> responseEntity = restTemplate.exchange(requestEntity, clazz);
        try {
            requestsContainer.doNotifyAllObservers(responseEntity, JsonUtil.convertToJsonString(requestEntity));
        } catch (JsonProcessingException e) {
            log.warn("Stringification Fail : Message : {}", e.getMessage());
        }
        return responseEntity;
    }

    private Pair<RestResponseResolutionStrategy, CompletableFuture<ResponseEntity<?>>> submitRequestToContainerAndGetResolutionStrategy(
            final RequestEntity<Object> requestEntity) {
        final String stringifiedRequest;
        try {
            stringifiedRequest = JsonUtil.convertToJsonString(requestEntity);
            Assert.notNull(stringifiedRequest, "Null Stringified Request after Conversion");
        } catch (final JsonProcessingException e) {
            log.warn("Stringification Fail : Message : {}", e.getMessage());
            return new Pair<>(RestResponseResolutionStrategy.PROCEED_WITH_INVOCATION, null);
        }

        return requestsContainer.registerRequestAsObserverAndProvideResponseResolutionStrategy(stringifiedRequest);
    }

    private Pair<RestResponseResolutionStrategy, CompletableFuture<ResponseEntity<?>>> acquireLockAndUpsertTheRequestToContainerAndGetResolutionStrategy(
            final RequestEntity<Object> requestEntity) throws InterruptedException {
        boolean isLockAcquired = lock.tryLock(10, TimeUnit.MICROSECONDS);
        if (isLockAcquired) {
            try {
                return this.submitRequestToContainerAndGetResolutionStrategy(requestEntity);
            } finally {
                lock.unlock();
            }

        } else {
            return new Pair<>(RestResponseResolutionStrategy.PROCEED_WITH_INVOCATION, null);
        }
    }

}
