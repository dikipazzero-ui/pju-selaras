package com.example.data

import android.content.Context
import com.example.R
import com.example.media.formatDurationSeconds
import com.example.model.ActivityReport
import com.example.model.ActivityReportStatus
import com.example.model.ActivityReportType
import com.example.model.ChatMessage
import com.example.model.ChecklistPeriod
import com.example.model.DEFAULT_MONTH
import com.example.model.DEFAULT_PIC_NAME
import com.example.model.DEFAULT_WORKSPACE_NAME
import com.example.model.DEFAULT_YEAR
import com.example.model.IssueStatus
import com.example.model.LampStatus
import com.example.model.PjuIssue
import com.example.model.TeamWorkspace
import com.example.model.UserProfile
import com.example.model.buildCellKey
import com.example.model.buildPeriodId
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

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

    private fun currentDisplayName(): String {
        val user = auth.currentUser
        val raw = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: "Petugas PJU"
        return raw.trim().take(80)
    }

    private fun currentEmail(): String {
        return auth.currentUser?.email?.trim()?.take(120) ?: ""
    }

    private fun workspaceRef(workspaceId: String) =
        db.collection("workspaces").document(workspaceId)

    private fun periodDocRef(workspaceId: String, periodId: String) =
        workspaceRef(workspaceId).collection("periods").document(periodId)

    private fun messagesCollection(workspaceId: String) =
        workspaceRef(workspaceId).collection("messages")

    private fun profilesCollection(workspaceId: String) =
        workspaceRef(workspaceId).collection("profiles")

    private fun activitiesCollection(workspaceId: String) =
        workspaceRef(workspaceId).collection("activities")

    private fun personalProfileDoc(uid: String) =
        db.collection("users").document(uid).collection("profile").document("main")

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..4).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
        return "BPS$suffix"
    }

    private fun initialPeriodPayload(
        workspace: TeamWorkspace,
        actorUid: String,
        actorName: String,
        month: String,
        year: Int,
        checks: Map<String, String> = emptyMap(),
        slotLabels: Map<String, List<String>> = emptyMap(),
        generalNotes: String = "",
        picName: String = DEFAULT_PIC_NAME
    ): Map<String, Any> {
        val members = workspace.memberUids.ifEmpty { listOf(actorUid) }.take(50)
        return mapOf(
            "workspaceId" to workspace.id,
            "memberUids" to members,
            "updatedByUid" to actorUid,
            "updatedByName" to actorName.take(80).ifBlank { "Petugas PJU" },
            "month" to month.uppercase(),
            "year" to year.toLong(),
            "checks" to checks,
            "slotLabels" to slotLabels,
            "generalNotes" to generalNotes.take(2000),
            "picName" to picName.take(150),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun observeUserWorkspaces(userId: String): Flow<List<TeamWorkspace>> = flow {
        val uid = requireUserId(userId)
        val query = db.collection("workspaces").whereArrayContains("memberUids", uid)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TeamWorkspace::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "workspaces")
                    }
                    throw error
                }
        )
    }

    suspend fun ensureDefaultWorkspace(userId: String): Result<TeamWorkspace> {
        val uid = try {
            requireUserId(userId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val existing = db.collection("workspaces")
                .whereArrayContains("memberUids", uid)
                .get()
                .await()
                .toObjects(
                    TeamWorkspace::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            if (existing.isNotEmpty()) {
                return Result.success(existing.first())
            }
            val created = createWorkspaceInternal(uid, DEFAULT_WORKSPACE_NAME, migrateLegacy = true)
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "workspaces")
            Result.failure(e)
        }
    }

    suspend fun createWorkspace(userId: String, name: String): Result<TeamWorkspace> {
        val uid = try {
            requireUserId(userId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val cleanName = name.trim().take(60)
            require(cleanName.length >= 2) { "Nama tim minimal 2 karakter" }
            val created = createWorkspaceInternal(uid, cleanName, migrateLegacy = false)
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "workspaces")
            Result.failure(e)
        }
    }

    private suspend fun createWorkspaceInternal(
        uid: String,
        name: String,
        migrateLegacy: Boolean
    ): TeamWorkspace {
        val actorName = currentDisplayName()
        var inviteCode = generateInviteCode()
        for (attempt in 0 until 4) {
            val existingInvite = db.collection("workspaceInvites").document(inviteCode).get().await()
            if (!existingInvite.exists()) break
            inviteCode = generateInviteCode()
        }

        val wsRef = db.collection("workspaces").document()
        val inviteRef = db.collection("workspaceInvites").document(inviteCode)

        val workspacePayload = mapOf(
            "name" to name.take(60),
            "ownerId" to uid,
            "inviteCode" to inviteCode,
            "memberUids" to listOf(uid),
            "memberNames" to mapOf(uid to actorName),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val invitePayload = mapOf(
            "workspaceId" to wsRef.id,
            "workspaceName" to name.take(60),
            "ownerId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(wsRef, workspacePayload)
        batch.set(inviteRef, invitePayload)

        // If user had legacy personal checklist data for default period, copy it into the new workspace
        if (migrateLegacy) {
            val defaultPeriodId = buildPeriodId(DEFAULT_YEAR, DEFAULT_MONTH)
            val legacyPeriodRef = db.collection("users").document(uid)
                .collection("periods").document(defaultPeriodId)
            val legacySnapshot = runCatching { legacyPeriodRef.get().await() }.getOrNull()
            if (legacySnapshot != null && legacySnapshot.exists()) {
                val legacy = legacySnapshot.toObject(ChecklistPeriod::class.java)
                if (legacy != null) {
                    val newPeriodRef = periodDocRef(wsRef.id, defaultPeriodId)
                    val tempWs = TeamWorkspace(
                        id = wsRef.id,
                        name = name,
                        ownerId = uid,
                        inviteCode = inviteCode,
                        memberUids = listOf(uid),
                        memberNames = mapOf(uid to actorName)
                    )
                    batch.set(
                        newPeriodRef,
                        initialPeriodPayload(
                            workspace = tempWs,
                            actorUid = uid,
                            actorName = actorName,
                            month = legacy.month.ifBlank { DEFAULT_MONTH },
                            year = legacy.year.toInt().takeIf { it in 2020..2100 } ?: DEFAULT_YEAR,
                            checks = legacy.checks,
                            slotLabels = legacy.slotLabels,
                            generalNotes = legacy.generalNotes,
                            picName = legacy.picName.ifBlank { DEFAULT_PIC_NAME }
                        )
                    )
                }
            }
        }

        batch.commit().await()
        return TeamWorkspace(
            id = wsRef.id,
            name = name,
            ownerId = uid,
            inviteCode = inviteCode,
            memberUids = listOf(uid),
            memberNames = mapOf(uid to actorName)
        )
    }

    suspend fun joinWorkspaceByCode(userId: String, rawCode: String): Result<TeamWorkspace> {
        val code = rawCode.trim().uppercase()
        var targetPath = "workspaceInvites/$code"
        return try {
            val uid = requireUserId(userId)
            require(code.length in 4..16 && code.matches(Regex("^[A-Z0-9_\\-]+$"))) {
                "Kode tim harus 4-16 huruf/angka (contoh: BPS2026)"
            }

            val inviteSnap = db.collection("workspaceInvites").document(code).get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("Kode tim \"$code\" tidak ditemukan."))
            }

            val workspaceId = inviteSnap.getString("workspaceId")
                ?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Data undangan tidak lengkap."))

            val wsRef = workspaceRef(workspaceId)
            targetPath = wsRef.path
            val actorName = currentDisplayName()

            // Try reading workspace first if already a member
            val alreadyMemberSnap = runCatching { wsRef.get().await() }.getOrNull()
            if (alreadyMemberSnap != null && alreadyMemberSnap.exists()) {
                val existingWs = alreadyMemberSnap.toObject(
                    TeamWorkspace::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existingWs != null && uid in existingWs.memberUids) {
                    return Result.success(existingWs)
                }
            }

            // Cannot read workspace before being a member, so fetch invite metadata and update atomically?
            // Note: Security rules validate existing().memberUids + 1, so we use FieldValue.arrayUnion
            // combined with map field update "memberNames.$uid"
            wsRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "memberNames.$uid" to actorName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val joinedSnap = wsRef.get().await()
            val joinedWs = joinedSnap.toObject(
                TeamWorkspace::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: TeamWorkspace(
                id = workspaceId,
                name = inviteSnap.getString("workspaceName") ?: DEFAULT_WORKSPACE_NAME,
                inviteCode = code,
                memberUids = listOf(uid),
                memberNames = mapOf(uid to actorName)
            )
            Result.success(joinedWs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, targetPath)
            Result.failure(e)
        }
    }

    suspend fun syncMemberListsOnWorkspaceDocs(
        workspace: TeamWorkspace,
        period: ChecklistPeriod?,
        issues: List<PjuIssue>,
        messages: List<ChatMessage>,
        profiles: List<UserProfile> = emptyList(),
        activities: List<ActivityReport> = emptyList()
    ) {
        val uid = auth.currentUser?.uid ?: return
        val targetMembers = workspace.memberUids
        if (targetMembers.isEmpty() || uid !in targetMembers) return

        val missingOnPeriod = period != null &&
            period.id.isNotBlank() &&
            uid in period.memberUids &&
            !period.memberUids.containsAll(targetMembers)

        val outdatedIssues = issues.filter {
            it.id.isNotBlank() && uid in it.memberUids && !it.memberUids.containsAll(targetMembers)
        }
        val outdatedMessages = messages.filter {
            it.id.isNotBlank() && uid in it.memberUids && !it.memberUids.containsAll(targetMembers)
        }
        val outdatedProfiles = profiles.filter {
            it.userId.isNotBlank() && uid in it.memberUids && !it.memberUids.containsAll(targetMembers)
        }
        val outdatedActivities = activities.filter {
            it.id.isNotBlank() && uid in it.memberUids && !it.memberUids.containsAll(targetMembers)
        }

        if (!missingOnPeriod && outdatedIssues.isEmpty() && outdatedMessages.isEmpty() && outdatedProfiles.isEmpty() && outdatedActivities.isEmpty()) return

        try {
            val batch = db.batch()
            var ops = 0
            val actorName = currentDisplayName()

            if (missingOnPeriod && period != null) {
                batch.update(
                    periodDocRef(workspace.id, period.id),
                    mapOf(
                        "memberUids" to targetMembers,
                        "updatedByUid" to uid,
                        "updatedByName" to actorName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                ops++
            }
            for (issue in outdatedIssues) {
                if (ops >= 450) break
                batch.update(
                    periodDocRef(workspace.id, issue.periodId)
                        .collection("issues").document(issue.id),
                    mapOf(
                        "memberUids" to targetMembers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                ops++
            }
            for (msg in outdatedMessages) {
                if (ops >= 450) break
                batch.update(
                    messagesCollection(workspace.id).document(msg.id),
                    mapOf("memberUids" to targetMembers)
                )
                ops++
            }
            for (profile in outdatedProfiles) {
                if (ops >= 450) break
                batch.update(
                    profilesCollection(workspace.id).document(profile.userId),
                    mapOf(
                        "memberUids" to targetMembers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                ops++
            }
            for (act in outdatedActivities) {
                if (ops >= 450) break
                batch.update(
                    activitiesCollection(workspace.id).document(act.id),
                    mapOf(
                        "memberUids" to targetMembers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                ops++
            }
            if (ops > 0) {
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "workspaces/${workspace.id}")
        }
    }

    fun observePeriod(workspace: TeamWorkspace, userId: String, month: String, year: Int): Flow<ChecklistPeriod> = flow {
        val uid = requireUserId(userId)
        val periodId = buildPeriodId(year, month)
        val docRef = periodDocRef(workspace.id, periodId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ChecklistPeriod::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: ChecklistPeriod(
                            id = periodId,
                            workspaceId = workspace.id,
                            memberUids = workspace.memberUids,
                            month = month.uppercase(),
                            year = year.toLong()
                        )
                    } else {
                        ChecklistPeriod(
                            id = periodId,
                            workspaceId = workspace.id,
                            memberUids = workspace.memberUids,
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

    fun observeIssues(workspace: TeamWorkspace, userId: String, month: String, year: Int): Flow<List<PjuIssue>> = flow {
        val uid = requireUserId(userId)
        val periodId = buildPeriodId(year, month)
        val collectionRef = periodDocRef(workspace.id, periodId).collection("issues")
        emitAll(
            collectionRef
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PjuIssue::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<PjuIssue> { it.date }
                            .thenBy { it.createdAt?.toDate()?.time ?: 0L }
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

    fun observeChatMessages(workspace: TeamWorkspace, userId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId(userId)
        val collectionRef = messagesCollection(workspace.id)
        emitAll(
            collectionRef
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
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

    fun observePersonalProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val docRef = personalProfileDoc(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
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

    fun observeWorkspaceProfiles(workspace: TeamWorkspace, userId: String): Flow<Map<String, UserProfile>> = flow {
        val uid = requireUserId(userId)
        val collectionRef = profilesCollection(workspace.id)
        emitAll(
            collectionRef
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).associateBy { profile -> profile.userId.ifBlank { profile.id } }
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

    suspend fun saveUserProfile(
        userId: String,
        workspaces: List<TeamWorkspace>,
        displayName: String,
        photoBase64: String
    ): Result<Unit> {
        var docPath = "users/$userId/profile/main"
        return try {
            val uid = requireUserId(userId)
            val cleanName = displayName.trim().take(80).ifBlank { currentDisplayName() }
            val cleanPhoto = photoBase64.take(120_000)

            val personalRef = personalProfileDoc(uid)
            docPath = personalRef.path
            val personalExists = personalRef.get().await().exists()
            if (personalExists) {
                personalRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "photoBase64" to cleanPhoto,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                personalRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "photoBase64" to cleanPhoto,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            for (ws in workspaces) {
                val wsProfileRef = profilesCollection(ws.id).document(uid)
                docPath = wsProfileRef.path
                val members = ws.memberUids.ifEmpty { listOf(uid) }.take(50)
                val wsProfileExists = wsProfileRef.get().await().exists()
                if (wsProfileExists) {
                    wsProfileRef.update(
                        mapOf(
                            "displayName" to cleanName,
                            "photoBase64" to cleanPhoto,
                            "memberUids" to members,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    wsProfileRef.set(
                        mapOf(
                            "workspaceId" to ws.id,
                            "userId" to uid,
                            "memberUids" to members,
                            "displayName" to cleanName,
                            "photoBase64" to cleanPhoto,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun ensurePersonalProfileSyncedToWorkspace(
        workspace: TeamWorkspace,
        userId: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        if (uid != userId || uid !in workspace.memberUids) return
        val wsProfileRef = profilesCollection(workspace.id).document(uid)
        try {
            val wsProfileSnap = wsProfileRef.get().await()
            if (wsProfileSnap.exists()) return
            val personalSnap = personalProfileDoc(uid).get().await()
            if (!personalSnap.exists()) return
            val personal = personalSnap.toObject(UserProfile::class.java) ?: return
            if (personal.photoBase64.isBlank() && personal.displayName.isBlank()) return
            val members = workspace.memberUids.ifEmpty { listOf(uid) }.take(50)
            wsProfileRef.set(
                mapOf(
                    "workspaceId" to workspace.id,
                    "userId" to uid,
                    "memberUids" to members,
                    "displayName" to personal.displayName.ifBlank { currentDisplayName() }.take(80),
                    "photoBase64" to personal.photoBase64.take(120_000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, wsProfileRef.path)
        }
    }

    suspend fun sendChatMessage(
        workspace: TeamWorkspace,
        userId: String,
        text: String,
        tagCluster: String = "",
        senderDisplayName: String? = null
    ): Result<Unit> {
        val collectionRef = messagesCollection(workspace.id)
        val docRef = collectionRef.document()
        return try {
            val uid = requireUserId(userId)
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotBlank()) { "Pesan tidak boleh kosong" }
            val cleanTag = tagCluster.trim().take(60)
            val cleanSenderName = senderDisplayName?.trim()?.take(80)?.ifBlank { null } ?: currentDisplayName()
            val members = workspace.memberUids.ifEmpty { listOf(uid) }.take(50)
            val payload = mapOf(
                "workspaceId" to workspace.id,
                "memberUids" to members,
                "senderId" to uid,
                "senderName" to cleanSenderName,
                "senderEmail" to currentEmail(),
                "text" to cleanText,
                "tagCluster" to cleanTag,
                "messageType" to "text",
                "audioBase64" to "",
                "audioDurationSec" to 0L,
                "imageBase64" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendVoiceMessage(
        workspace: TeamWorkspace,
        userId: String,
        audioBase64: String,
        durationSec: Int,
        tagCluster: String = "",
        senderDisplayName: String? = null
    ): Result<Unit> {
        val collectionRef = messagesCollection(workspace.id)
        val docRef = collectionRef.document()
        return try {
            val uid = requireUserId(userId)
            require(audioBase64.length in 10..450_000) { "Ukuran voice note tidak valid atau melebihi batas." }
            val safeDuration = durationSec.coerceIn(1, 120)
            val cleanTag = tagCluster.trim().take(60)
            val cleanSenderName = senderDisplayName?.trim()?.take(80)?.ifBlank { null } ?: currentDisplayName()
            val members = workspace.memberUids.ifEmpty { listOf(uid) }.take(50)
            val payload = mapOf(
                "workspaceId" to workspace.id,
                "memberUids" to members,
                "senderId" to uid,
                "senderName" to cleanSenderName,
                "senderEmail" to currentEmail(),
                "text" to "Pesan suara (${formatDurationSeconds(safeDuration)})",
                "tagCluster" to cleanTag,
                "messageType" to "voice",
                "audioBase64" to audioBase64,
                "audioDurationSec" to safeDuration.toLong(),
                "imageBase64" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendImageMessage(
        workspace: TeamWorkspace,
        userId: String,
        imageBase64: String,
        caption: String = "",
        tagCluster: String = "",
        senderDisplayName: String? = null
    ): Result<Unit> {
        val collectionRef = messagesCollection(workspace.id)
        val docRef = collectionRef.document()
        return try {
            val uid = requireUserId(userId)
            require(imageBase64.length in 10..450_000) { "Ukuran gambar tidak valid atau melebihi batas." }
            val cleanCaption = caption.trim().take(1000).ifBlank { "Foto laporan PJU" }
            val cleanTag = tagCluster.trim().take(60)
            val cleanSenderName = senderDisplayName?.trim()?.take(80)?.ifBlank { null } ?: currentDisplayName()
            val members = workspace.memberUids.ifEmpty { listOf(uid) }.take(50)
            val payload = mapOf(
                "workspaceId" to workspace.id,
                "memberUids" to members,
                "senderId" to uid,
                "senderName" to cleanSenderName,
                "senderEmail" to currentEmail(),
                "text" to cleanCaption,
                "tagCluster" to cleanTag,
                "messageType" to "image",
                "audioBase64" to "",
                "audioDurationSec" to 0L,
                "imageBase64" to imageBase64,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeActivityReports(workspace: TeamWorkspace, userId: String): Flow<List<ActivityReport>> = flow {
        val uid = requireUserId(userId)
        val collectionRef = activitiesCollection(workspace.id)
        emitAll(
            collectionRef
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ActivityReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<ActivityReport> { it.eventDate }
                            .thenByDescending { it.eventTime }
                            .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
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

    suspend fun addActivityReport(
        workspace: TeamWorkspace,
        userId: String,
        memberName: String,
        reportType: ActivityReportType,
        eventDate: String,
        eventTime: String,
        location: String,
        description: String,
        status: ActivityReportStatus,
        photoBase64: String = ""
    ): Result<Unit> {
        val docRef = activitiesCollection(workspace.id).document()
        return try {
            val uid = requireUserId(userId)
            val cleanMember = memberName.trim().take(80)
            val cleanDate = eventDate.trim()
            val cleanTime = eventTime.trim()
            val cleanLocation = location.trim().take(100)
            val cleanDesc = description.trim().take(1500)
            val cleanPhoto = photoBase64.take(450_000)

            require(cleanMember.isNotBlank()) { "Nama anggota wajib diisi" }
            require(cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) { "Format tanggal harus YYYY-MM-DD" }
            require(cleanTime.matches(Regex("^\\d{2}:\\d{2}$"))) { "Format jam harus HH:MM" }
            require(cleanDesc.isNotBlank()) { "Uraian kegiatan/kejadian wajib diisi" }

            val members = workspace.memberUids.ifEmpty { listOf(uid) }.take(50)
            val payload = mapOf(
                "workspaceId" to workspace.id,
                "memberUids" to members,
                "createdByUid" to uid,
                "memberName" to cleanMember,
                "reportType" to reportType.code,
                "eventDate" to cleanDate,
                "eventTime" to cleanTime,
                "location" to cleanLocation,
                "description" to cleanDesc,
                "status" to status.label,
                "photoBase64" to cleanPhoto,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cycleActivityReportStatus(
        workspace: TeamWorkspace,
        userId: String,
        report: ActivityReport
    ): Result<Unit> {
        val docRef = activitiesCollection(workspace.id).document(report.id)
        return try {
            val uid = requireUserId(userId)
            require(report.id.isNotBlank()) { "ID laporan tidak boleh kosong" }
            val nextStatus = ActivityReportStatus.fromLabel(report.status).next()
            docRef.update(
                mapOf(
                    "status" to nextStatus.label,
                    "memberUids" to workspace.memberUids.ifEmpty { listOf(uid) }.take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteActivityReport(
        workspace: TeamWorkspace,
        userId: String,
        reportId: String
    ): Result<Unit> {
        val docRef = activitiesCollection(workspace.id).document(reportId)
        return try {
            requireUserId(userId)
            require(reportId.isNotBlank()) { "ID laporan tidak boleh kosong" }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteChatMessage(
        workspace: TeamWorkspace,
        userId: String,
        messageId: String
    ): Result<Unit> {
        val docRef = messagesCollection(workspace.id).document(messageId)
        return try {
            requireUserId(userId)
            require(messageId.isNotBlank()) { "ID pesan tidak boleh kosong" }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setCellStatus(
        workspace: TeamWorkspace,
        userId: String,
        month: String,
        year: Int,
        clusterId: Int,
        slotIndex: Int,
        date: Int,
        nextStatus: LampStatus
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val docRef = periodDocRef(workspace.id, periodId)
        return try {
            val uid = requireUserId(userId)
            val actorName = currentDisplayName()
            require(clusterId in 1..9) { "Cluster ID tidak valid" }
            require(slotIndex in 0..2) { "Slot index tidak valid" }
            require(date in 1..31) { "Tanggal tidak valid" }

            val cellKey = buildCellKey(clusterId, slotIndex, date)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val initialChecks = if (nextStatus == LampStatus.EMPTY) emptyMap() else mapOf(cellKey to nextStatus.code)
                docRef.set(
                    initialPeriodPayload(
                        workspace = workspace,
                        actorUid = uid,
                        actorName = actorName,
                        month = month,
                        year = year,
                        checks = initialChecks
                    )
                ).await()
            } else {
                val valueToWrite: Any = if (nextStatus == LampStatus.EMPTY) FieldValue.delete() else nextStatus.code
                docRef.update(
                    mapOf(
                        "checks.$cellKey" to valueToWrite,
                        "memberUids" to workspace.memberUids.ifEmpty { listOf(uid) }.take(50),
                        "updatedByUid" to uid,
                        "updatedByName" to actorName,
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
        workspace: TeamWorkspace,
        userId: String,
        month: String,
        year: Int,
        clusterId: Int,
        labels: List<String>
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val docRef = periodDocRef(workspace.id, periodId)
        return try {
            val uid = requireUserId(userId)
            val actorName = currentDisplayName()
            require(clusterId in 1..9) { "Cluster ID tidak valid" }
            val sanitized = (0 until 3).map { idx -> labels.getOrNull(idx)?.trim()?.take(60) ?: "" }
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                docRef.set(
                    initialPeriodPayload(
                        workspace = workspace,
                        actorUid = uid,
                        actorName = actorName,
                        month = month,
                        year = year,
                        slotLabels = mapOf(clusterId.toString() to sanitized)
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "slotLabels.$clusterId" to sanitized,
                        "memberUids" to workspace.memberUids.ifEmpty { listOf(uid) }.take(50),
                        "updatedByUid" to uid,
                        "updatedByName" to actorName,
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
        workspace: TeamWorkspace,
        userId: String,
        month: String,
        year: Int,
        notes: String
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val docRef = periodDocRef(workspace.id, periodId)
        return try {
            val uid = requireUserId(userId)
            val actorName = currentDisplayName()
            val safeNotes = notes.take(2000)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                docRef.set(
                    initialPeriodPayload(
                        workspace = workspace,
                        actorUid = uid,
                        actorName = actorName,
                        month = month,
                        year = year,
                        generalNotes = safeNotes
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "generalNotes" to safeNotes,
                        "memberUids" to workspace.memberUids.ifEmpty { listOf(uid) }.take(50),
                        "updatedByUid" to uid,
                        "updatedByName" to actorName,
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
        workspace: TeamWorkspace,
        userId: String,
        month: String,
        year: Int,
        picName: String
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val docRef = periodDocRef(workspace.id, periodId)
        return try {
            val uid = requireUserId(userId)
            val actorName = currentDisplayName()
            val safePic = picName.trim().take(150)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                docRef.set(
                    initialPeriodPayload(
                        workspace = workspace,
                        actorUid = uid,
                        actorName = actorName,
                        month = month,
                        year = year,
                        picName = safePic
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "picName" to safePic,
                        "memberUids" to workspace.memberUids.ifEmpty { listOf(uid) }.take(50),
                        "updatedByUid" to uid,
                        "updatedByName" to actorName,
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
        workspace: TeamWorkspace,
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
        val periodRef = periodDocRef(workspace.id, periodId)
        val issueRef = periodRef.collection("issues").document()
        return try {
            val uid = requireUserId(userId)
            val actorName = currentDisplayName()
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
                batch.set(
                    periodRef,
                    initialPeriodPayload(
                        workspace = workspace,
                        actorUid = uid,
                        actorName = actorName,
                        month = month,
                        year = year
                    )
                )
            }
            val issuePayload = mapOf(
                "workspaceId" to workspace.id,
                "periodId" to periodId,
                "memberUids" to workspace.memberUids.ifEmpty { listOf(uid) }.take(50),
                "createdByUid" to uid,
                "createdByName" to actorName,
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
        workspace: TeamWorkspace,
        userId: String,
        month: String,
        year: Int,
        issue: PjuIssue
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val issueRef = periodDocRef(workspace.id, periodId).collection("issues").document(issue.id)
        return try {
            requireUserId(userId)
            require(issue.id.isNotBlank()) { "ID laporan tidak boleh kosong" }
            val nextStatus = IssueStatus.fromLabel(issue.status).next()
            issueRef.update(
                mapOf(
                    "status" to nextStatus.label,
                    "memberUids" to workspace.memberUids.ifEmpty { listOf(userId) }.take(50),
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
        workspace: TeamWorkspace,
        userId: String,
        month: String,
        year: Int,
        issueId: String
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val issueRef = periodDocRef(workspace.id, periodId).collection("issues").document(issueId)
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
        workspace: TeamWorkspace,
        userId: String,
        month: String,
        year: Int
    ): Result<Unit> {
        val periodId = buildPeriodId(year, month)
        val periodRef = periodDocRef(workspace.id, periodId)
        return try {
            val uid = requireUserId(userId)
            val issuesSnapshot = periodRef.collection("issues")
                .whereArrayContains("memberUids", uid)
                .get()
                .await()
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
