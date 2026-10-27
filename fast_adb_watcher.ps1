$adb = "C:\Users\dell\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = "c:\cdrive files\OneDrive\Desktop\ANORIDE APP\app\build\outputs\apk\debug\app-debug.apk"
$targetDir = "C:\Users\dell\.gemini\antigravity-ide\brain\87d678d8-5cfc-4abc-ba54-cf94eeb2121c"

Write-Host "FAST-RESPONSE ADB WATCHER ACTIVE..."
$startTime = Get-Date

while ((Get-Date) -lt $startTime.AddMinutes(10)) {
    $devLines = & $adb devices | Where-Object { $_ -match "^\s*(\S+)\s+device\b" }
    if ($devLines) {
        $firstLine = $devLines[0]
        if ($firstLine -match "^\s*(\S+)\s+device\b") {
            $devId = $Matches[1].Trim()
            if (-not [string]::IsNullOrWhiteSpace($devId)) {
                Write-Host "DEVICE CONNECTED: '$devId'! Executing deployment sequence..."
                
                # 1. Enable wireless ADB as backup
                & $adb -s $devId tcpip 5555
                
                # 2. Install updated APK
                Write-Host "Installing APK to $devId..."
                $installRes = & $adb -s $devId install -r $apk
                Write-Host "Install result: $installRes"
                
                # 3. Launch FocusCrew
                Write-Host "Launching FocusCrew..."
                $launchRes = & $adb -s $devId shell am start -n com.focuscrew.app/.ui.MainActivity
                Write-Host "Launch result: $launchRes"
                
                Start-Sleep -Seconds 2
                
                # 4. Pull screenshot
                Write-Host "Capturing verification screenshot..."
                & $adb -s $devId shell screencap -p /sdcard/focuscrew_screen.png
                & $adb -s $devId pull /sdcard/focuscrew_screen.png "$targetDir\focuscrew_screen.png"
                
                if (Test-Path "$targetDir\focuscrew_screen.png") {
                    Copy-Item -Path "$targetDir\focuscrew_screen.png" -Destination "C:\cdrive files\OneDrive\Desktop\focuscrew_screen.png" -Force
                    Write-Host "SUCCESS: Screenshot saved to desktop!"
                }
                
                Write-Host "SUCCESS: Deployed FocusCrew to $devId successfully!"
                exit 0
            }
        }
    }
    Start-Sleep -Milliseconds 400
}

Write-Host "TIMEOUT_REACHED"
