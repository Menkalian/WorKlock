package de.menkalian.worklock.ui.navigation

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import de.menkalian.worklock.R
import de.menkalian.worklock.controller.*
import de.menkalian.worklock.ui.theme.WorklockTheme
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter

private suspend fun refreshRecords(
    date: LocalDate,
    viewModel: LogicController,
    records: SnapshotStateList<Record>,
    refreshed: MutableState<Boolean>
) {
    val recordsForDay = viewModel.getAllRecordsForDay(date, true)
    records.clear()
    records.addAll(recordsForDay)
    refreshed.value = true
}

@Composable
fun DayRecordCorrectionView(
    date: LocalDate,
    navController: NavController,
    viewModel: LogicController = LogicController.get(),
    settingsController: SettingsController = SettingsController.get()
) {
    val ioScope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current

    var dangerousEdits by remember { mutableStateOf(false) }
    var showDetailed by remember { mutableStateOf(false) }
    val records = remember { mutableStateListOf<Record>() }
    val refreshed = mutableStateOf(false)

    var totalTime by remember { mutableIntStateOf(0) }
    var workTime by remember { mutableIntStateOf(0) }
    var pauseTime by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        refreshRecords(date, viewModel, records, refreshed)
        dangerousEdits = settingsController.isAllowPotentiallyDangerousEdits()
    }
    LaunchedEffect(refreshed) {
        refreshed.value = false
        val (e1, work) = getFullTimeForDayInMinutes(records, if (date == LocalDate.now()) LocalTime.now() else LocalTime.MAX)
        val (e2, pause) = getPauseTimeForDayInMinutes(records, if (date == LocalDate.now()) LocalTime.now() else LocalTime.MAX)
        error = e1 || e2
        totalTime = work + pause
        workTime = work
        pauseTime = pause
    }

    var timeSelectPrompt by remember { mutableStateOf("") }
    var timeSelectorOpen by remember { mutableStateOf(false) }
    var allowedSelectionRange by remember { mutableStateOf<Pair<Instant, Instant>?>(null) }
    var onTimeSelected by remember { mutableStateOf<((Instant) -> Unit)?>(null) }
    fun dismissTimeSelector() {
        timeSelectorOpen = false
        allowedSelectionRange = null
        onTimeSelected = null
    }

    if (timeSelectorOpen) {
        Dialog(onDismissRequest = { dismissTimeSelector() }) {
            TimestampSelector(
                allowedSelectionRange,
                timeSelectPrompt,
                onDismissed = { dismissTimeSelector() },
                onConfirmed = {
                    val selectAction = onTimeSelected
                    dismissTimeSelector()
                    selectAction?.invoke(it)
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        FloatingActionButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Default.ArrowBack, stringResource(R.string.correction_btn_back))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
        ) {
            Spacer(Modifier.height(10.dp))
            Text(
                date.format(DateTimeFormatter.ofPattern(stringResource(R.string.correction_title_date_pattern))),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Text(
                stringResource(
                    R.string.correction_text_stats,
                    totalTime / 60, totalTime % 60,
                    pauseTime / 60, pauseTime % 60,
                    workTime / 60, workTime % 60,
                ),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(error) {
                Text(
                    stringResource(R.string.correction_text_error),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                        .padding(5.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = {
                        selectRange(
                            date,
                            ioScope,
                            context,
                            openTimeSelector = { prompt, range, onSelected ->
                                timeSelectPrompt = prompt
                                allowedSelectionRange = range
                                onTimeSelected = onSelected
                                timeSelectorOpen = true
                            },
                            closeRangeSelector = { timeSelectorOpen = false },
                            rangeUntilLimiter = { viewModel.getAllowedManualPauseEndTimestamp(it) }
                        ) { (start, end) ->
                            viewModel.addManualPause(start, end)
                            refreshRecords(date, viewModel, records, refreshed)
                        }
                    },
                    enabled = dangerousEdits,
                ) {
                    Icon(painterResource(R.drawable.icon_pause), stringResource(R.string.correction_label_addpause))
                    Text(stringResource(R.string.correction_label_addpause))
                }
                Spacer(Modifier.width(20.dp))
                Button(
                    onClick = {
                        selectRange(
                            date,
                            ioScope,
                            context,
                            openTimeSelector = { prompt, range, onSelected ->
                                timeSelectPrompt = prompt
                                allowedSelectionRange = range
                                onTimeSelected = onSelected
                                timeSelectorOpen = true
                            },
                            closeRangeSelector = { timeSelectorOpen = false },
                            rangeUntilLimiter = { viewModel.getAllowedManualWorkPeriodEndTimestamp(it) }
                        ) { (start, end) ->
                            viewModel.addManualWorkPeriod(start, end)
                            refreshRecords(date, viewModel, records, refreshed)
                        }
                    },
                    enabled = dangerousEdits,
                ) {
                    Icon(Icons.Default.AddCircle, stringResource(R.string.correction_label_addwork))
                    Text(stringResource(R.string.correction_label_addwork))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.correction_section_entries),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.correction_label_showdetailed), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Switch(showDetailed, onCheckedChange = { showDetailed = it })
            }
            Spacer(Modifier.height(5.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(records.size, key = { records[it].id }) { index ->
                    val item = records[index]
                    val deleted = item.deleted
                    val isReplaced = records.any { it.correctionForId == item.id }
                    val show = showDetailed || (!deleted && !isReplaced)

                    AnimatedVisibility(show) {
                        RecordView(
                            item,
                            viewModel,
                            date,
                            records,
                            refreshed,
                            dangerousEdits,
                        ) { prompt, range, onSelected ->
                            timeSelectPrompt = prompt
                            allowedSelectionRange = range
                            onTimeSelected = onSelected
                            timeSelectorOpen = true
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimestampSelector(
    allowedSelectionRange: Pair<Instant, Instant>?,
    timeSelectPrompt: String,
    onDismissed: () -> Unit = {},
    onConfirmed: (Instant) -> Unit = {},
) {
    Card {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 20.dp).verticalScroll(rememberScrollState())) {
            val range = allowedSelectionRange ?: (Instant.EPOCH to Instant.MAX.minusSeconds(24 * 60 * 60))
            val minAllowed = LocalDateTime.ofInstant(range.first, ZoneId.systemDefault())
            val maxAllowed = LocalDateTime.ofInstant(range.second, ZoneId.systemDefault())
            val now = Instant.now()
            val initial = if (now < range.first) {
                range.first
            } else if (now > range.second) {
                range.second
            } else {
                now
            }
            val initialTime = initial.getTime()

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initial.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return range.first.toEpochMilli() <= utcTimeMillis && utcTimeMillis <= range.second.toEpochMilli()
                    }

                    override fun isSelectableYear(year: Int): Boolean {
                        return minAllowed.year <= year && maxAllowed.year <= year
                    }
                }
            )
            val timePickerState = rememberTimePickerState(
                initialHour = initialTime.hour,
                initialMinute = initialTime.minute,
                is24Hour = true,
            )

            Text(
                timeSelectPrompt,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            AnimatedVisibility(minAllowed.toLocalDate() != maxAllowed.toLocalDate()) {
                // TODO: Replace by compacter version.
                DatePicker(datePickerState)
            }
            TimePicker(
                timePickerState
            )

            val currentSelection =
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(datePickerState.selectedDateMillis ?: 0),
                    ZoneId.of("UTC")
                )
                    .toLocalDate()
                    .atStartOfDay()
                    .plusHours(timePickerState.hour.toLong())
                    .plusMinutes(timePickerState.minute.toLong())
                    .atZone(ZoneId.systemDefault())
                    .toInstant()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { onDismissed() },
                    colors = ButtonDefaults.textButtonColors()
                        .copy(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onConfirmed(currentSelection)
                    },
                    enabled = range.first <= currentSelection && currentSelection <= range.second,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun RecordView(
    item: Record,
    viewModel: LogicController,
    date: LocalDate,
    records: SnapshotStateList<Record>,
    refreshed: MutableState<Boolean>,
    dangerousEdits: Boolean,
    openTimeSelector: (
        prompt: String,
        range: Pair<Instant, Instant>,
        onSelected: (Instant) -> Unit
    ) -> Unit,
) {
    val ioScope = rememberCoroutineScope { Dispatchers.IO }

    val deleted = item.deleted
    val replacedBy = records.firstOrNull { it.correctionForId == item.id }
    val allowManualChanges = !item.deleted && replacedBy == null
    val bgColor = if (deleted) {
        LocalContentColor.current.copy(alpha = 0.5f)
            .compositeOver(MaterialTheme.colorScheme.surface)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val textDecoration = if (replacedBy != null) {
        TextDecoration.LineThrough
    } else {
        TextDecoration.None
    }

    val selectTimeEditPrompt = stringResource(R.string.correction_text_select_change)

    Card(
        colors = CardDefaults.cardColors().copy(containerColor = bgColor),
        modifier = Modifier.padding(vertical = 5.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        stringResource(R.string.correction_entry_id, item.id),
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = textDecoration
                    )
                    Spacer(Modifier.width(10.dp))
                    if (deleted) {
                        Icon(Icons.Default.Delete, stringResource(R.string.correction_label_deleted))
                    } else if (item.manual || replacedBy != null) {
                        Icon(Icons.Default.Edit, stringResource(R.string.correction_btn_edit))
                    }
                }
                Text(
                    stringResource(
                        R.string.correction_entry_timestamp,
                        item.timestamp.getTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = textDecoration
                )
                Row {
                    val name = stringResource(
                        getRecordTypeDescription(item)
                    )
                    val icon = getRecordTypeIcon(item)

                    Text(
                        stringResource(R.string.correction_entry_type, name),
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = textDecoration,
                    )
                    Spacer(Modifier.width(5.dp))
                    Icon(painterResource(icon), item.type.name)
                }
                AnimatedVisibility(replacedBy != null) {
                    Text(
                        stringResource(R.string.correction_entry_replaced_by, replacedBy?.id ?: 0),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column {
                Button(
                    onClick = {
                        ioScope.launch {
                            viewModel.deleteRecord(item.id)
                            refreshRecords(date, viewModel, records, refreshed)
                        }
                    },
                    enabled = dangerousEdits && !item.deleted && replacedBy == null,
                    contentPadding = PaddingValues(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, stringResource(R.string.correction_btn_delete))
                }
                Button(
                    onClick = {
                        ioScope.launch {
                            openTimeSelector(
                                selectTimeEditPrompt,
                                viewModel.getAllowedEditTimeRangeFor(item)
                            ) { changeTs ->
                                ioScope.launch {
                                    viewModel.updateRecordTime(item.id, changeTs)
                                    refreshRecords(date, viewModel, records, refreshed)
                                }
                            }
                        }
                    },
                    contentPadding = PaddingValues(4.dp),
                    enabled = allowManualChanges,
                ) {
                    Icon(Icons.Default.Edit, stringResource(R.string.correction_btn_edit))
                }
            }
        }
    }
}

private fun selectRange(
    date: LocalDate,
    ioScope: CoroutineScope,
    context: Context,
    openTimeSelector: (prompt: String, range: Pair<Instant, Instant>, onSelected: (Instant) -> Unit) -> Unit,
    closeRangeSelector: () -> Unit,
    rangeUntilLimiter: suspend (Instant) -> Instant?,
    onRangeSelected: suspend (range: Pair<Instant, Instant>) -> Unit
) {
    val initialPickFrom = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
    val initialPickUntil = date.atStartOfDay(ZoneId.systemDefault())
        .plusDays(1)
        .minusSeconds(1)
        .toInstant()
    openTimeSelector(
        context.getString(R.string.correction_text_select_start),
        initialPickFrom to initialPickUntil,
    ) { startTs ->
        ioScope.launch {
            closeRangeSelector()
            val allowedEnd = rangeUntilLimiter(startTs) ?: initialPickUntil
            delay(500)
            openTimeSelector(
                context.getString(R.string.correction_text_select_until),
                startTs to allowedEnd
            ) { untilTs ->
                ioScope.launch {
                    onRangeSelected(startTs to untilTs)
                }
            }
        }
    }
}

private fun getRecordTypeDescription(item: Record) = when (item.type) {
    RecordType.Start   -> R.string.correction_entry_type_start
    RecordType.End     -> R.string.correction_entry_type_end
    RecordType.Pause   -> R.string.correction_entry_type_pause
    RecordType.Unpause -> R.string.correction_entry_type_unpause
}

private fun getRecordTypeIcon(item: Record) = when (item.type) {
    RecordType.Start   -> R.drawable.icon_work
    RecordType.End     -> R.drawable.icon_endwork
    RecordType.Pause   -> R.drawable.icon_pause
    RecordType.Unpause -> R.drawable.icon_continue
}

@Preview
@Composable
fun DayCorrectionPreview() {
    WorklockTheme(darkTheme = false, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DayRecordCorrectionView(
                LocalDate.of(2024, 3, 27),
                rememberNavController(),
                DummyLogicController(),
                DummySettingsController(),
            )
        }
    }
}

@Preview(locale = "de")
@Composable
fun DayCorrectionDarkPreview() {
    WorklockTheme(darkTheme = true, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DayRecordCorrectionView(
                LocalDate.of(2024, 3, 27),
                rememberNavController(),
                DummyLogicController(),
                DummySettingsController(),
            )
        }
    }
}