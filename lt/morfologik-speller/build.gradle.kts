plugins {
    `java-library`
}

dependencies {
    api(platform("org.languagetool:languagetool-parent:6.8"))
    api("org.carrot2:morfologik-stemming")
}
