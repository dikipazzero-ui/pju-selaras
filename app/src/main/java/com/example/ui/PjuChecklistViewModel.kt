package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PjuChecklistRepository
import com.example.model.ChatMessage
import com.example.model.DailyReport
import com.example.model.TeamWorkspace
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.currentIndonesianMonth
import com.example.model.currentYearInt
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PjuChecklistViewModel(
    private val repository: PjuChecklistRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(currentIndonesianMonth())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(currentYearInt())
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _preferredWorkspaceId = MutableStateFlow<String?>(null)

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

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

    val dailyReports: StateFlow<UiState<List<DailyReport>>> = activeWorkspaceFlow
        .distinctUntilChanged { a, b -> a.id == b.id }
        .flatMapLatest { ws ->
            repository.observeDailyReports(ws, currentUserId)
        }
        .map<List<DailyReport>, UiState<List<DailyReport>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing daily reports", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat laporan harian"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
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

    init {
        viewModelScope.launch {
            combine(
                activeWorkspaceFlow,
                dailyReports,
                chatMessages,
                workspaceProfiles
            ) { ws, dailyState, chatState, profilesMap ->
                val reports = (dailyState as? UiState.Success)?.data.orEmpty()
                val msgs = (chatState as? UiState.Success)?.data.orEmpty()
                listOf(ws, reports, msgs, profilesMap.values.toList())
            }.debounce(800L).collect { tuple ->
                val ws = tuple[0] as TeamWorkspace
                @Suppress("UNCHECKED_CAST")
                val reports = tuple[1] as List<DailyReport>
                @Suppress("UNCHECKED_CAST")
                val msgs = tuple[2] as List<ChatMessage>
                @Suppress("UNCHECKED_CAST")
                val profiles = tuple[3] as List<UserProfile>
                repository.syncMemberListsOnWorkspaceDocs(
                    workspace = ws,
                    period = null,
                    issues = emptyList(),
                    messages = msgs,
                    profiles = profiles,
                    dailyReports = reports
                )
            }
        }
    }

    fun selectWorkspace(workspaceId: String) {
        _preferredWorkspaceId.value = workspaceId
    }

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
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

    fun saveDailyReport(
        existingReportId: String?,
        memberName: String,
        eventDate: String,
        eventTime: String,
        dayName: String,
        location: String,
        description: String,
        photos: List<String>,
        onDone: (Boolean) -> Unit = {}
    ) {
        val ws = activeWorkspace.value ?: return
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveDailyReport(
                workspace = ws,
                userId = currentUserId,
                existingReportId = existingReportId,
                memberName = memberName,
                eventDate = eventDate,
                eventTime = eventTime,
                dayName = dayName,
                location = location,
                description = description,
                photos = photos
            )
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessages.tryEmit(
                    if (existingReportId.isNullOrBlank()) "Laporan harian tersimpan online"
                    else "Perubahan laporan harian disimpan"
                )
                onDone(true)
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Gagal menyimpan laporan harian")
                onDone(false)
            }
        }
    }

    fun deleteDailyReport(reportId: String) {
        val ws = activeWorkspace.value ?: return
        viewModelScope.launch {
            val result = repository.deleteDailyReport(ws, currentUserId, reportId)
            result.onSuccess {
                _snackbarMessages.tryEmit("Laporan harian dihapus")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Gagal menghapus laporan harian")
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
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Gagal mengirim foto")
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

    fun showMessage(msg: String) {
        _snackbarMessages.tryEmit(msg)
    }

    private companion object {
        const val TAG = "LaporanHarianVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
