package com.frogdevelopment.micronaut.consul.watcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.KeyValueList;
import io.vertx.ext.consul.impl.WatchImpl;

@ExtendWith(MockitoExtension.class)
class NativeConsulKVWatcherTest {

    @InjectMocks
    private NativeConsulKVWatcher nativeConsulKVWatcher;

    @Mock
    private Vertx vertx;
    private final ConsulClientOptions consulClientOptions = new ConsulClientOptions();

    @Test
    void should_return_KeyPrefixWatcher() {
        // when
        var watcher = nativeConsulKVWatcher.getWatcher("key", vertx, consulClientOptions);

        // then
        assertThat(watcher).isExactlyInstanceOf(WatchImpl.KeyPrefix.class);
    }

    @Test
    void should_return_emptyMap_when_valueIsNull() {
        // when
        var properties = nativeConsulKVWatcher.toProperties("my_key", null);

        // then
        assertThat(properties).isEmpty();
    }

    @Test
    void should_return_emptyMap_when_noValuePresent() {
        // given
        final var keyValueList = new KeyValueList();

        // when
        var properties = nativeConsulKVWatcher.toProperties("my_key", keyValueList);

        // then
        assertThat(properties).isEmpty();
    }

    @Test
    void should_return_mapFromTheCorrectKeyOnly() {
        // given
        List<KeyValue> keyValues = new ArrayList<>();
        KeyValue keyValueA = new KeyValue();
        keyValueA.setKey("path/my_application/key");
        keyValueA.setValue("value_A");
        keyValues.add(keyValueA);

        KeyValue keyValueB = new KeyValue();
        keyValueB.setKey("path/my_application,my_profile/key");
        keyValueB.setValue("value_B");
        keyValues.add(keyValueB);

        var keyValueList = new KeyValueList();
        keyValueList.setList(keyValues);

        // when
        var properties = nativeConsulKVWatcher.toProperties("path/my_application,my_profile", keyValueList);

        // then
        assertThat(properties).containsExactlyEntriesOf(Map.of("key", "value_B"));
    }
}
