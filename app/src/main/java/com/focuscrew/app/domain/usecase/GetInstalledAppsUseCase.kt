package com.focuscrew.app.domain.usecase

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.focuscrew.app.data.model.InstalledAppItem
import com.focuscrew.app.service.BlockerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetInstalledAppsUseCase(private val context: Context) {

    private val safeSystemExemptions = setOf(
        "com.focuscrew.app",
        "com.android.settings",
        "com.android.systemui",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.phone",
        "com.google.android.emergency",
        "com.android.emergency"
    )

    suspend fun execute(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
        val blockedSet = BlockerManager.blockedPackages.value

        val appMap = mutableMapOf<String, InstalledAppItem>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (safeSystemExemptions.contains(pkg)) {
                continue
            }

            if (!appMap.containsKey(pkg)) {
                val label = resolveInfo.loadLabel(packageManager).toString()
                val icon = try {
                    resolveInfo.loadIcon(packageManager)
                } catch (e: Exception) {
                    null
                }

                appMap[pkg] = InstalledAppItem(
                    packageName = pkg,
                    appName = label,
                    icon = icon,
                    isBlocked = blockedSet.contains(pkg)
                )
            }
        }

        appMap.values.sortedBy { it.appName.lowercase() }
    }
}
