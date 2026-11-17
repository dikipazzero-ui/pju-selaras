package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.media.ChatPhotoHelper
import com.example.media.ProfilePhotoHelper
import com.example.model.DailyReport
import com.example.model.TeamWorkspace
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.currentIsoDate
import com.example.model.currentTimeHm
import com.example.model.formatIndonesianFullDate
import com.example.model.indonesianDayNameFromIsoDate
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
import com.example.ui.theme.Slate950
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DailyFilterMode(val label: String) {
    ALL("Semua Hari"),
    TODAY("Hari Ini")
}

@Composable
fun DailyReportsTabContent(
    workspace: TeamWorkspace,
    workspacesCount: Int,
    reportsState: UiState<List<DailyReport>>,
    profilesMap: Map<String, UserProfile>,
    currentUserId: String,
    myDisplayName: String,
    onOpenTeamDialog: () -> Unit,
    onSaveReport: (
        existingReportId: String?,
        memberName: String,
        eventDate: String,
        eventTime: String,
        dayName: String,
        location: String,
        description: String,
        photos: List<String>,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onDeleteReport: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    var filterMode by remember { mutableStateOf(DailyFilterMode.ALL) }
    var selectedMemberFilter by remember { mutableStateOf("") }
    var showFormDialog by remember { mutableStateOf(false) }
    var editingReport by remember { mutableStateOf<DailyReport?>(null) }
    var viewingPhotosState by remember { mutableStateOf<Pair<DailyReport, Int>?>(null) }

    val todayIso = remember { currentIsoDate() }
    val todayFullLabel = remember(todayIso) { formatIndonesianFullDate(todayIso) }

    val allReports = (reportsState as? UiState.Success)?.data.orEmpty()
    val todayReports = remember(allReports, todayIso) {
        allReports.filter { it.eventDate == todayIso }
    }
    val todayPhotoCount = remember(todayReports) { todayReports.sumOf { it.photos.size } }

    val memberOptions = remember(allReports) {
        allReports.map { it.memberName.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredReports = remember(allReports, filterMode, selectedMemberFilter, todayIso) {
        allReports.filter { r ->
            val dayMatch = when (filterMode) {
                DailyFilterMode.ALL -> true
                DailyFilterMode.TODAY -> r.eventDate == todayIso
            }
            val memberMatch = selectedMemberFilter.isBlank() ||
                r.memberName.equals(selectedMemberFilter, ignoreCase = true)
            dayMatch && memberMatch
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
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "team_banner") {
                        TeamBannerCard(
                            workspace = workspace,
                            workspacesCount = workspacesCount,
                            onOpenTeamDialog = onOpenTeamDialog,
                            onCopyMessage = onShowMessage
                        )
                    }

                    item(key = "daily_overview") {
                        DailyOverviewHeaderCard(
                            todayLabel = todayFullLabel,
                            todayReportCount = todayReports.size,
                            todayPhotoCount = todayPhotoCount,
                            totalReportCount = allReports.size,
                            filterMode = filterMode,
                            onSelectMode = { filterMode = it },
                            memberOptions = memberOptions,
                            selectedMember = selectedMemberFilter,
                            onSelectMember = { selectedMemberFilter = it },
                            onCreateReport = {
                                editingReport = null
                                showFormDialog = true
                            }
                        )
                    }

                    if (filteredReports.isEmpty()) {
                        item(key = "empty_daily_reports") {
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
                                        imageVector = Icons.Filled.AddAPhoto,
                                        contentDescription = null,
                                        tint = Amber500,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Text(
                                        text = if (filterMode == DailyFilterMode.TODAY) {
                                            "Belum Ada Laporan Hari Ini"
                                        } else {
                                            "Belum Ada Laporan Harian"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ketuk tombol + Buat Laporan untuk mencatat keterangan pelaporan beserta hari, tanggal, jam, dan hingga 4 foto dokumentasi.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            filteredReports,
                            key = { it.id.ifBlank { "${it.createdByUid}_${it.eventDate}_${it.eventTime}" } }
                        ) { report ->
                            val canModify = report.createdByUid == currentUserId || workspace.ownerId == currentUserId
                            DailyReportCard(
                                report = report,
                                creatorProfile = profilesMap[report.createdByUid],
                                canEdit = report.id.isNotBlank(),
                                canDelete = canModify && report.id.isNotBlank(),
                                onEdit = {
                                    editingReport = report
                                    showFormDialog = true
                                },
                                onDelete = { onDeleteReport(report.id) },
                                onOpenPhoto = { idx -> viewingPhotosState = report to idx }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingReport = null
                showFormDialog = true
            },
            containerColor = Amber500,
            contentColor = Slate950,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_daily_report_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Buat Laporan Harian")
                Text(
                    text = "Buat Laporan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showFormDialog) {
        val suggestedMembers = remember(workspace, profilesMap, myDisplayName) {
            val names = mutableListOf(myDisplayName)
            workspace.memberUids.forEach { uid ->
                val n = profilesMap[uid]?.displayName?.ifBlank { null } ?: workspace.memberNames[uid]
                if (!n.isNullOrBlank() && names.none { it.equals(n, ignoreCase = true) }) {
                    names.add(n)
                }
            }
            names
        }
        DailyReportFormDialog(
            editingReport = editingReport,
            defaultMemberName = myDisplayName,
            suggestedMembers = suggestedMembers,
            onDismiss = {
                showFormDialog = false
                editingReport = null
            },
            onSubmit = { reportId, memberName, date, time, dayName, location, desc, photos, onDone ->
                onSaveReport(reportId, memberName, date, time, dayName, location, desc, photos, onDone)
            },
            onShowError = onShowMessage
        )
    }

    viewingPhotosState?.let { (report, startIdx) ->
        MultiPhotoViewerDialog(
            report = report,
            initialIndex = startIdx,
            onDismiss = { viewingPhotosState = null }
        )
    }
}

@Composable
private fun DailyOverviewHeaderCard(
    todayLabel: String,
    todayReportCount: Int,
    todayPhotoCount: Int,
    totalReportCount: Int,
    filterMode: DailyFilterMode,
    onSelectMode: (DailyFilterMode) -> Unit,
    memberOptions: List<String>,
    selectedMember: String,
    onSelectMember: (String) -> Unit,
    onCreateReport: () -> Unit
) {
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
                                imageVector = Icons.Filled.Today,
                                contentDescription = null,
                                tint = Amber900,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Laporan Harian",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = todayLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = onCreateReport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber500,
                        contentColor = Slate950
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DailyStatPill(
                    label = "Hari Ini",
                    value = "$todayReportCount Laporan",
                    bgColor = Blue100,
                    fgColor = Blue900,
                    modifier = Modifier.weight(1f)
                )
                DailyStatPill(
                    label = "Foto Hari Ini",
                    value = "$todayPhotoCount Foto",
                    bgColor = Emerald100,
                    fgColor = Emerald900,
                    modifier = Modifier.weight(1f)
                )
                DailyStatPill(
                    label = "Total Arsip",
                    value = "$totalReportCount",
                    bgColor = Slate900,
                    fgColor = Color.White,
                    modifier = Modifier.weight(0.75f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DailyFilterMode.entries.forEach { mode ->
                    FilterChip(
                        selected = filterMode == mode,
                        onClick = { onSelectMode(mode) },
                        label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate900,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                if (memberOptions.isNotEmpty()) {
                    memberOptions.forEach { member ->
                        val isSel = selectedMember.equals(member, ignoreCase = true)
                        FilterChip(
                            selected = isSel,
                            onClick = { onSelectMember(if (isSel) "" else member) },
                            label = { Text(member, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
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
private fun DailyStatPill(
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.ExtraBold,
                color = fgColor,
                maxLines = 1
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
fun DailyReportCard(
    report: DailyReport,
    creatorProfile: UserProfile?,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenPhoto: (Int) -> Unit
) {
    val fullDate = remember(report.eventDate, report.dayName) {
        formatIndonesianFullDate(report.eventDate, report.dayName)
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
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MemberAvatar(
                        displayName = report.memberName,
                        photoBase64 = creatorProfile?.photoBase64 ?: "",
                        size = 40.dp
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
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = Blue600,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "$fullDate • Jam ${report.eventTime}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.SemiBold,
                                color = Blue600
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canEdit) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit laporan",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Hapus laporan",
                                tint = Rose600
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (report.location.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (report.photos.isNotEmpty()) Emerald100 else Amber100
                ) {
                    Text(
                        text = "${report.photos.size} / 4 Foto",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (report.photos.isNotEmpty()) Emerald900 else Amber900,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (report.photos.isNotEmpty()) {
                DailyPhotosGrid(
                    photos = report.photos,
                    onPhotoClick = onOpenPhoto
                )
            }
        }
    }
}

@Composable
private fun DailyPhotosGrid(
    photos: List<String>,
    onPhotoClick: (Int) -> Unit
) {
    val safePhotos = photos.take(4)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val rows = safePhotos.chunked(2)
        rows.forEachIndexed { rowIdx, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pair.forEachIndexed { colIdx, b64 ->
                    val globalIdx = rowIdx * 2 + colIdx
                    val bmp = remember(b64) {
                        ProfilePhotoHelper.decodeBase64ToBitmap(b64)?.asImageBitmap()
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (safePhotos.size == 1) 190.dp else 132.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onPhotoClick(globalIdx) }
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Foto dokumentasi ${globalIdx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate950.copy(alpha = 0.68f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "Foto ${globalIdx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (pair.size == 1 && safePhotos.size > 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DailyReportFormDialog(
    editingReport: DailyReport?,
    defaultMemberName: String,
    suggestedMembers: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (
        existingReportId: String?,
        memberName: String,
        eventDate: String,
        eventTime: String,
        dayName: String,
        location: String,
        description: String,
        photos: List<String>,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onShowError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var memberName by remember(editingReport, defaultMemberName) {
        mutableStateOf(editingReport?.memberName?.ifBlank { null } ?: defaultMemberName)
    }
    var eventDate by remember(editingReport) {
        mutableStateOf(editingReport?.eventDate?.ifBlank { null } ?: currentIsoDate())
    }
    var eventTime by remember(editingReport) {
        mutableStateOf(editingReport?.eventTime?.ifBlank { null } ?: currentTimeHm())
    }
    var location by remember(editingReport) {
        mutableStateOf(editingReport?.location ?: "")
    }
    var description by remember(editingReport) {
        mutableStateOf(editingReport?.description ?: "")
    }

    val photoSlots = remember(editingReport) {
        mutableStateListOf(
            editingReport?.photos?.getOrNull(0) ?: "",
            editingReport?.photos?.getOrNull(1) ?: "",
            editingReport?.photos?.getOrNull(2) ?: "",
            editingReport?.photos?.getOrNull(3) ?: ""
        )
    }

    var activeSlotIndex by remember { mutableIntStateOf(-1) }
    var encodingSlotIndex by remember { mutableIntStateOf(-1) }
    var menuSlotIndex by remember { mutableIntStateOf(-1) }
    var pendingCamUri by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val dayName = remember(eventDate) { indonesianDayNameFromIsoDate(eventDate).ifBlank { "Hari ini" } }
    val fullDateDisplay = remember(eventDate, dayName) { formatIndonesianFullDate(eventDate, dayName) }

    fun encodeSlotUri(slotIdx: Int, uri: Uri) {
        if (slotIdx !in 0..3) return
        encodingSlotIndex = slotIdx
        scope.launch {
            val res = ChatPhotoHelper.encodeDailySlotUriToBase64(context, uri)
            encodingSlotIndex = -1
            res.onSuccess { b64 ->
                photoSlots[slotIdx] = b64
            }.onFailure { err ->
                onShowError(err.localizedMessage ?: "Gagal memproses foto ke-${slotIdx + 1}")
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val slot = activeSlotIndex
        activeSlotIndex = -1
        if (uri != null && slot in 0..3) {
            encodeSlotUri(slot, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val slot = activeSlotIndex
        val uri = pendingCamUri
        activeSlotIndex = -1
        pendingCamUri = null
        if (success && uri != null && slot in 0..3) {
            encodeSlotUri(slot, uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingReport == null) "Buat Laporan Harian" else "Edit Laporan Harian",
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
                OutlinedTextField(
                    value = memberName,
                    onValueChange = {
                        memberName = it.take(80)
                        validationError = null
                    },
                    label = { Text("Nama Anggota / Pelapor") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_member_name_input")
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

                // Day, Date & Time Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Blue100.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, Blue600.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Waktu Pelaporan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Blue900
                            )
                            Text(
                                text = "Sekarang",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Blue600,
                                modifier = Modifier.clickable {
                                    eventDate = currentIsoDate()
                                    eventTime = currentTimeHm()
                                }
                            )
                        }
                        Text(
                            text = "$fullDateDisplay • Jam $eventTime WIB",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    val parts = eventDate.split("-")
                                    val initY = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
                                    val initM = (parts.getOrNull(1)?.toIntOrNull()?.minus(1)) ?: cal.get(Calendar.MONTH)
                                    val initD = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                                    DatePickerDialog(context, { _, y, m, d ->
                                        eventDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    }, initY, initM, initD).show()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tanggal", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    val tParts = eventTime.split(":")
                                    val initH = tParts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
                                    val initMin = tParts.getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)
                                    TimePickerDialog(context, { _, h, min ->
                                        eventTime = String.format("%02d:%02d", h, min)
                                    }, initH, initMin, true).show()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Jam", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it.take(100) },
                    label = { Text("Area / Lokasi (Opsional)") },
                    placeholder = { Text("Misal: Blok C, Pos Gerbang, Taman Fasum") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it.take(2000)
                        validationError = null
                    },
                    label = { Text("Keterangan Pelaporan") },
                    placeholder = { Text("Tulis rincian laporan harian, pekerjaan, atau kondisi lapangan...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_description_input")
                )

                HorizontalDivider()

                val filledCount = photoSlots.count { it.isNotBlank() }
                Text(
                    text = "Sisipkan Foto Harian ($filledCount / 4 Foto)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ketuk kotak Foto 1 s/d Foto 4 di bawah untuk mengambil foto kamera atau memilih dari galeri.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 2x2 Grid of 4 Photo Slots
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (rowIdx in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (colIdx in 0..1) {
                                val slotIdx = rowIdx * 2 + colIdx
                                val photoB64 = photoSlots[slotIdx]
                                val isSlotBusy = encodingSlotIndex == slotIdx
                                Box(modifier = Modifier.weight(1f)) {
                                    PhotoSlotTile(
                                        slotNumber = slotIdx + 1,
                                        photoBase64 = photoB64,
                                        isLoading = isSlotBusy,
                                        onClick = {
                                            if (!isSlotBusy && !isSaving) {
                                                menuSlotIndex = slotIdx
                                            }
                                        },
                                        onRemove = { photoSlots[slotIdx] = "" }
                                    )
                                    DropdownMenu(
                                        expanded = menuSlotIndex == slotIdx,
                                        onDismissRequest = { menuSlotIndex = -1 }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Ambil Foto Kamera") },
                                            leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                                            onClick = {
                                                menuSlotIndex = -1
                                                activeSlotIndex = slotIdx
                                                runCatching {
                                                    ChatPhotoHelper.createTempCameraUri(context)
                                                }.onSuccess { uri ->
                                                    pendingCamUri = uri
                                                    cameraLauncher.launch(uri)
                                                }.onFailure { err ->
                                                    activeSlotIndex = -1
                                                    onShowError(err.localizedMessage ?: "Gagal membuka kamera")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Pilih dari Galeri") },
                                            leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                                            onClick = {
                                                menuSlotIndex = -1
                                                activeSlotIndex = slotIdx
                                                galleryLauncher.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
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
                    val cleanMember = memberName.trim()
                    val cleanDesc = description.trim()
                    when {
                        cleanMember.isEmpty() -> validationError = "Nama anggota wajib diisi."
                        cleanDesc.isEmpty() -> validationError = "Keterangan pelaporan wajib diisi."
                        else -> {
                            isSaving = true
                            val compactPhotos = photoSlots.filter { it.isNotBlank() }.take(4)
                            onSubmit(
                                editingReport?.id,
                                cleanMember,
                                eventDate,
                                eventTime,
                                dayName,
                                location.trim(),
                                cleanDesc,
                                compactPhotos
                            ) { ok ->
                                isSaving = false
                                if (ok) onDismiss()
                            }
                        }
                    }
                },
                enabled = encodingSlotIndex == -1 && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber500,
                    contentColor = Slate950
                ),
                modifier = Modifier.testTag("submit_daily_report_button")
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
private fun PhotoSlotTile(
    slotNumber: Int,
    photoBase64: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val bmp = remember(photoBase64) {
        if (photoBase64.isNotBlank()) ProfilePhotoHelper.decodeBase64ToBitmap(photoBase64)?.asImageBitmap()
        else null
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("daily_photo_slot_$slotNumber"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(
            width = 1.dp,
            color = if (bmp != null) Emerald600 else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
            } else if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = "Foto slot $slotNumber",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = CircleShape,
                    color = Rose600,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .clickable(onClick = onRemove)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Hapus foto $slotNumber",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate950.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "Foto $slotNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        tint = Blue600,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "+ Foto $slotNumber",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MultiPhotoViewerDialog(
    report: DailyReport,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val photos = report.photos.take(4)
    var currentIdx by remember(initialIndex, photos.size) {
        mutableIntStateOf(initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)))
    }
    val currentB64 = photos.getOrNull(currentIdx) ?: ""
    val bmp = remember(currentB64) {
        ProfilePhotoHelper.decodeBase64ToBitmap(currentB64)?.asImageBitmap()
    }
    val fullDate = remember(report.eventDate, report.dayName) {
        formatIndonesianFullDate(report.eventDate, report.dayName)
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
                            text = "${report.memberName} (Foto ${currentIdx + 1}/${photos.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$fullDate • Jam ${report.eventTime}",
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
                            contentDescription = "Foto laporan ukuran penuh",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (photos.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentIdx > 0) currentIdx -= 1 },
                            enabled = currentIdx > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = Color.White)
                            Text("Sebelumnya", color = Color.White)
                        }
                        Text(
                            text = "Foto ${currentIdx + 1} / ${photos.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Amber500,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { if (currentIdx < photos.lastIndex) currentIdx += 1 },
                            enabled = currentIdx < photos.lastIndex
                        ) {
                            Text("Berikutnya", color = Color.White)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900.copy(alpha = 0.88f)
                ) {
                    Text(
                        text = report.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}
