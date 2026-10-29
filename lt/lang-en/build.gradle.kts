plugins {
    `java-library`
}

dependencies {
    api(project(":lt:core"))
    api("edu.washington.cs.knowitall:opennlp-chunk-models")
    api("edu.washington.cs.knowitall:opennlp-postag-models")
    api("edu.washington.cs.knowitall:opennlp-tokenize-models")
    api("org.apache.opennlp:opennlp-tools")
    api("org.languagetool:english-pos-dict")

    testImplementation(testFixtures(project(":lt:core")))
}
