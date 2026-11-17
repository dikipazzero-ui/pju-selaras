package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ChecklistPeriod
import com.example.model.DEFAULT_PIC_NAME
import com.example.model.IssueStatus
import com.example.model.LampStatus
import com.example.model.PjuIssue
import com.example.model.buildCellKey
import com.example.model.buildPeriodId
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PjuChecklistRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
        check(uid == expectedUserId) { "Sesi pengguna berubah; operasi dibatalkan." }
        return uid
    }

    private fun periodDocRef(userId: String, periodId: String) =
        db.collection("users").document(userId)
            .collection("periods").document(periodId)

    private fun initialPeriodPayload(
        userId: String,
        month: String,
        year: Int,
        checks: Map<String, String> = emptyMap(),
        slotLabels: Map<String, List<String>> = emptyMap(),
        generalNotes: String = "",
        picName: String = DEFAULT_PIC_NAME
    ): Map<String, Any> = mapOf(
        "userId" to userId,
        "month" to month.uppercase(),
        "year" to year.toLong(),
        "checks" to checks,
        "slotLabels" to slotLabels,
        "generalNotes" to generalNotes.take(2000),
        "picName" to picName.take(150),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun observePeriod(userId: String, month: String, year: Int): Flow<ChecklistPeriod> = flow {
        val uid = requireUserId(userId)
        val periodId = buildPeriodId(year, month)
        val docRef = periodDocRef(uid, periodId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ChecklistPeriod::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: ChecklistPeriod(
                            id = periodId,
                            userId = uid,
                            month = month.uppercase(),
                            year = year.toLong()
                        )
                    } else {
                        ChecklistPeriod(
                            id = periodId,
                            userId = uid,
                            month = month.uppercase(),
                            year = year.toLong()
                        )
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeIssues(userId: String, month: String, year: Int): Flow<List<PjuIssue>> = flow {
        val uid = requireUserId(userId)
        val periodId = buildPeriodId(year, month)
        val collectionRef = periodDocRef(uid, periodId).collection("issues")
        emitAll(
            collectionRef
                .orderBy("date", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PjuIssue::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, collectionRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun setCellStatus(
        userId: String,
        month: String,
        year: Int,
        clusterId: Int,
        slotIndex: Int,
        date: Int,
        nextStatus: LampStatus
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val docRef = periodDocRef(userId, periodId)
        return try {
            val uid = requireUserId(userId)
            require(clusterId in 1..9) { "Cluster ID tidak valid" }
            require(slotIndex in 0..2) { "Slot index tidak valid" }
            require(date in 1..31) { "Tanggal tidak valid" }

            val cellKey = buildCellKey(clusterId, slotIndex, date)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val initialChecks = if (nextStatus == LampStatus.EMPTY) emptyMap() else mapOf(cellKey to nextStatus.code)
                docRef.set(initialPeriodPayload(uid, month, year, checks = initialChecks)).await()
            } else {
                val valueToWrite: Any = if (nextStatus == LampStatus.EMPTY) FieldValue.delete() else nextStatus.code
                docRef.update(
                    mapOf(
                        "checks.$cellKey" to valueToWrite,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSlotLabels(
        userId: String,
        month: String,
        year: Int,
        clusterId: Int,
        labels: List<String>
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val docRef = periodDocRef(userId, periodId)
        return try {
            val uid = requireUserId(userId)
            require(clusterId in 1..9) { "Cluster ID tidak valid" }
            val sanitized = (0 until 3).map { idx -> labels.getOrNull(idx)?.trim()?.take(60) ?: "" }
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                docRef.set(
                    initialPeriodPayload(
                        userId = uid,
                        month = month,
                        year = year,
                        slotLabels = mapOf(clusterId.toString() to sanitized)
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "slotLabels.$clusterId" to sanitized,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateGeneralNotes(
        userId: String,
        month: String,
        year: Int,
        notes: String
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val docRef = periodDocRef(userId, periodId)
        return try {
            val uid = requireUserId(userId)
            val safeNotes = notes.take(2000)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                docRef.set(initialPeriodPayload(uid, month, year, generalNotes = safeNotes)).await()
            } else {
                docRef.update(
                    mapOf(
                        "generalNotes" to safeNotes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePicName(
        userId: String,
        month: String,
        year: Int,
        picName: String
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val docRef = periodDocRef(userId, periodId)
        return try {
            val uid = requireUserId(userId)
            val safePic = picName.trim().take(150)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                docRef.set(initialPeriodPayload(uid, month, year, picName = safePic)).await()
            } else {
                docRef.update(
                    mapOf(
                        "picName" to safePic,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addIssue(
        userId: String,
        month: String,
        year: Int,
        date: Int,
        clusterId: Int,
        clusterName: String,
        unit: String,
        desc: String,
        status: IssueStatus
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val periodRef = periodDocRef(userId, periodId)
        val issueRef = periodRef.collection("issues").document()
        return try {
            val uid = requireUserId(userId)
            val cleanUnit = unit.trim().take(100)
            val cleanDesc = desc.trim().take(500)
            val cleanClusterName = clusterName.trim().take(60)
            require(date in 1..31) { "Tanggal harus 1 sampai 31" }
            require(clusterId in 1..9) { "Cluster tidak valid" }
            require(cleanUnit.isNotBlank()) { "Titik/Slot lampu wajib diisi" }
            require(cleanDesc.isNotBlank()) { "Deskripsi permasalahan wajib diisi" }
            require(cleanClusterName.isNotBlank()) { "Nama cluster wajib diisi" }

            val periodExists = periodRef.get().await().exists()
            val batch = db.batch()
            if (!periodExists) {
                batch.set(periodRef, initialPeriodPayload(uid, month, year))
            }
            val issuePayload = mapOf(
                "userId" to uid,
                "periodId" to periodId,
                "date" to date.toLong(),
                "clusterId" to clusterId.toLong(),
                "clusterName" to cleanClusterName,
                "unit" to cleanUnit,
                "desc" to cleanDesc,
                "status" to status.label,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(issueRef, issuePayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, issueRef.path)
            Result.failure(e)
        }
    }

    suspend fun cycleIssueStatus(
        userId: String,
        month: String,
        year: Int,
        issue: PjuIssue
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val issueRef = periodDocRef(userId, periodId).collection("issues").document(issue.id)
        return try {
            requireUserId(userId)
            require(issue.id.isNotBlank()) { "ID laporan tidak boleh kosong" }
            val nextStatus = IssueStatus.fromLabel(issue.status).next()
            issueRef.update(
                mapOf(
                    "status" to nextStatus.label,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, issueRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteIssue(
        userId: String,
        month: String,
        year: Int,
        issueId: String
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val issueRef = periodDocRef(userId, periodId).collection("issues").document(issueId)
        return try {
            requireUserId(userId)
            require(issueId.isNotBlank()) { "ID laporan tidak boleh kosong" }
            issueRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, issueRef.path)
            Result.failure(e)
        }
    }

    suspend fun resetPeriod(
        userId: String,
        month: String,
        year: Int
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val periodRef = periodDocRef(userId, periodId)
        return try {
            requireUserId(userId)
            val issuesSnapshot = periodRef.collection("issues").get().await()
            val batch = db.batch()
            for (doc in issuesSnapshot.documents) {
                batch.delete(doc.reference)
            }
            if (periodRef.get().await().exists()) {
                batch.delete(periodRef)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, periodRef.path)
            Result.failure(e)
        }
    }
}
