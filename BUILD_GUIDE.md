# Build Guide for Aniyomi Plus

This guide explains how to build this fork and the optimizations made for low-RAM systems.

## System Requirements

- **RAM**: 4GB minimum (8GB recommended)
- **Storage**: 10GB+ free space
- **OS**: Windows, macOS, or Linux
- **Java**: JDK 17 (or higher)
- **Android SDK**: API 34+

## Quick Start

### 1. Install Requirements

```bash
# Check Java version
java -version
# Should be 17.0.x or higher

# If needed, install JDK 17
# Windows: https://adoptium.net/
# macOS: brew install openjdk@17
# Linux: sudo apt install openjdk-17-jdk
```

### 2. Setup Android SDK

Set `ANDROID_HOME` environment variable pointing to your Android SDK:

```bash
# Windows (PowerShell)
$env:ANDROID_HOME = "C:\Users\YourUser\AppData\Local\Android\Sdk"

# macOS/Linux
export ANDROID_HOME=$HOME/Android/Sdk
```

### 3. Build the APK

```bash
# Debug build (recommended first)
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### 4. Find the APK

```
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

---

## Build Configuration

### Current Optimizations (Low-RAM Settings)

The project includes optimized settings in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2000m -Dfile.encoding=UTF-8 -XX:MaxMetaspaceSize=512m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=96m
org.gradle.parallel=false
org.gradle.workers.max=1
kotlin.compiler.execution.strategy=in-process
```

**What these settings do:**

| Setting | Value | Purpose |
|---------|-------|---------|
| `-Xmx2000m` | 2GB heap | Limits JVM memory usage |
| `-XX:+UseSerialGC` | Serial GC | Uses less memory than parallel GCs |
| `parallel=false` | Off | Disables parallel builds (saves RAM) |
| `workers.max=1` | 1 worker | Single-threaded compilation |
| `in-process` | On | Kotlin compiles in Gradle process |

### If You Have More RAM

If your system has 8GB+ RAM, you can use faster settings:

```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8 -XX:MaxMetaspaceSize=768m -XX:+UseParallelGC -XX:ReservedCodeCacheSize=256m
org.gradle.parallel=true
org.gradle.workers.max=4
kotlin.compiler.execution.strategy=in-process
```

### Build All ABIs (Larger APK)

Currently configured for `arm64-v8a` only (smaller, faster build). To build all architectures:

```kotlin
// In app/build.gradle.kts, change splits.abi block:
abi {
    isEnable = true
    isUniversalApk = true
    reset()
    include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
}
```

---

## Build Commands Reference

```bash
# Clean build directory
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Build with no daemon (recommended for CI/low-RAM)
./gradlew assembleDebug --no-daemon

# Build with stacktrace (for debugging)
./gradlew assembleDebug --stacktrace

# Skip tests (faster build)
./gradlew assembleDebug -x test -x lint
```

---

## Common Build Issues

### Out of Memory (OOM)

**Error**: `java.lang.OutOfMemoryError: Java heap space`

**Fix**: Increase heap in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx3000m ...
```

Or use `--no-daemon` flag.

### Kotlin Daemon Issues

**Error**: `Detected multiple Kotlin daemon sessions`

**Fix**: Clean and rebuild:
```bash
./gradlew clean
./gradlew assembleDebug
```

### Lock File Issues

**Error**: `Timeout waiting to lock build logic queue`

**Fix**: Kill Gradle processes and remove lock:
```bash
# Windows
taskkill /F /IM java.exe

# Then delete lock files in .gradle/ folder
```

---

## CI/CD Notes

For automated builds:

```bash
# Full clean build
./gradlew clean assembleDebug --no-daemon --stacktrace
```

The `--no-daemon` flag is essential for CI environments to ensure fresh memory allocation.

---

## Rebuilding After Updates

When syncing with upstream (original Aniyomi):

1. **Pull changes** from upstream
2. **Resolve conflicts** in modified files
3. **Clean build**: `./gradlew clean`
4. **Build**: `./gradlew assembleDebug`

### Files Likely to Conflict

When updating from upstream, these files may need conflict resolution:

- `gradle.properties` (memory settings)
- `app/build.gradle.kts` (ABI splits)
- `domain/src/main/java/tachiyomi/domain/category/model/Category.kt`
- `domain/src/main/java/tachiyomi/domain/category/model/CategoryUpdate.kt`
- `data/src/main/sqldelight/data/categories.sq`
- `data/src/main/sqldelightanime/dataanime/categories.sq`

---

## Debug APK Location

After building, find your APK at:

```
# Debug APK (arm64-v8a only)
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk

# All debug APKs
app/build/outputs/apk/debug/

# Release APKs
app/build/outputs/apk/release/
```

---

## Need Help?

- **This fork issues**: https://github.com/Med-amine0/aniyomi_plus/issues
- **Aniyomi Discord**: https://discord.gg/F32UjdJZrR
- **Aniyomi GitHub**: https://github.com/aniyomiorg/aniyomi
