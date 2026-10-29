plugins {
    `java-library`
    `java-test-fixtures`
}

// Upstream ships its test utilities as a Maven test-jar used by the language modules.
// Here all of src/test is the test-fixtures source set, and the core tests run from it.
sourceSets {
    testFixtures {
        java.setSrcDirs(listOf("src/test/java"))
        resources.setSrcDirs(listOf("src/test/resources"))
    }
    test {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}
tasks.test {
    testClassesDirs = sourceSets.testFixtures.get().output.classesDirs
    // Some tests open their resources as files, so put the fixtures' directories ahead of the fixtures jar.
    classpath = sourceSets.testFixtures.get().output + classpath
}

dependencies {
    api(platform("org.languagetool:languagetool-parent:6.8"))
    testFixturesApi(platform("org.languagetool:languagetool-parent:6.8"))

    api("com.carrotsearch:hppc")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.gitlab.dumonts:hunspell")
    api("com.google.guava:guava")
    api("com.google.protobuf:protobuf-java")
    api("com.hankcs:aho-corasick-double-array-trie")
    api("com.optimaize.languagedetector:language-detector")
    api("commons-logging:commons-logging")
    api("commons-validator:commons-validator") {
        exclude(group = "commons-beanutils", module = "commons-beanutils")
        exclude(group = "commons-collections", module = "commons-collections")
        exclude(group = "commons-logging", module = "commons-logging")
    }
    api("io.github.resilience4j:resilience4j-circuitbreaker")
    api("io.github.resilience4j:resilience4j-micrometer")
    api("io.opentelemetry:opentelemetry-api")
    api("io.opentelemetry:opentelemetry-semconv")
    api("io.grpc:grpc-netty-shaded")
    api("io.grpc:grpc-protobuf")
    api("io.grpc:grpc-stub")
    api("io.micrometer:micrometer-registry-prometheus")
    api("io.prometheus:simpleclient")
    api("io.prometheus:simpleclient_guava")
    api("javax.activation:javax.activation-api")
    api("javax.annotation:javax.annotation-api")
    api("javax.measure:unit-api")
    api("javax.xml.bind:jaxb-api")
    api("net.loomchild:segment")
    api("org.apache.commons:commons-lang3")
    api("org.apache.commons:commons-pool2")
    api("org.apache.commons:commons-text")
    api("org.apache.lucene:lucene-backward-codecs")
    api("org.apache.lucene:lucene-core")
    api("org.carrot2:morfologik-fsa")
    api("org.carrot2:morfologik-fsa-builders")
    api(project(":lt:morfologik-speller"))
    api("org.carrot2:morfologik-stemming")
    api("org.glassfish.jaxb:jaxb-runtime")
    api("it.unimi.dsi:fastutil-core")
    api("org.json:json")
    api("org.slf4j:slf4j-api")
    api("tech.units:indriya")
    api("edu.washington.cs.knowitall:openregex")
    api("io.github.java-diff-utils:java-diff-utils")

    compileOnly("org.jetbrains:annotations")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor(platform("org.languagetool:languagetool-parent:6.8"))
    annotationProcessor("org.projectlombok:lombok")
    testFixturesCompileOnly("org.jetbrains:annotations")
    testFixturesCompileOnly("org.projectlombok:lombok")
    testFixturesAnnotationProcessor(platform("org.languagetool:languagetool-parent:6.8"))
    testFixturesAnnotationProcessor("org.projectlombok:lombok")

    testFixturesApi("junit:junit")
    testFixturesApi("org.mockito:mockito-core")
    testFixturesApi("org.awaitility:awaitility")
    testFixturesImplementation("ch.qos.logback:logback-classic")
}
