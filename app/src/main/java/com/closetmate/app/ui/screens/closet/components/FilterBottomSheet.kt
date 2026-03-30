package com.closetmate.app.ui.screens.closet.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.closetmate.app.data.local.entity.ClothingCategory
import com.closetmate.app.data.local.entity.ClothingSeason
import com.closetmate.app.data.local.entity.ClothingStatus
import com.closetmate.app.data.local.entity.ClothingStyle
import com.closetmate.app.ui.screens.closet.FilterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: FilterState,
    onFilterChange: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var tempFilter by remember { mutableStateOf(currentFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "筛选",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 品类筛选
            FilterSection(
                title = "品类",
                options = ClothingCategory.all,
                selected = tempFilter.selectedCategories,
                onSelectionChange = { tempFilter = tempFilter.copy(selectedCategories = it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 季节筛选
            FilterSection(
                title = "季节",
                options = ClothingSeason.all,
                selected = tempFilter.selectedSeasons,
                onSelectionChange = { tempFilter = tempFilter.copy(selectedSeasons = it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 风格筛选
            FilterSection(
                title = "风格",
                options = ClothingStyle.all,
                selected = tempFilter.selectedStyles,
                onSelectionChange = { tempFilter = tempFilter.copy(selectedStyles = it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 状态筛选
            FilterSection(
                title = "状态",
                options = ClothingStatus.all,
                selected = tempFilter.selectedStatuses,
                onSelectionChange = { tempFilter = tempFilter.copy(selectedStatuses = it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        tempFilter = FilterState()
                        onFilterChange(FilterState())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("重置")
                }
                Button(
                    onClick = {
                        onFilterChange(tempFilter)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    val count = tempFilter.selectedCategories.size +
                            tempFilter.selectedSeasons.size +
                            tempFilter.selectedStyles.size +
                            tempFilter.selectedStatuses.size
                    Text(if (count > 0) "应用筛选（$count）" else "应用筛选")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onSelectionChange: (Set<String>) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 10.dp)
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = selected.contains(option)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newSet = if (isSelected) selected - option else selected + option
                    onSelectionChange(newSet)
                },
                label = { Text(option, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
