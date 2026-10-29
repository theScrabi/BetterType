package org.schabi.bettertype

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.languagetool.JLanguageTool
import org.languagetool.Languages

@RunWith(AndroidJUnit4::class)
class LtSmokeTest {

    private fun check(code: String, text: String): List<String> {
        LtLanguages.all
        val lt = JLanguageTool(Languages.getLanguageForShortCode(code))
        val ids = lt.check(text).map { it.rule.id }
        Log.i("LtSmokeTest", "$code: $text -> $ids")
        return ids
    }

    @Test fun english() = assertTrue(check("en-US", "This are a test.").isNotEmpty())
    @Test fun french() = assertTrue(check("fr", "Il sont partis.").isNotEmpty())
    @Test fun spanish() = assertTrue(check("es", "Los niño juegan.").isNotEmpty())
    @Test fun german() = assertTrue(check("de-DE", "Ich habe einen Auto gekauft.").isNotEmpty())
}
