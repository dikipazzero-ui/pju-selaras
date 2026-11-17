package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.media.ChatPhotoHelper
import com.example.media.ProfilePhotoHelper
import com.example.media.VoiceNotePlayer
import com.example.media.VoiceNoteRecorder
import com.example.media.formatDurationSeconds
import com.example.model.CHAT_TOPICS
import com.example.model.ChatMessage
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
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val AVATAR_PALETTE = listOf(
    Color(0xFF0284C7),
    Color(0xFF059669),
    Color(0xFFD97706),
    Color(0xFF7C3AED),
    Color(0xFFDC2626),
    Color(0xFF0F766E)
)

@Composable
fun MemberAvatar(
    displayName: String,
    photoBase64: String,
    size: Dp = 36.dp,
    borderColor: Color = Amber500.copy(alpha = 0.7f),
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(photoBase64) {
        ProfilePhotoHelper.decodeBase64ToBitmap(photoBase64)?.asImageBitmap()
    }
    val initials = remember(displayName) {
        displayName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "PJ" }
    }
    val bgColor = remember(displayName) {
        AVATAR_PALETTE[abs(displayName.hashCode()) % AVATAR_PALETTE.size]
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Foto profil $displayName",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TeamBannerCard(
    workspace: TeamWorkspace,
    workspacesCount: Int,
    onOpenTeamDialog: () -> Unit,
    onCopyMessage: (String) -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald100,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = Emerald900,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workspace.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Online Bersama • ${workspace.memberUids.size} Anggota" +
                                    if (workspacesCount > 1) " ($workspacesCount tim)" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onOpenTeamDialog,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("manage_team_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tim / Gabung",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Blue100,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(workspace.inviteCode))
                            onCopyMessage("Kode tim ${workspace.inviteCode} disalin")
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kode Tim: ${workspace.inviteCode}",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.ExtraBold,
                            color = Blue900
                        )
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Salin kode tim",
                            tint = Blue900,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Gabung ke tim LAPORAN HARIAN BPS (${workspace.name})!\nGunakan Kode Tim: ${workspace.inviteCode}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Kode Tim LAPORAN HARIAN BPS"))
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("share_team_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Bagikan kode tim",
                        tint = Blue600
                    )
                }
            }
        }
    }
}

@Composable
fun LiveChatTabContent(
    workspace: TeamWorkspace,
    messagesState: UiState<List<ChatMessage>>,
    profilesMap: Map<String, UserProfile>,
    myProfile: UserProfile?,
    currentUserId: String,
    isSending: Boolean,
    onSendMessage: (text: String, tagCluster: String, onDone: () -> Unit) -> Unit,
    onSendVoiceMessage: (audioBase64: String, durationSec: Int, tagCluster: String, onDone: () -> Unit) -> Unit,
    onSendImageMessage: (imageBase64: String, caption: String, tagCluster: String, onDone: () -> Unit) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onOpenTeamDialog: () -> Unit,
    onOpenProfileDialog: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var messageDraft by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var pendingImageBase64 by remember { mutableStateOf<String?>(null) }
    var isEncodingImage by remember { mutableStateOf(false) }
    var showPhotoSourceMenu by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var viewingImageMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val recorder = remember { VoiceNoteRecorder() }
    val voicePlayer = remember(context) { VoiceNotePlayer(context.applicationContext) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var isEncodingVoice by remember { mutableStateOf(false) }

    fun encodePickedChatPhoto(uri: Uri) {
        isEncodingImage = true
        scope.launch {
            val res = ChatPhotoHelper.encodeUriToBase64(context, uri)
            isEncodingImage = false
            res.onSuccess { b64 ->
                pendingImageBase64 = b64
            }.onFailure { err ->
                onShowMessage(err.localizedMessage ?: "Gagal memproses foto laporan")
            }
        }
    }

    val galleryPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) encodePickedChatPhoto(uri)
    }

    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) {
            encodePickedChatPhoto(uri)
        }
    }

    fun beginRecording() {
        voicePlayer.stopPlayback()
        recorder.start(context).onSuccess {
            recordingSeconds = 0
            isRecording = true
        }.onFailure { err ->
            isRecording = false
            onShowMessage(err.localizedMessage ?: "Gagal memulai perekam suara")
        }
    }

    fun finishAndSendRecording() {
        if (!isRecording || isEncodingVoice) return
        isRecording = false
        isEncodingVoice = true
        scope.launch {
            val encodedResult = withContext(Dispatchers.IO) {
                recorder.stopAndEncode()
            }
            isEncodingVoice = false
            encodedResult.onSuccess { (audioBase64, durationSec) ->
                onSendVoiceMessage(audioBase64, durationSec, selectedTag) {}
            }.onFailure { err ->
                onShowMessage(err.localizedMessage ?: "Gagal memproses voice note")
            }
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginRecording()
        } else {
            onShowMessage("Izin mikrofon diperlukan untuk mengirim voice note.")
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000L)
                recordingSeconds += 1
                if (recordingSeconds >= 60) {
                    finishAndSendRecording()
                    break
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            voicePlayer.stopPlayback()
        }
    }

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val myDisplayName = myProfile?.displayName?.ifBlank { null }
        ?: workspace.memberNames[currentUserId]
        ?: "Anggota Tim"
    val myPhotoBase64 = myProfile?.photoBase64 ?: ""

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize()
        ) {
            // Compact chat header card with quick profile photo button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenProfileDialog)
                            .testTag("chat_header_profile_avatar")
                    ) {
                        MemberAvatar(
                            displayName = myDisplayName,
                            photoBase64 = myPhotoBase64,
                            size = 40.dp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Emerald600)
                            )
                            Text(
                                text = workspace.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${workspace.memberUids.size} anggota • Ketuk foto untuk atur profil",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenProfileDialog,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_profile_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Foto", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onOpenTeamDialog,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tim", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Messages area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (messagesState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
                                    text = messagesState.message,
                                    color = Rose600,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    is UiState.Success -> {
                        if (messages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(22.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Forum,
                                            contentDescription = null,
                                            tint = Amber500,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text(
                                            text = "Belum Ada Pesan Koordinasi",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Kirim pesan teks atau tekan tombol Mikrofon untuk kirim Voice Note ke anggota tim.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(messages, key = { it.id.ifBlank { "${it.senderId}_${it.text.hashCode()}" } }) { msg ->
                                    val msgKey = msg.id.ifBlank { msg.audioBase64.hashCode().toString() }
                                    val isCurrentPlayer = voicePlayer.playingMessageId == msgKey
                                    ChatMessageBubble(
                                        message = msg,
                                        senderProfile = profilesMap[msg.senderId]
                                            ?: if (msg.senderId == currentUserId) myProfile else null,
                                        isMine = msg.senderId == currentUserId,
                                        isAudioPlaying = isCurrentPlayer && voicePlayer.isPlaying,
                                        audioProgress = if (isCurrentPlayer) voicePlayer.progressFraction else 0f,
                                        audioCurrentSec = if (isCurrentPlayer) voicePlayer.currentPositionSec else 0,
                                        onToggleAudio = {
                                            voicePlayer.togglePlay(msg, scope, onShowMessage)
                                        },
                                        onOpenImage = {
                                            viewingImageMessage = msg
                                        },
                                        onDelete = { onDeleteMessage(msg.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Cluster tag selector + Message / Photo / Voice Note Composer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
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
                        FilterChip(
                            selected = selectedTag.isEmpty(),
                            onClick = { selectedTag = "" },
                            label = { Text("Umum", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate900,
                                selectedLabelColor = Color.White
                            )
                        )
                        CHAT_TOPICS.forEach { topic ->
                            val selected = selectedTag == topic
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedTag = if (selected) "" else topic
                                },
                                label = { Text(topic, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Amber500,
                                    selectedLabelColor = Slate950
                                )
                            )
                        }
                    }

                    if (isEncodingImage) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Mengompresi foto agar ringan dikirim...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    pendingImageBase64?.let { previewB64 ->
                        PendingPhotoPreviewStrip(
                            imageBase64 = previewB64,
                            onClear = { pendingImageBase64 = null }
                        )
                    }

                    if (isRecording) {
                        RecordingVoiceBar(
                            seconds = recordingSeconds,
                            onCancel = {
                                isRecording = false
                                recordingSeconds = 0
                                recorder.cancel()
                            },
                            onSend = { finishAndSendRecording() }
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box {
                                IconButton(
                                    onClick = { showPhotoSourceMenu = true },
                                    enabled = !isSending && !isEncodingVoice && !isEncodingImage,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("attach_chat_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddPhotoAlternate,
                                        contentDescription = "Kirim foto di chat",
                                        tint = Blue600
                                    )
                                }
                                DropdownMenu(
                                    expanded = showPhotoSourceMenu,
                                    onDismissRequest = { showPhotoSourceMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Ambil Foto Kamera") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.PhotoCamera,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showPhotoSourceMenu = false
                                            runCatching {
                                                ChatPhotoHelper.createTempCameraUri(context)
                                            }.onSuccess { uri ->
                                                pendingCameraUri = uri
                                                cameraPhotoLauncher.launch(uri)
                                            }.onFailure { err ->
                                                onShowMessage(err.localizedMessage ?: "Gagal membuka kamera")
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Pilih dari Galeri") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.PhotoLibrary,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showPhotoSourceMenu = false
                                            galleryPhotoLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = messageDraft,
                                onValueChange = { messageDraft = it.take(1000) },
                                placeholder = {
                                    Text(
                                        if (pendingImageBase64 != null) "Tulis keterangan foto (opsional)..."
                                        else "Tulis pesan, foto, atau suara..."
                                    )
                                },
                                maxLines = 3,
                                enabled = !isEncodingVoice && !isEncodingImage,
                                shape = RoundedCornerShape(22.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input")
                            )

                            val hasPendingSend = pendingImageBase64 != null || messageDraft.isNotBlank()
                            if (hasPendingSend) {
                                FilledIconButton(
                                    onClick = {
                                        val imgB64 = pendingImageBase64
                                        val text = messageDraft.trim()
                                        if (!isSending && !isEncodingImage) {
                                            if (imgB64 != null) {
                                                onSendImageMessage(imgB64, text, selectedTag) {
                                                    pendingImageBase64 = null
                                                    messageDraft = ""
                                                }
                                            } else if (text.isNotEmpty()) {
                                                onSendMessage(text, selectedTag) {
                                                    messageDraft = ""
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSending && !isEncodingVoice && !isEncodingImage,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Amber500,
                                        contentColor = Slate950
                                    ),
                                    modifier = Modifier
                                        .size(50.dp)
                                        .testTag("send_chat_button")
                                ) {
                                    if (isSending) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Slate950
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Kirim pesan"
                                        )
                                    }
                                }
                            } else {
                                FilledIconButton(
                                    onClick = {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            beginRecording()
                                        } else {
                                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    enabled = !isSending && !isEncodingVoice && !isEncodingImage,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Amber500,
                                        contentColor = Slate950
                                    ),
                                    modifier = Modifier
                                        .size(50.dp)
                                        .testTag("record_voice_button")
                                ) {
                                    if (isSending || isEncodingVoice) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Slate950
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Mic,
                                            contentDescription = "Rekam voice note"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    viewingImageMessage?.let { imgMsg ->
        ImageViewerDialog(
            message = imgMsg,
            senderName = profilesMap[imgMsg.senderId]?.displayName?.ifBlank { null } ?: imgMsg.senderName,
            onDismiss = { viewingImageMessage = null }
        )
    }
}

@Composable
private fun PendingPhotoPreviewStrip(
    imageBase64: String,
    onClear: () -> Unit
) {
    val bmp = remember(imageBase64) {
        ProfilePhotoHelper.decodeBase64ToBitmap(imageBase64)?.asImageBitmap()
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Blue100.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, Blue600.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = "Pratinjau foto laporan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Foto laporan siap dikirim",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Blue900
                )
                Text(
                    text = "Tambahkan keterangan jika perlu lalu ketuk Kirim.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Blue900.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Hapus lampiran foto",
                    tint = Rose600
                )
            }
        }
    }
}

@Composable
private fun RecordingVoiceBar(
    seconds: Int,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Rose600.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Rose600.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("cancel_voice_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Batalkan rekaman suara",
                    tint = Rose600
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(Rose600)
                )
                Text(
                    text = "Merekam ${formatDurationSeconds(seconds)} / 1:00",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                    fontWeight = FontWeight.Bold,
                    color = Rose600
                )
            }

            FilledIconButton(
                onClick = onSend,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Emerald600,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_voice_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim voice note"
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    senderProfile: UserProfile?,
    isMine: Boolean,
    isAudioPlaying: Boolean,
    audioProgress: Float,
    audioCurrentSec: Int,
    onToggleAudio: () -> Unit,
    onOpenImage: () -> Unit,
    onDelete: () -> Unit
) {
    val timeText = remember(message.createdAt) {
        val date = message.createdAt?.toDate() ?: Date()
        SimpleDateFormat("EEE, dd MMM • HH:mm", Locale("id", "ID")).format(date)
    }

    val effectiveSenderName = senderProfile?.displayName?.ifBlank { null } ?: message.senderName
    val avatarPhoto = senderProfile?.photoBase64 ?: ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            MemberAvatar(
                displayName = effectiveSenderName,
                photoBase64 = avatarPhoto,
                size = 34.dp,
                borderColor = Blue600.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier.widthIn(min = 170.dp, max = 285.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            color = if (isMine) {
                Slate900.copy(alpha = 0.94f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (isMine) Amber500.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isMine) "Anda ($effectiveSenderName)" else effectiveSenderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMine) Amber500 else Blue600,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isMine && message.id.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Hapus pesan",
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(onClick = onDelete)
                        )
                    }
                }

                if (message.tagCluster.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Amber100
                    ) {
                        Text(
                            text = message.tagCluster,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Amber900,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                when {
                    message.isImageMessage -> {
                        val photoBitmap = remember(message.imageBase64) {
                            ProfilePhotoHelper.decodeBase64ToBitmap(message.imageBase64)?.asImageBitmap()
                        }
                        if (photoBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onOpenImage)
                            ) {
                                Image(
                                    bitmap = photoBitmap,
                                    contentDescription = "Foto laporan dari $effectiveSenderName",
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
                        if (message.text.isNotBlank() && message.text != "Foto laporan PJU") {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    message.isVoiceNote -> {
                        VoiceNoteBubblePlayer(
                            totalDurationSec = message.audioDurationSec.toInt().coerceAtLeast(1),
                            currentSec = audioCurrentSec,
                            progress = audioProgress,
                            isPlaying = isAudioPlaying,
                            isMine = isMine,
                            onTogglePlay = onToggleAudio
                        )
                    }

                    else -> {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        if (isMine) {
            Spacer(modifier = Modifier.width(8.dp))
            MemberAvatar(
                displayName = effectiveSenderName,
                photoBase64 = avatarPhoto,
                size = 34.dp,
                borderColor = Amber500
            )
        }
    }
}

@Composable
private fun ImageViewerDialog(
    message: ChatMessage,
    senderName: String,
    onDismiss: () -> Unit
) {
    val bmp = remember(message.imageBase64) {
        ProfilePhotoHelper.decodeBase64ToBitmap(message.imageBase64)?.asImageBitmap()
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
                            text = senderName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (message.tagCluster.isNotBlank()) {
                            Text(
                                text = message.tagCluster,
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber500
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup pratinjau foto",
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
                            contentDescription = "Foto lampiran ukuran penuh",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (message.text.isNotBlank() && message.text != "Foto laporan PJU") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Slate900.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceNoteBubblePlayer(
    totalDurationSec: Int,
    currentSec: Int,
    progress: Float,
    isPlaying: Boolean,
    isMine: Boolean,
    onTogglePlay: () -> Unit
) {
    val accent = if (isMine) Amber500 else Blue600
    val contentOnAccent = if (isMine) Slate950 else Color.White
    val primaryTextColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isMine) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            onClick = onTogglePlay,
            shape = CircleShape,
            color = accent,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(44.dp)
                .testTag("voice_note_play_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Jeda voice note" else "Putar voice note",
                    tint = contentOnAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Voice Note",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )
                }
                Text(
                    text = if (isPlaying) {
                        "${formatDurationSeconds(currentSec)} / ${formatDurationSeconds(totalDurationSec)}"
                    } else {
                        formatDurationSeconds(totalDurationSec)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = secondaryTextColor
                )
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = if (isMine) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
