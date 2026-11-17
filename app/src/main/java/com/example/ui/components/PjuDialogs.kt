package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.media.ProfilePhotoHelper
import com.example.model.TeamWorkspace
import com.example.model.UserProfile
import com.example.ui.theme.Rose600
import kotlinx.coroutines.launch

@Composable
fun TeamWorkspaceDialog(
    activeWorkspace: TeamWorkspace,
    allWorkspaces: List<TeamWorkspace>,
    profilesMap: Map<String, UserProfile> = emptyMap(),
    onSelectWorkspace: (String) -> Unit,
    onJoinWorkspace: (code: String, onDone: (Boolean) -> Unit) -> Unit,
    onCreateWorkspace: (name: String, onDone: (Boolean) -> Unit) -> Unit,
    onCopyInviteCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var joinCodeInput by remember { mutableStateOf("") }
    var newTeamNameInput by remember { mutableStateOf("") }
    var actionError by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tim Online Bersama",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Kode Tim Aktif: ${activeWorkspace.inviteCode}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Bagikan kode ini ke anggota tim lain agar bisa mencatat laporan harian, melihat rangkuman bulanan, dan live chat dalam satu grup secara real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { onCopyInviteCode(activeWorkspace.inviteCode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salin Kode Tim (${activeWorkspace.inviteCode})")
                }

                if (activeWorkspace.memberUids.isNotEmpty()) {
                    Text(
                        text = "Anggota Terhubung (${activeWorkspace.memberUids.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeWorkspace.memberUids.forEachIndexed { idx, uid ->
                            val profile = profilesMap[uid]
                            val memberName = profile?.displayName?.ifBlank { null }
                                ?: activeWorkspace.memberNames[uid]
                                ?: "Anggota ${idx + 1}"
                            val photoBase64 = profile?.photoBase64 ?: ""
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MemberAvatar(
                                    displayName = memberName,
                                    photoBase64 = photoBase64,
                                    size = 32.dp
                                )
                                Text(
                                    text = memberName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (allWorkspaces.size > 1) {
                    Text(
                        text = "Pindah Tim Anda:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allWorkspaces.forEach { ws ->
                            val isSelected = ws.id == activeWorkspace.id
                            OutlinedButton(
                                onClick = {
                                    onSelectWorkspace(ws.id)
                                    onDismiss()
                                },
                                enabled = !isSelected,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isSelected) "${ws.name} (Aktif)" else "${ws.name} (${ws.inviteCode})"
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Gabung ke Tim Lain",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = joinCodeInput,
                    onValueChange = {
                        joinCodeInput = it.uppercase().take(16)
                        actionError = null
                    },
                    label = { Text("Masukkan Kode Tim") },
                    placeholder = { Text("Contoh: BPS7X2K") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_team_code_input")
                )
                Button(
                    onClick = {
                        val trimmed = joinCodeInput.trim()
                        if (trimmed.length < 4) {
                            actionError = "Masukkan kode tim minimal 4 huruf/angka."
                        } else {
                            isBusy = true
                            onJoinWorkspace(trimmed) { ok ->
                                isBusy = false
                                if (ok) onDismiss()
                            }
                        }
                    },
                    enabled = !isBusy && joinCodeInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_team_submit_button")
                ) {
                    Text("Gabung Tim Ini")
                }

                HorizontalDivider()

                Text(
                    text = "Atau Buat Tim Baru",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = newTeamNameInput,
                    onValueChange = {
                        newTeamNameInput = it.take(60)
                        actionError = null
                    },
                    label = { Text("Nama Tim Baru") },
                    placeholder = { Text("Misal: Tim Operasional BPS") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        val clean = newTeamNameInput.trim()
                        if (clean.length < 2) {
                            actionError = "Nama tim minimal 2 karakter."
                        } else {
                            isBusy = true
                            onCreateWorkspace(clean) { ok ->
                                isBusy = false
                                if (ok) onDismiss()
                            }
                        }
                    },
                    enabled = !isBusy && newTeamNameInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buat Tim Baru")
                }

                if (actionError != null) {
                    Text(
                        text = actionError ?: "",
                        color = Rose600,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ProfilePhotoDialog(
    initialDisplayName: String,
    initialPhotoBase64: String,
    onSave: (displayName: String, photoBase64: String, onDone: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nameDraft by remember(initialDisplayName) { mutableStateOf(initialDisplayName) }
    var photoDraft by remember(initialPhotoBase64) { mutableStateOf(initialPhotoBase64) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingImage = true
            errorMsg = null
            scope.launch {
                val encoded = ProfilePhotoHelper.encodeUriToBase64(context, uri)
                isProcessingImage = false
                encoded.onSuccess { b64 ->
                    photoDraft = b64
                }.onFailure { err ->
                    errorMsg = err.localizedMessage ?: "Gagal memproses foto."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Profil & Foto Anggota",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MemberAvatar(
                    displayName = nameDraft.ifBlank { "Anggota" },
                    photoBase64 = photoDraft,
                    size = 96.dp
                )

                Text(
                    text = "Foto profil dan nama ini akan muncul di setiap Laporan Harian serta gelembung Live Chat tim Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessingImage && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_profile_photo_button")
                ) {
                    Text(if (isProcessingImage) "Memproses foto..." else "Pilih Foto dari Galeri")
                }

                if (photoDraft.isNotBlank()) {
                    OutlinedButton(
                        onClick = { photoDraft = "" },
                        enabled = !isProcessingImage && !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hapus Foto Profil", color = Rose600)
                    }
                }

                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = {
                        nameDraft = it.take(80)
                        errorMsg = null
                    },
                    label = { Text("Nama Tampilan Anggota") },
                    placeholder = { Text("Misal: Pak Diki (Koordinator)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_display_name_input")
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = Rose600,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = nameDraft.trim()
                    if (clean.isEmpty()) {
                        errorMsg = "Nama tampilan tidak boleh kosong."
                    } else {
                        isSaving = true
                        onSave(clean, photoDraft) { ok ->
                            isSaving = false
                            if (ok) onDismiss()
                        }
                    }
                },
                enabled = !isProcessingImage && !isSaving,
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text(if (isSaving) "Menyimpan..." else "Simpan Profil")
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
