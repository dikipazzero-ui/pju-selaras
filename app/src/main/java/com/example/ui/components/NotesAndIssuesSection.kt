package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.model.IssueStatus
import com.example.model.PjuIssue
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Amber900
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Rose900

@Composable
fun IssuesAndNotesCard(
    month: String,
    issues: List<PjuIssue>,
    generalNotes: String,
    onNotesChanged: (String) -> Unit,
    onAddIssueClick: () -> Unit,
    onCycleIssueStatus: (PjuIssue) -> Unit,
    onDeleteIssue: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATATAN & LAPORAN PERMASALAHAN PJU :",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAddIssueClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                        modifier = Modifier.testTag("add_issue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lapor Masalah",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (issues.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircleOutline,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Belum ada laporan permasalahan PJU tercatat pada periode ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        issues.forEachIndexed { idx, issue ->
                            IssueItemRow(
                                index = idx + 1,
                                month = month,
                                issue = issue,
                                onCycleStatus = { onCycleIssueStatus(issue) },
                                onDelete = { onDeleteIssue(issue.id) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = generalNotes,
                    onValueChange = onNotesChanged,
                    label = { Text("Catatan Tambahan Petugas") },
                    placeholder = { Text("Tulis catatan tambahan hasil patroli PJU di sini...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("general_notes_input")
                )
            }
        }
    }
}

@Composable
private fun IssueItemRow(
    index: Int,
    month: String,
    issue: PjuIssue,
    onCycleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val statusEnum = IssueStatus.fromLabel(issue.status)
    val (badgeBg, badgeText, badgeBorder) = when (statusEnum) {
        IssueStatus.SELESAI -> Triple(Emerald100, Emerald900, Emerald600.copy(alpha = 0.4f))
        IssueStatus.PROSES -> Triple(Amber100, Amber900, Amber600.copy(alpha = 0.4f))
        IssueStatus.BELUM -> Triple(Rose100, Rose900, Rose600.copy(alpha = 0.4f))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Rose50.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, Rose600.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "$index. [Tgl ${issue.date} $month] ${issue.clusterName} — ${issue.unit}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = issue.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCycleStatus)
                        .testTag("issue_status_pill_${issue.id}")
                ) {
                    Text(
                        text = issue.status,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_issue_${issue.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Hapus laporan masalah",
                    tint = Rose600
                )
            }
        }
    }
}

@Composable
fun RulesAndPicCard(
    picName: String,
    isWideScreen: Boolean,
    onPicNameChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                RulesBlock(modifier = Modifier.weight(1.6f))
                PicBlock(
                    picName = picName,
                    onPicNameChanged = onPicNameChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RulesBlock(modifier = Modifier.fillMaxWidth())
                HorizontalDivider()
                PicBlock(
                    picName = picName,
                    onPicNameChanged = onPicNameChanged,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RulesBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "KETERANGAN :",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "1. WAJIB MELAKUKAN CHECKLIST PADA SEMUA PJU MAKSIMAL 2 HARI SEKALI",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "2. APABILA PADA SAAT PENGECEKAN TERDAPAT MASALAH, MAKA WAJIB MENGISI PERMASALAHAN PJU PADA FORM YANG TERSEDIA",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PicBlock(
    picName: String,
    onPicNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Penanggung Jawab",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedTextField(
            value = picName,
            onValueChange = onPicNameChanged,
            singleLine = true,
            label = { Text("Nama Penanggung Jawab") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pic_name_input")
        )
    }
}
