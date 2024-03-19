plugins {
    id("io.micronaut.minimal.library") version "4.3.4"
}

group = "com.frogdevelopment.micronaut.consul"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val lombokVersion = "1.18.24"
    val vertxConsul = "4.4.0"
    val commonsCollections4 = "4.4"
    val commonsLang3 = "3.12.0"
    val guava = "33.0.0-jre"
    val awaitility = "4.2.0"

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    // Micronaut processor defined after Lombok
    annotationProcessor("io.micronaut:micronaut-inject-java")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")

    implementation("org.yaml:snakeyaml")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("io.micronaut.reactor:micronaut-reactor")
    implementation("io.micronaut.discovery:micronaut-discovery-client")
    implementation("io.vertx:vertx-consul-client:${vertxConsul}")
    implementation("org.apache.commons:commons-collections4:${commonsCollections4}")
    implementation("org.apache.commons:commons-lang3:${commonsLang3}")
    implementation("com.google.guava:guava:${guava}")

    testCompileOnly("org.projectlombok:lombok:$lombokVersion")

    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation("ch.qos.logback:logback-classic")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.yaml:snakeyaml")
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.frogdevelopment.micronaut.*")
    }
}
