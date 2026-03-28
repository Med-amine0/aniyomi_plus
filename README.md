# Aniyomi Plus (Nested Categories Fork)

<div align="center">

<a href="https://aniyomi.org">
    <img src="./.github/assets/logo.png" alt="Aniyomi logo" title="Aniyomi logo" width="80"/>
</a>

# Aniyomi Nested Categories Fork

### A fork of [Aniyomi](https://github.com/aniyomiorg/aniyomi) with support for nested/subcategories

[![Discord server](https://img.shields.io/discord/841701076242530374.svg?label=&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF)](https://discord.gg/F32UjdJZrR)
[![License: Apache-2.0](https://img.shields.io/github/license/aniyomiorg/aniyomi?labelColor=27303D&color=818cf8)](/LICENSE)

</div>

---

## About This Fork

This fork adds **nested/hierarchical categories** support to Aniyomi, allowing users to:

- Create subcategories under existing categories
- Organize their library with nested category hierarchies
- Navigate through category levels in the library view
- Move items between nested categories

### Key Features

- **Unlimited nesting depth** - Create as many subcategory levels as needed
- **Intuitive navigation** - Browse through category hierarchies easily
- **Backward compatible** - Existing categories work without modification
- **Migration support** - Backups are restored correctly with nested structure

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

## Changes from Original

### Database Schema

Added `parentId` column to categories table to support hierarchy:
- `parentId = null` → Top-level category
- `parentId = categoryId` → Subcategory of that category

### Key Modified Files

| File | Purpose |
|------|---------|
| `Category.kt` | Added `parentId` field |
| `CategoryUpdate.kt` | Added `parentId` update support |
| `AnimeCategoryRepository.kt` | Added `parentId` parameter |
| `MangaCategoryRepository.kt` | Added `parentId` parameter |
| `AnimeCategoriesRestorer.kt` | Restores categories with hierarchy |
| `MangaCategoriesRestorer.kt` | Restores categories with hierarchy |
| `AnimeCategoryScreen.kt` | UI for managing anime categories |
| `MangaCategoryScreen.kt` | UI for managing manga categories |
| `AnimeLibraryContent.kt` | Library view with category navigation |
| `MangaLibraryContent.kt` | Library view with category navigation |

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

