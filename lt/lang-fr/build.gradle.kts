plugins {
    `java-library`
}

dependencies {
    api(project(":lt:core"))
    api("org.languagetool:french-pos-dict")

    testImplementation(testFixtures(project(":lt:core")))
}
