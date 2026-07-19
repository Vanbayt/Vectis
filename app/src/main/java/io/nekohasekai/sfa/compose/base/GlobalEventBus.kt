package io.nekohasekai.sfa.compose.base

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Global event bus that aggregates events from all ViewModels.
 * This allows ComposeActivity to handle all events in a centralized manner.
 */
object GlobalEventBus {
    private val _events = Channel<UiEvent>(Channel.BUFFERED)

    val events: Flow<UiEvent> = _events.receiveAsFlow()

    /**
     * Emit an event to the global event bus.
     * This should be called by ViewModels to send events that need global handling.
     */
    suspend fun emit(event: UiEvent) {
        _events.send(event)
    }

    /**
     * Try to emit an event without suspending.
     * Returns true if the event was emitted successfully.
     */
    fun tryEmit(event: UiEvent): Boolean = _events.trySend(event).isSuccess
}
