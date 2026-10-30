package org.schabi.bettertype

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageGuesserTest {

    private val all = setOf("de", "en", "fr", "es")

    private fun guess(text: String, preferred: String = "de") = LanguageGuesser.guess(text, all, preferred)

    @Test fun switchesForClearlyOtherLanguage() {
        assertEquals("en", guess("Der ladis and gentleman i am interested in how well this works."))
        assertEquals("en", guess("This are a test."))
        assertEquals("fr", guess("C'est une maison."))
        assertEquals("es", guess("Es una casa bonita."))
        assertEquals("de", guess("Ich habe einen Auto gekauft.", preferred = "en"))
    }

    @Test fun keepsPreferredWhenUnclear() {
        assertEquals("de", guess("ok"))
        assertEquals("de", guess("Haus"))
        assertEquals("de", guess("Das ist gut."))
        assertEquals("en", guess("Meeting", preferred = "en"))
    }

    @Test fun onlyConsidersEnabledLanguages() {
        assertEquals("de", LanguageGuesser.guess("This are a test.", setOf("de", "fr"), "de"))
    }
}
