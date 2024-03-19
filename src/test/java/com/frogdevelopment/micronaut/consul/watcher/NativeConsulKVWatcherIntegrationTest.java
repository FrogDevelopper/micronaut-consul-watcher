package com.frogdevelopment.micronaut.consul.watcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.frogdevelopment.micronaut.consul.ReactorConsulClient;

import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import com.frogdevelopment.micronaut.consul.ConsulTestHelper;

@MicronautTest(contextBuilder = NativeConsulKVWatcherIntegrationTest.CustomContextBuilder.class)
@Property(name = "consul.client.config.format", value = "native")
@Property(name = "consul.watcher.enabled", value = "true")
@Property(name = "micronaut.config-client.enabled", value = "true")
class NativeConsulKVWatcherIntegrationTest extends BaseConsulKVWatcherIntegrationTest {

    public static class CustomContextBuilder extends DefaultApplicationContextBuilder {

        public CustomContextBuilder() {
            updateConsul(ConsulTestHelper.getConsulClient(), "foo", "bar");
        }
    }

    private static final String APPLICATION_PROPERTY_FOO = "my.key.to_be_updated";
    private static final String APPLICATION_PROPERTY_BAR = "an.other.property";

    @Override
    protected void updateConsul(String foo, String bar) {
        updateConsul(consulClient, foo, bar);
    }

    private static void updateConsul(ReactorConsulClient consulClient, String foo, String bar) {
        var fooFuture = consulClient.putValue(ROOT + "application/" + APPLICATION_PROPERTY_FOO, foo);
        var barFuture = consulClient.putValue(ROOT + "application/" + APPLICATION_PROPERTY_BAR, bar);

        fooFuture.then(barFuture).block();
    }

    @Override
    protected void assertInstanceOfWatcher(ConsulKVWatcher consulKVWatcher) {
        assertThat(consulKVWatcher).isExactlyInstanceOf(NativeConsulKVWatcher.class);
    }

}
