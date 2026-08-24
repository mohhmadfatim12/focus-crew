package com.focuscrew.app.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ApkShareHelper {

    /**
     * Extracts the running app's APK installer, saves it to the cache directory,
     * and launches the Android system share sheet so the user can send it directly
     * via WhatsApp, Telegram, Quick Share, or email to their friends.
     */
    fun shareApk(context: Context) {
        try {
            val appInfo = context.applicationInfo
            val sourceApk = File(appInfo.sourceDir)
            if (!sourceApk.exists()) {
                Toast.makeText(context, "APK file not found on device", Toast.LENGTH_SHORT).show()
                return
            }

            // Target export directory in app cache
            val exportDir = File(context.cacheDir, "apk")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val targetFile = File(exportDir, "FocusCrew_v1.0.0.apk")
            
            // Copy source APK to shareable cache location
            FileInputStream(sourceApk).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Generate content URI via FileProvider
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Here is FocusCrew — an ad-free, 100% offline focus timer & distraction blocker. Enjoy!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share FocusCrew Installer via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
