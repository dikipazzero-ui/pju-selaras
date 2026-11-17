package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val DEFAULT_PIC_NAME = "R. MOH. GANDI / NERI WELAS ANTORO"
const val DEFAULT_MONTH = "NOVEMBER"
const val DEFAULT_YEAR = 2026
const val DEFAULT_TODAY_DATE = 17
const val DEFAULT_WORKSPACE_NAME = "Tim Laporan Harian BPS"

val INDONESIAN_MONTHS = listOf(
    "JANUARI",
    "FEBRUARI",
    "MARET",
    "APRIL",
    "MEI",
    "JUNI",
    "JULI",
    "AGUSTUS",
    "SEPTEMBER",
    "OKTOBER",
    "NOVEMBER",
    "DESEMBER"
)

val AVAILABLE_YEARS = listOf(2025, 2026, 2027, 2028)

val CHAT_TOPICS = listOf(
    "Lapangan",
    "Dokumentasi",
    "Kendala",
    "Koordinasi",
    "Keamanan",
    "Kebersihan"
)

data class PjuCluster(
    val id: Int,
    val name: String
)

val PJU_CLUSTERS = listOf(
    PjuCluster(1, "PJU KRISAN"),
    PjuCluster(2, "PJU CEMPAKA"),
    PjuCluster(3, "PJU CENDANA"),
    PjuCluster(4, "PJU CEMARA"),
    PjuCluster(5, "PJU FLAMBOYAN 1"),
    PjuCluster(6, "PJU FLAMBOYAN 2"),
    PjuCluster(7, "PJU FLAMBOYAN FASUM"),
    PjuCluster(8, "PJU AKASIA"),
    PjuCluster(9, "RUKO")
)

enum class LampStatus(
    val code: String,
    val symbol: String,
    val labelId: String,
    val csvLabel: String
) {
    EMPTY("", "", "Kosong", ""),
    NORMAL("normal", "✔", "Menyala / Normal", "MENYALA"),
    WARN("warn", "⚠", "Redup / Kedip", "REDUP"),
    ERROR("error", "✖", "Mati / Bermasalah", "MATI/RUSAK");

    fun next(): LampStatus = when (this) {
        EMPTY -> NORMAL
        NORMAL -> WARN
        WARN -> ERROR
        ERROR -> EMPTY
    }

    companion object {
        fun fromCode(code: String?): LampStatus =
            entries.firstOrNull { it.code == code } ?: EMPTY
    }
}

enum class IssueStatus(val label: String) {
    BELUM("Belum Diperbaiki"),
    PROSES("Dalam Proses"),
    SELESAI("Selesai Diperbaiki");

    fun next(): IssueStatus = when (this) {
        BELUM -> PROSES
        PROSES -> SELESAI
        SELESAI -> BELUM
    }

    companion object {
        fun fromLabel(raw: String?): IssueStatus =
            entries.firstOrNull { it.label == raw } ?: BELUM
    }
}

enum class DateRangePreset(
    val label: String,
    val shortLabel: String,
    val startDay: Int,
    val endDay: Int
) {
    FORM_DEFAULT("Tanggal 16 - 24 (Sesuai Form)", "16 - 24", 16, 24),
    FIRST_HALF("Tanggal 1 - 15", "1 - 15", 1, 15),
    SECOND_HALF("Tanggal 16 - 31", "16 - 31", 16, 31),
    FULL_MONTH("Semua Tanggal (1 - 31)", "1 - 31", 1, 31);

    fun dates(): List<Int> = (startDay..endDay).toList()
}

@Keep
@IgnoreExtraProperties
data class TeamWorkspace(
    @DocumentId val id: String = "",
    val name: String = DEFAULT_WORKSPACE_NAME,
    val ownerId: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChecklistPeriod(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val memberUids: List<String> = emptyList(),
    val updatedByUid: String = "",
    val updatedByName: String = "",
    val userId: String = "",
    val month: String = DEFAULT_MONTH,
    val year: Long = DEFAULT_YEAR.toLong(),
    val checks: Map<String, String> = emptyMap(),
    val slotLabels: Map<String, List<String>> = emptyMap(),
    val generalNotes: String = "",
    val picName: String = DEFAULT_PIC_NAME,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PjuIssue(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val periodId: String = "",
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    val createdByName: String = "",
    val userId: String = "",
    val date: Long = DEFAULT_TODAY_DATE.toLong(),
    val clusterId: Long = 1L,
    val clusterName: String = PJU_CLUSTERS.first().name,
    val unit: String = "",
    val desc: String = "",
    val status: String = IssueStatus.BELUM.label,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val memberUids: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val text: String = "",
    val tagCluster: String = "",
    val messageType: String = "text",
    val audioBase64: String = "",
    val audioDurationSec: Long = 0L,
    val imageBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val isVoiceNote: Boolean
        get() = messageType == "voice" && audioBase64.isNotBlank()

    val isImageMessage: Boolean
        get() = messageType == "image" && imageBase64.isNotBlank()
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val userId: String = "",
    val memberUids: List<String> = emptyList(),
    val displayName: String = "",
    val photoBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class ActivityReportType(val code: String, val labelId: String) {
    KEGIATAN("Kegiatan", "Uraian Kegiatan"),
    KEJADIAN("Kejadian", "Pelaporan Kejadian");

    companion object {
        fun fromCode(raw: String?): ActivityReportType =
            entries.firstOrNull { it.code == raw } ?: KEGIATAN
    }
}

enum class ActivityReportStatus(val label: String) {
    TERCATAT("Tercatat"),
    PROSES("Ditindaklanjuti"),
    SELESAI("Selesai");

    fun next(): ActivityReportStatus = when (this) {
        TERCATAT -> PROSES
        PROSES -> SELESAI
        SELESAI -> TERCATAT
    }

    companion object {
        fun fromLabel(raw: String?): ActivityReportStatus =
            entries.firstOrNull { it.label == raw } ?: TERCATAT
    }
}

@Keep
@IgnoreExtraProperties
data class ActivityReport(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    val memberName: String = "",
    val reportType: String = ActivityReportType.KEGIATAN.code,
    val eventDate: String = "",
    val eventTime: String = "",
    val location: String = "",
    val description: String = "",
    val status: String = ActivityReportStatus.SELESAI.label,
    val photoBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyReport(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    val memberName: String = "",
    val eventDate: String = "",
    val eventTime: String = "",
    val dayName: String = "",
    val location: String = "",
    val description: String = "",
    val photos: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun buildPeriodId(year: Int, month: String): String = "${year}_${month.uppercase()}"

fun buildCellKey(clusterId: Int, slotIndex: Int, date: Int): String = "${clusterId}_${slotIndex}_${date}"

fun currentIsoDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun currentTimeHm(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())

fun currentIndonesianMonth(): String {
    val idx = Calendar.getInstance().get(Calendar.MONTH)
    return INDONESIAN_MONTHS.getOrElse(idx) { DEFAULT_MONTH }
}

fun currentYearInt(): Int = Calendar.getInstance().get(Calendar.YEAR)

fun indonesianMonthNumber(monthName: String): Int {
    val idx = INDONESIAN_MONTHS.indexOfFirst { it.equals(monthName.trim(), ignoreCase = true) }
    return if (idx >= 0) idx + 1 else 1
}

fun monthPrefixIso(year: Int, monthName: String): String {
    val m = indonesianMonthNumber(monthName)
    return String.format(Locale.US, "%04d-%02d-", year, m)
}

fun indonesianDayNameFromIsoDate(isoDate: String): String {
    return try {
        val parts = isoDate.split("-")
        if (parts.size != 3) return ""
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, parts[0].toInt())
            set(Calendar.MONTH, parts[1].toInt() - 1)
            set(Calendar.DAY_OF_MONTH, parts[2].toInt())
        }
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> ""
        }
    } catch (_: Exception) {
        ""
    }
}

fun formatIndonesianFullDate(isoDate: String, dayName: String = ""): String {
    val parts = isoDate.split("-")
    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val resolvedDay = dayName.ifBlank { indonesianDayNameFromIsoDate(isoDate) }
    val dateLabel = if (parts.size == 3) {
        val y = parts[0]
        val mIdx = parts[1].toIntOrNull()?.minus(1) ?: -1
        val d = parts[2].toIntOrNull() ?: 0
        val mLabel = monthNames.getOrNull(mIdx) ?: parts[1]
        if (d > 0) "$d $mLabel $y" else isoDate
    } else {
        isoDate
    }
    return if (resolvedDay.isNotBlank()) "$resolvedDay, $dateLabel" else dateLabel
}
