package io.github.ossacolsale.noticontrol

enum class GlobalStatus(val allowed: Boolean?) {
    ENABLED(true), BLOCKED(false), PERMISSION_GRANTED(true), PERMISSION_DENIED(false), UNKNOWN(null)
}

enum class AppStatus { PERMISSION_ONLY, ACTIVE, PARTIAL, BLOCKED, CHANNELS_BLOCKED, UNKNOWN }

enum class AppFilter {
    ALL, PERMITTED, ACTIVE, PARTIAL, DISABLED, UNKNOWN;

    fun matches(status: AppStatus) = when (this) {
        ALL -> true
        PERMITTED -> status == AppStatus.PERMISSION_ONLY
        ACTIVE -> status == AppStatus.ACTIVE
        PARTIAL -> status == AppStatus.PARTIAL
        DISABLED -> status == AppStatus.BLOCKED || status == AppStatus.CHANNELS_BLOCKED
        UNKNOWN -> status == AppStatus.UNKNOWN
    }
}

enum class SoundKind { SILENT, DEFAULT, CUSTOM }

data class SoundKey(val kind: SoundKind, val uri: String? = null)

data class SettingsTarget(val packageName: String, val channelId: String? = null)

data class ChannelEntry(
    val target: SettingsTarget,
    val name: String,
    val active: Boolean,
    val sound: SoundKey,
    val soundTitle: String? = null,
    val observed: Boolean = false,
)

data class AppEntry(
    val packageName: String,
    val name: String,
    val system: Boolean,
    val globalStatus: GlobalStatus,
    val channels: List<ChannelEntry>?,
    val observedOnly: Boolean = false,
    val appUnavailable: Boolean = false,
) {
    val target get() = SettingsTarget(packageName)
    val activeChannels = channels?.count { it.active }
    val status = when {
        globalStatus.allowed == false -> AppStatus.BLOCKED
        appUnavailable -> AppStatus.UNKNOWN
        observedOnly && globalStatus == GlobalStatus.PERMISSION_GRANTED -> AppStatus.PERMISSION_ONLY
        observedOnly -> AppStatus.UNKNOWN
        channels.isNullOrEmpty() -> AppStatus.UNKNOWN
        activeChannels == 0 -> AppStatus.CHANNELS_BLOCKED
        globalStatus.allowed != true -> AppStatus.UNKNOWN
        activeChannels == channels.size -> AppStatus.ACTIVE
        else -> AppStatus.PARTIAL
    }
}

data class SoundChannel(val appName: String, val channel: ChannelEntry)

fun soundGroups(apps: List<AppEntry>): Map<SoundKey, List<SoundChannel>> =
    apps.flatMap { app -> app.channels.orEmpty().map { SoundChannel(app.name, it) } }
        .groupBy { it.channel.sound }
        .toList()
        .sortedWith(compareBy({ it.first.kind }, { it.second.first().channel.soundTitle.orEmpty() }))
        .toMap()

fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

fun <T> selectedVisible(selected: Set<T>, visible: List<T>): List<T> =
    visible.distinct().filter { it in selected }

enum class OpenResult { SPECIFIC, FALLBACK, UNAVAILABLE }

data class SettingsQueue(
    val targets: List<SettingsTarget>,
    val processed: Int = 0,
    val specific: Int = 0,
    val fallback: Int = 0,
) {
    val next get() = targets.getOrNull(processed)
    val unavailable get() = processed - specific - fallback

    fun record(result: OpenResult): SettingsQueue = if (next == null) this else copy(
        processed = processed + 1,
        specific = specific + if (result == OpenResult.SPECIFIC) 1 else 0,
        fallback = fallback + if (result == OpenResult.FALLBACK) 1 else 0,
    )
}
