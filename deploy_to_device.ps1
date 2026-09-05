$adb = "C:\Users\dell\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = "c:\cdrive files\OneDrive\Desktop\ANORIDE APP\app\build\outputs\apk\debug\app-debug.apk"
$desktopDir = "C:\cdrive files\OneDrive\Desktop"

Write-Host "=================================================="
Write-Host ">>> FOCUSCREW AUTO-DEPLOY WATCHER IS ACTIVE <<<"
Write-Host "Scanning for connected Android device..."
Write-Host "=================================================="

$timeout = (Get-Date).AddMinutes(15)

while ((Get-Date) -lt $timeout) {
    # Check for any device listed as 'device'
    $devicesOutput = & $adb devices
    $matching = $devicesOutput | Where-Object { $_ -match "^\s*(\S+)\s+device\b" }
    
    # Check for unauthorized devices
    $unauthorized = $devicesOutput | Where-Object { $_ -match "^\s*(\S+)\s+unauthorized\b" }
    if ($unauthorized) {
        Write-Host ">>> DEVICE DETECTED BUT UNAUTHORIZED! Check your phone screen and tap 'ALLOW USB DEBUGGING'!"
    }

    if ($matching) {
        $firstMatch = $matching[0]
        if ($firstMatch -match "^\s*(\S+)\s+device\b") {
            $devId = $Matches[1].Trim()
            Write-Host "`n>>> [SUCCESS] DEVICE DETECTED: $devId"
            Write-Host ">>> [1/4] Enabling TCP/IP mode for wireless convenience..."
            & $adb -s $devId tcpip 5555
            
            Write-Host ">>> [2/4] Installing FocusCrew APK ($apk)..."
            $installRes = & $adb -s $devId install -r -d $apk
            Write-Host "Install Result: $installRes"
            
            Write-Host ">>> [3/4] Launching FocusCrew App (com.focuscrew.app/.ui.MainActivity)..."
            $launchRes = & $adb -s $devId shell am start -n com.focuscrew.app/.ui.MainActivity
            Write-Host "Launch Result: $launchRes"
            
            Start-Sleep -Seconds 2
            
            Write-Host ">>> [4/4] Capturing verification screenshot..."
            & $adb -s $devId shell screencap -p /sdcard/focuscrew_deployed.png
            & $adb -s $devId pull /sdcard/focuscrew_deployed.png "$desktopDir\focuscrew_deployed.png"
            
            Write-Host "`n=================================================="
            Write-Host ">>> DEPLOYMENT COMPLETE! APP IS RUNNING ON DEVICE!"
            Write-Host "=================================================="
            exit 0
        }
    }
    Start-Sleep -Milliseconds 600
}

Write-Host "TIMEOUT: No device was connected within 15 minutes."
exit 1
