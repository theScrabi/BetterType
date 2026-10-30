package org.schabi.bettertype

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.schabi.bettertype.ui.theme.BetterTypeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        KeepAliveService.start(this)
        // The keep-alive notification shows how far the languages have loaded.
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        setContent {
            BetterTypeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("Better Type", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Grammar and spell checking for every text field, powered by LanguageTool and " +
                                "running entirely on your device. To turn it on, open Settings → System → " +
                                "Keyboard → Spell checker and choose Better Type.",
                        )
                        Button(onClick = ::openSpellCheckerSettings) {
                            Text("Open spell checker settings")
                        }
                        // A framework EditText, because the system spell checker reliably attaches to it.
                        val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
                        val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                        AndroidView(
                            factory = {
                                EditText(it).apply {
                                    hint = "Type here to try it, e.g. \"This are a test.\""
                                    setTextColor(textColor)
                                    setHintTextColor(hintColor)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("Licenses", style = MaterialTheme.typography.titleMedium)
                        Text(LICENSES, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // There is no public intent for System → Keyboard → Spell checker. AOSP exports this activity;
    // Settings apps without it get the general keyboard settings instead.
    private fun openSpellCheckerSettings() {
        try {
            startActivity(Intent().setClassName("com.android.settings", "com.android.settings.Settings\$SpellCheckersSettingsActivity"))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
    }
}

private const val LICENSES = """Better Type is free software under the GNU General Public License v3.0 or later.

It contains:
• LanguageTool 6.8 (languagetool.org), LGPL 2.1 or later
• Hunspell 1.7.2, MPL 1.1 / GPL 2.0 / LGPL 2.1
• Morfologik, BSD
• German Hunspell dictionaries (igerman98), GPL v2 / v3
• German and French POS dictionaries, CC BY-SA 4.0
• English and Spanish POS dictionaries, LGPL 2.1
• Apache OpenNLP and its English models, Apache License 2.0

Source code and full license texts: see LICENSE and NOTICE.md in the source repository."""
