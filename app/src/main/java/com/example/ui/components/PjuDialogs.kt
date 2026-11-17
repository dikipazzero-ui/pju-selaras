package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.IssueStatus
import com.example.model.PJU_CLUSTERS
import com.example.model.PjuCluster
import com.example.model.TeamWorkspace
import com.example.ui.IssueDialogPrefill
import com.example.ui.theme.Rose600
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIssueDialog(
    initialDay: Int,
    prefill: IssueDialogPrefill?,
    onDismiss: () -> Unit,
    onSubmit: (date: Int, cluster: PjuCluster, unit: String, desc: String, status: IssueStatus) -> Unit
) {
    var selectedDate by remember(prefill, initialDay) {
        mutableIntStateOf((prefill?.date ?: initialDay).coerceIn(1, 31))
    }
    var selectedCluster by remember(prefill) {
        mutableStateOf(prefill?.cluster ?: PJU_CLUSTERS.first())
    }
    var unitText by remember(prefill) {
        mutableStateOf(prefill?.unit ?: "")
    }
    var descText by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(IssueStatus.BELUM) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var dateExpanded by remember { mutableStateOf(false) }
    var clusterExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.ReportProblem,
                contentDescription = null,
                tint = Rose600
            )
        },
        title = {
            Text(
                text = "Laporan Permasalahan PJU",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = dateExpanded,
                        onExpandedChange = { dateExpanded = it },
                        modifier = Modifier.weight(0.42f)
                    ) {
                        OutlinedTextField(
                            value = "Tgl $selectedDate",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tanggal") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dateExpanded,
                            onDismissRequest = { dateExpanded = false }
                        ) {
                            (1..31).forEach { day ->
                                DropdownMenuItem(
                                    text = { Text("Tanggal $day") },
                                    onClick = {
                                        selectedDate = day
                                        dateExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = clusterExpanded,
                        onExpandedChange = { clusterExpanded = it },
                        modifier = Modifier.weight(0.58f)
                    ) {
                        OutlinedTextField(
                            value = "${selectedCluster.id}. ${selectedCluster.name}",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Cluster") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clusterExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = clusterExpanded,
                            onDismissRequest = { clusterExpanded = false }
                        ) {
                            PJU_CLUSTERS.forEach { cluster ->
                                DropdownMenuItem(
                                    text = { Text("${cluster.id}. ${cluster.name}") },
                                    onClick = {
                                        selectedCluster = cluster
                                        clusterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = unitText,
                    onValueChange = {
                        unitText = it.take(100)
                        validationError = null
                    },
                    label = { Text("Titik / Slot Lampu") },
                    placeholder = { Text("Misal: Titik 1 / Depan Blok A") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("issue_unit_input")
                )

                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedStatus.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status Perbaikan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        IssueStatus.entries.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.label) },
                                onClick = {
                                    selectedStatus = st
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = descText,
                    onValueChange = {
                        descText = it.take(500)
                        validationError = null
                    },
                    label = { Text("Deskripsi Permasalahan") },
                    placeholder = { Text("Contoh: Lampu mati total, MCB turun, atau kabel putus...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("issue_desc_input")
                )

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = Rose600,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val u = unitText.trim()
                    val d = descText.trim()
                    if (u.isBlank()) {
                        validationError = "Titik / Slot lampu wajib diisi."
                    } else if (d.isBlank()) {
                        validationError = "Deskripsi permasalahan wajib diisi."
                    } else {
                        onSubmit(selectedDate, selectedCluster, u, d, selectedStatus)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                modifier = Modifier.testTag("submit_issue_button")
            ) {
                Text("Simpan Laporan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun SlotLabelsDialog(
    cluster: PjuCluster,
    existingLabels: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var slot0 by remember(cluster.id) { mutableStateOf(existingLabels.getOrNull(0) ?: "") }
    var slot1 by remember(cluster.id) { mutableStateOf(existingLabels.getOrNull(1) ?: "") }
    var slot2 by remember(cluster.id) { mutableStateOf(existingLabels.getOrNull(2) ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Tag, contentDescription = null) },
        title = {
            Text(
                text = "Label Titik: ${cluster.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Beri keterangan nomor tiang/blok untuk 3 baris checklist pada cluster ini (opsional):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = slot0,
                    onValueChange = { slot0 = it.take(60) },
                    label = { Text("Baris 1") },
                    placeholder = { Text("Contoh: PJU 01 (Gerbang)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = slot1,
                    onValueChange = { slot1 = it.take(60) },
                    label = { Text("Baris 2") },
                    placeholder = { Text("Contoh: PJU 02 (Tengah)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = slot2,
                    onValueChange = { slot2 = it.take(60) },
                    label = { Text("Baris 3") },
                    placeholder = { Text("Contoh: PJU 03 (Ujung)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(listOf(slot0.trim(), slot1.trim(), slot2.trim())) },
                modifier = Modifier.testTag("save_slot_labels_button")
            ) {
                Text("Simpan Label")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ResetPeriodDialog(
    month: String,
    year: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = null,
                tint = Rose600
            )
        },
        title = {
            Text(
                text = "Reset Data $month $year?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Semua tanda checklist, label titik, catatan, dan laporan masalah pada bulan & tahun yang dipilih akan dihapus permanen.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                modifier = Modifier.testTag("confirm_reset_button")
            ) {
                Text("Ya, Hapus Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun TeamWorkspaceDialog(
    activeWorkspace: TeamWorkspace,
    allWorkspaces: List<TeamWorkspace>,
    profilesMap: Map<String, com.example.model.UserProfile> = emptyMap(),
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
                    text = "Bagikan kode ini ke petugas/pengurus lain agar bisa mengisi checklist dan live chat dalam satu tim secara real-time.",
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
                            androidx.compose.material3.OutlinedButton(
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

                androidx.compose.material3.HorizontalDivider()

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

                androidx.compose.material3.HorizontalDivider()

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
                    placeholder = { Text("Misal: Tim Keamanan BPS") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.OutlinedButton(
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var nameDraft by remember(initialDisplayName) { mutableStateOf(initialDisplayName) }
    var photoDraft by remember(initialPhotoBase64) { mutableStateOf(initialPhotoBase64) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingImage = true
            errorMsg = null
            scope.launch {
                val encoded = com.example.media.ProfilePhotoHelper.encodeUriToBase64(context, uri)
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
                text = "Profil & Foto Chat",
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
                    displayName = nameDraft.ifBlank { "Petugas" },
                    photoBase64 = photoDraft,
                    size = 96.dp
                )

                Text(
                    text = "Foto profil akan tampil di sebelah gelembung Live Chat dan daftar anggota tim agar Anda mudah dikenali.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
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
                    androidx.compose.material3.OutlinedButton(
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
                    label = { Text("Nama Tampilan di Chat") },
                    placeholder = { Text("Misal: Pak Diki (Keamanan)") },
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


