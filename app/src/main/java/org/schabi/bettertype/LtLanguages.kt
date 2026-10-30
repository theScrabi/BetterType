package org.schabi.bettertype

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import org.languagetool.JLanguageTool
import org.languagetool.Language
import org.languagetool.Languages
import org.languagetool.ResultCache
import org.languagetool.rules.spelling.hunspell.AndroidHunspellDictionary
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

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

    private val checkers = ConcurrentHashMap<Language, JLanguageTool>()

    /**
     * One checker per language for the whole process: creating one loads rules and dictionaries (seconds).
     * [JLanguageTool] isn't thread-safe, so callers synchronize on the returned instance.
     */
    fun checkerFor(locale: Locale): JLanguageTool {
        all
        return checkers.computeIfAbsent(Languages.getLanguageForLocale(locale)) {
            JLanguageTool(it, null, ResultCache(1000))
        }
    }

    /** The languages the user can switch on and off, as LanguageTool short codes. */
    val SUPPORTED = listOf("en", "de", "fr", "es")

    private fun settings(context: Context) = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun systemLocales(context: Context): List<Locale> =
        context.resources.configuration.locales.let { locales -> List(locales.size()) { locales[it] } }

    // SharedPreferences don't sync between processes, so the engine process reads the setting once when it starts
    // and is told about changes by KeepAliveService.applyLanguages.
    @Volatile private var enabledNow: Set<String>? = null

    /** Only English until the user picks languages. */
    fun enabled(context: Context): Set<String> = enabledNow
        ?: (settings(context).getStringSet("enabled_languages", null) ?: setOf("en")).also { enabledNow = it }

    /** Stores the choice; called in the process that shows the settings. */
    fun saveEnabled(context: Context, codes: Set<String>) {
        settings(context).edit().putStringSet("enabled_languages", codes).commit()
        enabledNow = codes
    }

    /** Called in the engine process. Only ever adds languages: removing one restarts the process. */
    fun setEnabled(context: Context, codes: Set<String>) {
        enabledNow = codes
        warmUpEnabled(context)
    }

    /** The variant from the system languages (en-GB for a British user), else LanguageTool's default variant. */
    fun localeFor(context: Context, code: String): Locale =
        systemLocales(context).firstOrNull { it.language == code } ?: Locale.forLanguageTag(code)

    /** All enabled languages stay loaded, because a text may switch to any of them (see [LanguageGuesser]). */
    fun warmUpEnabled(context: Context) {
        for (code in enabled(context)) warmUp(context, localeFor(context, code))
    }

    private val warmUpExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "lt-warm-up").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }
    }

    private val warmed = ConcurrentHashMap.newKeySet<Language>()
    private lateinit var loadTimes: SharedPreferences

    /** The language being loaded and when that started; only the warm-up thread writes it. */
    @Volatile private var current: Pair<Language, Long>? = null

    /** Called on the warm-up thread whenever a language starts or finishes loading. */
    @Volatile var onLoadingChanged: (() -> Unit)? = null

    /** [percent] is null on a language's very first load, when there is no earlier load time to compare with. */
    class Loading(val language: Language, val percent: Int?)

    /**
     * There is no way to see how far LanguageTool got, so this is the time since the load started compared with how
     * long the last load of the same language took, capped at 99 % until it is done.
     */
    fun loading(): Loading? {
        val (language, start) = current ?: return null
        val expected = loadTimes.getLong(language.shortCodeWithCountryAndVariant, 0)
        val percent = if (expected > 0) ((SystemClock.elapsedRealtime() - start) * 100 / expected).coerceAtMost(99).toInt() else null
        return Loading(language, percent)
    }

    /**
     * Loads the language in the background and runs one check: LanguageTool initializes much of its data
     * lazily on the first check (German: ~30 s on an emulator), which would otherwise delay the first result.
     */
    fun warmUp(context: Context, locale: Locale) {
        loadTimes = context.applicationContext.getSharedPreferences("load_times", Context.MODE_PRIVATE)
        warmUpExecutor.execute {
            try {
                all
                val language = Languages.getLanguageForLocale(locale)
                if (!warmed.add(language)) return@execute
                val start = SystemClock.elapsedRealtime()
                current = language to start
                onLoadingChanged?.invoke()
                val lt = checkerFor(locale)
                // The made-up word also initializes suggestion generation, which only happens for the first typo.
                synchronized(lt) { lt.check("Warm-up qxzv.") }
                loadTimes.edit().putLong(language.shortCodeWithCountryAndVariant, SystemClock.elapsedRealtime() - start).apply()
            } catch (e: Exception) {
                Log.e("BetterType", "Warm-up for $locale failed", e)
            } finally {
                current = null
                onLoadingChanged?.invoke()
            }
        }
    }
}
