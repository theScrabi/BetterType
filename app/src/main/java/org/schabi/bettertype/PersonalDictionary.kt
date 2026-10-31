package org.schabi.bettertype

import android.content.Context
import android.database.ContentObserver
import android.provider.UserDictionary
import android.util.Log

/**
 * The words the user added to Android's personal dictionary. Android lets only the selected spell checker and
 * enabled keyboards read it, so a failed read isn't kept and is retried on the next check.
 */
object PersonalDictionary {
    @Volatile private var words: Set<String>? = null
    @Volatile private var observing = false

    /** Words of every language: the user dictionary's locales rarely match the language a text is checked in. */
    fun contains(context: Context, word: String): Boolean {
        val all = words(context)
        return word in all || word.replaceFirstChar { it.lowercase() } in all
    }

    private fun words(context: Context): Set<String> {
        words?.let { return it }
        val resolver = context.applicationContext.contentResolver
        if (!observing) {
            resolver.registerContentObserver(UserDictionary.Words.CONTENT_URI, true, object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) { words = null }
            })
            observing = true
        }
        return try {
            resolver.query(UserDictionary.Words.CONTENT_URI, arrayOf(UserDictionary.Words.WORD), null, null, null)
                ?.use { cursor -> buildSet { while (cursor.moveToNext()) cursor.getString(0)?.let(::add) } }
                ?.also { words = it }
                ?: emptySet()
        } catch (e: SecurityException) {
            Log.w("BetterType", "Personal dictionary not readable", e)
            emptySet()
        }
    }
}
