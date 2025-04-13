package de.menkalian.worklock.controller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.menkalian.worklock.BuildConfig
import de.menkalian.worklock.R
import de.menkalian.worklock.database.DataAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.PrintWriter
import java.time.*
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ExportControllerImpl
@Inject constructor(
    private val application: Application,
    private val dataAccess: DataAccess,
) : AndroidViewModel(application), ExportController {
    private val exportsDir = File(application.filesDir, "exports")
    private val settingsController = SettingsControllerImpl(application, dataAccess)

    init {
        exportsDir.mkdirs()
        exportsDir.listFiles()?.forEach { it.delete() }
    }

    override suspend fun createExport(month: YearMonth, type: ExportType): File {
        return when (type) {
            ExportType.CSV_RECORDS     -> createCsvRecordExport(month)
            ExportType.CSV_AGGREGATE   -> createCsvAggregateExport(month)
            ExportType.EXCEL_RECORDS   -> createExcelRecordExport(month, month)
            ExportType.EXCEL_AGGREGATE -> createExcelAggregateExport(month, month)
        }
    }

    override suspend fun createRangeExport(start: YearMonth, until: YearMonth, type: ExportType): File {
        return when (type) {
            ExportType.CSV_RECORDS     -> throw UnsupportedOperationException("Range export is not supported for CSV")
            ExportType.CSV_AGGREGATE   -> throw UnsupportedOperationException("Range export is not supported for CSV")
            ExportType.EXCEL_RECORDS   -> createExcelRecordExport(start, until)
            ExportType.EXCEL_AGGREGATE -> createExcelAggregateExport(start, until)
        }
    }

    override fun getMimeType(exportType: ExportType): String {
        return when (exportType) {
            ExportType.CSV_RECORDS, ExportType.CSV_AGGREGATE     -> "text/csv"
            ExportType.EXCEL_RECORDS, ExportType.EXCEL_AGGREGATE -> "*/*"
        }
    }

    private suspend fun createExcelRecordExport(start: YearMonth, until: YearMonth): File {
        val exportFile = File(exportsDir, "Export_Excel_Records_${start.year}-${start.monthValue}_${start.year}-${start.monthValue}.xlsx")
        val workbook: Workbook = XSSFWorkbook()

        var month = start
        do {
            addMonthRecords(workbook, month)
            month = month.plusMonths(1)
        } while (month <= until)
        addPersonalDataSheet(workbook)

        exportFile.outputStream().use { str ->
            workbook.write(str)
            workbook.close()
        }
        return exportFile
    }

    private suspend fun createExcelAggregateExport(start: YearMonth, until: YearMonth): File {
        val exportFile = File(exportsDir, "Export_Excel_Aggregate_${start.year}-${start.monthValue}_${start.year}-${start.monthValue}.xlsx")
        val workbook: Workbook = XSSFWorkbook()

        var month = start
        do {
            addMonthAggregate(workbook, month)
            month = month.plusMonths(1)
        } while (month <= until)
        addPersonalDataSheet(workbook)

        exportFile.outputStream().use { str ->
            workbook.write(str)
            workbook.close()
        }
        return exportFile
    }

    private suspend fun addMonthRecords(workbook: Workbook, month: YearMonth) {
        val withManual = includeManualColumn()
        val records = dataAccess.getRecordsForMonth(month, includeCorrected = withManual, includeDeleted = withManual)

        val sheet = workbook.createSheet(month.format(DateTimeFormatter.ofPattern("LLLL yyyy")))
        sheet.setColumnWidth(0, 12 * 256)
        sheet.setColumnWidth(1, 20 * 256)
        sheet.setColumnWidth(2, 12 * 256)
        sheet.setColumnWidth(3, 7 * 256)
        sheet.setColumnWidth(4, 4 * 256)
        sheet.setColumnWidth(5, 12 * 256)
        sheet.setColumnWidth(6, 4 * 256)

        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue(application.getString(R.string.export_excel_sheet_month_record_id))
        headerRow.createCell(1).setCellValue(application.getString(R.string.export_excel_sheet_month_record_time))
        headerRow.createCell(2).setCellValue(application.getString(R.string.export_excel_sheet_month_record_time_unix))
        headerRow.createCell(3).setCellValue(application.getString(R.string.export_excel_sheet_month_record_type))
        if (withManual) {
            headerRow.createCell(4).setCellValue(application.getString(R.string.export_excel_sheet_month_record_manual))
            headerRow.createCell(5).setCellValue(application.getString(R.string.export_excel_sheet_month_record_overwrites))
            headerRow.createCell(6).setCellValue(application.getString(R.string.export_excel_sheet_month_record_deleted))
        }

        records.forEachIndexed { idx, record ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(record.id.toDouble())
            row.createCell(1).setCellValue(LocalDateTime.ofInstant(record.timestamp, ZoneId.systemDefault()))
            row.createCell(2).setCellValue(record.timestamp.epochSecond.toDouble())
            row.createCell(3).setCellValue(record.type.name)
            if (withManual) {
                row.createCell(4).setCellValue(record.manual)
                row.createCell(5).setCellValue(record.correctionForId?.toString() ?: "")
                row.createCell(6).setCellValue(record.deleted)
            }
            if (record.deleted || records.any { it.correctionForId == record.id }) {
                row.zeroHeight = true
            }
        }
    }

    private suspend fun addMonthAggregate(workbook: Workbook, month: YearMonth) {
        val withManual = includeManualColumn()
        val withPause = includePauseColumn()
        val withError = includeErrorColumn()
        val sheet = workbook.createSheet(month.format(DateTimeFormatter.ofPattern("LLLL yyyy")))
        sheet.setColumnWidth(0, 12 * 256)
        sheet.setColumnWidth(1, 15 * 256)
        sheet.setColumnWidth(2, 15 * 256)
        sheet.setColumnWidth(3, 15 * 256)
        sheet.setColumnWidth(4, 15 * 256)
        sheet.setColumnWidth(5, 15 * 256)

        val headerRow = sheet.createRow(0)
        var index = 0
        headerRow.createCell(index++).setCellValue(application.getString(R.string.export_excel_sheet_month_aggreg_date))
        if (withError) {
            headerRow.createCell(index++).setCellValue(application.getString(R.string.export_excel_sheet_month_aggreg_error))
        }
        if (withPause) {
            headerRow.createCell(index++).setCellValue(application.getString(R.string.export_excel_sheet_month_aggreg_total))
        }
        headerRow.createCell(index++).setCellValue(application.getString(R.string.export_excel_sheet_month_aggreg_worktime))
        if (withPause) {
            headerRow.createCell(index++).setCellValue(application.getString(R.string.export_excel_sheet_month_aggreg_pausetime))
        }
        if (withManual) {
            headerRow.createCell(index++).setCellValue(application.getString(R.string.export_excel_sheet_month_aggreg_manual))
        }

        (1..month.lengthOfMonth())
            .map { month.atDay(it) }
            .map { it to dataAccess.getRecordsForDate(it, includeCorrected = false, includeDeleted = false).sortedBy { it.timestamp } }
            .withIndex()
            .map { (idx, pair) ->
                val (date, records) = pair
                val (e1, worktime) = getFullTimeForDayInMinutes(records)
                val (e2, pausetime) = getPauseTimeForDayInMinutes(records)
                val total = worktime + pausetime
                fun Int.formatTime(): String = "${(this / 60).toString().padStart(2, '0')}:${(this % 60).toString().padStart(2, '0')}:00"

                val row = sheet.createRow(idx + 1)
                index = 0
                row.createCell(index++).setCellValue(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                if (withError) {
                    row.createCell(index++).setCellValue(e1 || e2)
                }
                if (withPause) {
                    row.createCell(index++).setCellValue(total.formatTime())
                }
                row.createCell(index++).setCellValue(worktime.formatTime())
                if (withPause) {
                    row.createCell(index++).setCellValue(pausetime.formatTime())
                }
                if (withManual) {
                    row.createCell(index++).setCellValue(records.any { it.manual })
                }

                row.zeroHeight = worktime == 0
            }
    }

    private suspend fun addPersonalDataSheet(workbook: Workbook) {
        val personalData = settingsController.getAllPersonalData()
        if (personalData.isEmpty() || personalData.all { !it.enabled }) {
            return
        }

        val sheet = workbook.createSheet(application.getString(R.string.export_excel_sheet_additional))
        sheet.setColumnWidth(0, 40 * 256)
        sheet.setColumnWidth(1, 40 * 256)

        for (idx in personalData.indices) {
            val data = personalData[idx]
            val row = sheet.createRow(idx)
            row.createCell(0).setCellValue(data.name)
            row.createCell(1).setCellValue(data.value)
        }
    }

    private suspend fun createCsvRecordExport(month: YearMonth): File {
        val manualColumn = includeManualColumn()

        val exportFile = File(exportsDir, "Export_CSV_Records_${month.year}_${month.monthValue}.csv")
        val printStream = withContext(Dispatchers.IO) {
            PrintWriter(exportFile)
        }

        printStream.use { printer ->
            val header = buildString {
                append("# Export from WorKlock V${BuildConfig.VERSION_NAME}\n")
                append("# Fields:\n")
                append("#   ID: Unique ID of the record\n")
                append("#   TIMESTAMP: Timestamp of the record in Unix-Seconds (since Epoch)\n")
                append("#   TYPE: Type of the record (one of 'Start', 'End', 'Pause', 'Unpause'\n")
                if (manualColumn) {
                    append("#   MANUAL: Flag whether the record was manually altered ('true' or 'false')\n")
                    append("#   OVERWRITES_ID: ID of another record that is replaced by this record (blank if none)\n")
                    append("#   DELETED: Flag whether the record is deleted ('true' or 'false')\n")
                }
                append("#\n")
                append("# Personal information:\n")
                settingsController.getAllPersonalData().forEach {
                    if (it.enabled) {
                        append("# ${it.name}: ${it.value}\n")
                    }
                }
                append("#\n")
                append("# <<Remove lines until this line if importing into any program fails.>>\n")
                append("ID,TIMESTAMP,TYPE")
                if (manualColumn) {
                    append(",MANUAL,OVERWRITES_ID,DELETED")
                }
            }
            printer.println(header)


            val allRecords = dataAccess.getRecordsForMonth(month, manualColumn, manualColumn).sortedBy { it.timestamp }
            allRecords
                .map { rec ->
                    buildString {
                        append(rec.id).append(",")
                        append(rec.timestamp.epochSecond).append(",")
                        append(rec.type)
                        if (manualColumn) {
                            append(",")
                            append(rec.manual).append(",")
                            append(rec.correctionForId ?: "").append(",")
                            append(rec.deleted)
                        }
                    }
                }
                .forEach {
                    printer.println(it)
                }
        }

        return exportFile
    }

    private suspend fun createCsvAggregateExport(month: YearMonth): File {
        val manualColumn = includeManualColumn()
        val errorColumn = includeErrorColumn()

        val exportFile = File(exportsDir, "Export_CSV_Aggregate_${month.year}_${month.monthValue}.csv")
        val printStream = withContext(Dispatchers.IO) {
            PrintWriter(exportFile)
        }

        printStream.use { printer ->
            val header = buildString {
                append("# Export from WorKlock V${BuildConfig.VERSION_NAME}\n")
                append("# Fields:\n")
                append("#   DATE: Date in format yyyy-mm-dd (e.g. 2024-04-24)\n")
                append("#   WORKTIME: Amount of worked time in minutes (already excluding pauses)\n")
                append("#   PAUSETIME: Amount of pause time in minutes\n")
                if (errorColumn) {
                    append("#   ERROR: Flag whether the day contains any errors ('*' or blank)\n")
                }
                if (manualColumn) {
                    append("#   MANUAL: Flag whether the day contains any manual records ('*' or blank)\n")
                }
                append("#\n")
                append("# Personal information:\n")
                settingsController.getAllPersonalData().forEach {
                    if (it.enabled) {
                        append("# ${it.name}: ${it.value}\n")
                    }
                }
                append("#\n")
                append("# <<Remove lines until this line if importing in any program fails.>>\n")
                append("DATE,WORKTIME,PAUSETIME")
                if (errorColumn) {
                    append(",ERROR")
                }
                if (manualColumn) {
                    append(",MANUAL")
                }
            }
            printer.println(header)

            (1..month.lengthOfMonth())
                .map { month.atDay(it) }
                .map { it to dataAccess.getRecordsForDate(it, includeCorrected = false, includeDeleted = false).sortedBy { it.timestamp } }
                .map { (date, records) ->
                    val (e1, worktime) = getFullTimeForDayInMinutes(records)
                    val (e2, pausetime) = getPauseTimeForDayInMinutes(records)

                    buildString {
                        append(date.format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",")
                        append(worktime).append(",")
                        append(pausetime)
                        if (errorColumn) {
                            append(",").append(if (e1 || e2) "*" else "")
                        }
                        if (manualColumn) {
                            append(",").append(if (records.any { it.manual }) "*" else "")
                        }
                    }
                }
                .forEach { printer.println(it) }
        }

        return exportFile
    }

    private suspend fun includeManualColumn(): Boolean {
        return settingsController.isExportIncludeManualFlag()
    }

    private suspend fun includeErrorColumn(): Boolean {
        return settingsController.isExportIncludeErrorsFlag()
    }

    private suspend fun includePauseColumn(): Boolean {
        return settingsController.isExportIncludePause()
    }
}