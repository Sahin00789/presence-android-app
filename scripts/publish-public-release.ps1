# Script to build release APK and publish ONLY docs & APK to public distribution repository/releases

param (
    [string]$PublicRepoUrl = "git@github.com:Sahin00789/presence-android-app.git",
    [string]$TagName = "latest"
)

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
    Write-Host "Publishing APK and Docs release ($TagName) to $PublicRepoUrl..." -ForegroundColor Cyan

    gh release create $TagName $apkPath `
        --repo $PublicRepoUrl `
        --title "Presence Android App - Latest Release ($TagName)" `
        --notes "Download the latest compiled Android APK (`presence.apk`). Source code is proprietary and private." `
        --clobber

    Write-Host "SUCCESS: Uploaded presence.apk to GitHub Release '$TagName' on public repository!" -ForegroundColor Green
} else {
    Write-Host "GitHub CLI (gh) not found. Please upload $apkPath and docs/ manually to GitHub Releases." -ForegroundColor Yellow
}
