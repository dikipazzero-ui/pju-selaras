package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ChecklistPeriod
import com.example.model.DateRangePreset
import com.example.model.LampStatus
import com.example.model.PJU_CLUSTERS
import com.example.model.PjuCluster
import com.example.model.buildCellKey
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Amber900
import com.example.ui.theme.Blue100
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue900
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate800

private val HEADER_TIER1_HEIGHT = 28.dp
private val HEADER_TIER2_HEIGHT = 26.dp
private val HEADER_TIER3_HEIGHT = 36.dp
private val TOTAL_HEADER_HEIGHT = HEADER_TIER1_HEIGHT + HEADER_TIER2_HEIGHT + HEADER_TIER3_HEIGHT
private val SLOT_ROW_HEIGHT = 48.dp
private val CLUSTER_BLOCK_HEIGHT = SLOT_ROW_HEIGHT * 3
private val DATE_COL_WIDTH = 48.dp

private fun Modifier.gridCellLines(
    borderColor: Color,
    strokeWidthDp: Float = 0.75f
): Modifier = drawBehind {
    val stroke = strokeWidthDp * density
    drawLine(
        color = borderColor,
        start = Offset(size.width, 0f),
        end = Offset(size.width, size.height),
        strokeWidth = stroke
    )
    drawLine(
        color = borderColor,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = stroke
    )
}

@Composable
fun ChecklistMatrixTable(
    period: ChecklistPeriod,
    rangePreset: DateRangePreset,
    todayDate: Int,
    overdueClusterIds: Set<Int>,
    isWideScreen: Boolean,
    onCellClick: (cluster: PjuCluster, slotIndex: Int, date: Int) -> Unit,
    onEditSlotLabels: (cluster: PjuCluster) -> Unit
) {
    val dates = remember(rangePreset) { rangePreset.dates() }
    val horizontalScrollState = rememberScrollState()
    val noColWidth: Dp = 36.dp
    val clusterColWidth: Dp = if (isWideScreen) 210.dp else 156.dp
    val gridBorderColor = Slate800
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val todayEmptyTint = Blue50.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, gridBorderColor),
        color = surfaceColor.copy(alpha = 0.93f)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Sticky Left Columns: NO. + CLUSTER
            Column(
                modifier = Modifier.width(noColWidth + clusterColWidth)
            ) {
                Row(
                    modifier = Modifier
                        .height(TOTAL_HEADER_HEIGHT)
                        .fillMaxWidth()
                        .background(surfaceVariantColor)
                ) {
                    Box(
                        modifier = Modifier
                            .width(noColWidth)
                            .fillMaxHeight()
                            .gridCellLines(gridBorderColor, 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(clusterColWidth)
                            .fillMaxHeight()
                            .gridCellLines(gridBorderColor, 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CLUSTER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                PJU_CLUSTERS.forEach { cluster ->
                    val labels = period.slotLabels[cluster.id.toString()].orEmpty()
                    val preview = labels.filter { it.isNotBlank() }.joinToString(", ")
                    val needsCheck = cluster.id in overdueClusterIds

                    Row(
                        modifier = Modifier
                            .height(CLUSTER_BLOCK_HEIGHT)
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(noColWidth)
                                .fillMaxHeight()
                                .gridCellLines(gridBorderColor, 1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cluster.id.toString(),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(clusterColWidth)
                                .fillMaxHeight()
                                .gridCellLines(gridBorderColor, 1f)
                                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 36.dp)
                                    .align(Alignment.CenterStart),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = cluster.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (preview.isNotBlank()) {
                                    Text(
                                        text = preview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (needsCheck) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Amber100
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ErrorOutline,
                                                contentDescription = null,
                                                tint = Amber900,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Perlu Cek",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Amber900
                                            )
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onEditSlotLabels(cluster) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(48.dp)
                                    .testTag("edit_slots_cluster_${cluster.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NewLabel,
                                    contentDescription = "Atur label titik PJU untuk ${cluster.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 1.5.dp, color = gridBorderColor)
                }
            }

            // Scrollable Right Columns: KONDISI LAMPU -> TANGGAL -> Date Matrix
            val dateMatrixWidth = DATE_COL_WIDTH * dates.size
            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column(modifier = Modifier.width(dateMatrixWidth)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HEADER_TIER1_HEIGHT)
                            .background(Slate200.copy(alpha = 0.7f))
                            .gridCellLines(gridBorderColor, 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "KONDISI LAMPU",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate800
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HEADER_TIER2_HEIGHT)
                            .background(surfaceVariantColor)
                            .gridCellLines(gridBorderColor, 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TANGGAL",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HEADER_TIER3_HEIGHT)
                    ) {
                        dates.forEach { day ->
                            val isToday = day == todayDate
                            Box(
                                modifier = Modifier
                                    .width(DATE_COL_WIDTH)
                                    .fillMaxHeight()
                                    .background(if (isToday) Blue100 else surfaceColor)
                                    .gridCellLines(gridBorderColor, 1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        textDecoration = if (isToday) TextDecoration.Underline else TextDecoration.None
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) Blue900 else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    PJU_CLUSTERS.forEach { cluster ->
                        val labels = period.slotLabels[cluster.id.toString()].orEmpty()
                        Column(modifier = Modifier.fillMaxWidth()) {
                            for (slot in 0 until 3) {
                                val slotTitle = labels.getOrNull(slot)?.takeIf { it.isNotBlank() }
                                    ?: "Baris ${slot + 1}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(SLOT_ROW_HEIGHT)
                                ) {
                                    dates.forEach { day ->
                                        val code = period.checks[buildCellKey(cluster.id, slot, day)]
                                        val status = LampStatus.fromCode(code)
                                        val isToday = day == todayDate
                                        val cellBg = when (status) {
                                            LampStatus.NORMAL -> Emerald50
                                            LampStatus.WARN -> Amber50
                                            LampStatus.ERROR -> Rose50
                                            LampStatus.EMPTY -> if (isToday) todayEmptyTint else surfaceColor
                                        }
                                        val cellTextColor = when (status) {
                                            LampStatus.NORMAL -> Emerald600
                                            LampStatus.WARN -> Amber600
                                            LampStatus.ERROR -> Rose600
                                            LampStatus.EMPTY -> Color.Transparent
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(DATE_COL_WIDTH)
                                                .fillMaxHeight()
                                                .background(cellBg)
                                                .gridCellLines(Slate300, 0.75f)
                                                .clickable { onCellClick(cluster, slot, day) }
                                                .testTag("cell_${cluster.id}_${slot}_${day}")
                                                .semantics {
                                                    contentDescription = "${cluster.name} $slotTitle tanggal $day status ${status.labelId}"
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (status != LampStatus.EMPTY) {
                                                Text(
                                                    text = status.symbol,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = cellTextColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(thickness = 1.5.dp, color = gridBorderColor)
                    }
                }
            }
        }
    }
}
