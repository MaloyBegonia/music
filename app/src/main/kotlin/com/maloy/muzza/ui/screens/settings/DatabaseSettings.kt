package com.maloy.muzza.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.maloy.muzza.LocalPlayerAwareWindowInsets
import com.maloy.muzza.R
import com.maloy.muzza.internal
import com.maloy.muzza.path
import com.maloy.muzza.service.PlayerService
import com.maloy.muzza.ui.components.themed.Header
import com.maloy.muzza.ui.styling.LocalAppearance
import com.maloy.muzza.utils.intent
import com.maloy.muzza.utils.toast
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.distinctUntilChanged

@ExperimentalAnimationApi
@Composable
fun DatabaseSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    val eventsCount by remember {
        com.maloy.muzza.Database.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.sqlite3")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            com.maloy.muzza.query {
                com.maloy.muzza.Database.checkpoint()

                context.applicationContext.contentResolver.openOutputStream(uri)
                    ?.use { outputStream ->
                        FileInputStream(com.maloy.muzza.Database.internal.path).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            com.maloy.muzza.query {
                com.maloy.muzza.Database.checkpoint()
                com.maloy.muzza.Database.internal.close()

                context.applicationContext.contentResolver.openInputStream(uri)
                    ?.use { inputStream ->
                        FileOutputStream(com.maloy.muzza.Database.internal.path).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                context.stopService(context.intent<PlayerService>())
                exitProcess(0)
            }
        }

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        Header(title = stringResource(R.string.database))

        SettingsEntryGroupText(title = stringResource(R.string.cleanup))

        SettingsEntry(
            title = stringResource(R.string.reset_quick_picks),
            text = if (eventsCount > 0) {
                stringResource(R.string.delete_playback_events, eventsCount)
            } else {
                stringResource(R.string.quick_picks_are_cleared)
            },
            isEnabled = eventsCount > 0,
            onClick = { com.maloy.muzza.query(com.maloy.muzza.Database::clearEvents) }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.backup))

        SettingsDescription(text = stringResource(R.string.personal_preference))

        SettingsEntry(
            title = stringResource(R.string.backup_1),
            text = stringResource(R.string.export_the_database),
            onClick = {
                @SuppressLint("SimpleDateFormat")
                val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")

                try {
                    backupLauncher.launch("muzza_${dateFormat.format(Date())}.db")
                } catch (e: ActivityNotFoundException) {
                    context.toast("Couldn't find an application to create documents")
                }
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.restore))

        ImportantSettingsDescription(text = stringResource(
            R.string.existing_data_will_be_overwritten,
            context.applicationInfo.nonLocalizedLabel
        ))

        SettingsEntry(
            title = stringResource(R.string.restore_1),
            text = stringResource(R.string.import_the_database),
            onClick = {
                try {
                    restoreLauncher.launch(
                        arrayOf(
                            "application/vnd.sqlite3",
                            "application/x-sqlite3",
                            "application/octet-stream"
                        )
                    )
                } catch (e: ActivityNotFoundException) {
                    context.toast("Couldn't find an application to open documents")
                }
            }
        )
    }
}
