package org.schabi.bettertype

import org.languagetool.JLanguageTool
import org.languagetool.Languages
import java.util.concurrent.ConcurrentHashMap

/**
 * Guesses a text's language by counting its words that are in each language's list of ~10,000 common words,
 * which every LanguageTool module ships. LanguageTool's own detector can't be used: it needs Catalan and
 * Portuguese installed, and its n-gram fallback is unreliable on texts as short as a single sentence.
 */
object LanguageGuesser {
    private val WORD = Regex("[\\p{L}']+")
    private val commonWords = ConcurrentHashMap<String, Set<String>>()

    private fun commonWords(code: String): Set<String> = commonWords.computeIfAbsent(code) {
        LtLanguages.all
        val path = Languages.getLanguageForShortCode(code).commonWordsPath
        JLanguageTool.getDataBroker().getFromResourceDirAsLines(path)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.lowercase() }
            .toSet()
    }

    /**
     * Returns [preferred] unless another of the [candidates] knows more of the text's words. In longer texts at
     * least two words must speak for the other language, so one borrowed word doesn't switch it. A one-word text
     * ("Dear", the first word while typing) switches on that word alone, else it would be checked as a misspelled
     * word of the preferred language. Words common to both ("ok") keep the preferred language.
     */
    fun guess(text: String, candidates: Set<String>, preferred: String): String {
        val words = WORD.findAll(text.replace('’', '\'')).map { it.value.lowercase() }.toList()
        val scores = candidates.associateWith { code -> words.count { it in commonWords(code) } }
        val best = scores.maxBy { it.value }
        val preferredScore = scores[preferred] ?: 0
        return if (best.key != preferred && best.value > preferredScore && best.value >= minOf(2, words.size)) best.key else preferred
    }
}
