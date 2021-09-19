package com.core.rerest.models;

import lombok.Data;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("ALL")
@Data
public class RestResponseSubscriptionList {

    private List<CompletableFuture<ResponseEntity<? extends Object>>> registeredObservers;

    public void registerObserver(final CompletableFuture<ResponseEntity<?>> observer) {
        if(Objects.isNull(registeredObservers)){
            registeredObservers =  new ArrayList<>();
        }
        registeredObservers.add(observer);
    }

    public static RestResponseSubscriptionList registerFirstObserver(final CompletableFuture<ResponseEntity<?>> observer) {
        final RestResponseSubscriptionList subscriptionList = new RestResponseSubscriptionList();
        subscriptionList.registerObserver(observer);
        return subscriptionList;
    }

    public void notifyAllObservers(final ResponseEntity<?> responseEntity) {
        registeredObservers.forEach(
                observer -> observer.complete(responseEntity)
        );
    }

    public void notifyAllObservers(final Exception restException) {
        registeredObservers.forEach(
                observer -> observer.completeExceptionally(restException)
        );
    }

}

