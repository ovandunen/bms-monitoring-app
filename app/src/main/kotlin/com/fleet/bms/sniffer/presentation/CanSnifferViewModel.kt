package com.fleet.bms.sniffer.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleet.bms.sniffer.domain.model.CanIdEntry
import com.fleet.bms.sniffer.domain.usecase.CanSniffingUseCase
import com.fleet.bms.sniffer.domain.usecase.DetectCanCandidatesUseCase
import com.fleet.bms.sniffer.service.CanSnifferService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CanSnifferViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sniffingUseCase: CanSniffingUseCase,
    private val detectCandidatesUseCase: DetectCanCandidatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CanSnifferUiState())
    val uiState: StateFlow<CanSnifferUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            sniffingUseCase.framesPerSecond.collect { fps ->
                _uiState.update { it.copy(framesPerSecond = fps) }
            }
        }
        viewModelScope.launch {
            sniffingUseCase.isSniffing.collect { sniffing ->
                _uiState.update { it.copy(isSniffing = sniffing) }
            }
        }
        viewModelScope.launch {
            sniffingUseCase.error.collect { err ->
                _uiState.update {
                    it.copy(
                        error = err,
                        isSniffing = false
                    )
                }
            }
        }
    }

    fun startSession() {
        val label = _uiState.value.sessionLabel.ifBlank { "session" }
        CanSnifferService.start(context, label)
        startRefreshLoop()
    }

    fun stopSession() {
        CanSnifferService.stop(context)
        refreshJob?.cancel()
        refreshJob = null
        updateEntries()
    }

    fun setSessionLabel(label: String) {
        _uiState.update { it.copy(sessionLabel = label) }
    }

    fun exportSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            sniffingUseCase.exportCurrentSession()
                .onSuccess { path ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportPath = path,
                            exportMessage = "Exported to $path"
                        )
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Export failed")
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportMessage = "Export failed: ${e.message}"
                        )
                    }
                }
        }
    }

    fun clearData() {
        sniffingUseCase.clearRegistry()
        _uiState.update {
            it.copy(
                entries = emptyList(),
                candidates = emptyList(),
                exportPath = null,
                exportMessage = null
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissExportMessage() {
        _uiState.update { it.copy(exportMessage = null) }
    }

    private fun startRefreshLoop() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(500)
                updateEntries()
            }
        }
    }

    private fun updateEntries() {
        val entries = sniffingUseCase.getEntries()
        val candidates = detectCandidatesUseCase.detectLikelyCandidates()
        _uiState.update {
            it.copy(
                entries = entries,
                candidates = candidates,
                uniqueIdCount = entries.size
            )
        }
    }
}

data class CanSnifferUiState(
    val framesPerSecond: Float = 0f,
    val uniqueIdCount: Int = 0,
    val entries: List<CanIdEntry> = emptyList(),
    val candidates: List<com.fleet.bms.sniffer.domain.model.CanIdCandidate> = emptyList(),
    val isSniffing: Boolean = false,
    val isExporting: Boolean = false,
    val sessionLabel: String = "baseline",
    val error: String? = null,
    val exportPath: String? = null,
    val exportMessage: String? = null
)
