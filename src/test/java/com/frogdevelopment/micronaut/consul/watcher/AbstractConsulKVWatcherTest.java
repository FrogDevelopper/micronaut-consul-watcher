package com.frogdevelopment.micronaut.consul.watcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.Watch;
import io.vertx.ext.consul.WatchResult;

@ExtendWith(MockitoExtension.class)
class AbstractConsulKVWatcherTest {

    private class TestConsulKVWatcher extends AbstractConsulKVWatcher<Map<String, Object>> {

        final Logger log = (Logger) LoggerFactory.getLogger(TestConsulKVWatcher.class);

        TestConsulKVWatcher(Environment environment,
                ApplicationEventPublisher<RefreshEvent> eventPublisher,
                ConsulConfiguration consulConfiguration, Vertx vertx,
                ConsulClientOptions consulClientOptions) {
            super(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions);
        }

        @Override
        protected Logger getLogger() {
            return log;
        }

        @Override
        protected Watch<Map<String, Object>> getWatcher(String key, Vertx vertx, ConsulClientOptions consulClientOptions) {
            if (key.contains("application")) {
                return watcherApplication;
            }
            return watcherConsulWatcher;
        }

        @Nonnull
        @Override
        protected Map<String, Object> toProperties(String key, @Nullable Map<String, Object> value) {
            if (value == null) {
                return Collections.emptyMap();
            }
            return value;
        }
    }

    TestConsulKVWatcher consulKVWatcher;

    @Mock
    Environment environment;
    @Mock
    ApplicationEventPublisher<RefreshEvent> eventPublisher;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ConsulConfiguration consulConfiguration;
    @Mock
    Vertx vertx;
    @Mock
    ConsulClientOptions consulClientOptions;

    @Mock
    Watch<Map<String, Object>> watcherApplication;
    @Captor
    ArgumentCaptor<Handler<WatchResult<Map<String, Object>>>> handlerWatcherApplication;
    @Mock
    Watch<Map<String, Object>> watcherConsulWatcher;
    @Captor
    ArgumentCaptor<Handler<WatchResult<Map<String, Object>>>> handlerWatcherConsulWatcher;
    @Captor
    ArgumentCaptor<PropertySource> propertySourceArgumentCaptor;
    @Captor
    ArgumentCaptor<RefreshEvent> refreshEventArgumentCaptor;

    @BeforeEach
    void setup() {
        consulKVWatcher = new TestConsulKVWatcher(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions);

        given(consulConfiguration.getServiceId()).willReturn(Optional.of("consul-watcher"));
        given(consulConfiguration.getConfiguration().getPath()).willReturn(Optional.of("config/test"));

        given(watcherApplication.setHandler(handlerWatcherApplication.capture())).willReturn(watcherApplication);
        given(watcherApplication.start()).willReturn(watcherApplication);

        given(watcherConsulWatcher.setHandler(handlerWatcherConsulWatcher.capture())).willReturn(watcherConsulWatcher);
        given(watcherConsulWatcher.start()).willReturn(watcherConsulWatcher);
    }

    @Test
    void should_updateContext_and_publishChanges() {
        // given
        given(environment.getActiveNames()).willReturn(Set.of("test"));

        Collection<PropertySource> propertySources = new ArrayList<>();
        propertySources.add(PropertySource.of("consul-consul-watcher[test]", Map.of("key_1", "value_1"), 99));
        propertySources.add(PropertySource.of("consul-application", Map.of("key_a", "value_a"), 66));
        given(environment.getPropertySources()).willReturn(propertySources);

        // when
        consulKVWatcher.start();
        var watchResultHandler = handlerWatcherConsulWatcher.getValue();
        watchResultHandler.handle(getWatchResult(Map.of("key_1", "value_1"), Map.of("key_1", "value_2")));

        // then
        then(environment).should(times(2)).addPropertySource(propertySourceArgumentCaptor.capture());
        var propertySource = propertySourceArgumentCaptor.getAllValues().get(1);
        assertThat(propertySource.get("key_1")).isEqualTo("value_2");

        then(eventPublisher).should().publishEvent(refreshEventArgumentCaptor.capture());
        var refreshEvent = refreshEventArgumentCaptor.getValue();
        assertThat(refreshEvent.getSource()).containsEntry("key_1", "value_1");
    }

    @Test
    void should_doNothing_when_resultNoSucceeded() {
        // given
        given(environment.getActiveNames()).willReturn(Set.of());
        var listAppender = getListAppender();

        // when
        consulKVWatcher.start();
        var watchResultHandler = handlerWatcherApplication.getValue();
        watchResultHandler.handle(new WatchResult<>() {
            @Override
            public Map<String, Object> prevResult() {
                return null;
            }

            @Override
            public Map<String, Object> nextResult() {
                return null;
            }

            @Override
            public Throwable cause() {
                return new RuntimeException("For test purpose");
            }

            @Override
            public boolean succeeded() {
                return false;
            }

            @Override
            public boolean failed() {
                return true;
            }
        });

        // then
        then(environment).shouldHaveNoMoreInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
        assertThat(listAppender.list)
                .filteredOn(loggingEvent -> loggingEvent.getLevel().equals(Level.ERROR))
                .hasSize(1)
                .first()
                .satisfies(loggingEvent -> assertSoftly(softAssertions -> {
                    softAssertions.assertThat(loggingEvent.getFormattedMessage())
                            .isEqualTo("An error occurred while listening to config changes for key [config/test/application]");
                    softAssertions.assertThat(loggingEvent.getThrowableProxy().getClassName()).isEqualTo("java.lang.RuntimeException");
                    softAssertions.assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("For test purpose");
                }));
    }

    @Test
    void should_doNothing_when_noDifference() {
        // given
        given(environment.getActiveNames()).willReturn(Set.of());

        // when
        consulKVWatcher.start();
        var watchResultHandler = handlerWatcherApplication.getValue();
        watchResultHandler.handle(getWatchResult(Map.of("key_a", "value_a"), Map.of("key_a", "value_a")));

        // then
        then(environment).shouldHaveNoMoreInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void should_notUpdatePropertySourceNorPublishChanges_when_incompatibleTypeOnDifferences() {
        // given
        given(environment.getActiveNames()).willReturn(Set.of());

        // when
        consulKVWatcher.start();
        var watchResultHandler = handlerWatcherApplication.getValue();
        watchResultHandler.handle(getWatchResult(Map.of("key_int", 1), Map.of("key_int", "1")));

        // then
        then(environment).shouldHaveNoMoreInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void should_keepGoing_when_typeClassAreDifferentButNumbers() {
        // given
        given(environment.getActiveNames()).willReturn(Set.of());

        Collection<PropertySource> propertySources = new ArrayList<>();
        propertySources.add(PropertySource.of("consul-application", Map.of("key_int", 1), 66));
        given(environment.getPropertySources()).willReturn(propertySources);

        // when
        consulKVWatcher.start();
        var watchResultHandler = handlerWatcherApplication.getValue();
        watchResultHandler.handle(getWatchResult(Map.of("key_int", 1), Map.of("key_int", 1.0)));

        // then
        then(environment).should().addPropertySource(propertySourceArgumentCaptor.capture());
        var propertySource = propertySourceArgumentCaptor.getValue();
        assertThat(propertySource.get("key_int")).isEqualTo(1.0);

        then(eventPublisher).should().publishEvent(refreshEventArgumentCaptor.capture());
        var refreshEvent = refreshEventArgumentCaptor.getValue();
        assertThat(refreshEvent.getSource()).containsEntry("key_int", 1);
    }

    @Test
    void should_stopAllWatchers_when_closing() {
        // when
        consulKVWatcher.start();
        consulKVWatcher.stop();

        // then
        then(watcherApplication).should().stop();
        then(watcherConsulWatcher).should().stop();
    }

    @Nonnull
    private WatchResult<Map<String, Object>> getWatchResult(Map<String, Object> prevResult,
            Map<String, Object> nextResult) {
        return new WatchResult<>() {
            @Override
            public Map<String, Object> prevResult() {
                return prevResult;
            }

            @Override
            public Map<String, Object> nextResult() {
                return nextResult;
            }

            @Override
            public Throwable cause() {
                return null;
            }

            @Override
            public boolean succeeded() {
                return true;
            }

            @Override
            public boolean failed() {
                return false;
            }
        };
    }

    private static ListAppender<ILoggingEvent> getListAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(TestConsulKVWatcher.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

}
