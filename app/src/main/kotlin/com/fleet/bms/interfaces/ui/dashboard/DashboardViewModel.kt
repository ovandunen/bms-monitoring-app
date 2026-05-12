package com.fleet.bms.interfaces.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleet.bms.application.usecase.*
import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.repository.ConnectionState
import com.fleet.bms.domain.repository.TelemetryPublisherPort
import com.fleet.bms.domain.service.BatteryAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel: Dashboard
 * 
 * Manages UI state for the battery monitoring dashboard.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val collectTelemetryUseCase: CollectTelemetryUseCase,
    private val publishTelemetryUseCase: PublishTelemetryUseCase,
    private val syncBufferedDataUseCase: SyncBufferedDataUseCase,
    private val startMonitoringUseCase: StartMonitoringUseCase,
    private val stopMonitoringUseCase: StopMonitoringUseCase,
    private val telemetryPublisher: TelemetryPublisherPort
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _socHistory = MutableStateFlow<List<Float>>(emptyList())
    val socHistory: StateFlow<List<Float>> = _socHistory.asStateFlow()

    private val _powerKwHistory = MutableStateFlow<List<Float>>(emptyList())
    val powerKwHistory: StateFlow<List<Float>> = _powerKwHistory.asStateFlow()
    
    private var isMonitoring = false
    
    /**
     * Start battery monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Timber.w("Already monitoring")
            return
        }
        
        viewModelScope.launch {
            Timber.i("Starting monitoring from ViewModel")
            
            // Initialize connections
            startMonitoringUseCase.execute()
                .onSuccess {
                    isMonitoring = true
                    _connectionState.value = ConnectionState.CONNECTED
                    collectTelemetryLoop()
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to start monitoring")
                    _uiState.value = DashboardUiState.Error(
                        error.message ?: "Failed to start monitoring"
                    )
                }
        }
    }
    
    /**
     * Stop battery monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        
        viewModelScope.launch {
            Timber.i("Stopping monitoring from ViewModel")
            stopMonitoringUseCase.execute()
            _connectionState.value = ConnectionState.DISCONNECTED
            _uiState.value = DashboardUiState.Loading
        }
    }
    
    /**
     * Sync buffered data to cloud
     */
    fun syncBufferedData() {
        viewModelScope.launch {
            syncBufferedDataUseCase.execute()
                .onSuccess { result ->
                    Timber.i("Synced ${result.synced} of ${result.total} buffered records")
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to sync buffered data")
                }
        }
    }
    
    /**
     * Main telemetry collection loop
     */
    private fun collectTelemetryLoop() {
        viewModelScope.launch {
            collectTelemetryUseCase.execute()
                .catch { error ->
                    Timber.e(error, "Error in telemetry collection")
                    _uiState.value = DashboardUiState.Error(
                        error.message ?: "Error collecting telemetry"
                    )
                }
                .collect { result ->
                    when (result) {
                        is TelemetryResult.Success -> {
                            handleTelemetrySuccess(result.telemetry, result.alerts)
                        }
                        is TelemetryResult.Partial -> {
                            _uiState.value = DashboardUiState.Collecting(result.percentage)
                        }
                        is TelemetryResult.Error -> {
                            Timber.e(result.error, "Telemetry collection error")
                        }
                    }
                }
        }
    }
    
    /**
     * Handle successful telemetry collection
     */
    private fun handleTelemetrySuccess(
        telemetry: BatteryTelemetry,
        alerts: List<BatteryAlert>
    ) {
        appendTelemetrySamples(telemetry)
        // Update UI
        _uiState.value = DashboardUiState.Success(
            telemetry = telemetry,
            alerts = alerts
        )
        
        // Update connection state
        _connectionState.value = if (telemetryPublisher.isConnected()) {
            ConnectionState.CONNECTED
        } else {
            ConnectionState.DISCONNECTED
        }
        
        // Publish to cloud (or buffer if offline)
        viewModelScope.launch {
            publishTelemetryUseCase.execute(telemetry)
                .onSuccess { publishResult ->
                    when (publishResult) {
                        is PublishResult.Published -> {
                            Timber.d("Telemetry published successfully")
                        }
                        is PublishResult.Buffered -> {
                            Timber.d("Telemetry buffered (${publishResult.bufferSize} total)")
                        }
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to publish/buffer telemetry")
                }
        }
    }

    private fun appendTelemetrySamples(telemetry: BatteryTelemetry) {
        val soc = telemetry.stateOfCharge.value.toFloat().coerceIn(0f, 100f)
        val kw = (telemetry.power.value / 1000.0).toFloat()
        _socHistory.update { (it + soc).takeLast(MAX_TELEMETRY_SAMPLES) }
        _powerKwHistory.update { (it + kw).takeLast(MAX_TELEMETRY_SAMPLES) }
    }
    
    override fun onCleared() {
        super.onCleared()
        if (isMonitoring) {
            viewModelScope.launch {
                stopMonitoringUseCase.execute()
            }
        }
    }

    private companion object {
        const val MAX_TELEMETRY_SAMPLES = 72
    }
}

/**
 * Dashboard UI State
 */
sealed class DashboardUiState {
    object Loading : DashboardUiState()
    
    data class Collecting(val percentage: Float) : DashboardUiState()
    
    data class Success(
        val telemetry: BatteryTelemetry,
        val alerts: List<BatteryAlert>
    ) : DashboardUiState()
    
    data class Error(val message: String) : DashboardUiState()
}
