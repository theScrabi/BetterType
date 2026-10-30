package org.schabi.bettertype

import android.service.textservice.SpellCheckerService
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import org.languagetool.JLanguageTool
import org.languagetool.rules.RuleMatch
import org.languagetool.rules.spelling.SpellingCheckRule
import java.util.Locale

/** Android's system spell checker backed by LanguageTool: word spelling plus sentence-level grammar. */
class BetterTypeSpellCheckerService : SpellCheckerService() {

    override fun onCreate() {
        super.onCreate()
        KeepAliveService.start(this)
        val locales = resources.configuration.locales
        for (i in 0 until locales.size()) LtLanguages.warmUp(this, locales[i])
    }

    override fun createSession(): Session = BetterTypeSession()
}

private class BetterTypeSession : SpellCheckerService.Session() {

    // Created on first use (a binder thread), not in onCreate, because loading a language takes seconds.
    private val lt: JLanguageTool by lazy {
        @Suppress("DEPRECATION") val tag = locale
        LtLanguages.checkerFor(if (tag.isNullOrEmpty()) Locale.getDefault() else Locale.forLanguageTag(tag.replace('_', '-')))
    }
    private val speller: SpellingCheckRule? by lazy {
        lt.allActiveRules.firstOrNull { it.isDictionaryBasedSpellingRule } as? SpellingCheckRule
    }

    override fun onCreate() {}

    override fun onGetSuggestions(textInfo: TextInfo, suggestionsLimit: Int): SuggestionsInfo {
        val word = textInfo.text
        val match = if (word.isBlank()) null else safely(null) {
            synchronized(lt) { speller?.match(lt.getAnalyzedSentence(word))?.firstOrNull() }
        }
        return if (match == null) {
            SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, emptyArray(), textInfo.cookie, textInfo.sequence)
        } else {
            suggestionsInfo(match, SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO, suggestionsLimit, textInfo)
        }
    }

    override fun onGetSentenceSuggestionsMultiple(
        textInfos: Array<TextInfo>,
        suggestionsLimit: Int,
    ): Array<SentenceSuggestionsInfo> = textInfos.map { textInfo ->
        val matches = safely(emptyList()) { synchronized(lt) { lt.check(textInfo.text) } }
        SentenceSuggestionsInfo(
            matches.map { match ->
                val kind = if (match.rule.isDictionaryBasedSpellingRule) SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
                else SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
                suggestionsInfo(match, kind, suggestionsLimit, textInfo)
            }.toTypedArray(),
            matches.map { it.fromPos }.toIntArray(),
            matches.map { it.toPos - it.fromPos }.toIntArray(),
        )
    }.toTypedArray()

    private fun suggestionsInfo(match: RuleMatch, kind: Int, limit: Int, textInfo: TextInfo): SuggestionsInfo {
        val suggestions = match.suggestedReplacements.take(limit).toTypedArray()
        val attrs = if (suggestions.isEmpty()) kind else kind or SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS
        return SuggestionsInfo(attrs, suggestions, textInfo.cookie, textInfo.sequence)
    }

    /** A failing check must not crash the service, which would take spell checking down for every app. */
    private inline fun <T> safely(fallback: T, block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        Log.e("BetterType", "LanguageTool check failed", e)
        fallback
    }
}
