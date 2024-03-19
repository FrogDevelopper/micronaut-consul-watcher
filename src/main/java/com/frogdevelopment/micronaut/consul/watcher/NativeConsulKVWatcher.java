package com.frogdevelopment.micronaut.consul.watcher;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.slf4j.Logger;

import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.KeyValueList;
import io.vertx.ext.consul.Watch;

@Slf4j
public class NativeConsulKVWatcher extends AbstractConsulKVWatcher<KeyValueList> {

    @Override
    protected Logger getLogger() {
        return log;
    }

    NativeConsulKVWatcher(final Environment environment,
            final ApplicationEventPublisher<RefreshEvent> eventPublisher,
            final ConsulConfiguration consulConfiguration,
            final Vertx vertx,
            final ConsulClientOptions consulClientOptions) {
        super(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions);
    }

    @Override
    protected Watch<KeyValueList> getWatcher(final String key, final Vertx vertx, final ConsulClientOptions consulClientOptions) {
        return Watch.keyPrefix(key, vertx, consulClientOptions);
    }

    @Nonnull
    @Override
    protected Map<String, Object> toProperties(final String key, @Nullable final KeyValueList value) {
        if (value == null || !value.isPresent()) {
            return Collections.emptyMap();
        }
        return value.getList()
                .stream()
                // excluding not matching KV for other active profile
                .filter(keyValue -> keyValue.getKey().startsWith(key + "/"))
                .collect(Collectors.toMap(this::toPropertyKey, KeyValue::getValue));
    }

    private String toPropertyKey(final KeyValue kv) {
        final var tokens = kv.getKey().split("/");
        return tokens[tokens.length - 1];
    }

}
