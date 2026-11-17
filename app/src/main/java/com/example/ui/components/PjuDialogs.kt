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
import com.example.ui.IssueDialogPrefill
import com.example.ui.theme.Rose600

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
