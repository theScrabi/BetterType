package org.schabi.bettertype

import android.content.ContentValues
import android.provider.UserDictionary
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionParams
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Talks to Better Type through the real framework path, the way keyboards and text fields do. */
@RunWith(AndroidJUnit4::class)
class SpellCheckerServiceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var previousChecker: String? = null
    private val sentenceResults = LinkedBlockingQueue<Array<SentenceSuggestionsInfo>>()
    private val wordResults = LinkedBlockingQueue<Array<SuggestionsInfo>>()
    private lateinit var session: SpellCheckerSession

    private fun shell(cmd: String): String =
        instrumentation.uiAutomation.executeShellCommand(cmd).use { pfd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().readText().trim()
        }

    @Before
    fun selectBetterType() {
        previousChecker = shell("settings get secure selected_spell_checker")
        shell("settings put secure spell_checker_enabled 1")
        shell("settings put secure selected_spell_checker org.schabi.bettertype/.BetterTypeSpellCheckerService")

        val tsm = instrumentation.targetContext.getSystemService(TextServicesManager::class.java)
        val params = SpellCheckerSessionParams.Builder()
            .setLocale(Locale.US)
            .setSupportedAttributes(
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or
                    SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR or
                    SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS,
            )
            .build()
        session = tsm.newSpellCheckerSession(params, Executors.newSingleThreadExecutor(), object : SpellCheckerSessionListener {
                override fun onGetSuggestions(results: Array<SuggestionsInfo>) { wordResults.put(results) }
                override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>) { sentenceResults.put(results) }
            }) ?: error("No spell checker session; is the spell checker disabled?")
        assertEquals("org.schabi.bettertype", session.spellChecker.packageName)
    }

    @After
    fun restore() {
        session.close()
        if (!previousChecker.isNullOrEmpty() && previousChecker != "null") {
            shell("settings put secure selected_spell_checker $previousChecker")
        }
    }

    @Test
    fun sentenceGetsGrammarAndTypoSpans() {
        val text = "This are a tset."
        session.getSentenceSuggestions(arrayOf(TextInfo(text)), 5)
        val info = sentenceResults.poll(120, TimeUnit.SECONDS)!!.single()

        val spans = (0 until info.suggestionsCount).map { i ->
            Triple(text.substring(info.getOffsetAt(i), info.getOffsetAt(i) + info.getLengthAt(i)),
                info.getSuggestionsInfoAt(i).suggestionsAttributes,
                (0 until info.getSuggestionsInfoAt(i).suggestionsCount).map { info.getSuggestionsInfoAt(i).getSuggestionAt(it) })
        }
        assertTrue(spans.toString(), spans.any { it.second and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR != 0 })
        assertTrue(spans.toString(), spans.any {
            it.first == "tset" && it.second and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0 && "test" in it.third
        })
    }

    @Test
    fun wordLevelSpelling() {
        session.getSentenceSuggestions(arrayOf(TextInfo("Warm up.")), 5)  // first use loads the language
        sentenceResults.poll(120, TimeUnit.SECONDS)
        @Suppress("DEPRECATION")
        session.getSuggestions(arrayOf(TextInfo("house"), TextInfo("hosue")), 5, false)
        val (good, bad) = wordResults.poll(60, TimeUnit.SECONDS)!!
        assertTrue(good.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0)
        assertTrue(bad.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0)
        assertTrue((0 until bad.suggestionsCount).map { bad.getSuggestionAt(it) }.contains("house"))
    }

    @Test
    fun personalDictionaryWordsAreNotTypos() {
        val word = "Qwzblorf"
        // Only the selected spell checker and keyboards may use the user dictionary; the test runs as Better Type.
        val resolver = instrumentation.targetContext.contentResolver
        resolver.insert(UserDictionary.Words.CONTENT_URI, ContentValues().apply {
            put(UserDictionary.Words.WORD, word)
            put(UserDictionary.Words.FREQUENCY, 250)
        })
        try {
            val text = "I like $word a lot."
            session.getSentenceSuggestions(arrayOf(TextInfo(text)), 5)
            val info = sentenceResults.poll(120, TimeUnit.SECONDS)!!.single()
            val flagged = (0 until info.suggestionsCount).map { text.substring(info.getOffsetAt(it), info.getOffsetAt(it) + info.getLengthAt(it)) }
            assertTrue(flagged.toString(), word !in flagged)

            @Suppress("DEPRECATION")
            session.getSuggestions(arrayOf(TextInfo(word)), 5, false)
            val result = wordResults.poll(60, TimeUnit.SECONDS)!!.single()
            assertTrue(result.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0)
        } finally {
            resolver.delete(UserDictionary.Words.CONTENT_URI, "${UserDictionary.Words.WORD}=?", arrayOf(word))
        }
    }
}
