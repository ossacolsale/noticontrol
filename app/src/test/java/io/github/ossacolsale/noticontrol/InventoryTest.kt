package io.github.ossacolsale.noticontrol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InventoryTest {
    @Test
    fun classifiesGlobalBlockBeforeChannelState() {
        assertEquals(AppStatus.BLOCKED, app(GlobalStatus.BLOCKED, listOf(channel("a"))).status)
        assertEquals(AppStatus.BLOCKED, app(GlobalStatus.PERMISSION_DENIED, null).status)
    }

    @Test
    fun missingOrEmptyChannelsDoNotImplyThatNotificationsAreBlocked() {
        assertEquals(AppStatus.UNKNOWN, app(GlobalStatus.PERMISSION_GRANTED, null).status)
        assertEquals(AppStatus.UNKNOWN, app(GlobalStatus.ENABLED, emptyList()).status)
        assertEquals(AppStatus.UNKNOWN, app(GlobalStatus.UNKNOWN, listOf(channel("a"))).status)
    }

    @Test
    fun classifiesKnownChannelsByEffectiveState() {
        val active = channel("a")
        val disabled = channel("b", active = false)
        assertEquals(AppStatus.ACTIVE, app(GlobalStatus.ENABLED, listOf(active)).status)
        assertEquals(AppStatus.PARTIAL, app(GlobalStatus.ENABLED, listOf(active, disabled)).status)
        assertEquals(AppStatus.CHANNELS_BLOCKED, app(GlobalStatus.UNKNOWN, listOf(disabled)).status)
    }

    @Test
    fun bothBlockedStatesMatchTheDisabledFilter() {
        val matched = AppStatus.entries.filter(AppFilter.DISABLED::matches)
        assertEquals(listOf(AppStatus.BLOCKED, AppStatus.CHANNELS_BLOCKED), matched)
    }

    @Test
    fun selectionPreservesIdentityAcrossSortingAndExcludesHiddenTargets() {
        val selected = emptySet<String>().toggle("first").toggle("last").toggle("hidden")
        assertEquals(listOf("last", "first"), selectedVisible(selected, listOf("last", "other", "first")))
        assertEquals(listOf("last"), selectedVisible(selected.toggle("first"), listOf("last", "first")))
    }

    @Test
    fun soundGroupsUseUrisEvenWhenTitlesMatchOrCannotBeRead() {
        val firstUri = SoundKey(SoundKind.CUSTOM, "content://tones/1")
        val secondUri = SoundKey(SoundKind.CUSTOM, "content://tones/2")
        val first = channel("one").copy(sound = firstUri, soundTitle = "Pop")
        val second = channel("two").copy(sound = secondUri, soundTitle = "Pop")
        val third = channel("three").copy(sound = firstUri)
        val groups = soundGroups(listOf(app(GlobalStatus.ENABLED, listOf(first, second, third))))
        assertEquals(2, groups.size)
        assertEquals(listOf(first, third), groups.getValue(firstUri).map { it.channel })
        assertEquals(listOf(second), groups.getValue(secondUri).map { it.channel })
    }

    @Test
    fun guidedQueueRecordsOpenedScreensWithoutClaimingSettingsChanged() {
        val targets = listOf(SettingsTarget("one"), SettingsTarget("two", "channel"), SettingsTarget("removed"))
        val queue = SettingsQueue(targets)
        val afterFirst = queue.record(OpenResult.SPECIFIC)
        assertEquals(targets[1], afterFirst.next)
        val finished = afterFirst.record(OpenResult.FALLBACK).record(OpenResult.UNAVAILABLE)
        assertNull(finished.next)
        assertEquals(1, finished.specific)
        assertEquals(1, finished.fallback)
        assertEquals(1, finished.unavailable)
        assertEquals(finished, finished.record(OpenResult.SPECIFIC))
    }

    @Test
    fun observedChannelsDoNotProveTheWholeAppIsActiveOrBlocked() {
        val observed = app(GlobalStatus.PERMISSION_GRANTED, listOf(channel("known"))).copy(observedOnly = true)
        assertEquals(AppStatus.PERMISSION_ONLY, observed.status)
        assertEquals(AppStatus.PERMISSION_ONLY, observed.copy(channels = listOf(channel("off", false))).status)
        assertEquals(AppStatus.UNKNOWN, observed.copy(appUnavailable = true).status)
        assertEquals(AppStatus.BLOCKED, observed.copy(globalStatus = GlobalStatus.PERMISSION_DENIED).status)
    }

    private fun app(status: GlobalStatus, channels: List<ChannelEntry>?) =
        AppEntry("example.app", "Example", false, status, channels)

    private fun channel(id: String, active: Boolean = true) =
        ChannelEntry(SettingsTarget("example.app", id), id, active, SoundKey(SoundKind.SILENT))
}
