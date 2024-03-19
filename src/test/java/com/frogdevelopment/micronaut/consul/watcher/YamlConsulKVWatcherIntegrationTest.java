package com.frogdevelopment.micronaut.consul.watcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.frogdevelopment.micronaut.consul.ReactorConsulClient;

import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import com.frogdevelopment.micronaut.consul.ConsulTestHelper;

@MicronautTest(contextBuilder = YamlConsulKVWatcherIntegrationTest.CustomContextBuilder.class)
@Property(name = "consul.client.config.format", value = "yaml")
@Property(name = "consul.watcher.enabled", value = "true")
@Property(name = "micronaut.config-client.enabled", value = "true")
class YamlConsulKVWatcherIntegrationTest extends BaseConsulKVWatcherIntegrationTest {

    public static class CustomContextBuilder extends DefaultApplicationContextBuilder {

        public CustomContextBuilder() {
            updateConsul(ConsulTestHelper.getConsulClient(), "foo", "bar");
        }
    }

    private static final String APPLICATION_YAML = "my:\n  key:\n    to_be_updated: %s\n \nan.other.property: %s\n";

    @Override
    protected void updateConsul(String foo, String bar) {
        updateConsul(consulClient, foo, bar);
    }

    private static void updateConsul(ReactorConsulClient consulClient, String foo, String bar) {
        consulClient.putValue(ROOT + "application", String.format(APPLICATION_YAML, foo, bar)).block();
    }

    @Override
    protected void assertInstanceOfWatcher(ConsulKVWatcher consulKVWatcher) {
        assertThat(consulKVWatcher).isExactlyInstanceOf(YamlConsulKVWatcher.class);
    }

}
