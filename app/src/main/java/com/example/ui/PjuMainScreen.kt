package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.signOut
import com.example.export.CsvAndPrintHelper
import com.example.model.PjuCluster
import com.example.model.UiState
import com.example.ui.components.AddIssueDialog
import com.example.ui.components.ChecklistMatrixTable
import com.example.ui.components.DashboardStatsSection
import com.example.ui.components.FormHeaderAndFiltersCard
import com.example.ui.components.IssuesAndNotesCard
import com.example.ui.components.ResetPeriodDialog
import com.example.ui.components.RulesAndPicCard
import com.example.ui.components.SlotLabelsDialog
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PjuMainScreen(
    viewModel: PjuChecklistViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val generalNotesDraft by viewModel.generalNotesDraft.collectAsStateWithLifecycle()
    val picNameDraft by viewModel.picNameDraft.collectAsStateWithLifecycle()
    val issuePrompt by viewModel.issuePrompt.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showManualIssueDialog by remember { mutableStateOf(false) }
    var editingSlotsCluster by remember { mutableStateOf<PjuCluster?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val currentUserEmail = Firebase.auth.currentUser?.email ?: "Petugas PJU"
    val dashboardData = (uiState as? UiState.Success)?.data

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate900,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Amber500,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = Slate950,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "PJU CHECKLIST DIGITAL",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Perumahan Bumi Papan Selaras",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val data = dashboardData
                            if (data != null) {
                                CsvAndPrintHelper.shareCsv(
                                    context = context,
                                    period = data.period,
                                    issues = data.issues,
                                    rangePreset = data.selection.rangePreset
                                ).onFailure {
                                    viewModel.showMessage(it.localizedMessage ?: "Gagal membuat file CSV")
                                }
                            }
                        },
                        enabled = dashboardData != null,
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TableView,
                            contentDescription = "Export CSV",
                            tint = Emerald600
                        )
                    }

                    IconButton(
                        onClick = {
                            val data = dashboardData
                            if (data != null) {
                                CsvAndPrintHelper.printChecklistForm(
                                    context = context,
                                    period = data.period,
                                    issues = data.issues,
                                    rangePreset = data.selection.rangePreset
                                ).onFailure {
                                    viewModel.showMessage(it.localizedMessage ?: "Gagal membuka dialog cetak")
                                }
                            }
                        },
                        enabled = dashboardData != null,
                        modifier = Modifier.testTag("print_form_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Print,
                            contentDescription = "Cetak Form"
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Menu lainnya"
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "Masuk sebagai",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = currentUserEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                },
                                onClick = {},
                                enabled = false
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Reset Data Periode Ini", color = Rose600) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.RestartAlt,
                                        contentDescription = null,
                                        tint = Rose600
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showResetDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Keluar Akun Google") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    signOut(credentialManager, scope)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if (isMutating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Amber500
                )
            }

            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Memuat formulir checklist PJU...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = Rose600,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Gagal Memuat Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val data = state.data
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val isWideScreen = maxWidth >= 600.dp
                        LazyColumn(
                            modifier = Modifier
                                .widthIn(max = 1100.dp)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = if (isWideScreen) 20.dp else 12.dp,
                                vertical = 14.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item(key = "stats") {
                                DashboardStatsSection(
                                    stats = data.stats,
                                    isWideScreen = isWideScreen
                                )
                            }

                            item(key = "header_filters") {
                                FormHeaderAndFiltersCard(
                                    selection = data.selection,
                                    isWideScreen = isWideScreen,
                                    onSelectMonth = viewModel::selectMonth,
                                    onSelectYear = viewModel::selectYear,
                                    onSelectRange = viewModel::selectRange,
                                    onSelectTodayDate = viewModel::selectTodayDate
                                )
                            }

                            item(key = "matrix_table") {
                                ChecklistMatrixTable(
                                    period = data.period,
                                    rangePreset = data.selection.rangePreset,
                                    todayDate = data.selection.todayDate,
                                    overdueClusterIds = data.stats.overdueClusterIds,
                                    isWideScreen = isWideScreen,
                                    onCellClick = { cluster, slotIndex, day ->
                                        viewModel.toggleCellStatus(cluster, slotIndex, day, data.period)
                                    },
                                    onEditSlotLabels = { cluster ->
                                        editingSlotsCluster = cluster
                                    }
                                )
                            }

                            item(key = "issues_and_notes") {
                                IssuesAndNotesCard(
                                    month = data.selection.month,
                                    issues = data.issues,
                                    generalNotes = generalNotesDraft,
                                    onNotesChanged = viewModel::onGeneralNotesChanged,
                                    onAddIssueClick = { showManualIssueDialog = true },
                                    onCycleIssueStatus = viewModel::cycleIssueStatus,
                                    onDeleteIssue = viewModel::deleteIssue
                                )
                            }

                            item(key = "rules_and_pic") {
                                RulesAndPicCard(
                                    picName = picNameDraft,
                                    isWideScreen = isWideScreen,
                                    onPicNameChanged = viewModel::onPicNameChanged
                                )
                            }

                            item(key = "bottom_spacer") {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    // Dialogs
                    val activePrefill = issuePrompt
                    if (showManualIssueDialog || activePrefill != null) {
                        AddIssueDialog(
                            initialDay = data.selection.todayDate,
                            prefill = activePrefill,
                            onDismiss = {
                                showManualIssueDialog = false
                                viewModel.dismissIssuePrompt()
                            },
                            onSubmit = { date, cluster, unit, desc, status ->
                                viewModel.addIssue(date, cluster, unit, desc, status)
                                showManualIssueDialog = false
                                viewModel.dismissIssuePrompt()
                            }
                        )
                    }

                    editingSlotsCluster?.let { cluster ->
                        val existing = data.period.slotLabels[cluster.id.toString()].orEmpty()
                        SlotLabelsDialog(
                            cluster = cluster,
                            existingLabels = existing,
                            onDismiss = { editingSlotsCluster = null },
                            onSave = { labels ->
                                viewModel.saveSlotLabels(cluster.id, labels)
                                editingSlotsCluster = null
                            }
                        )
                    }

                    if (showResetDialog) {
                        ResetPeriodDialog(
                            month = data.selection.month,
                            year = data.selection.year,
                            onDismiss = { showResetDialog = false },
                            onConfirm = {
                                showResetDialog = false
                                viewModel.resetCurrentPeriod()
                            }
                        )
                    }
                }
            }
        }
    }
}
