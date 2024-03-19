@Configuration
@RequiresConsul
@Requires(property = "consul.watcher.enabled", value = "true", defaultValue = "false")
package com.frogdevelopment.micronaut.consul.watcher;

import io.micronaut.context.annotation.Configuration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.discovery.consul.condition.RequiresConsul;
