package com.frogdevelopment.micronaut.consul;

import lombok.RequiredArgsConstructor;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Requires;
import io.vertx.core.Future;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.KeyValueList;
import reactor.core.publisher.Mono;

/**
 * Simple wrapper of {@link ConsulClient} to have the methods returning reactor {@link Mono} instead of {@link Future}
 */
@Singleton
@Requires(bean = ConsulClient.class)
@RequiredArgsConstructor
public class ReactorConsulClient {

    private final ConsulClient consulClient;

    private static <T> Mono<T> toMono(final Future<T> future) {
        return Mono.fromCompletionStage(future.toCompletionStage());
    }

    /**
     * @see ConsulClient#getValue(String)
     */
    public Mono<KeyValue> getValue(final String key) {
        return toMono(consulClient.getValue(key));
    }

    /**
     * @see ConsulClient#getValues(String)
     */
    public Mono<KeyValueList> getValues(final String keyPrefix) {
        return toMono(consulClient.getValues(keyPrefix));
    }

    /**
     * @see ConsulClient#putValue(String, String)
     */
    public Mono<Boolean> putValue(final String key, final String value) {
        return toMono(consulClient.putValue(key, value));
    }

    /**
     * @see ConsulClient#deleteValue(String)
     */
    public Mono<Void> deleteValue(final String key) {
        return toMono(consulClient.deleteValue(key));
    }

    /**
     * @see ConsulClient#deleteValues(String)
     */
    public Mono<Void> deleteValues(final String keyPrefix) {
        return toMono(consulClient.deleteValues(keyPrefix));
    }

}
