package com.example.export

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.model.ActivityReport
import com.example.model.ChecklistPeriod
import com.example.model.DateRangePreset
import com.example.model.LampStatus
import com.example.model.PJU_CLUSTERS
import com.example.model.PjuIssue
import com.example.model.buildCellKey
import java.io.File

object CsvAndPrintHelper {

    // Hold a strong reference during async WebView layout until print job starts
    private var activePrintWebView: WebView? = null

    fun buildCsvContent(
        period: ChecklistPeriod,
        issues: List<PjuIssue>,
        rangePreset: DateRangePreset
    ): String {
        val dates = rangePreset.dates()
        val lines = mutableListOf<String>()

        fun csvEscape(value: String): String =
            "\"" + value.replace("\"", "\"\"") + "\""

        lines.add(csvEscape("PERUMAHAN BUMI PAPAN SELARAS"))
        lines.add(csvEscape("FORM CHECKLIST PENERANGAN JALAN UMUM - BULAN ${period.month} ${period.year}"))
        lines.add("")

        val headerRow = listOf("NO.", "CLUSTER", "BARIS / TITIK PJU") + dates.map { "TGL $it" }
        lines.add(headerRow.joinToString(",") { csvEscape(it) })

        PJU_CLUSTERS.forEach { cluster ->
            val labels = period.slotLabels[cluster.id.toString()].orEmpty()
            for (slot in 0 until 3) {
                val slotLabel = labels.getOrNull(slot)?.takeIf { it.isNotBlank() } ?: "Baris ${slot + 1}"
                val row = mutableListOf(
                    if (slot == 0) cluster.id.toString() else "",
                    if (slot == 0) csvEscape(cluster.name) else "",
                    csvEscape(slotLabel)
                )
                dates.forEach { day ->
                    val code = period.checks[buildCellKey(cluster.id, slot, day)]
                    val status = LampStatus.fromCode(code)
                    row.add(csvEscape(status.csvLabel))
                }
                lines.add(row.joinToString(","))
            }
        }

        lines.add("")
        lines.add(csvEscape("LAPORAN PERMASALAHAN PJU"))
        lines.add(listOf("Tanggal", "Cluster", "Titik PJU", "Permasalahan", "Status").joinToString(",") { csvEscape(it) })
        issues.forEach { issue ->
            lines.add(
                listOf(
                    "${issue.date} ${period.month}",
                    issue.clusterName,
                    issue.unit,
                    issue.desc,
                    issue.status
                ).joinToString(",") { csvEscape(it) }
            )
        }

        lines.add("")
        lines.add("${csvEscape("CATATAN UMUM:")},${csvEscape(period.generalNotes)}")
        lines.add("${csvEscape("PENANGGUNG JAWAB:")},${csvEscape(period.picName)}")

        return lines.joinToString("\n")
    }

    fun shareCsv(
        context: Context,
        period: ChecklistPeriod,
        issues: List<PjuIssue>,
        rangePreset: DateRangePreset
    ): Result<Unit> = runCatching {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "Checklist_PJU_BPS_${period.month}_${period.year}.csv"
        val outFile = File(exportDir, fileName)
        val csv = buildCsvContent(period, issues, rangePreset)
        outFile.writeText("\uFEFF$csv", Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Checklist PJU BPS - ${period.month} ${period.year}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Bagikan / Simpan Export CSV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun printChecklistForm(
        context: Context,
        period: ChecklistPeriod,
        issues: List<PjuIssue>,
        rangePreset: DateRangePreset
    ): Result<Unit> = runCatching {
        val html = buildPrintableHtml(period, issues, rangePreset)
        val webView = WebView(context)
        activePrintWebView = webView
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null && view != null) {
                    val jobName = "Form Checklist PJU ${period.month} ${period.year}"
                    val adapter = view.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                        .build()
                    printManager.print(jobName, adapter, attributes)
                }
                activePrintWebView = null
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun htmlEscape(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun buildPrintableHtml(
        period: ChecklistPeriod,
        issues: List<PjuIssue>,
        rangePreset: DateRangePreset
    ): String {
        val dates = rangePreset.dates()
        val dateHeaders = dates.joinToString("") { "<th>$it</th>" }

        val rowsHtml = buildString {
            PJU_CLUSTERS.forEach { cluster ->
                val labels = period.slotLabels[cluster.id.toString()].orEmpty()
                for (slot in 0 until 3) {
                    val customLabel = labels.getOrNull(slot)?.takeIf { it.isNotBlank() }
                    append("<tr>")
                    if (slot == 0) {
                        append("<td rowspan='3' class='center bold'>${cluster.id}</td>")
                        val preview = labels.filter { it.isNotBlank() }.joinToString(", ")
                        append("<td rowspan='3' class='bold'>${htmlEscape(cluster.name)}")
                        if (preview.isNotBlank()) {
                            append("<div class='sublabel'>${htmlEscape(preview)}</div>")
                        }
                        append("</td>")
                    }
                    dates.forEach { day ->
                        val status = LampStatus.fromCode(period.checks[buildCellKey(cluster.id, slot, day)])
                        val titleAttr = customLabel?.let { " title='${htmlEscape(it)}'" } ?: ""
                        append("<td class='center symbol'$titleAttr>${status.symbol}</td>")
                    }
                    append("</tr>")
                }
            }
        }

        val issuesHtml = if (issues.isEmpty()) {
            "<p class='muted'>-</p>"
        } else {
            issues.mapIndexed { idx, issue ->
                "<div>${idx + 1}. [Tgl ${issue.date} ${htmlEscape(period.month)}] <b>${htmlEscape(issue.clusterName)}</b> (${htmlEscape(issue.unit)}): ${htmlEscape(issue.desc)} — <i>${htmlEscape(issue.status)}</i></div>"
            }.joinToString("")
        }

        return """
            <!DOCTYPE html>
            <html lang="id">
            <head>
              <meta charset="UTF-8" />
              <style>
                body { font-family: sans-serif; font-size: 9pt; color: #000; margin: 12px; }
                h1, h2, h3 { margin: 0; text-transform: uppercase; }
                h1 { font-size: 12pt; }
                h2 { font-size: 11pt; }
                .period { font-weight: bold; margin: 4px 0 10px 0; }
                table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }
                th, td { border: 1px solid #000; padding: 3px 5px; }
                th { background: #f1f5f9; text-align: center; }
                .center { text-align: center; }
                .bold { font-weight: bold; }
                .symbol { min-width: 22px; height: 18px; }
                .sublabel { font-weight: normal; font-size: 7.5pt; color: #475569; margin-top: 2px; }
                .notes-box { border: 1.5px solid #000; padding: 6px 8px; margin-bottom: 10px; min-height: 44px; }
                .footer { display: flex; justify-content: space-between; margin-top: 10px; }
                .ket { width: 65%; font-size: 8.5pt; font-weight: bold; }
                .pic { width: 32%; font-size: 8.5pt; font-weight: bold; }
                .sig-space { height: 38px; }
                .muted { color: #64748b; margin: 2px 0; }
              </style>
            </head>
            <body>
              <h1>PERUMAHAN BUMI PAPAN SELARAS</h1>
              <h2>FORM CHECKLIST PENERANGAN JALAN UMUM</h2>
              <div class="period">BULAN : ${htmlEscape(period.month)} ${period.year}</div>
              <table>
                <thead>
                  <tr>
                    <th rowspan="3" style="width:28px;">NO.</th>
                    <th rowspan="3" style="width:155px;">CLUSTER</th>
                    <th colspan="${dates.size}">KONDISI LAMPU</th>
                  </tr>
                  <tr>
                    <th colspan="${dates.size}">TANGGAL</th>
                  </tr>
                  <tr>$dateHeaders</tr>
                </thead>
                <tbody>$rowsHtml</tbody>
              </table>
              <div class="notes-box">
                <div class="bold" style="margin-bottom:4px;">CATATAN &amp; LAPORAN PERMASALAHAN PJU :</div>
                $issuesHtml
                ${if (period.generalNotes.isNotBlank()) "<div style='margin-top:4px;'>${htmlEscape(period.generalNotes).replace("\n", "<br/>")}</div>" else ""}
              </div>
              <div class="footer">
                <div class="ket">
                  <u>KETERANGAN :</u>
                  <ol style="margin:4px 0 0 16px; padding:0;">
                    <li>WAJIB MELAKUKAN CHECKLIST PADA SEMUA PJU MAKSIMAL 2 HARI SEKALI</li>
                    <li>APABILA PADA SAAT PENGECEKAN TERDAPAT MASALAH, MAKA WAJIB MENGISI PERMASALAHAN PJU PADA FORM YANG TERSEDIA</li>
                  </ol>
                </div>
                <div class="pic">
                  <div>Penanggung Jawab</div>
                  <div class="sig-space"></div>
                  <div style="border-top:1px dashed #333; padding-top:3px;">${htmlEscape(period.picName)}</div>
                </div>
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun shareActivitiesCsv(
        context: Context,
        workspaceName: String,
        reports: List<ActivityReport>,
        filterLabel: String
    ): Result<Unit> = runCatching {
        fun csvEscape(value: String): String =
            "\"" + value.replace("\"", "\"\"") + "\""

        val lines = mutableListOf<String>()
        lines.add(csvEscape("PERUMAHAN BUMI PAPAN SELARAS - $workspaceName"))
        lines.add(csvEscape("RANGKUMAN URAIAN KEGIATAN & PELAPORAN KEJADIAN ($filterLabel)"))
        lines.add("")
        lines.add(
            listOf("NO", "TANGGAL", "JAM", "JENIS", "NAMA ANGGOTA", "LOKASI", "URAIAN KEGIATAN / KEJADIAN", "STATUS")
                .joinToString(",") { csvEscape(it) }
        )
        reports.forEachIndexed { idx, r ->
            lines.add(
                listOf(
                    (idx + 1).toString(),
                    r.eventDate,
                    r.eventTime,
                    r.reportType,
                    r.memberName,
                    r.location.ifBlank { "-" },
                    r.description,
                    r.status
                ).joinToString(",") { csvEscape(it) }
            )
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(exportDir, "Rangkuman_Kegiatan_Kejadian_BPS.csv")
        outFile.writeText("\uFEFF${lines.joinToString("\n")}", Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Rangkuman Kegiatan & Kejadian - $workspaceName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Bagikan / Simpan CSV Kegiatan")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun printActivitySummary(
        context: Context,
        workspaceName: String,
        reports: List<ActivityReport>,
        filterLabel: String
    ): Result<Unit> = runCatching {
        val kegiatanCount = reports.count { it.reportType == "Kegiatan" }
        val kejadianCount = reports.count { it.reportType == "Kejadian" }
        val rowsHtml = if (reports.isEmpty()) {
            "<tr><td colspan='7' style='text-align:center;'>Belum ada laporan kegiatan atau kejadian.</td></tr>"
        } else {
            reports.mapIndexed { idx, r ->
                """
                <tr>
                  <td style='text-align:center;'>${idx + 1}</td>
                  <td>${htmlEscape(r.eventDate)}<br/><b>${htmlEscape(r.eventTime)}</b></td>
                  <td>${htmlEscape(r.reportType)}</td>
                  <td><b>${htmlEscape(r.memberName)}</b></td>
                  <td>${htmlEscape(r.location.ifBlank { "-" })}</td>
                  <td>${htmlEscape(r.description).replace("\n", "<br/>")}</td>
                  <td>${htmlEscape(r.status)}</td>
                </tr>
                """.trimIndent()
            }.joinToString("")
        }

        val html = """
            <!DOCTYPE html>
            <html lang="id">
            <head>
              <meta charset="UTF-8" />
              <style>
                body { font-family: sans-serif; font-size: 9.5pt; color: #000; margin: 14px; }
                h1, h2 { margin: 0; text-transform: uppercase; }
                h1 { font-size: 12.5pt; }
                h2 { font-size: 10.5pt; margin-top: 2px; }
                .meta { margin: 6px 0 12px 0; font-weight: bold; font-size: 9pt; }
                table { width: 100%; border-collapse: collapse; }
                th, td { border: 1px solid #1e293b; padding: 5px 6px; vertical-align: top; }
                th { background: #f1f5f9; text-align: left; }
              </style>
            </head>
            <body>
              <h1>PERUMAHAN BUMI PAPAN SELARAS</h1>
              <h2>RANGKUMAN URAIAN KEGIATAN &amp; PELAPORAN KEJADIAN (${htmlEscape(workspaceName)})</h2>
              <div class="meta">Filter: ${htmlEscape(filterLabel)} &bull; Total: ${reports.size} Laporan (Kegiatan: $kegiatanCount | Kejadian: $kejadianCount)</div>
              <table>
                <thead>
                  <tr>
                    <th style="width:28px;">No</th>
                    <th style="width:82px;">Tanggal / Jam</th>
                    <th style="width:68px;">Jenis</th>
                    <th style="width:110px;">Nama Anggota</th>
                    <th style="width:95px;">Lokasi</th>
                    <th>Uraian Kegiatan / Kejadian</th>
                    <th style="width:85px;">Status</th>
                  </tr>
                </thead>
                <tbody>$rowsHtml</tbody>
              </table>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        activePrintWebView = webView
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null && view != null) {
                    val jobName = "Rangkuman Kegiatan BPS"
                    val adapter = view.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asPortrait())
                        .build()
                    printManager.print(jobName, adapter, attributes)
                }
                activePrintWebView = null
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
