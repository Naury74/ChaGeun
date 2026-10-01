package com.naury.chageun.feature.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.RawRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class OpenSourceLibrary(
    val id: String,
    val name: String,
    val version: String?,
    val licenses: List<String>,
    val licenseText: String?,
)

/** [librariesRes] is the metadata the AboutLibraries Gradle plugin generates in the app module at build time. */
@Composable
fun OpenSourceLicensesRoute(@RawRes librariesRes: Int, onBack: () -> Unit) {
    val resources = LocalResources.current
    val libraries by produceState<List<OpenSourceLibrary>?>(null, librariesRes) {
        value = withContext(Dispatchers.IO) {
            parseOpenSourceLibraries(resources.openRawResource(librariesRes).bufferedReader().use { it.readText() })
        }
    }
    OpenSourceLicensesScreen(libraries, onBack)
}

@Composable
fun OpenSourceLicensesScreen(libraries: List<OpenSourceLibrary>?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = libraries?.firstOrNull { it.id == selectedId }
    BackHandler(enabled = selected != null) { selectedId = null }
    Column(modifier.fillMaxSize().padding(horizontal = ChageunTheme.spacing.gutter)) {
        SettingsTopBar(
            titleRes = R.string.settings_licenses,
            onBack = { if (selected != null) selectedId = null else onBack() },
        )
        when {
            libraries == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            selected != null -> LicenseDetail(selected)
            else -> LazyColumn(
                modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH),
                contentPadding = PaddingValues(vertical = ChageunTheme.spacing.sm),
            ) {
                items(libraries, key = { it.id }) { library ->
                    LibraryRow(library, onClick = { selectedId = library.id })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(library: OpenSourceLibrary, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = ChageunTheme.spacing.sm),
    ) {
        Text(library.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            listOfNotNull(library.version, library.licenses.joinToString().ifEmpty { null }).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LicenseDetail(library: OpenSourceLibrary) {
    Column(
        Modifier
            .widthIn(max = CONTENT_MAX_WIDTH)
            .verticalScroll(rememberScrollState())
            .padding(vertical = ChageunTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        Text(
            library.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            library.licenses.joinToString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        library.licenseText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

internal fun parseOpenSourceLibraries(json: String): List<OpenSourceLibrary> =
    Libs.Builder().withJson(json).build().libraries
        .map { library ->
            OpenSourceLibrary(
                id = library.uniqueId,
                name = library.name,
                version = library.artifactVersion,
                licenses = library.licenses.map { it.name },
                licenseText = library.licenses.mapNotNull { it.licenseContent }.joinToString("\n\n").ifBlank { null },
            )
        }
        .sortedBy { it.name.lowercase() }

private val CONTENT_MAX_WIDTH = 720.dp
