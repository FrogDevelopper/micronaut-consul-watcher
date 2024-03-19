package com.frogdevelopment.micronaut.consul;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import jakarta.annotation.Nonnull;

import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConsulTestHelper {

    @Nonnull
    public static ReactorConsulClient getConsulClient() {
        return new ReactorConsulClient(ConsulClient.create(Vertx.vertx(), getConsulClientOptions()));
    }

    @Nonnull
    public static ConsulClientOptions getConsulClientOptions() {
        // retrieving property set by docker-compose plugin, or using default values for local tests
        final var consulHost = System.getProperty("consul.host", "localhost");
        final var consulPort = System.getProperty("consul.tcp.8500", "8500");

        final var consulClientOptions = new ConsulClientOptions();
        consulClientOptions.setHost(consulHost);
        consulClientOptions.setPort(Integer.parseInt(consulPort));
        return consulClientOptions;
    }
}
