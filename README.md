# Aniyomi Plus (Nested Categories + Dashboard Fork)

<div align="center">

<a href="https://aniyomi.org">
    <img src="./.github/assets/logo.png" alt="Aniyomi logo" title="Aniyomi logo" width="80"/>
</a>

# Aniyomi Plus Fork

### A fork of [Aniyomi](https://github.com/aniyomiorg/aniyomi) with Dashboard, Nested Categories, AniList API integration, Torrent Streaming, and current manga extensions ([Keiyoushi](https://github.com/keiyoushi/extensions) / extensions-lib 1.6)

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

### Torrent Streaming (TorrServer)
Built-in torrent streaming support powered by TorrServer:

- Stream anime directly from magnet links and .torrent files
- Embedded TorrServer (no external server needed)
- **Turn it on first** — Player Settings → Torrents → *Enable torrent streaming*
  (off by default; the toggle lives with the port and tracker settings)
- Auto-starts once enabled whenever you play torrent content
- Known limitation: *Proxy mode* / *Proxy URL* on that screen are stored but not
  wired up by the current torrent engine

### Manga Extensions — Keiyoushi & extensions-lib 1.6
The manga side is in sync with the extensions people actually publish:

- **extensions-lib 1.6** — sources that supply details and chapters from the
  combined hook (`getMangaUpdate(manga, chapters, fetchDetails, fetchChapters)`,
  which Keiyoushi's `KeiSource` wraps) are supported, `SMangaUpdate` included.
  Sources built for lib 1.4 / 1.5 keep behaving exactly as they did — nothing
  about their code path changed.
- **Extension stores** — *Manga extension repos* takes protobuf `index.pb`,
  `repo.json` and the legacy `index.min.json` repos, all merged into one
  extension list, and stores can be removed from that same screen. No new
  screens or buttons: the URL is sorted out when you add it.
- **Anime side** — anime stores already worked the same way (*Anime extension
  repos*), and anime extensions at lib v14 / v16 / v17 load.

Adding the Keiyoushi store — **Settings → Browse → Manga extension repos → +**:

```
https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json
https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.pb
https://raw.githubusercontent.com/keiyoushi/extensions/repo/repo.json
```

Any of the three works — the app follows `index.min.json` → `repo.json` →
`index.pb` on its own. The Keiyoushi site's "add to app" button works too, via
the `mihon://extension-store?url=…` deeplink.

### Fixed
- **Crashes with no crash screen** — R8 was stripping `com.squareup.zstd.**`
  while the bundled native `libzstd-kmp.so` still resolved those classes by
  name, so any `Content-Encoding: zstd` response killed the app from native
  code. The package is kept now.
- **Manga entries that refused to load** on extensions-lib 1.6 sources
  (`AbstractMethodError` on `chapterListParse`). Details and chapters are also
  fetched one after the other now instead of in parallel, because 1.6 sources
  reject concurrent updates for the same manga.

### Upstream Sync
Based on **[Aniyomi v0.18.2.1](https://github.com/aniyomiorg/aniyomi)**. The most
recent sync brought in:

- **Extension stores** — the extension repository list is now a protobuf/gzip
  store index (aniyomix 1.6), migrated automatically from the legacy index
- **extensions-lib v17** — related entries, hoster/video memo, an in-app HTTP
  server for extensions, seek thumbnails, and new `source-api` models
- **Upstream torrent streaming**, merged in alongside this fork's implementation

---

## Download

Grab the latest signed APK from the
[Releases page](https://github.com/Med-amine0/aniyomi_plus/releases/latest)
(**Assets** → `.apk`).

Release builds are signed with the fork's key, so every release installs as an
in-place update of the previous one. Pushing a `v*` tag builds the APKs and
publishes them as a release; every branch push is built too, with its APK under
that run's **Artifacts** tab.

---

## About This Fork

This fork builds upon **Aniyomi** with:

- Nested/hierarchical categories support
- Dashboard with AniList API discovery
- Configurable library columns
- Developer tools for API debugging
- Manga extension stores (protobuf `index.pb` / `repo.json` / legacy `index.min.json`)
- Current manga extension API (extensions-lib 1.6, e.g. Keiyoushi)

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

### Manga Extension Support
- **[Keiyoushi](https://github.com/keiyoushi/extensions)** - The manga extension catalogue this fork's extension stores are wired up for
- **[keiyoushi/extensions-source](https://github.com/keiyoushi/extensions-source)** - `KeiSource` and the sources the extensions-lib 1.6 support is built around
- **[Mihon](https://github.com/mihonapp/mihon)** - `source-api` and the extension-store design (`getMangaUpdate`, `SMangaUpdate`, `index.pb` stores)
- **Every extension author** - none of this is worth anything without them

### Torrent Implementation
- **[Animetail](https://github.com/Animetailapp/Animetail)** - TorrServer implementation reference
- **[torrentserver-aniyomi](https://github.com/Diegopyl1209/torrentserver-aniyomi)** - The torrent engine this fork uses
- **[TorrServer](https://github.com/YouROK/TorrServer)** - The streaming server it is based on
- [Torrent Streaming](https://github.com/Med-amine0/aniyomi_plus#torrent-streaming-torrserver) - Stream anime directly from magnet links and .torrent files

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
