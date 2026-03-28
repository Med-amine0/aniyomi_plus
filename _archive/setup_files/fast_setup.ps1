$ErrorActionPreference = "Stop"
Write-Host "Creating C:\Android\cmdline-tools..."
New-Item -ItemType Directory -Force -Path "C:\Android\cmdline-tools" | Out-Null

Write-Host "Downloading cmdline-tools with curl..."
curl.exe -L -o "C:\Android\cmdline-tools.zip" "https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip"

Write-Host "Extracting cmdline-tools..."
Expand-Archive -Path "C:\Android\cmdline-tools.zip" -DestinationPath "C:\Android\cmdline-tools" -Force

Write-Host "Renaming to 'latest'..."
if (Test-Path "C:\Android\cmdline-tools\cmdline-tools") {
    Rename-Item "C:\Android\cmdline-tools\cmdline-tools" "latest"
}

Write-Host "Accepting licenses..."
$env:ANDROID_HOME = "C:\Android"
$env:PATH += ";C:\Android\cmdline-tools\latest\bin"
1..100 | ForEach-Object { "y" } | sdkmanager.bat --licenses

Write-Host "Building APK..."
Set-Location "c:\Users\HP\Desktop\aniyomi_plus"
./gradlew assembleDebug
