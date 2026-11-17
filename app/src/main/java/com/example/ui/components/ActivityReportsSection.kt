package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.media.ChatPhotoHelper
import com.example.media.ProfilePhotoHelper
import com.example.model.ActivityReport
import com.example.model.ActivityReportStatus
import com.example.model.ActivityReportType
import com.example.model.TeamWorkspace
import com.example.model.UiState
import com.example.model.UserProfile
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
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose600
import com.example.ui.theme.Rose900
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ActivityFilterMode(val label: String) {
    ALL("Semua"),
    TODAY("Hari Ini"),
    KEGIATAN("Kegiatan"),
    KEJADIAN("Kejadian")
}

@Composable
fun ActivityReportsTabContent(
    workspace: TeamWorkspace,
    reportsState: UiState<List<ActivityReport>>,
    profilesMap: Map<String, UserProfile>,
    currentUserId: String,
    myDisplayName: String,
    onAddReport: (
        memberName: String,
        reportType: ActivityReportType,
        eventDate: String,
        eventTime: String,
        location: String,
        description: String,
        status: ActivityReportStatus,
        photoBase64: String,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onCycleStatus: (ActivityReport) -> Unit,
    onDeleteReport: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var filterMode by remember { mutableStateOf(ActivityFilterMode.ALL) }
    var selectedMemberFilter by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var viewingPhotoReport by remember { mutableStateOf<ActivityReport?>(null) }

    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    val allReports = (reportsState as? UiState.Success)?.data.orEmpty()
    val memberOptions = remember(allReports) {
        allReports.map { it.memberName.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredReports = remember(allReports, filterMode, selectedMemberFilter, todayIso) {
        allReports.filter { r ->
            val modeMatch = when (filterMode) {
                ActivityFilterMode.ALL -> true
                ActivityFilterMode.TODAY -> r.eventDate == todayIso
                ActivityFilterMode.KEGIATAN -> r.reportType == ActivityReportType.KEGIATAN.code
                ActivityFilterMode.KEJADIAN -> r.reportType == ActivityReportType.KEJADIAN.code
            }
            val memberMatch = selectedMemberFilter.isBlank() ||
                r.memberName.equals(selectedMemberFilter, ignoreCase = true)
            modeMatch && memberMatch
        }
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
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "activity_summary_card") {
                        ActivitySummaryCard(
                            workspaceName = workspace.name,
                            reports = filteredReports,
                            filterLabel = buildString {
                                append(filterMode.label)
                                if (selectedMemberFilter.isNotBlank()) append(" • $selectedMemberFilter")
                            },
                            onCopySummary = {
                                val summaryText = buildWhatsAppSummaryText(
                                    workspaceName = workspace.name,
                                    reports = filteredReports,
                                    filterLabel = filterMode.label
                                )
                                clipboard.setText(AnnotatedString(summaryText))
                                onShowMessage("Rangkuman kegiatan & kejadian disalin")
                            },
                            onShareSummary = {
                                val summaryText = buildWhatsAppSummaryText(
                                    workspaceName = workspace.name,
                                    reports = filteredReports,
                                    filterLabel = filterMode.label
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, summaryText)
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Bagikan Rangkuman Kegiatan")
                                )
                            },
                            onAddClick = { showAddDialog = true }
                        )
                    }

                    item(key = "activity_filters") {
                        ActivityFilterCard(
                            filterMode = filterMode,
                            onSelectMode = { filterMode = it },
                            memberOptions = memberOptions,
                            selectedMember = selectedMemberFilter,
                            onSelectMember = { selectedMemberFilter = it }
                        )
                    }

                    if (filteredReports.isEmpty()) {
                        item(key = "empty_activity_state") {
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
                                        imageVector = Icons.Filled.Assignment,
                                        contentDescription = null,
                                        tint = Amber500,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "Belum Ada Catatan Kegiatan / Kejadian",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ketuk tombol + Catat Laporan untuk mencatat uraian kegiatan patroli atau kejadian beserta nama anggota, jam, dan tanggal.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredReports, key = { it.id.ifBlank { "${it.createdByUid}_${it.eventDate}_${it.eventTime}" } }) { report ->
                            val canDelete = report.createdByUid == currentUserId || workspace.ownerId == currentUserId
                            ActivityReportCard(
                                report = report,
                                creatorProfile = profilesMap[report.createdByUid],
                                canDelete = canDelete,
                                onCycleStatus = { onCycleStatus(report) },
                                onOpenPhoto = { viewingPhotoReport = report },
                                onDelete = { onDeleteReport(report.id) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Amber500,
            contentColor = Slate950,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_activity_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Catat Kegiatan")
                Text(
                    text = "Catat Laporan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showAddDialog) {
        val suggestedMembers = remember(workspace, profilesMap, myDisplayName) {
            val names = mutableListOf(myDisplayName)
            workspace.memberUids.forEach { uid ->
                val name = profilesMap[uid]?.displayName?.ifBlank { null }
                    ?: workspace.memberNames[uid]
                if (!name.isNullOrBlank() && names.none { it.equals(name, ignoreCase = true) }) {
                    names.add(name)
                }
            }
            names
        }
        AddActivityReportDialog(
            defaultMemberName = myDisplayName,
            suggestedMembers = suggestedMembers,
            onDismiss = { showAddDialog = false },
            onSubmit = { memberName, type, date, time, location, desc, status, photoB64, onDone ->
                onAddReport(memberName, type, date, time, location, desc, status, photoB64, onDone)
            },
            onShowError = onShowMessage
        )
    }

    viewingPhotoReport?.let { photoReport ->
        ActivityPhotoViewerDialog(
            report = photoReport,
            onDismiss = { viewingPhotoReport = null }
        )
    }
}

@Composable
private fun ActivitySummaryCard(
    workspaceName: String,
    reports: List<ActivityReport>,
    filterLabel: String,
    onCopySummary: () -> Unit,
    onShareSummary: () -> Unit,
    onAddClick: () -> Unit
) {
    val kegiatanCount = reports.count { it.reportType == ActivityReportType.KEGIATAN.code }
    val kejadianCount = reports.count { it.reportType == ActivityReportType.KEJADIAN.code }
    val pendingCount = reports.count { it.status != ActivityReportStatus.SELESAI.label }
    val perMemberCounts = remember(reports) {
        reports.groupingBy { it.memberName.trim().ifBlank { "Anggota" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
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
                            text = "Rangkuman Kegiatan & Kejadian",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$workspaceName • $filterLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryStatChip(
                    label = "Total",
                    value = reports.size.toString(),
                    bgColor = Slate900,
                    fgColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatChip(
                    label = "Kegiatan",
                    value = kegiatanCount.toString(),
                    bgColor = Blue100,
                    fgColor = Blue900,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatChip(
                    label = "Kejadian",
                    value = kejadianCount.toString(),
                    bgColor = Rose100,
                    fgColor = Rose900,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatChip(
                    label = "Proses",
                    value = pendingCount.toString(),
                    bgColor = Amber100,
                    fgColor = Amber900,
                    modifier = Modifier.weight(1f)
                )
            }

            if (perMemberCounts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pelapor:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    perMemberCounts.forEach { (name, count) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald100
                        ) {
                            Text(
                                text = "$name ($count)",
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
                    onClick = onCopySummary,
                    enabled = reports.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_activity_summary_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onShareSummary,
                    enabled = reports.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_activity_summary_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bagikan", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber500,
                        contentColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SummaryStatChip(
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
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
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
private fun ActivityFilterCard(
    filterMode: ActivityFilterMode,
    onSelectMode: (ActivityFilterMode) -> Unit,
    memberOptions: List<String>,
    selectedMember: String,
    onSelectMember: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActivityFilterMode.entries.forEach { mode ->
                    val selected = filterMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectMode(mode) },
                        label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate900,
                            selectedLabelColor = Color.White
                        )
                    )
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
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    FilterChip(
                        selected = selectedMember.isBlank(),
                        onClick = { onSelectMember("") },
                        label = { Text("Semua Anggota", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Blue600,
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
private fun ActivityReportCard(
    report: ActivityReport,
    creatorProfile: UserProfile?,
    canDelete: Boolean,
    onCycleStatus: () -> Unit,
    onOpenPhoto: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncident = report.reportType == ActivityReportType.KEJADIAN.code
    val statusObj = ActivityReportStatus.fromLabel(report.status)
    val (statusBg, statusFg) = when (statusObj) {
        ActivityReportStatus.TERCATAT -> Blue100 to Blue900
        ActivityReportStatus.PROSES -> Amber100 to Amber900
        ActivityReportStatus.SELESAI -> Emerald100 to Emerald900
    }
    val formattedDate = remember(report.eventDate) { formatIndonesianDate(report.eventDate) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isIncident) Rose600.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    MemberAvatar(
                        displayName = report.memberName,
                        photoBase64 = creatorProfile?.photoBase64 ?: "",
                        size = 38.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = report.memberName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$formattedDate • Jam ${report.eventTime}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (canDelete && report.id.isNotBlank()) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Hapus laporan",
                            tint = Rose600
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isIncident) Rose100 else Blue100
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isIncident) Icons.Filled.ReportProblem else Icons.Filled.Assignment,
                            contentDescription = null,
                            tint = if (isIncident) Rose900 else Blue900,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isIncident) "Pelaporan Kejadian" else "Uraian Kegiatan",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncident) Rose900 else Blue900
                        )
                    }
                }

                if (report.location.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (report.photoBase64.isNotBlank()) {
                val bmp = remember(report.photoBase64) {
                    ProfilePhotoHelper.decodeBase64ToBitmap(report.photoBase64)?.asImageBitmap()
                }
                if (bmp != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenPhoto)
                    ) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Foto dokumentasi kegiatan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate950.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ZoomIn,
                                    contentDescription = "Perbesar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Perbesar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ketuk status untuk memperbarui:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    onClick = onCycleStatus,
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = report.status,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityReportDialog(
    defaultMemberName: String,
    suggestedMembers: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (
        memberName: String,
        reportType: ActivityReportType,
        eventDate: String,
        eventTime: String,
        location: String,
        description: String,
        status: ActivityReportStatus,
        photoBase64: String,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onShowError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val nowCal = remember { Calendar.getInstance() }
    var reportType by remember { mutableStateOf(ActivityReportType.KEGIATAN) }
    var memberName by remember(defaultMemberName) { mutableStateOf(defaultMemberName) }
    var eventDate by remember {
        mutableStateOf(
            String.format(
                Locale.US,
                "%04d-%02d-%02d",
                nowCal.get(Calendar.YEAR),
                nowCal.get(Calendar.MONTH) + 1,
                nowCal.get(Calendar.DAY_OF_MONTH)
            )
        )
    }
    var eventTime by remember {
        mutableStateOf(
            String.format(
                Locale.US,
                "%02d:%02d",
                nowCal.get(Calendar.HOUR_OF_DAY),
                nowCal.get(Calendar.MINUTE)
            )
        )
    }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ActivityReportStatus.SELESAI) }
    var statusExpanded by remember { mutableStateOf(false) }

    var photoBase64 by remember { mutableStateOf("") }
    var isEncodingPhoto by remember { mutableStateOf(false) }
    var showPhotoMenu by remember { mutableStateOf(false) }
    var pendingCamUri by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    fun encodeUri(uri: Uri) {
        isEncodingPhoto = true
        scope.launch {
            val res = ChatPhotoHelper.encodeUriToBase64(context, uri)
            isEncodingPhoto = false
            res.onSuccess { b64 ->
                photoBase64 = b64
            }.onFailure { err ->
                onShowError(err.localizedMessage ?: "Gagal memproses foto")
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) encodeUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCamUri
        pendingCamUri = null
        if (success && uri != null) encodeUri(uri)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Catat Kegiatan / Kejadian",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActivityReportType.entries.forEach { t ->
                        val selected = reportType == t
                        FilterChip(
                            selected = selected,
                            onClick = {
                                reportType = t
                                if (t == ActivityReportType.KEJADIAN && status == ActivityReportStatus.SELESAI) {
                                    status = ActivityReportStatus.PROSES
                                }
                            },
                            label = {
                                Text(
                                    text = t.labelId,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (t == ActivityReportType.KEJADIAN) Rose600 else Blue600,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = memberName,
                    onValueChange = {
                        memberName = it.take(80)
                        validationError = null
                    },
                    label = { Text("Nama Anggota") },
                    placeholder = { Text("Nama petugas yang bertugas") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_member_name_input")
                )

                if (suggestedMembers.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedMembers.forEach { name ->
                            FilterChip(
                                selected = memberName.equals(name, ignoreCase = true),
                                onClick = { memberName = name },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val parts = eventDate.split("-")
                            val y = parts.getOrNull(0)?.toIntOrNull() ?: nowCal.get(Calendar.YEAR)
                            val m = (parts.getOrNull(1)?.toIntOrNull() ?: (nowCal.get(Calendar.MONTH) + 1)) - 1
                            val d = parts.getOrNull(2)?.toIntOrNull() ?: nowCal.get(Calendar.DAY_OF_MONTH)
                            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                eventDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            }, y, m, d).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.25f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatIndonesianDate(eventDate),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val parts = eventTime.split(":")
                            val hh = parts.getOrNull(0)?.toIntOrNull() ?: nowCal.get(Calendar.HOUR_OF_DAY)
                            val mm = parts.getOrNull(1)?.toIntOrNull() ?: nowCal.get(Calendar.MINUTE)
                            TimePickerDialog(context, { _, hourOfDay, minute ->
                                eventTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                            }, hh, mm, true).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.95f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = eventTime,
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily)
                        )
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it.take(100) },
                    label = { Text("Lokasi / Cluster (Opsional)") },
                    placeholder = { Text("Contoh: Pos Gerbang / Cluster Cempaka") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it.take(1500)
                        validationError = null
                    },
                    label = {
                        Text(
                            if (reportType == ActivityReportType.KEJADIAN) "Uraian / Kronologi Kejadian"
                            else "Uraian Kegiatan"
                        )
                    },
                    placeholder = {
                        Text(
                            if (reportType == ActivityReportType.KEJADIAN)
                                "Jelaskan kejadian, waktu, pihak terkait, atau tindakan awal..."
                            else
                                "Contoh: Patroli keliling blok A-D, pengecekan gerbang & penerangan aman..."
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_description_input")
                )

                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = status.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        ActivityReportStatus.entries.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.label) },
                                onClick = {
                                    status = st
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { showPhotoMenu = true },
                        enabled = !isEncodingPhoto && !isSaving,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when {
                                isEncodingPhoto -> "Memproses foto..."
                                photoBase64.isNotBlank() -> "Ganti Foto Lampiran"
                                else -> "Lampirkan Foto (Opsional)"
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = showPhotoMenu,
                        onDismissRequest = { showPhotoMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ambil Foto Kamera") },
                            leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                            onClick = {
                                showPhotoMenu = false
                                runCatching { ChatPhotoHelper.createTempCameraUri(context) }
                                    .onSuccess { uri ->
                                        pendingCamUri = uri
                                        cameraLauncher.launch(uri)
                                    }
                                    .onFailure { err ->
                                        onShowError(err.localizedMessage ?: "Gagal membuka kamera")
                                    }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pilih dari Galeri") },
                            leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                            onClick = {
                                showPhotoMenu = false
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                }

                if (photoBase64.isNotBlank()) {
                    val bmp = remember(photoBase64) {
                        ProfilePhotoHelper.decodeBase64ToBitmap(photoBase64)?.asImageBitmap()
                    }
                    if (bmp != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Pratinjau foto",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = "Foto siap dilampirkan",
                                style = MaterialTheme.typography.bodySmall,
                                color = Emerald600,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { photoBase64 = "" }) {
                                Text("Hapus", color = Rose600)
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = Rose600,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mName = memberName.trim()
                    val desc = description.trim()
                    if (mName.isBlank()) {
                        validationError = "Nama anggota wajib diisi."
                    } else if (desc.isBlank()) {
                        validationError = "Uraian kegiatan/kejadian wajib diisi."
                    } else {
                        isSaving = true
                        onSubmit(mName, reportType, eventDate, eventTime, location.trim(), desc, status, photoBase64) { ok ->
                            isSaving = false
                            if (ok) onDismiss()
                        }
                    }
                },
                enabled = !isEncodingPhoto && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber500,
                    contentColor = Slate950
                ),
                modifier = Modifier.testTag("submit_activity_report_button")
            ) {
                Text(if (isSaving) "Menyimpan..." else "Simpan Laporan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ActivityPhotoViewerDialog(
    report: ActivityReport,
    onDismiss: () -> Unit
) {
    val bmp = remember(report.photoBase64) {
        ProfilePhotoHelper.decodeBase64ToBitmap(report.photoBase64)?.asImageBitmap()
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${report.memberName} • ${report.reportType}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${formatIndonesianDate(report.eventDate)} • ${report.eventTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Amber500
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup foto",
                            tint = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Foto dokumentasi ukuran penuh",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = report.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

private fun formatIndonesianDate(isoDate: String): String {
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = parser.parse(isoDate)
        if (parsed != null) {
            SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(parsed)
        } else {
            isoDate
        }
    }.getOrDefault(isoDate)
}

fun buildWhatsAppSummaryText(
    workspaceName: String,
    reports: List<ActivityReport>,
    filterLabel: String
): String = buildString {
    val kegiatanCount = reports.count { it.reportType == ActivityReportType.KEGIATAN.code }
    val kejadianCount = reports.count { it.reportType == ActivityReportType.KEJADIAN.code }
    appendLine("*RANGKUMAN KEGIATAN & KEJADIAN*")
    appendLine("*$workspaceName* ($filterLabel)")
    appendLine("Total: ${reports.size} laporan (Kegiatan: $kegiatanCount | Kejadian: $kejadianCount)")
    appendLine("--------------------------------")
    reports.forEachIndexed { idx, r ->
        val locPart = if (r.location.isNotBlank()) " di ${r.location}" else ""
        appendLine("${idx + 1}. *[${r.reportType.uppercase()}]* ${formatIndonesianDate(r.eventDate)} Jam ${r.eventTime}")
        appendLine("   Anggota: ${r.memberName}$locPart")
        appendLine("   Uraian: ${r.description}")
        appendLine("   Status: ${r.status}")
    }
}.trimEnd()
