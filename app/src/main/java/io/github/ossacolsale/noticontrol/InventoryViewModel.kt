package io.github.ossacolsale.noticontrol

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class InventoryState(
    val apps: List<AppEntry> = emptyList(),
    val listening: Boolean = false,
    val loading: Boolean = true,
    val error: Boolean = false,
    val filter: AppFilter = AppFilter.ALL,
    val showSystem: Boolean = false,
    val reverseOrder: Boolean = false,
)

internal class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val inventory = AndroidInventory(application)
    private val preferences = application.getSharedPreferences("inventory", 0)
    private val mutableState = MutableStateFlow(
        InventoryState(
            filter = AppFilter.entries.firstOrNull { it.name == preferences.getString("filter", null) }
                ?: AppFilter.ALL,
            showSystem = preferences.getBoolean("showSystem", false),
            reverseOrder = preferences.getBoolean("reverseOrder", false),
        ),
    )
    val state = mutableState.asStateFlow()
    private var scan: Job? = null

    init {
        viewModelScope.launch {
            ChannelListener.connection.collect { refresh() }
        }
    }

    fun refresh() {
        scan?.cancel()
        scan = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = false) }
            val listener = ChannelListener.connection.value
            val apps = withContext(Dispatchers.IO) {
                try {
                    inventory.read(listener)
                } catch (_: SecurityException) {
                    null
                }
            }
            mutableState.update {
                if (apps == null) it.copy(apps = emptyList(), listening = false, loading = false, error = true)
                else it.copy(apps = apps, listening = listener != null, loading = false, error = false)
            }
        }
    }

    fun setFilter(filter: AppFilter) {
        mutableState.update { it.copy(filter = filter) }
        preferences.edit().putString("filter", filter.name).apply()
    }

    fun toggleSystemApps() {
        val show = !state.value.showSystem
        mutableState.update { it.copy(showSystem = show) }
        preferences.edit().putBoolean("showSystem", show).apply()
    }

    fun toggleSort() {
        val reverse = !state.value.reverseOrder
        mutableState.update { it.copy(reverseOrder = reverse) }
        preferences.edit().putBoolean("reverseOrder", reverse).apply()
    }
}
