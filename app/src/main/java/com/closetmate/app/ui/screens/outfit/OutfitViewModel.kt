package com.closetmate.app.ui.screens.outfit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.closetmate.app.data.local.entity.ClothingEntity
import com.closetmate.app.data.local.entity.OutfitEntity
import com.closetmate.app.data.repository.ClothingRepository
import com.closetmate.app.data.repository.OutfitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OutfitUiState(
    val outfitList: List<OutfitEntity> = emptyList(),
    val isLoading: Boolean = true,
    val selectedSceneFilter: String = "",   // "" = 全部
    val showFavoritesOnly: Boolean = false
)

class OutfitViewModel(application: Application) : AndroidViewModel(application) {

    private val outfitRepository = OutfitRepository(application)
    private val clothingRepository = ClothingRepository(application)

    private val _selectedScene = MutableStateFlow("")
    private val _showFavoritesOnly = MutableStateFlow(false)

    val uiState: StateFlow<OutfitUiState> = combine(
        _selectedScene,
        _showFavoritesOnly
    ) { scene, favOnly -> Pair(scene, favOnly) }
        .flatMapLatest { (scene, favOnly) ->
            val baseFlow = when {
                favOnly -> outfitRepository.getFavoriteOutfits()
                scene.isNotBlank() -> outfitRepository.getByScene(scene)
                else -> outfitRepository.getAllOutfits()
            }
            baseFlow.map { list ->
                OutfitUiState(
                    outfitList = list,
                    isLoading = false,
                    selectedSceneFilter = scene,
                    showFavoritesOnly = favOnly
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OutfitUiState(isLoading = true)
        )

    // 获取所有衣物（用于搭配选择器）
    val allClothing: StateFlow<List<ClothingEntity>> = clothingRepository.getAllClothing()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSceneFilter(scene: String) {
        _selectedScene.value = if (_selectedScene.value == scene) "" else scene
    }

    fun toggleFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(outfit: OutfitEntity) {
        viewModelScope.launch {
            outfitRepository.updateFavorite(outfit.id, !outfit.isFavorite)
        }
    }

    fun deleteOutfit(outfit: OutfitEntity) {
        viewModelScope.launch {
            outfitRepository.delete(outfit)
        }
    }

    fun wearOutfit(outfit: OutfitEntity) {
        viewModelScope.launch {
            // 更新搭配穿着次数
            outfitRepository.incrementWearCount(outfit.id)
            // 更新搭配中每件衣物的穿着次数
            val clothingIds = listOfNotNull(
                outfit.topId.takeIf { it.isNotBlank() },
                outfit.bottomId.takeIf { it.isNotBlank() },
                outfit.outerId.takeIf { it.isNotBlank() },
                outfit.shoesId.takeIf { it.isNotBlank() },
                outfit.bagId.takeIf { it.isNotBlank() }
            ) + outfit.accessoryIds.split(",").filter { it.isNotBlank() }

            clothingIds.forEach { id ->
                clothingRepository.incrementWearCount(id)
            }
        }
    }
}
