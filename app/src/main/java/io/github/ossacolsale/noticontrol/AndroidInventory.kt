package io.github.ossacolsale.noticontrol

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import java.text.Collator

internal class CapabilityDetector(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun globalStatus(info: PackageInfo): GlobalStatus = when {
        info.packageName == context.packageName ->
            if (manager.areNotificationsEnabled()) GlobalStatus.ENABLED else GlobalStatus.BLOCKED
        Build.VERSION.SDK_INT < 33 -> GlobalStatus.UNKNOWN
        Manifest.permission.POST_NOTIFICATIONS !in info.requestedPermissions.orEmpty() ->
            GlobalStatus.UNKNOWN
        context.packageManager.checkPermission(
            Manifest.permission.POST_NOTIFICATIONS,
            info.packageName,
        ) == PackageManager.PERMISSION_GRANTED -> GlobalStatus.PERMISSION_GRANTED
        else -> GlobalStatus.PERMISSION_DENIED
    }

    fun ownChannels(packageName: String): List<ChannelEntry>? {
        if (packageName != context.packageName) return null
        val blockedGroups = if (Build.VERSION.SDK_INT >= 28) {
            manager.notificationChannelGroups.filter { it.isBlocked }.map { it.id }.toSet()
        } else {
            emptySet()
        }
        return manager.notificationChannels.map { channel -> entry(packageName, channel, channel.group in blockedGroups) }
    }

    fun entry(packageName: String, channel: NotificationChannel, groupBlocked: Boolean?): ChannelEntry {
        val sound = readSound(channel)
        return ChannelEntry(
            target = SettingsTarget(packageName, channel.id),
            name = channel.name?.toString().orEmpty().ifBlank { channel.id },
            active = channel.importance > NotificationManager.IMPORTANCE_NONE &&
                groupBlocked != true,
            sound = sound.first,
            soundTitle = sound.second,
            observed = groupBlocked == null,
        )
    }

    private fun readSound(channel: NotificationChannel): Pair<SoundKey, String?> {
        val uri = channel.sound?.normalizeScheme() ?: return SoundKey(SoundKind.SILENT) to null
        if (uri == RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) {
            return SoundKey(SoundKind.DEFAULT) to null
        }
        val title = if (uri.scheme == "content" && uri.authority == "media") readSoundTitle(uri) else null
        return SoundKey(SoundKind.CUSTOM, uri.toString()) to title
    }

    private fun readSoundTitle(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
            }?.takeIf { it.isNotBlank() }
    } catch (_: RuntimeException) {
        null
    }
}

internal class AndroidInventory(private val context: Context) {
    private val capabilities = CapabilityDetector(context)

    @Suppress("DEPRECATION")
    fun read(listener: ChannelListener?): List<AppEntry> {
        val packages = context.packageManager
        val installed = if (Build.VERSION.SDK_INT >= 33) {
            packages.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            packages.getInstalledPackages(0)
        }
        val observed = listener?.channels().orEmpty()
        val collator = Collator.getInstance(context.resources.configuration.locales[0])
        return installed.mapNotNull { info ->
            val application = info.applicationInfo ?: return@mapNotNull null
            val name = try {
                application.loadLabel(packages).toString().trim().ifBlank { info.packageName }
            } catch (_: android.content.res.Resources.NotFoundException) {
                info.packageName
            }
            val status = try {
                capabilities.globalStatus(info)
            } catch (_: SecurityException) {
                GlobalStatus.UNKNOWN
            }
            val channels = try {
                capabilities.ownChannels(info.packageName)
            } catch (_: SecurityException) {
                null
            }
            AppEntry(
                packageName = info.packageName,
                name = name,
                system = application.flags and
                    (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                globalStatus = status,
                channels = channels ?: observed[info.packageName]?.map { channel ->
                    capabilities.entry(info.packageName, channel, groupBlocked = null)
                },
                observedOnly = info.packageName != context.packageName,
                appUnavailable = !application.enabled || application.flags and ApplicationInfo.FLAG_SUSPENDED != 0 ||
                    try {
                        packages.getApplicationEnabledSetting(info.packageName) ==
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
                    } catch (_: IllegalArgumentException) {
                        true
                    },
            )
        }.sortedWith { first, second ->
            collator.compare(first.name, second.name).takeIf { it != 0 }
                ?: first.packageName.compareTo(second.packageName)
        }
    }
}

internal fun Activity.openNativeSettings(target: SettingsTarget): OpenResult {
    val installed = try {
        @Suppress("DEPRECATION")
        packageManager.getApplicationInfo(target.packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
    if (!installed) return OpenResult.UNAVAILABLE

    if (target.channelId != null && tryOpen(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, target.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, target.channelId),
        )
    ) return OpenResult.SPECIFIC

    if (tryOpen(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, target.packageName),
        )
    ) return if (target.channelId == null) OpenResult.SPECIFIC else OpenResult.FALLBACK

    return if (tryOpen(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", target.packageName, null)),
        )
    ) OpenResult.FALLBACK else OpenResult.UNAVAILABLE
}

private fun Activity.tryOpen(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}
