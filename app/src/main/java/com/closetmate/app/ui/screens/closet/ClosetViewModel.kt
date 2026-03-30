package com.closetmate.app.ui.screens.closet

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.closetmate.app.data.local.entity.ClothingEntity
import com.closetmate.app.data.repository.ClothingRepository
import com.closetmate.app.ui.screens.closet.copyUriToPrivateStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FilterState(
    val selectedCategories: Set<String> = emptySet(),
    val selectedSeasons: Set<String> = emptySet(),
    val selectedStatuses: Set<String> = emptySet(),
    val selectedStyles: Set<String> = emptySet()
) {
    val isActive: Boolean
        get() = selectedCategories.isNotEmpty() ||
                selectedSeasons.isNotEmpty() ||
                selectedStatuses.isNotEmpty() ||
                selectedStyles.isNotEmpty()
}

data class ClosetUiState(
    val clothingList: List<ClothingEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filterState: FilterState = FilterState(),
    val isSearchActive: Boolean = false,
    val gridColumns: Int = 2,
    val deletedItem: ClothingEntity? = null  // for undo
)

@OptIn(ExperimentalCoroutinesApi::class)
class ClosetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ClothingRepository(application)

    init {
        // 延迟迁移，等待权限对话框处理完毕（用户点击允许后再执行）
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(3000)
            migrateContentUris()
        }
    }

    /** 可从外部手动触发迁移（如权限授予后） */
    fun retryMigration() {
        viewModelScope.launch(Dispatchers.IO) {
            migrateContentUris()
        }
    }

    /**
     * 将数据库中旧的 content:// URI 复制到 App 私有目录，
     * 避免重装 App 后因权限失效导致图片无法显示。
     */
    private suspend fun migrateContentUris() {
        val context = getApplication<Application>()
        val allItems = repository.getAllOnce()
        for (item in allItems) {
            val uris = item.imageUris.split(",").filter { it.isNotBlank() }
            val hasContentUri = uris.any { it.startsWith("content://") }
            if (!hasContentUri) continue

            val migratedUris = uris.map { uriStr ->
                if (uriStr.startsWith("content://")) {
                    try {
                        val newUri = copyUriToPrivateStorage(context, Uri.parse(uriStr))
                        newUri.toString()
                    } catch (e: Exception) {
                        uriStr // 迁移失败保留原值
                    }
                } else {
                    uriStr
                }
            }
            val newImageUris = migratedUris.joinToString(",")
            if (newImageUris != item.imageUris) {
                repository.update(item.copy(imageUris = newImageUris))
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    private val _filterState = MutableStateFlow(FilterState())
    private val _isSearchActive = MutableStateFlow(false)
    private val _gridColumns = MutableStateFlow(2)
    private val _deletedItem = MutableStateFlow<ClothingEntity?>(null)

    val uiState: StateFlow<ClosetUiState> = combine(
        _searchQuery,
        _filterState,
        _isSearchActive,
        _gridColumns,
        _deletedItem
    ) { query, filter, isSearchActive, columns, deletedItem ->
        Triple(Pair(query, filter), Triple(isSearchActive, columns, deletedItem), Unit)
    }.flatMapLatest { (queryFilter, rest, _) ->
        val (query, filter) = queryFilter
        val (isSearchActive, columns, deletedItem) = rest

        val baseFlow: Flow<List<ClothingEntity>> = when {
            query.isNotBlank() -> repository.search(query)
            else -> repository.getAllClothing()
        }

        baseFlow.map { list ->
            val filtered = if (filter.isActive) {
                list.filter { item ->
                    val categoryMatch = filter.selectedCategories.isEmpty() ||
                            filter.selectedCategories.contains(item.category)
                    val seasonMatch = filter.selectedSeasons.isEmpty() ||
                            filter.selectedSeasons.any { season -> item.seasons.contains(season) }
                    val statusMatch = filter.selectedStatuses.isEmpty() ||
                            filter.selectedStatuses.contains(item.status)
                    val styleMatch = filter.selectedStyles.isEmpty() ||
                            filter.selectedStyles.any { style -> item.styles.contains(style) }
                    categoryMatch && seasonMatch && statusMatch && styleMatch
                }
            } else list

            ClosetUiState(
                clothingList = filtered,
                isLoading = false,
                searchQuery = query,
                filterState = filter,
                isSearchActive = isSearchActive,
                gridColumns = columns,
                deletedItem = deletedItem
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClosetUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun toggleGridColumns() {
        _gridColumns.value = if (_gridColumns.value == 2) 3 else 2
    }

    fun updateFilter(filter: FilterState) {
        _filterState.value = filter
    }

    fun clearFilter() {
        _filterState.value = FilterState()
    }

    fun deleteClothing(clothing: ClothingEntity) {
        viewModelScope.launch {
            repository.delete(clothing)
            _deletedItem.value = clothing
        }
    }

    fun undoDelete() {
        val item = _deletedItem.value ?: return
        viewModelScope.launch {
            repository.insert(item)
            _deletedItem.value = null
        }
    }

    fun clearDeletedItem() {
        _deletedItem.value = null
    }

    fun incrementWearCount(id: String) {
        viewModelScope.launch {
            repository.incrementWearCount(id)
        }
    }

    fun updateStatus(id: String, status: String) {
        viewModelScope.launch {
            repository.updateStatus(id, status)
        }
    }
}
