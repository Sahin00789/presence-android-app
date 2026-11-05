# PowerShell script to build release APK and publish to GitHub Releases
param (
    [string]$TagName = "latest",
    [string]$ReleaseTitle = "Presence Android App (Latest Release)"
)

Write-Host "Building Release APK..." -ForegroundColor Cyan
.\gradlew assembleRelease

$apkPath = "app\build\outputs\apk\release\presence.apk"

if (Test-Path $apkPath) {
    Write-Host "Found APK at $apkPath" -ForegroundColor Green

    if (Get-Command gh -ErrorAction SilentlyContinue) {
        Write-Host "Publishing/updating release '$TagName' on GitHub..." -ForegroundColor Cyan
        gh release create $TagName $apkPath --title $ReleaseTitle --notes "Latest build of presence.apk" --latest
        if ($LASTEXITCODE -ne 0) {
            gh release upload $TagName $apkPath --clobber
        }
        Write-Host "Successfully uploaded presence.apk to GitHub Release '$TagName'!" -ForegroundColor Green
    } else {
        Write-Host "GitHub CLI (gh) not installed. The APK is ready at: $apkPath" -ForegroundColor Yellow
        Write-Host "You can manually attach this file to your GitHub Release." -ForegroundColor Yellow
    }
} else {
    Write-Host "Build failed or APK not found at $apkPath" -ForegroundColor Red
}
