plugins {
    `java-library`
}

dependencies {
    api(project(":lt:core"))
    api("de.danielnaber:german-pos-dict")
    api("de.danielnaber:jwordsplitter")
    api("edu.washington.cs.knowitall:openregex")
    api("org.apache.commons:commons-lang3")
    compileOnly("org.jetbrains:annotations")

    testImplementation(testFixtures(project(":lt:core")))
}
