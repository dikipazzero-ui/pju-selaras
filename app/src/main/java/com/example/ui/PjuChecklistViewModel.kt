package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PjuChecklistRepository
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
import com.example.model.UiState
import com.example.model.buildCellKey
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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

    private var lastSyncedNotes = ""
    private var lastSyncedPic = DEFAULT_PIC_NAME
    private var activePeriodKey = ""

    private val periodFlow = _selection
        .map { it.month to it.year }
        .distinctUntilChanged()
        .flatMapLatest { (month, year) ->
            repository.observePeriod(currentUserId, month, year)
        }

    private val issuesFlow = _selection
        .map { it.month to it.year }
        .distinctUntilChanged()
        .flatMapLatest { (month, year) ->
            repository.observeIssues(currentUserId, month, year)
        }

    val uiState: StateFlow<UiState<PjuDashboardData>> = combine(
        periodFlow,
        issuesFlow,
        _selection
    ) { period, issues, sel ->
        val periodKey = "${period.year}_${period.month}"
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
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
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
                    val sel = _selection.value
                    val key = "${sel.year}_${sel.month}"
                    if (key == activePeriodKey && draft != lastSyncedNotes) {
                        val result = repository.updateGeneralNotes(currentUserId, sel.month, sel.year, draft)
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
                    val sel = _selection.value
                    val key = "${sel.year}_${sel.month}"
                    val trimmed = draft.trim()
                    if (key == activePeriodKey && trimmed != lastSyncedPic) {
                        val result = repository.updatePicName(currentUserId, sel.month, sel.year, trimmed)
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
        val currentCode = period.checks[buildCellKey(cluster.id, slotIndex, date)]
        val nextStatus = LampStatus.fromCode(currentCode).next()
        val sel = _selection.value
        viewModelScope.launch {
            val result = repository.setCellStatus(
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
        val sel = _selection.value
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateSlotLabels(
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
        val sel = _selection.value
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.addIssue(
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
        val sel = _selection.value
        viewModelScope.launch {
            val result = repository.cycleIssueStatus(
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
        val sel = _selection.value
        viewModelScope.launch {
            val result = repository.deleteIssue(
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
        val sel = _selection.value
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.resetPeriod(currentUserId, sel.month, sel.year)
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
