package com.frogdevelopment.micronaut.consul.watcher;

import static io.micronaut.context.env.Environment.DEFAULT_NAME;
import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.DEFAULT_PATH;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.Watch;
import io.vertx.ext.consul.WatchResult;

@RequiredArgsConstructor
abstract class AbstractConsulKVWatcher<V> implements ConsulKVWatcher {

    private final Environment environment;
    private final ApplicationEventPublisher<RefreshEvent> eventPublisher;
    private final ConsulConfiguration consulConfiguration;
    private final Vertx vertx;
    private final ConsulClientOptions consulClientOptions;

    protected final Map<String, Watch<V>> watchers = new HashMap<>();

    private boolean isStarted;

    protected abstract Logger getLogger();

    @Override
    public synchronized void start() {
        if (!isStarted) {
            getLogger().info("Monitoring Consul changes");
            final var applicationName = consulConfiguration.getServiceId().orElseThrow();
            final var configurationPath = getConfigurationPath();

            // Configuration shared by all applications
            final var commonConfigPath = configurationPath + DEFAULT_NAME;
            addKeyWatcher(commonConfigPath);

            // Application-specific configuration
            final var applicationSpecificPath = configurationPath + applicationName;
            addKeyWatcher(applicationSpecificPath);

            for (final var activeName : environment.getActiveNames()) {
                // Configuration shared by all applications by active environments
                addKeyWatcher(toProfiledPath(commonConfigPath, activeName));
                // Application-specific configuration by active environments
                addKeyWatcher(toProfiledPath(applicationSpecificPath, activeName));
            }

            isStarted = true;
        }
    }

    private String getConfigurationPath() {
        return consulConfiguration.getConfiguration().getPath()
                .map(path -> {
                    if (!path.endsWith("/")) {
                        path += "/";
                    }

                    return path;
                })
                .orElse(DEFAULT_PATH);
    }

    private static String toProfiledPath(final String resource, final String activeName) {
        return resource + "," + activeName;
    }

    private void addKeyWatcher(final String key) {
        getLogger().debug("Watching [{}] in the KV store ", key);
        this.watchers.put(key, getWatcher(key, vertx, consulClientOptions)
                .setHandler(handle(key))
                .start());
    }

    protected abstract Watch<V> getWatcher(final String key, Vertx vertx, ConsulClientOptions consulClientOptions);

    private Handler<WatchResult<V>> handle(final String key) {
        return event -> {
            if (event.succeeded()) {
                // prevResult is null on the watcher registering return
                // and nextResult contains the current data
                if (event.prevResult() != null) {
                    try {
                        handleChanges(key, event.prevResult(), event.nextResult());
                    } catch (final Exception e) {
                        onError(key, e);
                    }
                }
            } else {
                onError(key, event.cause());
            }
        };
    }

    @Nonnull
    protected abstract Map<String, Object> toProperties(final String key, @Nullable final V value);

    private synchronized void handleChanges(final String key, @Nonnull final V previous, @Nonnull final V next) {
        final var previousProperties = toProperties(key, previous);
        final var nextProperties = toProperties(key, next);
        try {
            final var difference = Maps.difference(previousProperties, nextProperties);
            if (!difference.areEqual()) {
                checkClassesTypeOnDifference(difference);
                updatePropertySource(key, nextProperties);
                publishDifferences(difference);
            }
        } catch (final Exception e) {
            getLogger().error("Unable to apply configuration changes for key={}, previous={} and next={}", key, previous, next, e);
        }
    }

    private void checkClassesTypeOnDifference(@Nonnull final MapDifference<String, Object> difference) {
        for (final var entry : difference.entriesDiffering().entrySet()) {
            final var leftValue = entry.getValue().leftValue();
            final var rightValue = entry.getValue().rightValue();
            if (leftValue != null && rightValue != null) {
                final var leftClass = leftValue.getClass();
                final var rightClass = rightValue.getClass();
                if (areClassesTypeIncompatible(leftClass, rightClass)) {
                    throw new IllegalStateException(String.format("Incompatible type for %s: [%s] <-> [%s]", entry.getKey(), leftClass, rightClass));
                }
            }
        }
    }

    private boolean areClassesTypeIncompatible(final Class<?> leftClass, final Class<?> rightClass) {
        if (leftClass.equals(rightClass)) {
            return false;
        }

        // Micronaut will handle the conversion between then if needed
        return isNotNumber(leftClass) || isNotNumber(rightClass);

        // maybe later some other check will come
    }

    private static boolean isNotNumber(final Class<?> clazz) {
        return !Number.class.isAssignableFrom(clazz);
    }

    private void updatePropertySource(final String key, @Nonnull final Map<String, Object> nextProperties) {
        getLogger().debug("Updating context with new configuration from [{}]", key);

        final var sourceName = resolvePropertySourceName(key);
        final var propertySourceName = ConsulClient.SERVICE_ID + '-' + sourceName;
        final var updatedPropertySources = new ArrayList<PropertySource>();
        for (final var propertySource : environment.getPropertySources()) {
            if (propertySource.getName().equals(propertySourceName)) {
                // creating a new PropertySource with new values but keeping the order
                updatedPropertySources.add(PropertySource.of(propertySourceName, nextProperties, propertySource.getOrder()));
            } else {
                updatedPropertySources.add(propertySource);
            }
        }

        updatedPropertySources.stream()
                // /!\ re-setting all the propertySources sorted by Order, to keep precedence
                .sorted(Comparator.comparing(PropertySource::getOrder))
                .forEach(environment::addPropertySource);
    }

    private String resolvePropertySourceName(final String key) {
        final var configurationPath = getConfigurationPath();
        final var propertySourceName = key.replace(configurationPath, "");

        final var tokens = propertySourceName.split(",");
        if (tokens.length == 1) {
            return propertySourceName;
        }
        final var name = tokens[0];
        final var envName = tokens[1];

        return name + '[' + envName + ']';
    }

    private void publishDifferences(@Nonnull final MapDifference<String, Object> difference) {
        getLogger().debug("Configuration has been updated, publishing RefreshEvent.");
        final var changes = difference.entriesDiffering()
                .entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().leftValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        eventPublisher.publishEvent(new RefreshEvent(changes));
    }

    private void onError(final String key, final Throwable error) {
        getLogger().error("An error occurred while listening to config changes for key [{}]", key, error);
    }

    @Override
    public synchronized void stop() {
        if (isStarted) {
            watchers.forEach((key, watcher) -> {
                getLogger().debug("Stop watching [{}]", key);
                watcher.stop();
            });
            isStarted = false;
        }
    }

}
