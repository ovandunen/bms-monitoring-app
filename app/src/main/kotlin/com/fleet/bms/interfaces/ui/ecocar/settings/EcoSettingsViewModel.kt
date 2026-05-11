package com.fleet.bms.interfaces.ui.ecocar.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleet.bms.infrastructure.android.preferences.BmsUserSettings
import com.fleet.bms.infrastructure.android.preferences.BmsSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EcoSettingsViewModel @Inject constructor(
    private val repository: BmsSettingsRepository,
) : ViewModel() {

    private val _settings = MutableStateFlow(BmsUserSettings.defaults())
    val settings: StateFlow<BmsUserSettings> = _settings.asStateFlow()

    private val _loadFailed = MutableStateFlow<String?>(null)
    val loadFailed: StateFlow<String?> = _loadFailed.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.load() }
                .onSuccess { _settings.value = it }
                .onFailure { e -> _loadFailed.value = e.message ?: "Laden fehlgeschlagen" }
        }
    }

    fun update(block: (BmsUserSettings) -> BmsUserSettings) {
        _settings.value = block(_settings.value)
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.save(_settings.value) }
                .onSuccess { onDone() }
                .onFailure { e -> _loadFailed.value = e.message ?: "Speichern fehlgeschlagen" }
        }
    }
}
