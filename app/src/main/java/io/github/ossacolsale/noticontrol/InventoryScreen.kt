package io.github.ossacolsale.noticontrol

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Section { APPS, SOUNDS }

@Composable
internal fun InventoryScreen(
    model: InventoryViewModel,
    openSettings: (SettingsTarget) -> OpenResult,
) {
    val state by model.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    var section by rememberSaveable { mutableStateOf(Section.APPS) }
    var search by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<SettingsTarget>()) }
    var expanded by remember { mutableStateOf(emptySet<SoundKey>()) }
    var preview by remember { mutableStateOf(emptyList<SettingsTarget>()) }
    var queue by remember { mutableStateOf<SettingsQueue?>(null) }

    val scope = remember(state.apps, state.showSystem, state.reverseOrder) {
        state.apps.filter { state.showSystem || !it.system }
            .let { if (state.reverseOrder) it.asReversed() else it }
    }
    val searched = remember(scope, search) {
        val term = search.trim()
        scope.filter { it.name.contains(term, true) || it.packageName.contains(term, true) }
    }
    val visibleApps = remember(searched, state.filter) { searched.filter { state.filter.matches(it.status) } }
    val currentApp = scope.firstOrNull { it.packageName == detail }
    val groups = remember(scope) { soundGroups(scope) }
    val visibleTargets = remember(detail, currentApp, section, visibleApps, groups, expanded) {
        when {
            detail != null -> currentApp?.channels.orEmpty().map { it.target }
            section == Section.APPS -> visibleApps.map { it.target }
            else -> groups.filterKeys { it in expanded }.values.flatten().map { it.channel.target }
        }
    }
    val selection = selectedVisible(selected, visibleTargets)

    fun stopSelection() {
        selected = emptySet()
        selecting = false
    }

    fun toggleSelection(target: SettingsTarget) {
        keyboard?.hide()
        selecting = true
        selected = selected.toggle(target)
    }

    fun openNext(current: SettingsQueue) {
        val target = current.next ?: return
        queue = current.record(openSettings(target))
    }

    fun openOne(target: SettingsTarget) = openNext(SettingsQueue(listOf(target)))

    BackHandler(selecting || detail != null || section != Section.APPS || queue != null) {
        when {
            queue != null -> queue = null
            selecting -> stopSelection()
            detail != null -> detail = null
            else -> section = Section.APPS
        }
    }

    Scaffold(
        bottomBar = {
            val activeQueue = queue
            when {
                activeQueue != null -> QueueBar(activeQueue, { openNext(activeQueue) }, { queue = null })
                selecting -> SelectionBar(
                    selection.size,
                    visibleTargets.isNotEmpty() && selection.size != visibleTargets.size,
                    selectAll = { selected = selected + visibleTargets },
                    close = ::stopSelection,
                    open = {
                        if (selection.size == 1) openOne(selection.single())
                        else preview = selection
                    },
                )
                detail == null -> NavigationBar {
                    NavigationBarItem(
                        selected = section == Section.APPS,
                        onClick = { section = Section.APPS },
                        icon = { Icon(painterResource(R.drawable.ic_apps), contentDescription = null) },
                        label = { Text(stringResource(R.string.apps)) },
                    )
                    NavigationBarItem(
                        selected = section == Section.SOUNDS,
                        onClick = {
                            keyboard?.hide()
                            section = Section.SOUNDS
                        },
                        icon = { Icon(painterResource(R.drawable.ic_sounds), contentDescription = null) },
                        label = { Text(stringResource(R.string.sounds)) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            InventoryHeader(
                title = currentApp?.name ?: stringResource(
                    when {
                        detail != null -> R.string.app_unavailable
                        section == Section.APPS -> R.string.app_name
                        else -> R.string.sounds
                    },
                ),
                state = state,
                back = if (detail != null) {
                    {
                        stopSelection()
                        detail = null
                    }
                } else null,
                startSelection = if (!selecting && visibleTargets.isNotEmpty() && queue == null) {
                    {
                        keyboard?.hide()
                        selecting = true
                    }
                } else null,
                model = model,
            )

            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.error) Note(R.string.scan_error, error = true)

            when {
                detail != null -> if (currentApp != null) {
                    AppDetail(currentApp, selecting, selected, ::toggleSelection, ::openOne)
                }
                section == Section.APPS -> {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text(stringResource(R.string.search_apps)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    )
                    AppFilters(searched, state.filter, model::setFilter)
                    if (visibleApps.isEmpty() && !state.loading && !state.error) Note(R.string.empty_apps)
                    LazyColumn {
                        items(visibleApps, key = { it.packageName }) { app ->
                            EntryRow(
                                name = app.name,
                                subtitle = appSubtitle(app),
                                selecting = selecting,
                                selected = app.target in selected,
                                leading = { AppIcon(app.packageName) },
                                click = {
                                    if (selecting) toggleSelection(app.target) else {
                                        keyboard?.hide()
                                        detail = app.packageName
                                    }
                                },
                                longClick = { toggleSelection(app.target) },
                            )
                        }
                    }
                }
                else -> SoundsList(
                    groups, expanded, selecting, selected,
                    showEmpty = !state.loading && !state.error,
                    expand = { expanded = expanded.toggle(it) },
                    open = ::openOne,
                    select = ::toggleSelection,
                )
            }
        }
    }

    if (preview.isNotEmpty()) {
        val appCount = preview.map { it.packageName }.distinct().size
        val channelCount = preview.count { it.channelId != null }
        AlertDialog(
            onDismissRequest = { preview = emptyList() },
            title = { Text(stringResource(R.string.guided_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.guided_count, appCount, channelCount))
                    Text(stringResource(R.string.guided_description))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val targets = preview
                    preview = emptyList()
                    openNext(SettingsQueue(targets))
                }) { Text(stringResource(R.string.open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { preview = emptyList() }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun InventoryHeader(
    title: String,
    state: InventoryState,
    back: (() -> Unit)?,
    startSelection: (() -> Unit)?,
    model: InventoryViewModel,
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(painterResource(R.drawable.ic_more), stringResource(R.string.options))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (state.showSystem) R.string.hide_system else R.string.show_system)) },
                        onClick = {
                            menu = false
                            model.toggleSystemApps()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(if (state.reverseOrder) R.string.sort_asc else R.string.sort_desc)) },
                        onClick = {
                            menu = false
                            model.toggleSort()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.refresh)) },
                        onClick = {
                            menu = false
                            model.refresh()
                        },
                    )
                }
            }
        }
        if (back != null || startSelection != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (back != null) TextButton(onClick = back) { Text(stringResource(R.string.back_to_apps)) }
                if (startSelection != null) {
                    TextButton(onClick = startSelection) { Text(stringResource(R.string.select)) }
                }
            }
        }
    }
}

@Composable
private fun AppFilters(apps: List<AppEntry>, current: AppFilter, onFilter: (AppFilter) -> Unit) {
    val counts = remember(apps) { AppFilter.entries.associateWith { filter -> apps.count { filter.matches(it.status) } } }
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(AppFilter.entries) { filter ->
            val label = stringResource(
                when (filter) {
                    AppFilter.ALL -> R.string.filter_all
                    AppFilter.ACTIVE -> R.string.status_active
                    AppFilter.PARTIAL -> R.string.filter_partial
                    AppFilter.DISABLED -> R.string.filter_disabled
                    AppFilter.UNKNOWN -> R.string.status_unknown
                },
            )
            FilterChip(
                selected = filter == current,
                onClick = { onFilter(filter) },
                label = { Text(stringResource(R.string.counted_label, label, counts.getValue(filter))) },
            )
        }
    }
}

@Composable
private fun AppDetail(
    app: AppEntry,
    selecting: Boolean,
    selected: Set<SettingsTarget>,
    select: (SettingsTarget) -> Unit,
    open: (SettingsTarget) -> Unit,
) {
    LazyColumn {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(app.packageName, style = MaterialTheme.typography.labelMedium)
                Text(appSubtitle(app))
                TextButton(onClick = { open(app.target) }) { Text(stringResource(R.string.open_app_settings)) }
                when {
                    app.channels == null -> Text(stringResource(R.string.channels_unavailable))
                    app.channels.isEmpty() -> Text(stringResource(R.string.no_channels))
                }
            }
        }
        items(app.channels.orEmpty(), key = { it.target.channelId!! }) { channel ->
            ChannelRow(channel.name, channel, selecting, selected, select, open)
        }
    }
}

@Composable
private fun SoundsList(
    groups: Map<SoundKey, List<SoundChannel>>,
    expanded: Set<SoundKey>,
    selecting: Boolean,
    selected: Set<SettingsTarget>,
    showEmpty: Boolean,
    expand: (SoundKey) -> Unit,
    open: (SettingsTarget) -> Unit,
    select: (SettingsTarget) -> Unit,
) {
    LazyColumn {
        if (groups.isEmpty() && showEmpty) {
            item { Note(R.string.sounds_unavailable) }
        }
        groups.forEach { (sound, channels) ->
            item {
                val title = soundTitle(sound, channels.firstNotNullOfOrNull { it.channel.soundTitle })
                val count = pluralStringResource(R.plurals.channel_count, channels.size, channels.size)
                TextButton(onClick = { expand(sound) }, Modifier.fillMaxWidth()) {
                    Text(stringResource(if (sound in expanded) R.string.collapse_group else R.string.expand_group, title, count))
                }
                HorizontalDivider()
            }
            if (sound in expanded) {
                items(channels, key = { it.channel.target.packageName + "/" + it.channel.target.channelId }) { item ->
                    ChannelRow(
                        stringResource(R.string.channel_in_app, item.channel.name, item.appName),
                        item.channel, selecting, selected, select, open,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    name: String,
    channel: ChannelEntry,
    selecting: Boolean,
    selected: Set<SettingsTarget>,
    select: (SettingsTarget) -> Unit,
    open: (SettingsTarget) -> Unit,
) {
    val status = stringResource(if (channel.active) R.string.channel_active else R.string.channel_blocked)
    EntryRow(
        name, stringResource(R.string.join_details, status, soundTitle(channel.sound, channel.soundTitle)),
        selecting, channel.target in selected,
        click = { if (selecting) select(channel.target) else open(channel.target) },
        longClick = { select(channel.target) },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    name: String,
    subtitle: String,
    selecting: Boolean,
    selected: Boolean,
    click: () -> Unit,
    longClick: () -> Unit,
    leading: @Composable () -> Unit = {},
) {
    val interaction = if (selecting) {
        Modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { click() })
    } else {
        Modifier.combinedClickable(onClick = click, onLongClick = longClick)
    }
    Surface(color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth()
                .then(interaction)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selecting) Checkbox(checked = selected, onCheckedChange = null)
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun AppIcon(packageName: String) {
    val packages = LocalContext.current.applicationContext.packageManager
    val bitmap by produceState<ImageBitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { packages.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun SelectionBar(count: Int, canSelectAll: Boolean, selectAll: () -> Unit, close: () -> Unit, open: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(pluralStringResource(R.plurals.selected_count, count, count), Modifier.padding(horizontal = 4.dp))
                TextButton(onClick = selectAll, enabled = canSelectAll) { Text(stringResource(R.string.select_visible)) }
                TextButton(onClick = close) { Text(stringResource(R.string.close)) }
            }
            Button(onClick = open, enabled = count > 0, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}

@Composable
private fun QueueBar(queue: SettingsQueue, next: () -> Unit, close: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.queue_progress, queue.processed, queue.targets.size))
            if (queue.fallback > 0 || queue.unavailable > 0) {
                Text(stringResource(R.string.queue_outcomes, queue.fallback, queue.unavailable))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (queue.next != null) Button(onClick = next) { Text(stringResource(R.string.next_settings)) }
                TextButton(onClick = close) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

@Composable
private fun Note(@StringRes text: Int, error: Boolean = false) {
    Text(
        stringResource(text), Modifier.padding(16.dp),
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun appSubtitle(app: AppEntry): String {
    val status = stringResource(
        when (app.status) {
            AppStatus.ACTIVE -> R.string.status_active
            AppStatus.PARTIAL -> R.string.status_partial
            AppStatus.BLOCKED -> R.string.status_blocked
            AppStatus.CHANNELS_BLOCKED -> R.string.status_channels_blocked
            AppStatus.UNKNOWN -> R.string.status_unknown
        },
    )
    val permissionText = when (app.globalStatus) {
        GlobalStatus.PERMISSION_GRANTED -> stringResource(R.string.permission_allowed)
        GlobalStatus.PERMISSION_DENIED -> stringResource(R.string.permission_denied)
        else -> null
    }
    val channels = app.channels
    if (channels.isNullOrEmpty()) {
        return permissionText?.let { stringResource(R.string.join_details, status, it) } ?: status
    }
    val counts = stringResource(R.string.channel_summary, app.activeChannels!!, channels.size)
    val sounds = channels.map { it.sound }.distinct()
    val sound = if (sounds.size > 1) stringResource(R.string.mixed_sounds)
    else soundTitle(sounds.single(), channels.firstNotNullOfOrNull { it.soundTitle })
    return listOfNotNull(status, permissionText, counts, sound)
        .reduce { first, second -> stringResource(R.string.join_details, first, second) }
}

@Composable
private fun soundTitle(sound: SoundKey, title: String?) = when (sound.kind) {
    SoundKind.SILENT -> stringResource(R.string.sound_silent)
    SoundKind.DEFAULT -> stringResource(R.string.sound_default)
    SoundKind.CUSTOM -> title ?: stringResource(R.string.sound_unknown)
}
