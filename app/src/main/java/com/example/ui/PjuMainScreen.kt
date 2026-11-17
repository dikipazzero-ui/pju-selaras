import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOut
import com.example.export.CsvAndPrintHelper
import com.example.model.UiState
import com.example.model.monthPrefixIso
import com.example.ui.components.DailyReportsTabContent
import com.example.ui.components.LiveChatTabContent
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MonthlySummaryTabContent
import com.example.ui.components.ProfilePhotoDialog
import com.example.ui.components.TeamWorkspaceDialog
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber900
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PjuMainScreen(
    viewModel: PjuChecklistViewModel,
    currentUserId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()
    val personalProfile by viewModel.personalProfile.collectAsStateWithLifecycle()
    val workspaceProfiles by viewModel.workspaceProfiles.collectAsStateWithLifecycle()
    val dailyReportsState by viewModel.dailyReports.collectAsStateWithLifecycle()
    val chatMessagesState by viewModel.chatMessages.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val isSendingChat by viewModel.isSendingChat.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Harian, 1 = Rangkuman, 2 = Live Chat
    var showTeamDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val currentFirebaseUser = Firebase.auth.currentUser
    val currentUserEmail = currentFirebaseUser?.email ?: "Anggota BPS"
    val myProfile = workspaceProfiles[currentUserId] ?: personalProfile
    val myDisplayName = myProfile?.displayName?.ifBlank { null }
        ?: activeWorkspace?.memberNames?.get(currentUserId)?.ifBlank { null }
        ?: currentFirebaseUser?.displayName?.ifBlank { null }
        ?: currentUserEmail.substringBefore('@')
    val myPhotoBase64 = myProfile?.photoBase64 ?: ""

    val allReports = (dailyReportsState as? UiState.Success)?.data.orEmpty()
    val activeMonthPrefix = remember(selectedYear, selectedMonth) {
        monthPrefixIso(selectedYear, selectedMonth)
    }
    val currentMonthReports = remember(allReports, activeMonthPrefix) {
        allReports.filter { it.eventDate.startsWith(activeMonthPrefix) }
    }
    val dailyCount = allReports.size
    val chatCount = (chatMessagesState as? UiState.Success)?.data?.size ?: 0

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_bps_roundabout),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.52f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.62f)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Slate900.copy(alpha = 0.94f),
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
                                        imageVector = Icons.Filled.Assignment,
                                        contentDescription = null,
                                        tint = Slate950,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "LAPORAN HARIAN BPS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeWorkspace?.let { "${it.name} • ${it.memberUids.size} anggota" }
                                        ?: "Perumahan Bumi Papan Selaras",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier.testTag("top_profile_avatar_button")
                        ) {
                            MemberAvatar(
                                displayName = myDisplayName,
                                photoBase64 = myPhotoBase64,
                                size = 32.dp
                            )
                        }

                        if (activeWorkspace != null) {
                            IconButton(
                                onClick = { showTeamDialog = true },
                                modifier = Modifier.testTag("team_dialog_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Groups,
                                    contentDescription = "Kelola Tim & Gabung",
                                    tint = Amber500
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val ws = activeWorkspace
                                if (ws != null) {
                                    CsvAndPrintHelper.shareDailyReportsCsv(
                                        context = context,
                                        workspaceName = ws.name,
                                        monthLabel = selectedMonth,
                                        year = selectedYear,
                                        reports = currentMonthReports
                                    ).onFailure {
                                        viewModel.showMessage(it.localizedMessage ?: "Gagal membuat file CSV")
                                    }
                                }
                            },
                            enabled = activeWorkspace != null,
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
                                val ws = activeWorkspace
                                if (ws != null) {
                                    CsvAndPrintHelper.printDailyReportsSummary(
                                        context = context,
                                        workspaceName = ws.name,
                                        monthLabel = selectedMonth,
                                        year = selectedYear,
                                        reports = currentMonthReports
                                    ).onFailure {
                                        viewModel.showMessage(it.localizedMessage ?: "Gagal membuka cetak PDF")
                                    }
                                }
                            },
                            enabled = activeWorkspace != null,
                            modifier = Modifier.testTag("print_form_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Print,
                                contentDescription = "Cetak Rangkuman PDF"
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
                                    text = { Text("Foto & Nama Profil") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.AccountCircle,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showProfileDialog = true
                                    }
                                )
                                if (activeWorkspace != null) {
                                    DropdownMenuItem(
                                        text = { Text("Tim Online & Kode Gabung") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Groups,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showTeamDialog = true
                                        }
                                    )
                                }
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
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Slate900.copy(alpha = 0.96f),
                    contentColor = Color.White,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (dailyCount > 0) {
                                        Badge(
                                            containerColor = Amber100,
                                            contentColor = Amber900
                                        ) {
                                            Text(dailyCount.coerceAtMost(99).toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.Assignment else Icons.Outlined.Assignment,
                                    contentDescription = "Laporan Harian"
                                )
                            }
                        },
                        label = { Text("Harian", fontWeight = FontWeight.SemiBold, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Slate950,
                            selectedTextColor = Amber500,
                            indicatorColor = Amber500,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("nav_daily_reports_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                contentDescription = "Rangkuman Bulanan"
                            )
                        },
                        label = { Text("Rangkuman", fontWeight = FontWeight.SemiBold, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Slate950,
                            selectedTextColor = Amber500,
                            indicatorColor = Amber500,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("nav_monthly_summary_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (chatCount > 0) {
                                        Badge(
                                            containerColor = Amber100,
                                            contentColor = Amber900
                                        ) {
                                            Text(chatCount.coerceAtMost(99).toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.Forum else Icons.Outlined.Forum,
                                    contentDescription = "Live Chat Tim"
                                )
                            }
                        },
                        label = { Text("Live Chat", fontWeight = FontWeight.SemiBold, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Slate950,
                            selectedTextColor = Amber500,
                            indicatorColor = Amber500,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("nav_live_chat_tab")
                    )
                }
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

                val ws = activeWorkspace
                if (ws == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Menyiapkan ruang online LAPORAN HARIAN BPS...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    when (selectedTab) {
                        0 -> DailyReportsTabContent(
                            workspace = ws,
                            workspacesCount = workspaces.size,
                            reportsState = dailyReportsState,
                            profilesMap = workspaceProfiles,
                            currentUserId = currentUserId,
                            myDisplayName = myDisplayName,
                            onOpenTeamDialog = { showTeamDialog = true },
                            onSaveReport = viewModel::saveDailyReport,
                            onDeleteReport = viewModel::deleteDailyReport,
                            onShowMessage = viewModel::showMessage
                        )

                        1 -> MonthlySummaryTabContent(
                            workspace = ws,
                            reportsState = dailyReportsState,
                            profilesMap = workspaceProfiles,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            onSelectMonth = viewModel::selectMonth,
                            onSelectYear = viewModel::selectYear,
                            onExportCsv = { filteredReports ->
                                CsvAndPrintHelper.shareDailyReportsCsv(
                                    context = context,
                                    workspaceName = ws.name,
                                    monthLabel = selectedMonth,
                                    year = selectedYear,
                                    reports = filteredReports
                                ).onFailure {
                                    viewModel.showMessage(it.localizedMessage ?: "Gagal membuat file CSV")
                                }
                            },
                            onPrintPdf = { filteredReports ->
                                CsvAndPrintHelper.printDailyReportsSummary(
                                    context = context,
                                    workspaceName = ws.name,
                                    monthLabel = selectedMonth,
                                    year = selectedYear,
                                    reports = filteredReports
                                ).onFailure {
                                    viewModel.showMessage(it.localizedMessage ?: "Gagal membuka cetak PDF")
                                }
                            },
                            onShowMessage = viewModel::showMessage
                        )

                        else -> LiveChatTabContent(
                            workspace = ws,
                            messagesState = chatMessagesState,
                            profilesMap = workspaceProfiles,
                            myProfile = myProfile,
                            currentUserId = currentUserId,
                            isSending = isSendingChat,
                            onSendMessage = viewModel::sendChatMessage,
                            onSendVoiceMessage = viewModel::sendVoiceMessage,
                            onSendImageMessage = viewModel::sendImageMessage,
                            onDeleteMessage = viewModel::deleteChatMessage,
                            onOpenTeamDialog = { showTeamDialog = true },
                            onOpenProfileDialog = { showProfileDialog = true },
                            onShowMessage = viewModel::showMessage
                        )
                    }
                }

                activeWorkspace?.let { ws ->
                    if (showTeamDialog) {
                        TeamWorkspaceDialog(
                            activeWorkspace = ws,
                            allWorkspaces = workspaces,
                            profilesMap = workspaceProfiles,
                            onSelectWorkspace = viewModel::selectWorkspace,
                            onJoinWorkspace = viewModel::joinWorkspace,
                            onCreateWorkspace = viewModel::createWorkspace,
                            onCopyInviteCode = { code ->
                                clipboard.setText(AnnotatedString(code))
                                viewModel.showMessage("Kode tim $code disalin")
                            },
                            onDismiss = { showTeamDialog = false }
                        )
                    }
                }

                if (showProfileDialog) {
                    ProfilePhotoDialog(
                        initialDisplayName = myDisplayName,
                        initialPhotoBase64 = myPhotoBase64,
                        onSave = viewModel::saveUserProfile,
                        onDismiss = { showProfileDialog = false }
                    )
                }
            }
        }
    }
}
