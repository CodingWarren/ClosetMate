# ClosetMate 👗

> **AI Your Closet, Style Your Day.**

ClosetMate 是一款 Android 原生衣橱管理应用，帮助用户数字化管理个人衣橱、记录穿搭搭配，并通过 AI 技术提供智能穿搭推荐。

---

## 📱 项目概述

| 属性 | 内容 |
|------|------|
| 平台 | Android 10+（API 26+） |
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository Pattern |
| 数据库 | Room (SQLite) |
| 当前版本 | v0.0.1 |

---

## ✨ 当前功能（v1.0）

### 衣橱管理
- 📷 拍照或从相册添加衣物，支持图片裁剪
- 🏷️ 为衣物添加品类、颜色、季节、风格等标签
- 🔍 多维度筛选与搜索衣物
- 📊 衣物统计数据概览

### 搭配管理
- 👔 手动创建穿搭搭配方案
- 💾 保存与管理搭配记录
- 📅 记录穿着日期

### 数据安全
- 🔒 应用锁（生物识别 / PIN 码）
- 💾 本地数据备份与恢复

### 设置
- ⚙️ 个性化设置管理

---

## 🚀 未来规划（v1.5 AI 版）

- 🤖 **AI 图片处理**：自动去除背景，生成干净的商品图效果
- 🏷️ **AI 智能打标**：图片上传后自动识别品类、颜色、风格
- 🌤️ **AI 每日穿搭推荐**：结合天气、场景、闲置情况智能推荐
- 👗 **核心单品搭配**：选中一件衣物，AI 生成多套搭配方案
- ☁️ **云端同步**：登录账号后数据自动同步至云端（Firebase）
- 👤 **用户账户系统**：手机号登录，支持游客模式

详细规划请参阅 [ClosetMate AI PRD v1.5](ClosetMate_AI_PRD_v1.5.md)。

---

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose, Material 3 |
| 架构 | MVVM, Repository Pattern |
| 导航 | Navigation Compose |
| 数据库 | Room 2.6 |
| 异步 | Kotlin Coroutines + Flow |
| 图片加载 | Coil 2.6 |
| 本地存储 | DataStore Preferences |
| 生物识别 | AndroidX Biometric |
| 构建工具 | Gradle (Kotlin DSL) |

---

## 📁 项目结构

```
app/src/main/java/com/closetmate/app/
├── MainActivity.kt                  # 应用入口
├── data/
│   ├── backup/
│   │   └── BackupManager.kt         # 数据备份与恢复
│   ├── local/
│   │   ├── AppDatabase.kt           # Room 数据库
│   │   ├── dao/
│   │   │   ├── ClothingDao.kt       # 衣物数据访问对象
│   │   │   └── OutfitDao.kt         # 搭配数据访问对象
│   │   └── entity/
│   │       ├── ClothingEntity.kt    # 衣物数据模型
│   │       └── OutfitEntity.kt      # 搭配数据模型
│   └── repository/
│       ├── ClothingRepository.kt    # 衣物数据仓库
│       └── OutfitRepository.kt      # 搭配数据仓库
├── security/
│   └── AppLockManager.kt            # 应用锁管理
└── ui/
    ├── navigation/
    │   └── ClosetMateNavigation.kt  # 导航路由
    ├── screens/
    │   ├── closet/                  # 衣橱相关页面
    │   │   ├── ClosetScreen.kt
    │   │   ├── ClosetViewModel.kt
    │   │   ├── AddClothingScreen.kt
    │   │   ├── ClothingDetailScreen.kt
    │   │   └── components/
    │   ├── outfit/                  # 搭配相关页面
    │   ├── stats/                   # 统计页面
    │   ├── settings/                # 设置页面
    │   └── lock/                    # 应用锁页面
    └── theme/                       # 主题配置
```

---

## 🏗️ 本地构建

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK API 34
- Gradle 8.x

### 构建步骤

1. 克隆仓库：
   ```bash
   git clone https://github.com/CodingWarren/ClosetMate.git
   ```

2. 用 Android Studio 打开项目。

3. 等待 Gradle 同步完成。

4. 连接 Android 设备或启动模拟器（API 26+）。

5. 点击 **Run** 或执行：
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📋 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.0.1 | 2026-03-30 | 初始版本，包含衣橱管理、搭配记录、应用锁、数据备份等核心功能 |

---

## 📄 许可证

本项目仅供个人学习与使用。

---

> Made with ❤️ by [CodingWarren](https://github.com/CodingWarren)
