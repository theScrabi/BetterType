package org.schabi.bettertype

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.textservice.TextServicesManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.color.DynamicColors
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import org.schabi.bettertype.ui.Preference
import org.schabi.bettertype.ui.PreferenceCategory
import org.schabi.bettertype.ui.PreferenceFooter
import org.schabi.bettertype.ui.PreferenceGroup
import org.schabi.bettertype.ui.SwitchPreference
import org.schabi.bettertype.ui.theme.BetterTypeTheme

class MainActivity : ComponentActivity() {
    private var spellCheckerSelected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Material You colors for the view-based text field; Compose gets them from BetterTypeTheme.
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        KeepAliveService.start(this)
        // The keep-alive notification shows how far the languages have loaded.
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        setContent {
            BetterTypeTheme {
                SettingsScreen(spellCheckerSelected, ::openSpellCheckerSettings)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may come back from the system settings.
        val tsm = getSystemService(TextServicesManager::class.java)
        spellCheckerSelected = tsm.isSpellCheckerEnabled && tsm.currentSpellCheckerInfo?.packageName == packageName
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(spellCheckerSelected: Boolean, openSpellCheckerSettings: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val background = MaterialTheme.colorScheme.surfaceContainer
    var showLicenses by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background, scrolledContainerColor = background),
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                // Edge-to-edge apps no longer get resized for the keyboard, so make room for it here.
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (spellCheckerSelected) {
                PreferenceGroup(listOf {
                    Preference(
                        stringResource(R.string.spell_checker),
                        stringResource(R.string.spell_checker_summary),
                        onClick = openSpellCheckerSettings,
                    )
                })
            } else {
                SetupCard(openSpellCheckerSettings)
            }

            PreferenceCategory(stringResource(R.string.try_it))
            PreferenceGroup(listOf { TryItField() })

            PreferenceCategory(stringResource(R.string.languages))
            LanguageSwitches()
            PreferenceFooter(stringResource(R.string.languages_footer))

            PreferenceCategory(stringResource(R.string.about))
            val context = LocalContext.current
            PreferenceGroup(listOf(
                { Preference(stringResource(R.string.version), context.packageManager.getPackageInfo(context.packageName, 0).versionName) },
                { Preference(stringResource(R.string.licenses), stringResource(R.string.licenses_summary), onClick = { showLicenses = true }) },
            ))
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text(stringResource(R.string.close)) } },
            title = { Text(stringResource(R.string.licenses)) },
            text = {
                val licenses = stringResource(R.string.licenses_intro) + LICENSES + stringResource(R.string.licenses_outro)
                Text(licenses, Modifier.verticalScroll(rememberScrollState()))
            },
        )
    }
}

@Composable
private fun SetupCard(openSpellCheckerSettings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.setup_text),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Button(onClick = openSpellCheckerSettings) { Text(stringResource(R.string.setup_button)) }
        }
    }
}

/** A framework EditText (in a Material text field), because the system spell checker reliably attaches to it. */
@Composable
private fun TryItField() {
    AndroidView(
        factory = { context ->
            TextInputLayout(context, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
                hint = context.getString(R.string.try_it_hint)
                placeholderText = context.getString(R.string.try_it_placeholder)
                addView(TextInputEditText(this.context).apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    minLines = 4
                    gravity = Gravity.TOP or Gravity.START
                })
            }
        },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}

@Composable
private fun LanguageSwitches() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(LtLanguages.enabled(context)) }
    PreferenceGroup(LtLanguages.SUPPORTED.map { code ->
        {
            SwitchPreference(
                LtLanguages.displayName(Locale.forLanguageTag(code)),
                checked = code in enabled,
                onCheckedChange = { on ->
                    enabled = if (on) enabled + code else enabled - code
                    KeepAliveService.applyLanguages(context, enabled)
                },
            )
        }
    })
}

// Names of the components and licenses, the same in every language.
private const val LICENSES = """
• LanguageTool 6.8 (languagetool.org), LGPL 2.1 or later
• Hunspell 1.7.2, MPL 1.1 / GPL 2.0 / LGPL 2.1
• Morfologik, BSD
• German Hunspell dictionaries (igerman98), GPL v2 / v3
• German and French POS dictionaries, CC BY-SA 4.0
• English and Spanish POS dictionaries, LGPL 2.1
• Apache OpenNLP and its English models, Apache License 2.0

"""
