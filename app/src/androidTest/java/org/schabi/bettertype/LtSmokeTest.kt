package org.schabi.bettertype

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.languagetool.JLanguageTool
import org.languagetool.Languages
import org.languagetool.rules.RuleMatch
import kotlin.system.measureTimeMillis

/** Runs real LanguageTool checks on the device, one known grammar error and one typo per language. */
@RunWith(AndroidJUnit4::class)
class LtSmokeTest {

    private fun tool(code: String): JLanguageTool {
        LtLanguages.all
        lateinit var lt: JLanguageTool
        val initMs = measureTimeMillis { lt = JLanguageTool(Languages.getLanguageForShortCode(code)) }
        val firstCheckMs = measureTimeMillis { lt.check("Warm-up.") }
        Log.i("LtSmokeTest", "$code: init ${initMs}ms, first check ${firstCheckMs}ms")
        return lt
    }

    private fun JLanguageTool.ids(text: String): List<String> = check(text).map { it.rule.id }

    private fun JLanguageTool.typo(text: String): RuleMatch =
        check(text).single { it.rule.isDictionaryBasedSpellingRule }

    @Test fun english() {
        val lt = tool("en-US")
        assertTrue(lt.ids("This are a test.").contains("PLURAL_VERB_AFTER_THIS"))
        assertTrue(lt.typo("I like ths.").suggestedReplacements.contains("this"))
    }

    @Test fun german() {
        val lt = tool("de-DE")
        assertTrue(lt.ids("Ich habe einen Auto gekauft.").contains("DE_AGREEMENT"))
        assertTrue(lt.typo("Das ist ein Tset.").suggestedReplacements.contains("Test"))
        // Compound words are what native Hunspell is needed for.
        assertEquals(emptyList<String>(), lt.ids("Ich habe meinen Haustürschlüssel verloren."))
    }

    @Test fun french() {
        val lt = tool("fr")
        assertTrue(lt.ids("Il sont partis.").contains("ILS_ONT"))
        assertTrue(lt.typo("C'est une maisn.").suggestedReplacements.contains("maison"))
    }

    @Test fun spanish() {
        val lt = tool("es")
        assertTrue(lt.ids("Los niño juegan.").contains("AGREEMENT_DET_NOUN"))
        assertTrue(lt.typo("Es una csa bonita.").suggestedReplacements.contains("casa"))
    }
}
