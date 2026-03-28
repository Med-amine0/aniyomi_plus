# Aniyomi Plus (Enhanced Fork)

<div align="center">

<a href="https://aniyomi.org">
    <img src="./.github/assets/logo.png" alt="Aniyomi logo" title="Aniyomi logo" width="80"/>
</a>

# Aniyomi Plus Enhanced Fork

### An enhanced fork of [Aniyomi](https://github.com/aniyomiorg/aniyomi) with nested categories, thumbnails, and more

[![Discord server](https://img.shields.io/discord/841701076242530374.svg?label=&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF)](https://discord.gg/F32UjdJZrR)
[![License: Apache-2.0](https://img.shields.io/github/license/aniyomiorg/aniyomi?labelColor=27303D&color=818cf8)](/LICENSE)

</div>

---

## About This Fork

This fork adds **enhanced nested/hierarchical categories** support to Aniyomi, along with category thumbnails and various bug fixes. All features were developed with the assistance of an AI agent (OpenCode) to implement complex functionality.

### Key Features

- **Nested/Subcategories** - Unlimited nesting depth for organizing your library
- **Category Thumbnails** - Custom thumbnail images for categories (URL, upload, or auto from first entry)
- **Category Reordering** - Move categories up/down in the list, or move to parent level
- **Convert Entry to Category** - Long-press any entry and create a category at that level with the entry inside
- **Adjustable Grid** - Scale entries per row from 1 to 10 at any category level
- **Fixed Back Navigation** - Properly navigates back through nested category levels
- **Fixed Category Deletion** - Properly handles nested categories and unfavorites orphaned manga
- **Fixed Duplicate Entries** - No more duplicate entries when browsing library
- **Fixed Add to Library** - Works correctly after category deletion

---

## Base Project

This fork is based on **Aniyomi** - a full-featured manga and anime reader for Android.

- **Original Project**: [aniyomiorg/aniyomi](https://github.com/aniyomiorg/aniyomi)
- **Documentation**: [aniyomi.org](https://aniyomi.org)
- **Discord**: [Join the community](https://discord.gg/F32UjdJZrR)

---

## Building

### Requirements

- Android SDK 34+
- Java 17+
- Gradle 8.13+

### Quick Build (Debug APK)

```bash
# Build debug APK for arm64-v8a (recommended for low-RAM systems)
./gradlew assembleDebug

# The APK will be at: app/build/outputs/apk/debug/
```

### Build Configuration

For low-memory systems, the project includes optimized gradle.properties:

- Single-worker compilation
- In-process Kotlin compilation
- Serial garbage collection
- 2GB heap limit

---

## Project Structure

```
aniyomi_plus/
├── app/                    # Main application module
├── data/                   # Data layer (database, repositories)
├── domain/                 # Domain layer (use cases, models)
├── presentation-core/      # Shared UI components
├── presentation-widget/    # Widget components
├── core/                   # Core utilities
├── i18n/                   # Internationalization
├── i18n-aniyomi/          # Aniyomi strings
├── source-api/             # Extension source API
├── source-local/          # Local source implementation
├── buildSrc/              # Gradle build logic
└── gradle/                # Gradle wrapper
```

---

## Features & Changes

### Database Schema Changes

#### Categories Table (both manga and anime)

Added columns for nested categories and thumbnails:

| Column | Type | Description |
|--------|------|-------------|
| `parentId` | INTEGER | Parent category ID (null for top-level) |
| `thumbnailUrl` | TEXT | Custom thumbnail URL for category |

#### Migrations

- **Manga**: Migration 34 (adds parent_id and thumbnail_url)
- **Anime**: Migration 137 (adds parent_id and thumbnail_url)

---

### Feature: Nested Categories

Create unlimited levels of subcategories to organize your library hierarchically.

#### Key Models

| File | Purpose |
|------|---------|
| `domain/src/main/java/tachiyomi/domain/category/model/Category.kt` | Category model with `parentId` and `thumbnailUrl` |
| `domain/src/main/java/tachiyomi/domain/category/model/CategoryUpdate.kt` | Update model with `parentId` and `thumbnailUrl` |
| `domain/src/main/java/tachiyomi/domain/category/model/CategoryWithEntries.kt` | Category with its entries count |

#### Repository Layer

| File | Purpose |
|------|---------|
| `data/src/main/java/tachiyomi/data/category/manga/MangaCategoryRepositoryImpl.kt` | Manga category CRUD with parentId |
| `data/src/main/java/tachiyomi/data/category/anime/AnimeCategoryRepositoryImpl.kt` | Anime category CRUD with parentId |
| `data/src/main/sqldelight/data/categories.sq` | Manga categories SQL queries |
| `data/src/main/sqldelightanime/dataanime/categories.sq` | Anime categories SQL queries |

---

### Feature: Category Reordering

Easily reorder categories with the popup menu:

- **Move Up** - Move category up in the list
- **Move Down** - Move category down in the list
- **Move to Parent** - Move category to become a sibling (same parent as current)

#### Key Files

| File | Purpose |
|------|---------|
| `app/src/main/java/eu/kanade/presentation/category/components/CategoryListItem.kt` | Dropdown menu with reorder actions |
| `app/src/main/java/eu/kanade/tachiyomi/ui/category/manga/MangaCategoryScreenModel.kt` | `moveUp`, `moveDown`, `moveToParent` methods |
| `app/src/main/java/eu/kanade/tachiyomi/ui/category/anime/AnimeCategoryScreenModel.kt` | `moveUp`, `moveDown`, `moveToParent` methods |
| `app/src/main/java/eu/kanade/tachiyomi/ui/category/manga/MangaCategoryTab.kt` | Category tab UI |
| `app/src/main/java/eu/kanade/tachiyomi/ui/category/anime/AnimeCategoryTab.kt` | Category tab UI |

---

### Feature: Category Thumbnails

Customize category appearance with thumbnails:

- **URL Input** - Enter any image URL
- **Upload Image** - Pick from device gallery
- **Auto Thumbnail** - Uses first entry's cover as default

#### Key Files

| File | Purpose |
|------|---------|
| `app/src/main/java/eu/kanade/presentation/category/components/CategoryDialogs.kt` | `ThumbnailUrlDialog` composable |
| `app/src/main/java/eu/kanade/presentation/category/components/CategoryListItem.kt` | Thumbnail display in list |
| `app/src/main/java/eu/kanade/tachiyomi/ui/category/manga/MangaCategoryScreenModel.kt` | `setThumbnail` method |
| `app/src/main/java/eu/kanade/tachiyomi/ui/category/anime/AnimeCategoryScreenModel.kt` | `setThumbnail` method |

---

### Feature: Convert Entry to Category

Long-press any entry in the library and convert it into a category at the current level. The new category will:
1. Have the same name as the entry
2. Be created at the current category level
3. Automatically contain the selected entry

#### Key Files

| File | Purpose |
|------|---------|
| `app/src/main/java/eu/kanade/presentation/entries/components/EntryBottomActionMenu.kt` | Bottom action menu with create category option |
| `app/src/main/java/eu/kanade/tachiyomi/ui/library/manga/MangaLibraryTab.kt` | Manga library with action callbacks |
| `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibraryTab.kt` | Anime library with action callbacks |
| `domain/src/main/java/tachiyomi/domain/category/manga/interactor/CreateMangaCategoryWithName.kt` | Create category from name |
| `domain/src/main/java/tachiyomi/domain/category/anime/interactor/CreateAnimeCategoryWithName.kt` | Create category from name |

---

### Feature: Adjustable Grid

Scale the number of entries per row from 1 to 10 at any category level using the grid icon in the toolbar.

- **Grid Icon** - Click to open column selector dialog
- **Slider Control** - Drag to select between 1-10 columns
- **Persisted** - Your preference is saved automatically

#### Key Files

| File | Purpose |
|------|---------|
| `app/src/main/java/eu/kanade/presentation/library/components/LibraryToolbar.kt` | Added column selector dialog and grid icon |
| `app/src/main/java/eu/kanade/tachiyomi/ui/library/manga/MangaLibraryTab.kt` | Grid control integration |
| `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibraryTab.kt` | Grid control integration |

---

### Bug Fixes

#### Fixed: Back Navigation for Nested Categories
- Categories now properly navigate back through nested levels
- `AnimeLibraryContent.kt` and `MangaLibraryContent.kt` use proper back navigation

#### Fixed: Category Deletion with Nested Categories
- `DeleteMangaCategory.kt` properly handles nested categories
- Automatically unfavorites manga orphaned by deletion
- Same fix applied to `DeleteAnimeCategory.kt`

#### Fixed: Duplicate Entries in Library
- Fixed entries appearing multiple times in browsing view
- Category filtering logic corrected in library content

#### Fixed: Add to Library After Category Deletion
- Adding entries to library works correctly after category deletion
- `AnimeCategoryRepositoryImpl` and `MangaCategoryRepositoryImpl` handle null categories properly

---

### UI Components Modified

| File | Changes |
|------|---------|
| `app/src/main/java/eu/kanade/presentation/category/MangaCategoryScreen.kt` | Simplified UI, dropdown menu actions |
| `app/src/main/java/eu/kanade/presentation/category/AnimeCategoryScreen.kt` | Simplified UI, dropdown menu actions |
| `app/src/main/java/eu/kanade/presentation/library/manga/MangaLibraryContent.kt` | Back navigation, AutoMirrored icons |
| `app/src/main/java/eu/kanade/presentation/library/anime/AnimeLibraryContent.kt` | Back navigation, AutoMirrored icons |

---

### Interactors Modified

| File | Changes |
|------|---------|
| `domain/src/main/java/tachiyomi/domain/category/manga/interactor/DeleteMangaCategory.kt` | Handles nested categories, unfavorites orphaned manga |
| `domain/src/main/java/tachiyomi/domain/category/anime/interactor/DeleteAnimeCategory.kt` | Handles nested categories, unfavorites orphaned anime |

---

### Internationalization

Updated strings in `i18n/src/commonMain/moko-resources/base/strings.xml`:
- App name changed to "Aniyomi Plus"
- Added strings for reorder actions (Move Up, Move Down, Move to Parent)
- Added strings for thumbnail actions (Edit Thumbnail, Use Default Thumbnail)

---

## Complete File Change Log

### Domain Layer (Models & Use Cases)
- `domain/src/main/java/tachiyomi/domain/category/model/Category.kt` - Added `thumbnailUrl` field
- `domain/src/main/java/tachiyomi/domain/category/model/CategoryUpdate.kt` - Added `thumbnailUrl` field
- `domain/src/main/java/tachiyomi/domain/category/manga/interactor/DeleteMangaCategory.kt` - Fixed nested category handling
- `domain/src/main/java/tachiyomi/domain/category/anime/interactor/DeleteAnimeCategory.kt` - Fixed nested category handling
- `domain/src/main/java/tachiyomi/domain/category/manga/interactor/CreateMangaCategoryWithName.kt` - Create category from entry name
- `domain/src/main/java/tachiyomi/domain/category/anime/interactor/CreateAnimeCategoryWithName.kt` - Create category from entry name

### Data Layer (Database & Repositories)
- `data/src/main/sqldelight/data/categories.sq` - Added `thumbnail_url` column
- `data/src/main/sqldelightanime/dataanime/categories.sq` - Added `thumbnail_url` column
- `data/src/main/sqldelight/migrations/34.sqm` - Manga schema migration
- `data/src/main/sqldelightanime/migrations/137.sqm` - Anime schema migration
- `data/src/main/java/tachiyomi/data/category/manga/MangaCategoryRepositoryImpl.kt` - Thumbnail support
- `data/src/main/java/tachiyomi/data/category/anime/AnimeCategoryRepositoryImpl.kt` - Thumbnail support

### Presentation Layer (UI)
- `app/src/main/java/eu/kanade/presentation/category/components/CategoryListItem.kt` - Dropdown menu with all actions
- `app/src/main/java/eu/kanade/presentation/category/components/CategoryDialogs.kt` - ThumbnailUrlDialog
- `app/src/main/java/eu/kanade/presentation/category/MangaCategoryScreen.kt` - Simplified UI
- `app/src/main/java/eu/kanade/presentation/category/AnimeCategoryScreen.kt` - Simplified UI
- `app/src/main/java/eu/kanade/presentation/library/manga/MangaLibraryContent.kt` - Back navigation fix
- `app/src/main/java/eu/kanade/presentation/library/anime/AnimeLibraryContent.kt` - Back navigation fix
- `app/src/main/java/eu/kanade/presentation/entries/components/EntryBottomActionMenu.kt` - Create category action

### ScreenModel Layer
- `app/src/main/java/eu/kanade/tachiyomi/ui/category/manga/MangaCategoryScreenModel.kt` - Reorder & thumbnail methods
- `app/src/main/java/eu/kanade/tachiyomi/ui/category/anime/AnimeCategoryScreenModel.kt` - Reorder & thumbnail methods
- `app/src/main/java/eu/kanade/tachiyomi/ui/category/manga/MangaCategoryTab.kt` - Action callbacks
- `app/src/main/java/eu/kanade/tachiyomi/ui/category/anime/AnimeCategoryTab.kt` - Action callbacks
- `app/src/main/java/eu/kanade/tachiyomi/ui/library/manga/MangaLibraryTab.kt` - Create category callback
- `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibraryTab.kt` - Create category callback

### Internationalization
- `i18n/src/commonMain/moko-resources/base/strings.xml` - App name & new action strings

---

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

### Guidelines

- Follow the existing code style
- Test your changes thoroughly
- Update documentation as needed
- Be respectful in communications

---

## References & Credits

- **[Aniyomi](https://github.com/aniyomiorg/aniyomi)** - Base project
- **[Mihon](https://github.com/mihonapp/mihon)** - Forked from (Tachiyomi fork)
- **[Tachiyomi](https://github.com/tachiyomiorg/tachiyomi)** - Original manga reader
- **OpenCode AI Agent** - Assisted in developing all features in this fork

### Libraries & Dependencies

This project uses many open-source libraries. See `build.gradle.kts` files for full dependencies.

---

## License

<pre>
Copyright © 2015 Javier Tomás
Copyright © 2024 Mihon Open Source Project
Copyright © 2024 Aniyomi Open Source Project
Copyright © 2025 Aniyomi Plus Fork

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>

---

## Disclaimer

The developer(s) of this application do not have any affiliation with the content providers available, and this application hosts zero content.

---

## Support

For issues specific to this fork, please open an issue on this repository.

For general Aniyomi support, visit:
- [Aniyomi Discord](https://discord.gg/F32UjdJZrR)
- [Aniyomi Website](https://aniyomi.org)
- [Aniyomi GitHub](https://github.com/aniyomiorg/aniyomi)
