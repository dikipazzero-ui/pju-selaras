package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AVAILABLE_YEARS
import com.example.model.DateRangePreset
import com.example.model.INDONESIAN_MONTHS
import com.example.ui.ChecklistStats
import com.example.ui.PeriodSelection
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber600
import com.example.ui.theme.Amber900
import com.example.ui.theme.Blue100
import com.example.ui.theme.Blue50
import com.example.ui.theme.Blue600
import com.example.ui.theme.Blue900
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Rose900

@Composable
fun DashboardStatsSection(
    stats: ChecklistStats,
    isWideScreen: Boolean
) {
    val overdueCount = stats.overdueClusterIds.size
    val complianceValue = if (overdueCount == 0) "Semua Terpantau ✔" else "$overdueCount Cluster Perlu Cek"
    val complianceTint = if (overdueCount == 0) Emerald600 else Amber600
    val complianceBg = if (overdueCount == 0) Emerald50 else Amber50

    if (isWideScreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = "TOTAL SEL TERISI",
                value = stats.totalFilled.toString(),
                icon = Icons.Filled.AssignmentTurnedIn,
                accentColor = Blue600,
                badgeBg = Blue50,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "MENYALA / NORMAL (✔)",
                value = stats.normalCount.toString(),
                icon = Icons.Filled.CheckCircle,
                accentColor = Emerald600,
                badgeBg = Emerald50,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "BERMASALAH / REDUP",
                value = stats.issueCount.toString(),
                icon = Icons.Filled.Warning,
                accentColor = Rose600,
                badgeBg = Rose50,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "KEPATUHAN (MAX 2 HARI)",
                value = complianceValue,
                icon = Icons.Filled.Schedule,
                accentColor = complianceTint,
                badgeBg = complianceBg,
                isCompactValue = true,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "SEL TERISI",
                    value = stats.totalFilled.toString(),
                    icon = Icons.Filled.AssignmentTurnedIn,
                    accentColor = Blue600,
                    badgeBg = Blue50,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "MENYALA (✔)",
                    value = stats.normalCount.toString(),
                    icon = Icons.Filled.CheckCircle,
                    accentColor = Emerald600,
                    badgeBg = Emerald50,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "BERMASALAH",
                    value = stats.issueCount.toString(),
                    icon = Icons.Filled.Warning,
                    accentColor = Rose600,
                    badgeBg = Rose50,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "KEPATUHAN 2 HARI",
                    value = complianceValue,
                    icon = Icons.Filled.Schedule,
                    accentColor = complianceTint,
                    badgeBg = complianceBg,
                    isCompactValue = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    badgeBg: Color,
    isCompactValue: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = if (isCompactValue) {
                        MaterialTheme.typography.titleSmall
                    } else {
                        MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFamily)
                    },
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeBg,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormHeaderAndFiltersCard(
    selection: PeriodSelection,
    isWideScreen: Boolean,
    onSelectMonth: (String) -> Unit,
    onSelectYear: (Int) -> Unit,
    onSelectRange: (DateRangePreset) -> Unit,
    onSelectTodayDate: (Int) -> Unit
) {
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var rangeExpanded by remember { mutableStateOf(false) }
    var todayExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.residential_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.form_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)

            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonthDropdown(selection.month, monthExpanded, { monthExpanded = it }, onSelectMonth, Modifier.weight(1f))
                    YearDropdown(selection.year, yearExpanded, { yearExpanded = it }, onSelectYear, Modifier.weight(0.7f))
                    RangeDropdown(selection.rangePreset, rangeExpanded, { rangeExpanded = it }, onSelectRange, Modifier.weight(1.3f))
                    TodayDropdown(selection.todayDate, todayExpanded, { todayExpanded = it }, onSelectTodayDate, Modifier.weight(0.9f))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonthDropdown(selection.month, monthExpanded, { monthExpanded = it }, onSelectMonth, Modifier.weight(1.25f))
                    YearDropdown(selection.year, yearExpanded, { yearExpanded = it }, onSelectYear, Modifier.weight(0.75f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RangeDropdown(selection.rangePreset, rangeExpanded, { rangeExpanded = it }, onSelectRange, Modifier.weight(1.25f))
                    TodayDropdown(selection.todayDate, todayExpanded, { todayExpanded = it }, onSelectTodayDate, Modifier.weight(0.75f))
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Blue50,
                border = BorderStroke(1.dp, Blue100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = Blue600,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ketuk sel pada tabel untuk mengubah status lampu:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Blue900
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LegendChip("✔ Menyala", Emerald50, Emerald900, Emerald600.copy(alpha = 0.35f))
                        LegendChip("⚠ Redup/Kedip", Amber50, Amber900, Amber600.copy(alpha = 0.35f))
                        LegendChip("✖ Mati/Rusak", Rose50, Rose900, Rose600.copy(alpha = 0.35f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthDropdown(
    selectedMonth: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange, modifier = modifier) {
        OutlinedTextField(
            value = selectedMonth,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Bulan") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            INDONESIAN_MONTHS.forEach { month ->
                DropdownMenuItem(
                    text = { Text(month) },
                    onClick = {
                        onSelect(month)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearDropdown(
    selectedYear: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange, modifier = modifier) {
        OutlinedTextField(
            value = selectedYear.toString(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Tahun") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            AVAILABLE_YEARS.forEach { yr ->
                DropdownMenuItem(
                    text = { Text(yr.toString()) },
                    onClick = {
                        onSelect(yr)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeDropdown(
    selectedRange: DateRangePreset,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (DateRangePreset) -> Unit,
    modifier: Modifier
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange, modifier = modifier) {
        OutlinedTextField(
            value = selectedRange.shortLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Rentang Tgl") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DateRangePreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.label) },
                    onClick = {
                        onSelect(preset)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayDropdown(
    todayDate: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange, modifier = modifier) {
        OutlinedTextField(
            value = "Tgl $todayDate",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Cek Hari Ini") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            (1..31).forEach { day ->
                DropdownMenuItem(
                    text = { Text("Tanggal $day") },
                    onClick = {
                        onSelect(day)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun LegendChip(
    text: String,
    bgColor: Color,
    textColor: Color,
    borderColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
