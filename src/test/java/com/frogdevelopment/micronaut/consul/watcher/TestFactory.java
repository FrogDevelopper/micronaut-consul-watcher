package com.frogdevelopment.micronaut.consul.watcher;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;

import com.frogdevelopment.micronaut.consul.ConsulTestHelper;

@Factory
public class TestFactory {

    @Singleton
    @Bean(preDestroy = "close")
    ConsulClient consulClient(final Vertx vertx) {
        return ConsulClient.create(vertx, ConsulTestHelper.getConsulClientOptions());
    }
}
