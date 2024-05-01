package com.example.skyllsync

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class StopWatchViewModel: ViewModel(){

    private val _elapsedTime = MutableStateFlow(0L)
    private val _timerState = MutableStateFlow(TimerState.RESET)
    val timerState = _timerState.asStateFlow()
    //drink notification management
    var showDrinkNotification = MutableLiveData<Boolean>(false)
    private var lastNotificationTime = 0L
    //start and stop training time for mobile
    private var _trainingDuration = MutableLiveData<Long>(0L)
    val trainingDuration: LiveData<Long>
        get() = _trainingDuration

    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    @RequiresApi(Build.VERSION_CODES.O)
    val stopWatchText = _elapsedTime
        .map { millis ->
            LocalTime.ofNanoOfDay(millis * 1_000_000).format(formatter)
        }
        .stateIn( //cash this value and converts it into a state flow
            viewModelScope,
            SharingStarted.WhileSubscribed(5000), //map block will be updated 5s after the last collector disappears
            "00:00:00.000"
        )

    init {
        showDrinkNotification.value = false

        _timerState.flatMapLatest { //whenever _timerState changes, this block will be executed
            getTimerFlow(it == TimerState.RUNNING)
        }
            .onEach { timeDiff ->
                _elapsedTime.update { it + timeDiff }
            }
            .launchIn(viewModelScope)
    }

    fun toggleIsRunning() {
        when(_timerState.value) {
            TimerState.RUNNING -> _timerState.update { TimerState.PAUSED }
            TimerState.PAUSED,
            TimerState.RESET -> {_timerState.update { TimerState.RUNNING } }
        }
    }

    fun resetTimer() {
        _trainingDuration.value = _elapsedTime.value
        _elapsedTime.update { 0L }
        _timerState.update { TimerState.RESET }
        lastNotificationTime = 0L
    }


    private fun getTimerFlow(isRunning: Boolean): Flow<Long> {
        return flow {
            var startMillis = System.currentTimeMillis()
            while(isRunning) {
                val currentMillis = System.currentTimeMillis()
                //always calculate the time this iteration took rather than assuming 10ms
                val timeDiff = if(currentMillis > startMillis) {
                    currentMillis - startMillis
                } else {
                    0L
                }
                emit(timeDiff)
                startMillis = System.currentTimeMillis()
                delay(500L)

                // Check if 15 min have passed since the last notification
                if (_elapsedTime.value - lastNotificationTime >= 900000L) {
                    // Set flag to true to show notification
                    showDrinkNotification.value = true
                    // Update the last notification time
                    lastNotificationTime = _elapsedTime.value
                }
            }
        }
    }
}