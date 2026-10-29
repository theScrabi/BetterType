package org.schabi.bettertype

import org.languagetool.Language
import org.languagetool.Languages
import org.languagetool.rules.spelling.hunspell.AndroidHunspellDictionary

/**
 * LanguageTool normally finds languages via META-INF/org/languagetool/language-module.properties,
 * one file per module; an APK can hold only one, so the classes from those files are listed here.
 */
object LtLanguages {
    private val CLASSES = listOf(
        "org.languagetool.language.English",
        "org.languagetool.language.AmericanEnglish",
        "org.languagetool.language.BritishEnglish",
        "org.languagetool.language.AustralianEnglish",
        "org.languagetool.language.CanadianEnglish",
        "org.languagetool.language.NewZealandEnglish",
        "org.languagetool.language.SouthAfricanEnglish",
        "org.languagetool.language.German",
        "org.languagetool.language.GermanyGerman",
        "org.languagetool.language.AustrianGerman",
        "org.languagetool.language.SwissGerman",
        "org.languagetool.language.French",
        "org.languagetool.language.CanadianFrench",
        "org.languagetool.language.SwissFrench",
        "org.languagetool.language.BelgianFrench",
        "org.languagetool.language.Spanish",
        "org.languagetool.language.SpanishVoseo",
    )

    val all: List<Language> by lazy {
        // OpenNLP (English chunker) uses XMLReaderFactory, which on Android needs the SAX driver named explicitly.
        System.setProperty("org.xml.sax.driver", "org.apache.harmony.xml.ExpatReader")
        AndroidHunspellDictionary.install()
        CLASSES.map(Languages::getOrAddLanguageByClassName)
    }
}
