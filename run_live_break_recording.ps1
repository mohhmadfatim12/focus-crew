$adb = "C:\Users\dell\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$desktopDir = "C:\cdrive files\OneDrive\Desktop"

Write-Host "=========================================================="
Write-Host "  FOCUSCREW 5-SCENARIO AUTOMATED BREAK TEST & SCREENRECORD"
Write-Host "=========================================================="

Write-Host "Waiting for device connection via ADB..."
$startTime = Get-Date

while ((Get-Date) -lt $startTime.AddMinutes(5)) {
    $devLines = @(& $adb devices | Where-Object { $_ -match "^\s*(\S+)\s+device\b" })
    if ($devLines.Count -gt 0) {
        if ($devLines[0] -match "^\s*(\S+)\s+device\b") {
            $devId = $Matches[1].Trim()
            Write-Host "DEVICE DETECTED: $devId"

            # Install fresh APK with navigation fix
            $apkPath = "c:\cdrive files\OneDrive\Desktop\ANORIDE APP\app\build\outputs\apk\debug\app-debug.apk"
            Write-Host "Installing latest APK ($apkPath) onto device $devId..."
            & $adb -s $devId install -r $apkPath
            Copy-Item $apkPath "$desktopDir\FocusCrew-v1.1-BreakFeature.apk" -Force
            Write-Host "Updated FocusCrew-v1.1-BreakFeature.apk copied to Desktop."

            # Kill any lingering screenrecord
            & $adb -s $devId shell "pkill -2 screenrecord"
            Start-Sleep -Seconds 1
            
            # 1. Start Android Screen Recording in background on device
            Write-Host "Starting Android live screen recording on device (/sdcard/break_test_recording.mp4)..."
            $recProcess = Start-Process -FilePath $adb -ArgumentList @("-s", $devId, "shell", "screenrecord", "--size", "720x1600", "--bit-rate", "4000000", "/sdcard/break_test_recording.mp4") -PassThru
            Start-Sleep -Seconds 2

        # ----------------------------------------------------
        # SCENARIO 1: 15-Minute Session (Must NOT have break button)
        # ----------------------------------------------------
        Write-Host "TEST 1/5: Launching 15m session (5m elapsed)..."
        & $adb -s $devId shell am start -W -n com.focuscrew.app/.ui.MainActivity --ez start_preset_timer true --ei planned_minutes 15 --ei elapsed_minutes 5
        Start-Sleep -Seconds 3
        & $adb -s $devId shell screencap -p /sdcard/test1_15m_no_break.png
        & $adb -s $devId pull /sdcard/test1_15m_no_break.png "$desktopDir\test1_15m_no_break.png"

        # ----------------------------------------------------
        # SCENARIO 2A: 20-Minute Session Before Threshold (18:00 left -> Locked Badge)
        # ----------------------------------------------------
        Write-Host "TEST 2/5 (Part A): Launching 20m session at 18:00 remaining (Locked Rest)..."
        & $adb -s $devId shell am start -W -n com.focuscrew.app/.ui.MainActivity --ez start_preset_timer true --ei planned_minutes 20 --ei elapsed_minutes 2
        Start-Sleep -Seconds 3
        & $adb -s $devId shell screencap -p /sdcard/test2a_20m_locked.png
        & $adb -s $devId pull /sdcard/test2a_20m_locked.png "$desktopDir\test2a_20m_locked.png"

        # ----------------------------------------------------
        # SCENARIO 2B: 20-Minute Session At Threshold (16:00 left -> Unlocked 5m Rest Button)
        # ----------------------------------------------------
        Write-Host "TEST 2/5 (Part B): Launching 20m session at 16:00 remaining (Unlocked 5m Rest Button)..."
        & $adb -s $devId shell am start -W -n com.focuscrew.app/.ui.MainActivity --ez start_preset_timer true --ei planned_minutes 20 --ei elapsed_minutes 4
        Start-Sleep -Seconds 3
        & $adb -s $devId shell screencap -p /sdcard/test2b_20m_unlocked.png
        & $adb -s $devId pull /sdcard/test2b_20m_unlocked.png "$desktopDir\test2b_20m_unlocked.png"

        # ----------------------------------------------------
        # SCENARIO 3: 25-Minute Pomodoro (20:00 left -> Unlocked 5m Rest Button)
        # ----------------------------------------------------
        Write-Host "TEST 3/5: Launching 25m Pomodoro at 20:00 remaining (Unlocked 5m Rest Button)..."
        & $adb -s $devId shell am start -W -n com.focuscrew.app/.ui.MainActivity --ez start_preset_timer true --ei planned_minutes 25 --ei elapsed_minutes 5
        Start-Sleep -Seconds 3
        & $adb -s $devId shell screencap -p /sdcard/test3_25m_pomodoro_unlocked.png
        & $adb -s $devId pull /sdcard/test3_25m_pomodoro_unlocked.png "$desktopDir\test3_25m_pomodoro_unlocked.png"

        # ----------------------------------------------------
        # SCENARIO 4: 50-Minute Standard Focus (40:00 left -> Unlocked 10m Rest Button)
        # ----------------------------------------------------
        Write-Host "TEST 4/5: Launching 50m Focus at 40:00 remaining (Unlocked 10m Rest Button)..."
        & $adb -s $devId shell am start -W -n com.focuscrew.app/.ui.MainActivity --ez start_preset_timer true --ei planned_minutes 50 --ei elapsed_minutes 10
        Start-Sleep -Seconds 3
        & $adb -s $devId shell screencap -p /sdcard/test4_50m_unlocked.png
        & $adb -s $devId pull /sdcard/test4_50m_unlocked.png "$desktopDir\test4_50m_unlocked.png"

        # ----------------------------------------------------
        # SCENARIO 5: 90-Minute Deep Work (75:00 left -> Unlocked 15m Rest Button)
        # ----------------------------------------------------
        Write-Host "TEST 5/5: Launching 90m Deep Work at 75:00 remaining (Unlocked 15m Rest Button)..."
        & $adb -s $devId shell am start -W -n com.focuscrew.app/.ui.MainActivity --ez start_preset_timer true --ei planned_minutes 90 --ei elapsed_minutes 15
        Start-Sleep -Seconds 3
        & $adb -s $devId shell screencap -p /sdcard/test5_90m_unlocked.png
        & $adb -s $devId pull /sdcard/test5_90m_unlocked.png "$desktopDir\test5_90m_unlocked.png"

        # Stop Screen Recording cleanly
        Write-Host "Stopping screen recording on device..."
        & $adb -s $devId shell "pkill -2 screenrecord"
        Start-Sleep -Seconds 3

        # Pull recorded MP4 video to physical desktop
        Write-Host "Pulling screen recording video to Desktop..."
        & $adb -s $devId pull /sdcard/break_test_recording.mp4 "$desktopDir\break_test_recording.mp4"

        Write-Host "=========================================================="
        Write-Host "ALL 5 SCENARIOS COMPLETED & RECORDED SUCCESSFULLY!"
        Write-Host "Video: $desktopDir\break_test_recording.mp4"
        Write-Host "Screenshots: $desktopDir\test*.png"
        Write-Host "=========================================================="
        exit 0
        }
    }
    Start-Sleep -Milliseconds 500
}

Write-Host "DEVICE_WAIT_TIMEOUT"
