# Aniyomi Plus (Nested Categories + Dashboard Fork)

<div align="center">

<a href="https://aniyomi.org">
    <img src="./.github/assets/logo.png" alt="Aniyomi logo" title="Aniyomi logo" width="80"/>
</a>

# Aniyomi Plus Fork

### A fork of [Aniyomi](https://github.com/aniyomiorg/aniyomi) with Dashboard, Nested Categories, and AniList API integration

[![Discord server](https://img.shields.io/discord/841701076242530374.svg?label=&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF)](https://discord.gg/F32UjdJZrR)
[![License: Apache-2.0](https://img.shields.io/github/license/aniyomiorg/aniyomi?labelColor=27303D&color=818cf8)](/LICENSE)

</div>

---

## What's New in This Fork

### Dashboard Tab (Dach)
A new dashboard featuring **AniList API** integration for anime discovery:

- **Genre Pills** - Dynamically fetched from AniList, sorted alphabetically
- **Tags Dropdown** - Full list of AniList tags for detailed filtering
- **Anime & Movies Rows** - Horizontal scrolling discovery with search functionality
- **Search Icon** - Click to search anime directly in-app via AniList

### Nested Categories
Organize your library with **hierarchical categories**:

- Unlimited nesting depth for anime and manga
- Long-press to move items between nested categories
- Intuitive navigation through category levels

### Configurable Columns
Adjust library display with slider controls:

- Anime library: customizable column count (default: 3)
- Manga library: customizable column count (default: 3)

### Settings Dashboard
Debug panel in **More > Dash** for API testing:

- Test AniList GraphQL queries
- View response logs
- Copy logs to clipboard

---

## About This Fork

This fork builds upon **Aniyomi** with:

- Nested/hierarchical categories support
- Dashboard with AniList API discovery
- Configurable library columns
- Developer tools for API debugging

---

## AniList GraphQL API Reference

This fork uses the [AniList GraphQL API](https://github.com/AniList/docs/tree/master/source/api-docs) for anime discovery.

### Base Endpoint
```
https://graphql.anilist.co
```

### Key Queries Used

**Fetch Genre/Tag Metadata:**
```graphql
query {
  GenreCollection
  MediaTagCollection {
    name
  }
}
```

**Fetch Anime (Trending):**
```graphql
query($page: Int) {
  Page(page: $page, perPage: 12) {
    pageInfo { hasNextPage }
    media(type: ANIME, sort: TRENDING_DESC) {
      title { romaji }
      coverImage { medium }
      siteUrl
    }
  }
}
```

**Fetch Anime by Genre:**
```graphql
query($page: Int, $genre: String) {
  Page(page: $page, perPage: 12) {
    pageInfo { hasNextPage }
    media(type: ANIME, genre: $genre, sort: SCORE_DESC) {
      title { romaji }
      coverImage { medium }
      siteUrl
    }
  }
}
```

**Fetch Movies:**
```graphql
query($page: Int) {
  Page(page: $page, perPage: 12) {
    pageInfo { hasNextPage }
    media(type: ANIME, format: MOVIE, sort: TRENDING_DESC) {
      title { romaji }
      coverImage { medium }
      siteUrl
    }
  }
}
```

### API Rate Limits
- Standard rate limit: 90 requests per minute
- Note: Movies and genre filters may occasionally return server errors (5xx)

### Documentation
- [AniList API Docs](https://github.com/AniList/docs/tree/master/source/api-docs)
- [GraphQL Schema](https://github.com/AniList/docs/blob/master/source/api-docs/graphql/schema.md)

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
# Build debug APK
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

Added `parentId` column to categories table for hierarchy support:
- `parentId = null` → Top-level category
- `parentId = categoryId` → Subcategory of that category

### Key Modified Files

| Component | Purpose |
|----------|---------|
| `DashboardScreenModel.kt` | AniList GraphQL API integration |
| `DashboardTab.kt` | Dashboard UI with genre pills and discovery |
| `DashSettingsScreen.kt` | API testing debug panel |
| `Category.kt` | Added `parentId` field for nested categories |
| `AnimeCategoryScreen.kt` | UI for managing anime categories |
| `MangaCategoryScreen.kt` | UI for managing manga categories |

---

## Credits

### Development
- **OpenCode Big Pickle** - Primary developer of this fork

### Base Projects
- **[Aniyomi](https://github.com/aniyomiorg/aniyomi)** - Base manga/anime reader
- **[Mihon](https://github.com/mihonapp/mihon)** - Forked from (Tachiyomi fork)
- **[Tachiyomi](https://github.com/tachiyomiorg/tachiyomi)** - Original manga reader

### APIs
- **[AniList](https://github.com/AniList/docs)** - GraphQL API for anime discovery

### Libraries & Dependencies

This project uses many open-source libraries. See `build.gradle.kts` files for full dependencies.

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
