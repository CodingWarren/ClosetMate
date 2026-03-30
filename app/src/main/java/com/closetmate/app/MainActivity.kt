package com.closetmate.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.closetmate.app.security.AppLockManager
import com.closetmate.app.ui.navigation.bottomNavItems
import com.closetmate.app.ui.screens.closet.AddClothingScreen
import com.closetmate.app.ui.screens.closet.ClothingDetailScreen
import com.closetmate.app.ui.screens.closet.ClosetScreen
import com.closetmate.app.ui.screens.lock.LockScreen
import com.closetmate.app.ui.screens.outfit.CreateOutfitScreen
import com.closetmate.app.ui.screens.outfit.OutfitScreen
import com.closetmate.app.ui.screens.settings.SettingsScreen
import com.closetmate.app.ui.screens.stats.StatsScreen
import com.closetmate.app.ui.theme.ClosetMateTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// 路由常量
object Routes {
    const val CLOSET = "closet"
    const val OUTFIT = "outfit"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ADD_CLOTHING = "add_clothing"
    const val EDIT_CLOTHING = "edit_clothing/{clothingId}"
    const val CLOTHING_DETAIL = "clothing_detail/{clothingId}"
    const val CREATE_OUTFIT = "create_outfit"
    const val EDIT_OUTFIT = "edit_outfit/{outfitId}"

    fun editClothing(id: String) = "edit_clothing/$id"
    fun clothingDetail(id: String) = "clothing_detail/$id"
    fun editOutfit(id: String) = "edit_outfit/$id"
}

class MainActivity : AppCompatActivity() {

    /** 是否从后台返回（用于触发锁定） */
    private var wasInBackground = false

    private val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 权限结果无需处理 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求媒体读取权限
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        mediaPermissionLauncher.launch(permissions)

        // 首次启动：如果应用锁已启用，立即锁定
        lifecycleScope.launch {
            AppLockManager.lockIfEnabled(this@MainActivity)
        }

        setContent {
            ClosetMateTheme {
                val isLocked by AppLockManager.isLocked
                val biometricEnabled = remember { mutableStateOf(false) }

                // 收集生物识别设置
                LaunchedEffect(Unit) {
                    AppLockManager.isBiometricEnabled(this@MainActivity).collectLatest {
                        biometricEnabled.value = it
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    ClosetMateApp()

                    // 锁定覆盖层
                    if (isLocked) {
                        LockScreen(
                            showBiometric = biometricEnabled.value && isBiometricAvailable(),
                            onBiometricRequest = { showBiometricPrompt() }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        wasInBackground = true
    }

    override fun onStart() {
        super.onStart()
        // 从后台返回时，如果应用锁已启用则重新锁定
        if (wasInBackground) {
            wasInBackground = false
            lifecycleScope.launch {
                AppLockManager.lockIfEnabled(this@MainActivity)
            }
        }
    }

    // ── 生物识别 ──────────────────────────────────────────────────────────────

    /** 检查设备是否支持生物识别 */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /** 显示生物识别验证弹窗 */
    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AppLockManager.unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 用户取消或设备错误，保持锁定状态
                }

                override fun onAuthenticationFailed() {
                    // 生物识别不匹配，保持锁定状态
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁 ClosetMate")
            .setSubtitle("使用生物识别解锁应用")
            .setNegativeButtonText("使用 PIN 码")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

// 判断当前路由是否应该显示底部导航栏
private fun shouldShowBottomBar(route: String?): Boolean {
    return route in listOf(
        Routes.CLOSET,
        Routes.OUTFIT,
        Routes.STATS,
        Routes.SETTINGS
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetMateApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar(currentRoute)) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = androidx.compose.ui.unit.Dp(0f)
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CLOSET,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── 主 Tab 页面 ──────────────────────────────────────────────────
            composable(Routes.CLOSET) {
                ClosetScreen(
                    onAddClothing = { navController.navigate(Routes.ADD_CLOTHING) },
                    onClothingClick = { clothingId ->
                        navController.navigate(Routes.clothingDetail(clothingId))
                    }
                )
            }

            composable(Routes.OUTFIT) {
                OutfitScreen(
                    onCreateOutfit = { navController.navigate(Routes.CREATE_OUTFIT) },
                    onOutfitClick = { outfitId ->
                        navController.navigate(Routes.editOutfit(outfitId))
                    }
                )
            }

            composable(Routes.STATS) { StatsScreen() }

            composable(Routes.SETTINGS) { SettingsScreen() }

            // ── 搭配页面 ─────────────────────────────────────────────────────
            composable(Routes.CREATE_OUTFIT) {
                CreateOutfitScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.EDIT_OUTFIT,
                arguments = listOf(navArgument("outfitId") { type = NavType.StringType })
            ) { backStackEntry ->
                val outfitId = backStackEntry.arguments?.getString("outfitId") ?: return@composable
                CreateOutfitScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    editOutfitId = outfitId
                )
            }

            // ── 衣物管理页面 ─────────────────────────────────────────────────
            composable(Routes.ADD_CLOTHING) {
                AddClothingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.EDIT_CLOTHING,
                arguments = listOf(navArgument("clothingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val clothingId = backStackEntry.arguments?.getString("clothingId") ?: return@composable
                AddClothingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    editClothingId = clothingId
                )
            }

            composable(
                route = Routes.CLOTHING_DETAIL,
                arguments = listOf(navArgument("clothingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val clothingId = backStackEntry.arguments?.getString("clothingId") ?: return@composable
                ClothingDetailScreen(
                    clothingId = clothingId,
                    onNavigateBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.editClothing(id)) },
                    onDeleted = { navController.popBackStack() }
                )
            }
        }
    }
}
