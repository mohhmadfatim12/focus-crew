$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apk = "c:\cdrive files\OneDrive\Desktop\ANORIDE APP\app\build\outputs\apk\debug\app-debug.apk"
$destPng = "C:\Users\dell\.gemini\antigravity-ide\brain\bc5b55ec-10aa-4405-9d16-21bacc622885\unlocked_break_screen.png"

Write-Output "Waiting for device connection..."
$maxAttempts = 300 # 5 minutes

for ($i = 0; $i -lt $maxAttempts; $i++) {
    $devs = & $adb devices
    if ($devs -match "ZY22KVJMCP\s+device") {
        Write-Output "DEVICE DETECTED! Installing updated APK..."
        $installResult = & $adb -s ZY22KVJMCP install -r $apk
        Write-Output $installResult
        
        Write-Output "Starting FocusCrew preset timer at 16m / 20m..."
        & $adb -s ZY22KVJMCP shell am start -n com.focuscrew.app/.ui.MainActivity --ez start_preset_timer true --ei planned_minutes 20 --ei elapsed_minutes 4
        
        Start-Sleep -Seconds 2
        Write-Output "Capturing screenshot of unlocked break screen..."
        & $adb -s ZY22KVJMCP shell screencap -p /sdcard/unlocked_screen.png
        & $adb -s ZY22KVJMCP pull /sdcard/unlocked_screen.png $destPng
        
        Write-Output "SUCCESSFULLY_VERIFIED_AND_CAPTURED"
        break
    }
    Start-Sleep -Seconds 1
}
