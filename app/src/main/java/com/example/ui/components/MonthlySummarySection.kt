package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.media.ProfilePhotoHelper
import com.example.model.AVAILABLE_YEARS
import com.example.model.DailyReport
import com.example.model.INDONESIAN_MONTHS
import com.example.model.TeamWorkspace
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.formatIndonesianFullDate
import com.example.model.monthPrefixIso
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber900
import com.example.ui.theme.Blue100
import com.example.ui.theme.Blue600
import com.example.ui.theme.Blue900
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlySummaryTabContent(
    workspace: TeamWorkspace,
    reportsState: UiState<List<DailyReport>>,
    profilesMap: Map<String, UserProfile>,
    selectedMonth: String,
    selectedYear: Int,
    onSelectMonth: (String) -> Unit,
    onSelectYear: (Int) -> Unit,
    onExportCsv: (List<DailyReport>) -> Unit,
    onPrintPdf: (List<DailyReport>) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var selectedMemberFilter by remember { mutableStateOf("") }
    var viewingPhotosState by remember { mutableStateOf<Pair<DailyReport, Int>?>(null) }

    val monthPrefix = remember(selectedYear, selectedMonth) {
        monthPrefixIso(selectedYear, selectedMonth)
    }

    val allReports = (reportsState as? UiState.Success)?.data.orEmpty()
    val monthReportsAllMembers = remember(allReports, monthPrefix) {
        allReports.filter { it.eventDate.startsWith(monthPrefix) }
    }
    val memberOptions = remember(monthReportsAllMembers) {
        monthReportsAllMembers.map { it.memberName.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredMonthReports = remember(monthReportsAllMembers, selectedMemberFilter) {
        if (selectedMemberFilter.isBlank()) monthReportsAllMembers
        else monthReportsAllMembers.filter { it.memberName.equals(selectedMemberFilter, ignoreCase = true) }
    }

    val groupedByDate = remember(filteredMonthReports) {
        filteredMonthReports
            .groupBy { it.eventDate }
            .entries
            .sortedByDescending { it.key }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (reportsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, Rose600.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = reportsState.message,
                            color = Rose600,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 980.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "month_selector_card") {
                        MonthlyPeriodSelectorCard(
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            onSelectMonth = onSelectMonth,
                            onSelectYear = onSelectYear,
                            memberOptions = memberOptions,
                            selectedMember = selectedMemberFilter,
                            onSelectMember = { selectedMemberFilter = it }
                        )
                    }

                    item(key = "monthly_summary_card") {
                        MonthlyOverviewSummaryCard(
                            workspaceName = workspace.name,
                            monthLabel = selectedMonth,
                            year = selectedYear,
                            reports = filteredMonthReports,
                            onCopyWhatsApp = {
                                val text = buildMonthlyWhatsAppSummary(
                                    workspaceName = workspace.name,
                                    monthLabel = selectedMonth,
                                    year = selectedYear,
                                    reports = filteredMonthReports
                                )
                                clipboard.setText(AnnotatedString(text))
                                onShowMessage("Rangkuman bulan $selectedMonth $selectedYear disalin")
                            },
                            onShareWhatsApp = {
                                val text = buildMonthlyWhatsAppSummary(
                                    workspaceName = workspace.name,
                                    monthLabel = selectedMonth,
                                    year = selectedYear,
                                    reports = filteredMonthReports
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Bagikan Rangkuman Bulanan")
                                )
                            },
                            onExportCsv = { onExportCsv(filteredMonthReports) },
                            onPrintPdf = { onPrintPdf(filteredMonthReports) }
                        )
                    }

                    if (groupedByDate.isEmpty()) {
                        item(key = "empty_monthly_state") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = Amber500,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "Belum Ada Laporan di Bulan $selectedMonth $selectedYear",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Semua laporan harian yang dicatat pada bulan ini akan otomatis dirangkum di halaman ini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(groupedByDate, key = { it.first }) { (dateIso, dayReports) ->
                            val firstDayName = dayReports.firstOrNull()?.dayName ?: ""
                            MonthlyDayGroupCard(
                                dateIso = dateIso,
                                dayName = firstDayName,
                                reports = dayReports,
                                profilesMap = profilesMap,
                                onOpenPhoto = { report, photoIdx ->
                                    viewingPhotosState = report to photoIdx
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    viewingPhotosState?.let { (report, idx) ->
        MultiPhotoViewerDialog(
            report = report,
            initialIndex = idx,
            onDismiss = { viewingPhotosState = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyPeriodSelectorCard(
    selectedMonth: String,
    selectedYear: Int,
    onSelectMonth: (String) -> Unit,
    onSelectYear: (Int) -> Unit,
    memberOptions: List<String>,
    selectedMember: String,
    onSelectMember: (String) -> Unit
) {
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Pilih Periode Rangkuman Bulanan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = monthExpanded,
                    onExpandedChange = { monthExpanded = it },
                    modifier = Modifier.weight(0.62f)
                ) {
                    OutlinedTextField(
                        value = selectedMonth,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bulan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = monthExpanded,
                        onDismissRequest = { monthExpanded = false }
                    ) {
                        INDONESIAN_MONTHS.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month) },
                                onClick = {
                                    onSelectMonth(month)
                                    monthExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = it },
                    modifier = Modifier.weight(0.38f)
                ) {
                    OutlinedTextField(
                        value = selectedYear.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tahun") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false }
                    ) {
                        AVAILABLE_YEARS.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    onSelectYear(year)
                                    yearExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (memberOptions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedMember.isBlank(),
                        onClick = { onSelectMember("") },
                        label = { Text("Semua Anggota", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate900,
                            selectedLabelColor = Color.White
                        )
                    )
                    memberOptions.forEach { member ->
                        val isSel = selectedMember.equals(member, ignoreCase = true)
                        FilterChip(
                            selected = isSel,
                            onClick = { onSelectMember(if (isSel) "" else member) },
                            label = { Text(member, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue600,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyOverviewSummaryCard(
    workspaceName: String,
    monthLabel: String,
    year: Int,
    reports: List<DailyReport>,
    onCopyWhatsApp: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onExportCsv: () -> Unit,
    onPrintPdf: () -> Unit
) {
    val activeDays = remember(reports) { reports.map { it.eventDate }.distinct().size }
    val totalPhotos = remember(reports) { reports.sumOf { it.photos.size } }
    val perMember = remember(reports) {
        reports.groupBy { it.memberName.trim().ifBlank { "Anggota" } }
            .mapValues { (_, list) -> list.size to list.sumOf { it.photos.size } }
            .entries
            .sortedByDescending { it.value.first }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Amber100,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Summarize,
                            contentDescription = null,
                            tint = Amber900,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Rangkuman Bulan $monthLabel $year",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = workspaceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonthlyKpiChip("Laporan", reports.size.toString(), Slate900, Color.White, Modifier.weight(1f))
                MonthlyKpiChip("Hari Aktif", activeDays.toString(), Blue100, Blue900, Modifier.weight(1f))
                MonthlyKpiChip("Total Foto", totalPhotos.toString(), Emerald100, Emerald900, Modifier.weight(1f))
                MonthlyKpiChip("Anggota", perMember.size.toString(), Amber100, Amber900, Modifier.weight(1f))
            }

            if (perMember.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    perMember.forEach { (name, pair) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald100
                        ) {
                            Text(
                                text = "$name: ${pair.first} lap • ${pair.second} foto",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Emerald900,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyWhatsApp,
                    enabled = reports.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin WA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onShareWhatsApp,
                    enabled = reports.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bagikan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportCsv,
                    enabled = reports.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("monthly_export_csv_button")
                ) {
                    Icon(Icons.Filled.TableView, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onPrintPdf,
                    enabled = reports.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("monthly_print_pdf_button")
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, tint = Blue600, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cetak PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MonthlyKpiChip(
    label: String,
    value: String,
    bgColor: Color,
    fgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.ExtraBold,
                color = fgColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MonthlyDayGroupCard(
    dateIso: String,
    dayName: String,
    reports: List<DailyReport>,
    profilesMap: Map<String, UserProfile>,
    onOpenPhoto: (DailyReport, Int) -> Unit
) {
    val fullDate = remember(dateIso, dayName) { formatIndonesianFullDate(dateIso, dayName) }
    val totalDayPhotos = remember(reports) { reports.sumOf { it.photos.size } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fullDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Blue900
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Amber100
                ) {
                    Text(
                        text = "${reports.size} lap • $totalDayPhotos foto",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Amber900,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            reports.forEachIndexed { idx, report ->
                if (idx > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                val profile = profilesMap[report.createdByUid]
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            MemberAvatar(
                                displayName = report.memberName,
                                photoBase64 = profile?.photoBase64 ?: "",
                                size = 30.dp
                            )
                            Text(
                                text = report.memberName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Jam ${report.eventTime}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (report.location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = report.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = report.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (report.photos.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            report.photos.take(4).forEachIndexed { photoIdx, b64 ->
                                val bmp = remember(b64) {
                                    ProfilePhotoHelper.decodeBase64ToBitmap(b64)?.asImageBitmap()
                                }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = "Thumbnail foto ${photoIdx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onOpenPhoto(report, photoIdx) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildMonthlyWhatsAppSummary(
    workspaceName: String,
    monthLabel: String,
    year: Int,
    reports: List<DailyReport>
): String {
    val activeDays = reports.map { it.eventDate }.distinct().size
    val totalPhotos = reports.sumOf { it.photos.size }
    return buildString {
        appendLine("*RANGKUMAN BULANAN LAPORAN HARIAN BPS*")
        appendLine("Periode: *$monthLabel $year* | Tim: *$workspaceName*")
        appendLine("Total: *${reports.size} Laporan* ($activeDays hari aktif, $totalPhotos foto dokumentasi)")
        appendLine("--------------------------------")
        reports.forEachIndexed { idx, r ->
            val fullDate = formatIndonesianFullDate(r.eventDate, r.dayName)
            append("${idx + 1}. *$fullDate* • Jam *${r.eventTime}*")
            appendLine(" — *${r.memberName}*")
            if (r.location.isNotBlank()) {
                appendLine("   Area: ${r.location}")
            }
            appendLine("   Keterangan: ${r.description}")
            appendLine("   Lampiran: ${r.photos.size} foto")
        }
    }.trim()
}
