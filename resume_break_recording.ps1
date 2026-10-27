$adb = "C:\Users\dell\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$desktopDir = "C:\cdrive files\OneDrive\Desktop"

Write-Host "RESUME RUNNER: Waiting for phone connection..."
$startTime = Get-Date

while ((Get-Date) -lt $startTime.AddMinutes(5)) {
    $devLines = & $adb devices | Where-Object { $_ -match "^\s*(\S+)\s+device\b" }
    if ($devLines) {
        if ($devLines[0] -match "^\s*(\S+)\s+device\b") {
            $devId = $Matches[1].Trim()
            Write-Host "DEVICE CONNECTED: $devId"

            # Check if screenrecord is still running or restart it
            Write-Host "Ensuring screenrecord is recording on device..."
            $recProc = Start-Process -FilePath $adb -ArgumentList "-s", $devId, "shell", "screenrecord", "--size", "720x1600", "--bit-rate", "4000000", "/sdcard/break_test_recording_part2.mp4" -PassThru
            Start-Sleep -Seconds 2

            # SCENARIO 3: 25m Pomodoro (5m elapsed, 20:00 left - Unlocked 5m Rest)
            Write-Host "RUNNING SCENARIO 3: 25m Pomodoro at 20:00 left..."
            & $adb -s $devId shell am start -n com.focuscrew.app/.ui.MainActivity --ez start_preset_timer true --ei planned_minutes 25 --ei elapsed_minutes 5
            Start-Sleep -Seconds 3
            & $adb -s $devId shell screencap -p /sdcard/test3_25m_pomodoro_unlocked.png
            & $adb -s $devId pull /sdcard/test3_25m_pomodoro_unlocked.png "$desktopDir\test3_25m_pomodoro_unlocked.png"

            # SCENARIO 4: 50m Focus (10m elapsed, 40:00 left - Unlocked 10m Rest)
            Write-Host "RUNNING SCENARIO 4: 50m Focus at 40:00 left..."
            & $adb -s $devId shell am start -n com.focuscrew.app/.ui.MainActivity --ez start_preset_timer true --ei planned_minutes 50 --ei elapsed_minutes 10
            Start-Sleep -Seconds 3
            & $adb -s $devId shell screencap -p /sdcard/test4_50m_unlocked.png
            & $adb -s $devId pull /sdcard/test4_50m_unlocked.png "$desktopDir\test4_50m_unlocked.png"

            # SCENARIO 5: 90m Deep Work (15m elapsed, 75:00 left - Unlocked 15m Rest)
            Write-Host "RUNNING SCENARIO 5: 90m Deep Work at 75:00 left..."
            & $adb -s $devId shell am start -n com.focuscrew.app/.ui.MainActivity --ez start_preset_timer true --ei planned_minutes 90 --ei elapsed_minutes 15
            Start-Sleep -Seconds 3
            & $adb -s $devId shell screencap -p /sdcard/test5_90m_unlocked.png
            & $adb -s $devId pull /sdcard/test5_90m_unlocked.png "$desktopDir\test5_90m_unlocked.png"

            # Stop screen recording
            Write-Host "Stopping screen recording..."
            & $adb -s $devId shell "pkill -2 screenrecord"
            Start-Sleep -Seconds 3

            # Pull video files
            Write-Host "Pulling video recordings to Desktop..."
            & $adb -s $devId pull /sdcard/break_test_recording.mp4 "$desktopDir\break_test_recording.mp4"
            & $adb -s $devId pull /sdcard/break_test_recording_part2.mp4 "$desktopDir\break_test_recording_part2.mp4"

            Write-Host "ALL REMAINING SCENARIOS CAPTURED & SAVED TO DESKTOP!"
            exit 0
        }
    }
    Start-Sleep -Milliseconds 300
}

Write-Host "TIMEOUT"
