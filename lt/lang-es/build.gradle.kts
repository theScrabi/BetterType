plugins {
    `java-library`
}

dependencies {
    api(project(":lt:core"))
    api("org.softcatala:spanish-pos-dict")

    testImplementation(testFixtures(project(":lt:core")))
}
