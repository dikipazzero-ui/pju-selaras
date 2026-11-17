package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PjuChecklistRepository
import com.example.model.ChatMessage
import com.example.model.ChecklistPeriod
import com.example.model.DEFAULT_MONTH
import com.example.model.DEFAULT_PIC_NAME
import com.example.model.DEFAULT_TODAY_DATE
import com.example.model.DEFAULT_YEAR
import com.example.model.DateRangePreset
import com.example.model.IssueStatus
import com.example.model.LampStatus
import com.example.model.PJU_CLUSTERS
import com.example.model.PjuCluster
import com.example.model.PjuIssue
import com.example.model.TeamWorkspace
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.buildCellKey
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PeriodSelection(
    val month: String = DEFAULT_MONTH,
    val year: Int = DEFAULT_YEAR,
    val rangePreset: DateRangePreset = DateRangePreset.FORM_DEFAULT,
    val todayDate: Int = DEFAULT_TODAY_DATE
)

data class ChecklistStats(
    val totalFilled: Int = 0,
    val normalCount: Int = 0,
    val issueCount: Int = 0,
    val overdueClusterIds: Set<Int> = emptySet()
)

data class IssueDialogPrefill(
    val date: Int,
    val cluster: PjuCluster,
    val unit: String
)

data class PjuDashboardData(
    val workspace: TeamWorkspace,
    val period: ChecklistPeriod,
    val issues: List<PjuIssue>,
    val selection: PeriodSelection,
    val stats: ChecklistStats
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PjuChecklistViewModel(
    private val repository: PjuChecklistRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selection = MutableStateFlow(PeriodSelection())
    val selection: StateFlow<PeriodSelection> = _selection.asStateFlow()

    private val _preferredWorkspaceId = MutableStateFlow<String?>(null)

    private val _generalNotesDraft = MutableStateFlow("")
    val generalNotesDraft: StateFlow<String> = _generalNotesDraft.asStateFlow()

    private val _picNameDraft = MutableStateFlow(DEFAULT_PIC_NAME)
    val picNameDraft: StateFlow<String> = _picNameDraft.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _issuePrompt = MutableStateFlow<IssueDialogPrefill?>(null)
    val issuePrompt: StateFlow<IssueDialogPrefill?> = _issuePrompt.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

    private var lastSyncedNotes = ""
    private var lastSyncedPic = DEFAULT_PIC_NAME
    private var activePeriodKey = ""
    private var ensuredDefaultWorkspace = false

    val workspaces: StateFlow<List<TeamWorkspace>> = repository.observeUserWorkspaces(currentUserId)
        .onEach { list ->
            if (list.isEmpty() && !ensuredDefaultWorkspace) {
                ensuredDefaultWorkspace = true
                viewModelScope.launch {
                    val res = repository.ensureDefaultWorkspace(currentUserId)
                    res.onSuccess { ws ->
                        _preferredWorkspaceId.value = ws.id
                    }.onFailure { err ->
                        ensuredDefaultWorkspace = false
                        _snackbarMessages.tryEmit(
                            err.localizedMessage ?: "Gagal menyiapkan ruang tim online"
                        )
                    }
                }
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing workspaces", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val activeWorkspace: StateFlow<TeamWorkspace?> = combine(
        workspaces,
        _preferredWorkspaceId
    ) { list, preferredId ->
        if (list.isEmpty()) null
        else list.firstOrNull { it.id == preferredId } ?: list.last()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    private val activeWorkspaceFlow = activeWorkspace.filterNotNull()

    private val periodFlow = combine(
        activeWorkspaceFlow,
        _selection.map { it.month to it.year }.distinctUntilChanged()
    ) { ws, (month, year) -> Triple(ws, month, year) }
        .distinctUntilChanged()
        .flatMapLatest { (ws, month, year) ->
            repository.observePeriod(ws, currentUserId, month, year)
        }

    private val issuesFlow = combine(
        activeWorkspaceFlow,
        _selection.map { it.month to it.year }.distinctUntilChanged()
    ) { ws, (month, year) -> Triple(ws, month, year) }
        .distinctUntilChanged()
        .flatMapLatest { (ws, month, year) ->
            repository.observeIssues(ws, currentUserId, month, year)
        }

    val personalProfile: StateFlow<UserProfile?> = repository.observePersonalProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing personal profile", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val workspaceProfiles: StateFlow<Map<String, UserProfile>> = activeWorkspaceFlow
        .distinctUntilChanged { a, b -> a.id == b.id }
        .onEach { ws ->
            viewModelScope.launch {
                repository.ensurePersonalProfileSyncedToWorkspace(ws, currentUserId)
            }
        }
        .flatMapLatest { ws ->
            repository.observeWorkspaceProfiles(ws, currentUserId)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing workspace profiles", firestoreError ?: error)
            emit(emptyMap())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyMap()
        )

    val chatMessages: StateFlow<UiState<List<ChatMessage>>> = activeWorkspaceFlow
        .distinctUntilChanged { a, b -> a.id == b.id }
        .flatMapLatest { ws ->
            repository.observeChatMessages(ws, currentUserId)
        }
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing live chat", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat live chat"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val uiState: StateFlow<UiState<PjuDashboardData>> = combine(
        activeWorkspaceFlow,
        periodFlow,
        issuesFlow,
        _selection
    ) { ws, period, issues, sel ->
        val periodKey = "${ws.id}_${period.year}_${period.month}"
        if (periodKey != activePeriodKey) {
            activePeriodKey = periodKey
            lastSyncedNotes = period.generalNotes
            lastSyncedPic = period.picName
            _generalNotesDraft.value = period.generalNotes
            _picNameDraft.value = period.picName
        } else {
            if (period.generalNotes != lastSyncedNotes && _generalNotesDraft.value == lastSyncedNotes) {
                lastSyncedNotes = period.generalNotes
                _generalNotesDraft.value = period.generalNotes
            }
            if (period.picName != lastSyncedPic && _picNameDraft.value == lastSyncedPic) {
                lastSyncedPic = period.picName
                _picNameDraft.value = period.picName
            }
        }
        val stats = computeStats(period, sel.todayDate)
        UiState.Success(
            PjuDashboardData(
                workspace = ws,
                period = period,
                issues = issues,
                selection = sel,
                stats = stats
            )
        ) as UiState<PjuDashboardData>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing checklist data", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat data checklist PJU"))
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            _generalNotesDraft
                .debounce(650L)
                .collect { draft ->
                    val ws = activeWorkspace.value ?: return@collect
                    val sel = _selection.value
                    val key = "${ws.id}_${sel.year}_${sel.month}"
                    if (key == activePeriodKey && draft != lastSyncedNotes) {
                        val result = repository.updateGeneralNotes(ws, currentUserId, sel.month, sel.year, draft)
                        if (result.isSuccess) {
                            lastSyncedNotes = draft
                        } else {
                            _snackbarMessages.tryEmit(
                                result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan catatan"
                            )
                        }
                    }
                }
        }

        viewModelScope.launch {
            _picNameDraft
                .debounce(650L)
                .collect { draft ->
                    val ws = activeWorkspace.value ?: return@collect
                    val sel = _selection.value
                    val key = "${ws.id}_${sel.year}_${sel.month}"
                    val trimmed = draft.trim()
                    if (key == activePeriodKey && trimmed != lastSyncedPic) {
                        val result = repository.updatePicName(ws, currentUserId, sel.month, sel.year, trimmed)
                        if (result.isSuccess) {
                            lastSyncedPic = trimmed
                        } else {
                            _snackbarMessages.tryEmit(
                                result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan Penanggung Jawab"
                            )
                        }
                    }
                }
        }

        // Automatically sync memberUids onto period, issues, messages, and profiles when a new member joins
        viewModelScope.launch {
            combine(
                uiState,
                chatMessages,
                workspaceProfiles
            ) { dashState, chatState, profilesMap ->
                val dash = (dashState as? UiState.Success)?.data
                val msgs = (chatState as? UiState.Success)?.data.orEmpty()
                Triple(dash, msgs, profilesMap.values.toList())
            }.debounce(800L).collect { (dash, msgs, profiles) ->
                if (dash != null) {
                    repository.syncMemberListsOnWorkspaceDocs(
                        workspace = dash.workspace,
                        period = dash.period,
                        issues = dash.issues,
                        messages = msgs,
                        profiles = profiles
                    )
                }
            }
        }
    }

    fun selectWorkspace(workspaceId: String) {
        _preferredWorkspaceId.value = workspaceId
    }

    fun joinWorkspace(inviteCode: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.joinWorkspaceByCode(currentUserId, inviteCode)
            _isMutating.value = false
            result.onSuccess { ws ->
                _preferredWorkspaceId.value = ws.id
                _snackbarMessages.tryEmit("Berhasil bergabung ke ${ws.name}")
                onDone(true)
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Gagal bergabung ke tim")
                onDone(false)
            }
        }
    }

    fun createWorkspace(name: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.createWorkspace(currentUserId, name)
            _isMutating.value = false
            result.onSuccess { ws ->
                _preferredWorkspaceId.value = ws.id
                _snackbarMessages.tryEmit("Tim baru \"${ws.name}\" siap digunakan (Kode: ${ws.inviteCode})")
                onDone(true)
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Gagal membuat tim baru")
                onDone(false)
            }
        }
    }

    fun saveUserProfile(displayName: String, photoBase64: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val list = workspaces.value
            val result = repository.saveUserProfile(currentUserId, list, displayName, photoBase64)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit("Foto & nama profil berhasil disimpan")
                onDone(true)
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Gagal menyimpan profil")
                onDone(false)
            }
        }
    }

    fun sendChatMessage(text: String, tagCluster: String = "", onSent: () -> Unit = {}) {
        val ws = activeWorkspace.value ?: return
        val senderName = workspaceProfiles.value[currentUserId]?.displayName
            ?: personalProfile.value?.displayName
        viewModelScope.launch {
            _isSendingChat.value = true
            val result = repository.sendChatMessage(ws, currentUserId, text, tagCluster, senderName)
            _isSendingChat.value = false
            result.onSuccess {
                onSent()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Gagal mengirim pesan")
            }
        }
    }

    fun sendVoiceMessage(
        audioBase64: String,
        durationSec: Int,
        tagCluster: String = "",
        onSent: () -> Unit = {}
    ) {
        val ws = activeWorkspace.value ?: return
        val senderName = workspaceProfiles.value[currentUserId]?.displayName
            ?: personalProfile.value?.displayName
        viewModelScope.launch {
            _isSendingChat.value = true
            val result = repository.sendVoiceMessage(
                workspace = ws,
                userId = currentUserId,
                audioBase64 = audioBase64,
                durationSec = durationSec,
                tagCluster = tagCluster,
                senderDisplayName = senderName
            )
            _isSendingChat.value = false
            result.onSuccess {
                onSent()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Gagal mengirim voice note")
            }
        }
    }

    fun sendImageMessage(
        imageBase64: String,
        caption: String = "",
        tagCluster: String = "",
        onSent: () -> Unit = {}
    ) {
        val ws = activeWorkspace.value ?: return
        val senderName = workspaceProfiles.value[currentUserId]?.displayName
            ?: personalProfile.value?.displayName
        viewModelScope.launch {
            _isSendingChat.value = true
            val result = repository.sendImageMessage(
                workspace = ws,
                userId = currentUserId,
                imageBase64 = imageBase64,
                caption = caption,
                tagCluster = tagCluster,
                senderDisplayName = senderName
            )
            _isSendingChat.value = false
            result.onSuccess {
                onSent()
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Gagal mengirim foto laporan")
            }
        }
    }

    fun deleteChatMessage(messageId: String) {
        val ws = activeWorkspace.value ?: return
        viewModelScope.launch {
            val result = repository.deleteChatMessage(ws, currentUserId, messageId)
            result.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Gagal menghapus pesan")
            }
        }
    }

    fun selectMonth(month: String) {
        _selection.value = _selection.value.copy(month = month)
    }

    fun selectYear(year: Int) {
        _selection.value = _selection.value.copy(year = year)
    }

    fun selectRange(preset: DateRangePreset) {
        _selection.value = _selection.value.copy(rangePreset = preset)
    }

    fun selectTodayDate(day: Int) {
        _selection.value = _selection.value.copy(todayDate = day.coerceIn(1, 31))
    }

    fun onGeneralNotesChanged(text: String) {
        _generalNotesDraft.value = text.take(2000)
    }

    fun onPicNameChanged(text: String) {
        _picNameDraft.value = text.take(150)
    }

    fun dismissIssuePrompt() {
        _issuePrompt.value = null
    }

    fun toggleCellStatus(cluster: PjuCluster, slotIndex: Int, date: Int, period: ChecklistPeriod) {
        val ws = activeWorkspace.value ?: return
        val currentCode = period.checks[buildCellKey(cluster.id, slotIndex, date)]
        val nextStatus = LampStatus.fromCode(currentCode).next()
        val sel = _selection.value
        viewModelScope.launch {
            val result = repository.setCellStatus(
                workspace = ws,
                userId = currentUserId,
                month = sel.month,
                year = sel.year,
                clusterId = cluster.id,
                slotIndex = slotIndex,
                date = date,
                nextStatus = nextStatus
            )
            if (result.isSuccess) {
                if (nextStatus == LampStatus.ERROR) {
                    val customLabel = period.slotLabels[cluster.id.toString()]
                        ?.getOrNull(slotIndex)
                        ?.takeIf { it.isNotBlank() }
                    _issuePrompt.value = IssueDialogPrefill(
                        date = date,
                        cluster = cluster,
                        unit = customLabel ?: "Baris/Titik ${slotIndex + 1}"
                    )
                }
            } else {
                _snackbarMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Gagal memperbarui status lampu"
                )
            }
        }
    }

    fun saveSlotLabels(clusterId: Int, labels: List<String>) {
        val ws = activeWorkspace.value ?: return
        val sel = _selection.value
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateSlotLabels(
                workspace = ws,
                userId = currentUserId,
                month = sel.month,
                year = sel.year,
                clusterId = clusterId,
                labels = labels
            )
            _isMutating.value = false
            if (result.isSuccess) {
                _snackbarMessages.tryEmit("Label titik lampu tersimpan")
            } else {
                _snackbarMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan label titik"
                )
            }
        }
    }

    fun addIssue(
        date: Int,
        cluster: PjuCluster,
        unit: String,
        desc: String,
        status: IssueStatus
    ) {
        val ws = activeWorkspace.value ?: return
        val sel = _selection.value
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.addIssue(
                workspace = ws,
                userId = currentUserId,
                month = sel.month,
                year = sel.year,
                date = date,
                clusterId = cluster.id,
                clusterName = cluster.name,
                unit = unit,
                desc = desc,
                status = status
            )
            _isMutating.value = false
            if (result.isSuccess) {
                _snackbarMessages.tryEmit("Laporan permasalahan PJU tersimpan")
            } else {
                _snackbarMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan laporan masalah"
                )
            }
        }
    }

    fun cycleIssueStatus(issue: PjuIssue) {
        val ws = activeWorkspace.value ?: return
        val sel = _selection.value
        viewModelScope.launch {
            val result = repository.cycleIssueStatus(
                workspace = ws,
                userId = currentUserId,
                month = sel.month,
                year = sel.year,
                issue = issue
            )
            if (result.isFailure) {
                _snackbarMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Gagal mengubah status perbaikan"
                )
            }
        }
    }

    fun deleteIssue(issueId: String) {
        val ws = activeWorkspace.value ?: return
        val sel = _selection.value
        viewModelScope.launch {
            val result = repository.deleteIssue(
                workspace = ws,
                userId = currentUserId,
                month = sel.month,
                year = sel.year,
                issueId = issueId
            )
            if (result.isSuccess) {
                _snackbarMessages.tryEmit("Laporan permasalahan dihapus")
            } else {
                _snackbarMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus laporan"
                )
            }
        }
    }

    fun resetCurrentPeriod() {
        val ws = activeWorkspace.value ?: return
        val sel = _selection.value
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.resetPeriod(ws, currentUserId, sel.month, sel.year)
            _isMutating.value = false
            if (result.isSuccess) {
                lastSyncedNotes = ""
                lastSyncedPic = DEFAULT_PIC_NAME
                _generalNotesDraft.value = ""
                _picNameDraft.value = DEFAULT_PIC_NAME
                _snackbarMessages.tryEmit("Data periode ${sel.month} ${sel.year} berhasil direset")
            } else {
                _snackbarMessages.tryEmit(
                    result.exceptionOrNull()?.localizedMessage ?: "Gagal mereset data periode"
                )
            }
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessages.tryEmit(msg)
    }

    private fun computeStats(period: ChecklistPeriod, todayDate: Int): ChecklistStats {
        var total = 0
        var normal = 0
        var issue = 0
        period.checks.values.forEach { code ->
            when (LampStatus.fromCode(code)) {
                LampStatus.NORMAL -> {
                    total++
                    normal++
                }
                LampStatus.WARN, LampStatus.ERROR -> {
                    total++
                    issue++
                }
                LampStatus.EMPTY -> Unit
            }
        }

        val checkDays = listOf(todayDate, todayDate - 1).filter { it >= 1 }
        val overdue = mutableSetOf<Int>()
        for (cluster in PJU_CLUSTERS) {
            var checkedRecently = false
            for (d in checkDays) {
                for (slot in 0 until 3) {
                    val code = period.checks[buildCellKey(cluster.id, slot, d)]
                    if (!code.isNullOrBlank()) {
                        checkedRecently = true
                        break
                    }
                }
                if (checkedRecently) break
            }
            if (!checkedRecently) {
                overdue.add(cluster.id)
            }
        }

        return ChecklistStats(
            totalFilled = total,
            normalCount = normal,
            issueCount = issue,
            overdueClusterIds = overdue
        )
    }

    private companion object {
        const val TAG = "PjuChecklistVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
