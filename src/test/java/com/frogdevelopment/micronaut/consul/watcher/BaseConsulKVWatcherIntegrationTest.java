package com.frogdevelopment.micronaut.consul.watcher;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.frogdevelopment.micronaut.consul.ReactorConsulClient;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.context.scope.Refreshable;

@Tag("integrationTest")
abstract class BaseConsulKVWatcherIntegrationTest {

    protected static final String ROOT = "test/";

    @Inject
    protected ReactorConsulClient consulClient;

    @Inject
    BeanContext beanContext;

    @Inject
    private ConsulKVWatcher consulKVWatcher;

    protected abstract void updateConsul(String foo, String bar) throws ExecutionException, InterruptedException;

    protected abstract void assertInstanceOfWatcher(ConsulKVWatcher consulKVWatcher);

    @BeforeEach
    void setUp() {
        consulKVWatcher.start();
    }

    @AfterEach
    void cleanUp() {
        consulKVWatcher.stop();
        consulClient.deleteValues(ROOT).block();
    }

    @Test
    void should_refresh_only_updated_property() throws ExecutionException, InterruptedException {
        // given
        assertInstanceOfWatcher(consulKVWatcher);

        assertSoftly(softAssertions -> {
            var refreshableBean = beanContext.createBean(RefreshableBean.class);
            softAssertions.assertThat(refreshableBean.keyToBeUpdated).isEqualTo("foo");
            softAssertions.assertThat(refreshableBean.otherKey).isEqualTo("bar");
        });

        // when
        var randomFoo = RandomStringUtils.randomAlphanumeric(10);
        updateConsul(randomFoo, "bar");

        // then
        Awaitility.with().pollDelay(1, TimeUnit.SECONDS).await().untilAsserted(() -> assertSoftly(softAssertions -> {
            var refreshedBean = beanContext.getBean(RefreshableBean.class);
            softAssertions.assertThat(refreshedBean.keyToBeUpdated).isEqualTo(randomFoo);
            softAssertions.assertThat(refreshedBean.otherKey).isEqualTo("bar");
        }));
    }

    @Refreshable
    public static class RefreshableBean {

        @Value("${my.key.to_be_updated:foo}")
        public String keyToBeUpdated;

        @Value("${an.other.property:bar}")
        public String otherKey;

    }
}
