package de.menkalian.worklock.ui.navigation

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sd.lib.compose.wheel_picker.FVerticalWheelPicker
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import de.menkalian.worklock.R
import de.menkalian.worklock.controller.*
import de.menkalian.worklock.ui.theme.WorklockTheme
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val MAGIC_START_YEAR = 2024

@Composable
fun ExportView(
    viewModel: ExportController = ExportController.get()
) {
    var loading by remember { mutableStateOf(false) }
    var lastExportType by remember { mutableStateOf(ExportType.EXCEL_AGGREGATE) }
    var lastExportFile by remember { mutableStateOf<File?>(null) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val ioScope = rememberCoroutineScope { Dispatchers.IO }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        pickedImageUri = it.data?.data ?: Uri.EMPTY // Returns empty when sharing/selecting was cancelled.
    }

    LaunchedEffect(lastExportFile, pickedImageUri) {
        if (lastExportFile != null && pickedImageUri != null) {
            if (pickedImageUri != Uri.EMPTY) {
                writeFile(context, lastExportFile!!, pickedImageUri!!)
                lastExportFile = null
                pickedImageUri = null
            } else {
                Toast.makeText(
                    context,
                    R.string.export_save_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    if (lastExportFile != null) {
        Dialog(
            onDismissRequest = { lastExportFile = null },
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.export_finished),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            shareFile(context, lastExportFile!!) {
                                // Close Dialog on success.
                                lastExportFile = null
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, stringResource(R.string.export_share))
                        Text(stringResource(R.string.export_share))
                    }
                    Button(
                        onClick = {
                            requestFilePath(launcher, lastExportFile!!, viewModel.getMimeType(lastExportType))
                        }
                    ) {
                        Icon(painterResource(R.drawable.icon_save), stringResource(R.string.export_save))
                        Text(stringResource(R.string.export_save))
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(loading, Modifier.align(Alignment.TopEnd)) {
            CircularProgressIndicator()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        var excelFormat by remember { mutableStateOf(true) }
        var aggregateType by remember { mutableStateOf(true) }
        var multiMonth by remember { mutableStateOf(false) }
        val startMonth = remember { mutableStateOf(YearMonth.now().minusMonths(1)) }
        val untilMonth = remember { mutableStateOf(YearMonth.now()) }
        val selectedExportType = when {
            excelFormat && aggregateType -> ExportType.EXCEL_AGGREGATE
            excelFormat && !aggregateType -> ExportType.EXCEL_RECORDS
            !excelFormat && aggregateType -> ExportType.CSV_AGGREGATE
            else -> ExportType.CSV_RECORDS
        }

        Text(stringResource(R.string.export_format), style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(!loading) { excelFormat = true }) {
            RadioButton(excelFormat, { excelFormat = true }, enabled = !loading)
            Text(stringResource(R.string.export_radio_format_excel))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(!loading) { excelFormat = false }) {
            RadioButton(!excelFormat, { excelFormat = false }, enabled = !loading)
            Text(stringResource(R.string.export_radio_format_csv))
        }
        Spacer(Modifier.height(30.dp))

        Text(stringResource(R.string.export_type), style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(!loading) { aggregateType = true }) {
            RadioButton(aggregateType, { aggregateType = true }, enabled = !loading)
            Text(stringResource(R.string.export_radio_type_aggregate))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(!loading) { aggregateType = false }) {
            RadioButton(!aggregateType, { aggregateType = false }, enabled = !loading)
            Text(stringResource(R.string.export_radio_type_record))
        }
        Spacer(Modifier.height(30.dp))

        Text(stringResource(R.string.export_range), style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(!loading && selectedExportType.supportRange) { multiMonth = !multiMonth }) {
            Checkbox(multiMonth && selectedExportType.supportRange, enabled = !loading && selectedExportType.supportRange, onCheckedChange = { multiMonth = it })
            Text(stringResource(R.string.export_checkbox_range))
        }
        AnimatedContent(multiMonth && selectedExportType.supportRange, label = "multimonth-text-start") { mm ->
            Text(
                if (mm) {
                    stringResource(R.string.export_range_start)
                } else {
                    stringResource(R.string.export_range_month)
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        YearMonthSelector(startMonth, enabled = !loading)

        AnimatedVisibility(multiMonth && selectedExportType.supportRange) {
            Text(
                stringResource(R.string.export_range_until),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        AnimatedVisibility(multiMonth && selectedExportType.supportRange) {
            YearMonthSelector(untilMonth, enabled = !loading)
        }
        Spacer(Modifier.height(30.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            onClick = {
                loading = true
                ioScope.launch {
                    lastExportType = selectedExportType
                    lastExportFile = if (multiMonth && selectedExportType.supportRange) {
                        viewModel.createRangeExport(
                            startMonth.value,
                            untilMonth.value,
                            selectedExportType
                        )
                    } else {
                        viewModel.createExport(
                            startMonth.value,
                            selectedExportType
                        )
                    }
                    loading = false
                }
            }
        ) {
            Icon(painterResource(R.drawable.icon_export), stringResource(R.string.export_start))
            Text(stringResource(R.string.export_start))
        }
    }
}

@Composable
fun YearMonthSelector(state: MutableState<YearMonth>, enabled: Boolean = true) {
    var month by state
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val currentMonth = YearMonth.now()
        val monthState = rememberFWheelPickerState(month.monthValue - 1)
        val yearState = rememberFWheelPickerState(month.year - MAGIC_START_YEAR)
        LaunchedEffect(monthState.currentIndex, yearState.currentIndex) {
            if (yearState.currentIndex >= 0 && monthState.currentIndex >= 0) {
                month = YearMonth.of(
                    yearState.currentIndex + MAGIC_START_YEAR,
                    monthState.currentIndex + 1
                )
            }
        }

        FVerticalWheelPicker(
            modifier = Modifier.weight(1f),
            count = 12,
            userScrollEnabled = enabled,
            state = monthState
        ) {
            val elem = Month.entries[it]
            Text(elem.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
        }
        Text("-", style = MaterialTheme.typography.bodyMedium)
        FVerticalWheelPicker(
            modifier = Modifier.weight(1f),
            count = currentMonth.year - MAGIC_START_YEAR + 1,
            userScrollEnabled = enabled,
            state = yearState
        ) {
            Text((it + MAGIC_START_YEAR).toString())
        }
    }
}


private fun requestFilePath(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>, file: File, mimeType: String) {
    val scope = CoroutineScope(Dispatchers.Main)
    scope.launch {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, file.name)

            if (VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Optionally, specify a URI for the directory that should be opened in
                // the system file picker before your app creates the document.
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
            }
        }
        launcher.launch(intent)
    }
}

private fun writeFile(context: Context, file: File, uri: Uri) {
    try {
        context.contentResolver.openFileDescriptor(uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { os ->
                os.write(file.readBytes())
            }
        }
    } catch (ex: Throwable) {
        Log.e("WCLK", "Error when saving export", ex)
    }
}

private fun shareFile(context: Context, file: File, onSuccess: () -> Unit) {
    val scope = CoroutineScope(Dispatchers.Main)
    if (VERSION.SDK_INT >= VERSION_CODES.Q) {
        val mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download")
            put(MediaStore.Downloads.MIME_TYPE, mimeTypeFromExtension)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        )
        if (uri != null) {
            writeFile(context, file, uri)
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, file.name)
                type = mimeTypeFromExtension
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(shareIntent, null))
            onSuccess()
        } else {
            scope.launch {
                Toast.makeText(
                    context,
                    R.string.export_share_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    } else {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(null)
        ) { _, shareUri ->
            val mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            scope.launch {
                if (shareUri != null) {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        putExtra(Intent.EXTRA_TITLE, file.name)
                        putExtra(Intent.EXTRA_SUBJECT, file.name)
                        type = mimeTypeFromExtension
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                    onSuccess()
                } else {
                    Toast.makeText(
                        context,
                        R.string.export_share_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

@Preview
@Composable
fun ExportPreview() {
    WorklockTheme(darkTheme = false, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ExportView(DummyExportController())
        }
    }
}

@Preview
@Composable
fun ExportDarkPreview() {
    WorklockTheme(darkTheme = true, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ExportView(DummyExportController())
        }
    }
}
