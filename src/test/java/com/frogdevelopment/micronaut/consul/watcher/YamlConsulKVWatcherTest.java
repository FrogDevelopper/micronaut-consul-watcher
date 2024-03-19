package com.frogdevelopment.micronaut.consul.watcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.impl.WatchImpl;

@ExtendWith(MockitoExtension.class)
class YamlConsulKVWatcherTest {

    @InjectMocks
    private YamlConsulKVWatcher yamlConsulKVWatcher;

    @Mock
    private Vertx vertx;
    private final ConsulClientOptions consulClientOptions = new ConsulClientOptions();

    @Test
    void should_return_KeyWatcher() {
        // when
        var watcher = yamlConsulKVWatcher.getWatcher("key", vertx, consulClientOptions);

        // then
        assertThat(watcher).isExactlyInstanceOf(WatchImpl.Key.class);
    }

    @Test
    void should_return_emptyMap_when_valueIsNull() {
        // when
        var properties = yamlConsulKVWatcher.toProperties("my_key", null);

        // then
        assertThat(properties).isEmpty();
    }

    @Test
    void should_return_emptyMap_when_noValuePresent() {
        // given
        final var keyValue = new KeyValue();

        // when
        var properties = yamlConsulKVWatcher.toProperties("my_key", keyValue);

        // then
        assertThat(properties).isEmpty();
    }

    @Test
    void should_return_mapFromTheCorrectKeyOnly() {
        // given
        KeyValue keyValue = new KeyValue();
        keyValue.setKey("path/my_application,my_profile");
        keyValue.setValue("key: value_B");

        // when
        var properties = yamlConsulKVWatcher.toProperties("path/my_application,my_profile", keyValue);

        // then
        assertThat(properties).containsExactlyEntriesOf(Map.of("key", "value_B"));
    }

}
