package io.github.ossacolsale.noticontrol

import android.app.NotificationChannel
import android.os.Process
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChannelListener : NotificationListenerService() {
    companion object {
        private val connected = MutableStateFlow<ChannelListener?>(null)
        internal val connection = connected.asStateFlow()
    }

    override fun onListenerConnected() {
        connected.value = this
    }

    override fun onListenerDisconnected() {
        connected.compareAndSet(this, null)
    }

    override fun onDestroy() {
        connected.compareAndSet(this, null)
        super.onDestroy()
    }

    // Called only by an inventory refresh. Use the envelope for identity and
    // the ranking for channel settings. Never inspect Notification or its extras.
    internal fun channels(): Map<String, List<NotificationChannel>> {
        val result = mutableMapOf<String, MutableMap<String, NotificationChannel>>()
        try {
            val rankingMap = currentRanking ?: return emptyMap()
            val ranking = Ranking()
            for (item in activeNotifications.orEmpty()) {
                if (item.user != Process.myUserHandle() || !rankingMap.getRanking(item.key, ranking)) continue
                val channel = ranking.channel ?: continue
                result.getOrPut(item.packageName) { mutableMapOf() }[channel.id] = channel
            }
        } catch (_: SecurityException) {
            return emptyMap()
        }
        return result.mapValues { it.value.values.toList() }
    }
}
