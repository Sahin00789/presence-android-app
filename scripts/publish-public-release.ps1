# Script to build release APK and publish ONLY docs & APK to public distribution repository/releases
param (
    [string]$PublicRepoUrl = "Sahin00789/presence-android-app",
    [string]$TagName = ""
)

if ([string]::IsNullOrWhiteSpace($TagName)) {
    $versionMatch = Select-String -Path 'app\build.gradle.kts' -Pattern 'versionName\s*=\s*"([^"]+)"' | Select-Object -First 1
    if ($null -eq $versionMatch) {
        Write-Host "ERROR: Could not read versionName from app/build.gradle.kts" -ForegroundColor Red
        exit 1
    }
    $TagName = "v" + $versionMatch.Matches[0].Groups[1].Value
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Building Release APK for Public Distribution" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Step 1: Build APK
.\gradlew assembleRelease

$apkPath = "app\build\outputs\apk\release\presence.apk"

if (-not (Test-Path $apkPath)) {
    Write-Host "ERROR: APK build failed or presence.apk not found at $apkPath" -ForegroundColor Red
    exit 1
}

Write-Host "SUCCESS: APK built at $apkPath" -ForegroundColor Green

# Step 2: Check GitHub CLI
if (Get-Command gh -ErrorAction SilentlyContinue) {
    Write-Host "Publishing APK release ($TagName) to $PublicRepoUrl..." -ForegroundColor Cyan

    gh release create $TagName $apkPath `
        --repo $PublicRepoUrl `
        --title "Presence Android App - $TagName" `
        --notes "Download the latest compiled Android APK (presence.apk)." `
        --latest

    if ($LASTEXITCODE -ne 0) {
        Write-Host "Release '$TagName' already exists. Uploading/updating asset..." -ForegroundColor Yellow
        gh release upload $TagName $apkPath --repo $PublicRepoUrl --clobber
    }

    Write-Host "SUCCESS: Uploaded presence.apk to GitHub Release '$TagName' on public repository!" -ForegroundColor Green
} else {
    Write-Host "GitHub CLI (gh) not found. Please upload $apkPath manually to GitHub Releases." -ForegroundColor Yellow
}
