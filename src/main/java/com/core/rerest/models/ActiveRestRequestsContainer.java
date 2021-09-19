package com.core.rerest.models;

import com.core.rerest.enums.RestResponseResolutionStrategy;
import javafx.util.Pair;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
@SuppressWarnings("ALL")
public class ActiveRestRequestsContainer {

    private final Map<String,RestResponseSubscriptionList> requestToSubscriptionListMap = new HashMap<>();

    public synchronized Pair<RestResponseResolutionStrategy,CompletableFuture<ResponseEntity<?>>> registerRequestAsObserverAndProvideResponseResolutionStrategy(final String stringifiedRequestEntity) {
        final CompletableFuture<ResponseEntity<?>> completableFuture = new CompletableFuture<>();
        if (requestToSubscriptionListMap.containsKey(stringifiedRequestEntity)) {
            requestToSubscriptionListMap.get(stringifiedRequestEntity).registerObserver(
                    completableFuture
            );
            return new Pair<>(RestResponseResolutionStrategy.AWAIT_RESPONSE,completableFuture);
        }
        log.debug("Inserting New Observable List. Current State : {}",requestToSubscriptionListMap);
        requestToSubscriptionListMap.put(
                stringifiedRequestEntity,
                RestResponseSubscriptionList.registerFirstObserver(completableFuture)
        );
        return new Pair<>(RestResponseResolutionStrategy.PROCEED_WITH_INVOCATION,null);
    }

    @Async
    public synchronized void doNotifyAllObservers(final ResponseEntity<?> responseEntity, final String stringifiedRequestEntity) {
        if(!requestToSubscriptionListMap.containsKey(stringifiedRequestEntity)){
            return;
        }
        requestToSubscriptionListMap.get(stringifiedRequestEntity).notifyAllObservers(responseEntity);
        requestToSubscriptionListMap.remove(stringifiedRequestEntity);
    }

    @Async
    public synchronized void doNotifyAllObservers(final Exception restException, final String stringifiedRequestEntity) {
        if(!requestToSubscriptionListMap.containsKey(stringifiedRequestEntity)){
            return;
        }
        requestToSubscriptionListMap.get(stringifiedRequestEntity).notifyAllObservers(restException);
        requestToSubscriptionListMap.remove(stringifiedRequestEntity);
    }

}
