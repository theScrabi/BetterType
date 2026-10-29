package org.schabi.bettertype

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.languagetool.RuleEntityResolver
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import java.util.zip.ZipFile
import javax.xml.parsers.SAXParserFactory

/**
 * Compiles every regex in every bundled LanguageTool rule file with Android's ICU-based regex engine,
 * which is stricter than the JDK's. Reports all failures at once instead of one per rule-file load.
 */
@RunWith(AndroidJUnit4::class)
class RegexCompatTest {

    @Test
    fun allRuleRegexesCompileOnAndroid() {
        val apk = InstrumentationRegistry.getInstrumentation().targetContext.packageCodePath
        val failures = mutableListOf<String>()
        var count = 0
        ZipFile(apk).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.startsWith("org/languagetool/") && it.name.endsWith(".xml") }
                .forEach { entry ->
                    val handler = RegexCollector()
                    // Rule files may reference a DTD next to them; resolve it from the APK as well.
                    handler.dir = entry.name.substringBeforeLast('/')
                    handler.zip = zip
                    zip.getInputStream(entry).use {
                        SAXParserFactory.newInstance().newSAXParser().parse(RuleEntityResolver.inlineEntityFilesOnAndroid(it), handler)
                    }
                    for ((line, regex) in handler.regexes) {
                        count++
                        try {
                            Pattern.compile(regex.replace("(?U)", ""))
                        } catch (e: PatternSyntaxException) {
                            failures += "${entry.name}:$line  ${e.description}  <<$regex>>"
                        }
                    }
                }
        }
        failures.forEach { Log.e("RegexCompat", it) }
        Log.i("RegexCompat", "checked $count regexes, ${failures.size} failures")
        assertTrue("${failures.size} of $count regexes fail on Android, see logcat tag RegexCompat", failures.isEmpty())
    }

    private class RegexCollector : DefaultHandler() {
        val regexes = mutableListOf<Pair<Int, String>>()
        lateinit var zip: ZipFile
        lateinit var dir: String
        private var locator: org.xml.sax.Locator? = null
        private val textStack = ArrayDeque<StringBuilder?>()

        override fun setDocumentLocator(l: org.xml.sax.Locator) { locator = l }

        override fun resolveEntity(publicId: String?, systemId: String?): org.xml.sax.InputSource? {
            val name = systemId?.substringAfterLast('/') ?: return null
            val entry = zip.getEntry("$dir/$name") ?: return null
            return org.xml.sax.InputSource(zip.getInputStream(entry))
        }

        override fun startElement(uri: String?, localName: String?, qName: String, a: Attributes) {
            val line = locator?.lineNumber ?: -1
            if (a.getValue("postag_regexp") == "yes") a.getValue("postag")?.let { regexes += line to it }
            a.getValue("chunk_re")?.let { regexes += line to it }
            a.getValue("regexp_match")?.let { regexes += line to it }
            val textIsRegex = qName == "regexp" || (a.getValue("regexp") == "yes" && qName != "match")
            textStack.addLast(if (textIsRegex) StringBuilder() else null)
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            textStack.lastOrNull()?.append(ch, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            val text = textStack.removeLast()?.toString()?.trim()
            if (!text.isNullOrEmpty()) regexes += (locator?.lineNumber ?: -1) to text
        }
    }
}
