package com.frogdevelopment.micronaut.consul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Future;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.KeyValueList;

@ExtendWith(MockitoExtension.class)
class ReactorConsulClientTest {

    private static final String KEY = "key";
    private static final String KEY_PREFIX = "keyPrefix";

    @InjectMocks
    private ReactorConsulClient reactorConsulClient;

    @Mock
    private ConsulClient consulClient;

    @Test
    void should_call_consulClient_getValue() {
        // given
        var keyValue = new KeyValue();
        given(consulClient.getValue(KEY)).willReturn(Future.succeededFuture(keyValue));

        // when
        var result = reactorConsulClient.getValue(KEY).block();

        // then
        assertThat(result).isEqualTo(keyValue);
    }

    @Test
    void should_call_consulClient_getValues() {
        // given
        var keyValueList = new KeyValueList();
        given(consulClient.getValues(KEY_PREFIX)).willReturn(Future.succeededFuture(keyValueList));

        // when
        var result = reactorConsulClient.getValues(KEY_PREFIX).block();

        // then
        assertThat(result).isEqualTo(keyValueList);
    }

    @Test
    void should_call_consulClient_putValue() {
        // given
        given(consulClient.putValue(KEY, "value")).willReturn(Future.succeededFuture(true));

        // when
        var result = reactorConsulClient.putValue(KEY, "value").block();

        // then
        assertThat(result).isTrue();
    }

    @Test
    void should_call_consulClient_deleteValue() {
        // given
        given(consulClient.deleteValue(KEY)).willReturn(Future.succeededFuture());

        // when
        var result = reactorConsulClient.deleteValue(KEY).block();

        // then
        assertThat(result).isNull();
    }

    @Test
    void should_call_consulClient_deleteValues() {
        // given
        given(consulClient.deleteValues(KEY_PREFIX)).willReturn(Future.succeededFuture());

        // when
        var result = reactorConsulClient.deleteValues(KEY_PREFIX).block();

        // then
        assertThat(result).isNull();
    }

}
