package com.closetmate.app.ui.screens.closet

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.closetmate.app.ui.screens.closet.components.ClothingCard
import com.closetmate.app.ui.screens.closet.components.FilterBottomSheet
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen(
    onAddClothing: () -> Unit = {},
    onClothingClick: (String) -> Unit = {},
    viewModel: ClosetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    // 删除撤销提示
    LaunchedEffect(uiState.deletedItem) {
        if (uiState.deletedItem != null) {
            val result = snackbarHostState.showSnackbar(
                message = "已删除",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else {
                viewModel.clearDeletedItem()
            }
        }
    }

    // 搜索框自动聚焦
    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSearchActive) {
                // 搜索模式 TopBar
                TopAppBar(
                    title = {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = { Text("搜索品牌、备注、位置...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.setSearchActive(false) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "退出搜索")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                // 普通模式 TopBar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "我的衣橱",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!uiState.isLoading && uiState.clothingList.isNotEmpty()) {
                                Text(
                                    text = "${uiState.clothingList.size} 件",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setSearchActive(true) }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        // 筛选按钮（有筛选时显示角标）
                        BadgedBox(
                            badge = {
                                if (uiState.filterState.isActive) {
                                    Badge()
                                }
                            }
                        ) {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "筛选")
                            }
                        }
                        // 网格列数切换
                        IconButton(onClick = viewModel::toggleGridColumns) {
                            Icon(
                                imageVector = if (uiState.gridColumns == 2)
                                    Icons.Outlined.GridView
                                else
                                    Icons.Default.ViewModule,
                                contentDescription = "切换视图"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClothing,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加衣物",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.clothingList.isEmpty() -> {
                    EmptyClosetContent(
                        isFiltered = uiState.filterState.isActive || uiState.searchQuery.isNotBlank(),
                        onAddClothing = onAddClothing,
                        onClearFilter = {
                            viewModel.clearFilter()
                            viewModel.setSearchActive(false)
                        }
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(uiState.gridColumns),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.clothingList,
                            key = { it.id }
                        ) { clothing ->
                            ClothingCard(
                                clothing = clothing,
                                onClick = { onClothingClick(clothing.id) }
                            )
                        }
                        // 底部留白，避免被 FAB 遮挡
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // 筛选底部弹窗
    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = uiState.filterState,
            onFilterChange = viewModel::updateFilter,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun EmptyClosetContent(
    isFiltered: Boolean,
    onAddClothing: () -> Unit,
    onClearFilter: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isFiltered) {
            Text(text = "🔍", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "没有找到匹配的衣物",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "试试调整筛选条件或搜索关键词",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onClearFilter) {
                Text("清除筛选")
            }
        } else {
            Text(
                text = "👗",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize * 2
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "衣橱还是空的",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角 + 开始添加你的第一件衣物",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddClothing) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加衣物")
            }
        }
    }
}
