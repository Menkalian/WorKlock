package de.menkalian.worklock.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.menkalian.worklock.R
import de.menkalian.worklock.controller.*
import de.menkalian.worklock.ui.theme.WorklockTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsView(
    settingsViewModel: SettingsController = SettingsController.get()
) {
    val ioScope = rememberCoroutineScope { Dispatchers.IO }

    val personalEntries = remember {
        mutableStateListOf<PersonalDataEntry>()
    }
    LaunchedEffect(Unit) {
        personalEntries.addAll(settingsViewModel.getAllPersonalData())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        item {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Spacer(Modifier.height(20.dp))
        }

        item {
            Text(stringResource(R.string.settings_section_export), style = MaterialTheme.typography.headlineSmall)
        }
        item {
            var enabled by remember { mutableStateOf(true) }
            var active by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                active = settingsViewModel.isExportIncludeManualFlag()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_section_export_manual_edit),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = active,
                    enabled = enabled,
                    onCheckedChange = {
                        enabled = false
                        active = it
                        ioScope.launch {
                            settingsViewModel.setExportIncludeManualFlag(it)
                            enabled = true
                        }
                    }
                )
            }
        }
        item {
            var enabled by remember { mutableStateOf(true) }
            var active by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                active = settingsViewModel.isExportIncludeErrorsFlag()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_section_export_errors),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = active,
                    enabled = enabled,
                    onCheckedChange = {
                        enabled = false
                        active = it
                        ioScope.launch {
                            settingsViewModel.setExportIncludeErrorsFlag(it)
                            enabled = true
                        }
                    }
                )
            }
        }
        item {
            var enabled by remember { mutableStateOf(true) }
            var active by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                active = settingsViewModel.isExportIncludePause()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_section_export_pause),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = active,
                    enabled = enabled,
                    onCheckedChange = {
                        enabled = false
                        active = it
                        ioScope.launch {
                            settingsViewModel.setExportIncludePause(it)
                            enabled = true
                        }
                    }
                )
            }
        }
        item {
            Spacer(Modifier.height(10.dp))
        }

        item {
            Text(stringResource(R.string.settings_section_app), style = MaterialTheme.typography.headlineSmall)
        }
        item {
            var enabled by remember { mutableStateOf(true) }
            var active by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                active = settingsViewModel.isAllowPotentiallyDangerousEdits()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_section_app_dangerous_edit),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = active,
                    enabled = enabled,
                    onCheckedChange = {
                        enabled = false
                        active = it
                        ioScope.launch {
                            settingsViewModel.setAllowPotentiallyDangerousEdits(it)
                            enabled = true
                        }
                    }
                )
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
        }

        item {
            Text(stringResource(R.string.settings_section_personal), style = MaterialTheme.typography.headlineSmall)
        }
        items(personalEntries.size, key = { personalEntries[it].id }) { index ->
            val entry = personalEntries[index]
            Card(Modifier.height(232.dp).padding(vertical = 5.dp)) { // this is definitely eyeballed :)
                LazyVerticalGrid(
                    GridCells.Fixed(2),
                    modifier = Modifier.padding(10.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.settings_section_personal_entry_active))
                            Spacer(Modifier.width(10.dp))
                            Switch(entry.enabled, onCheckedChange = {
                                personalEntries[index] = entry.copy(enabled = it)
                            })
                        }
                    }
                    item {
                        Button(onClick = { personalEntries.remove(entry) }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.settings_section_personal_entry_delete))
                            Text(stringResource(R.string.settings_section_personal_entry_delete))
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        OutlinedTextField(
                            entry.name,
                            isError = personalEntries.count { it.name == entry.name } > 1,
                            label = { Text(stringResource(R.string.settings_section_personal_entry_name)) },
                            singleLine = true,
                            onValueChange = {
                                personalEntries[index] = entry.copy(name = it)
                            })
                    }
                    item(span = { GridItemSpan(2) }) {
                        Spacer(Modifier.height(10.dp))
                    }
                    item(span = { GridItemSpan(2) }) {
                        OutlinedTextField(
                            entry.value,
                            label = { Text(stringResource(R.string.settings_section_personal_entry_value)) },
                            singleLine = true,
                            onValueChange = {
                                personalEntries[index] = entry.copy(value = it)
                            })
                    }
                }
            }
        }
        item {
            Row {
                Button(onClick = {
                    personalEntries.add(PersonalDataEntry((personalEntries.maxOfOrNull { it.id } ?: 0) + 1, true, "#${personalEntries.size}", ""))
                }) {
                    Icon(Icons.Default.Add, stringResource(R.string.settings_section_personal_add))
                    Text(stringResource(R.string.settings_section_personal_add))
                }
                Spacer(Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.End) {
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    ioScope.launch {
                        personalEntries.clear()
                        personalEntries.addAll(settingsViewModel.getAllPersonalData())
                    }
                }) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.settings_section_personal_reset))
                    Text(stringResource(R.string.settings_section_personal_reset))
                }
                Spacer(Modifier.width(5.dp))
                Button(onClick = {
                    ioScope.launch {
                        settingsViewModel.setAllPersonalData(personalEntries)
                    }
                }) {
                    Icon(painterResource(R.drawable.icon_save), stringResource(R.string.settings_section_personal_save))
                    Text(stringResource(R.string.settings_section_personal_save))
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsPreview() {
    WorklockTheme(darkTheme = false, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SettingsView(DummySettingsController())
        }
    }
}

@Preview
@Composable
fun SettingsDarkPreview() {
    WorklockTheme(darkTheme = true, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SettingsView(DummySettingsController())
        }
    }
}