package ar.jornadas.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Groups refresh taps made during one request into one following request. Called on the main thread. */
internal class RefreshQueue(
    private val scope: CoroutineScope,
    private val attempt: suspend () -> Unit,
    private val onFailure: (Exception) -> Unit,
) {
    private var active = false
    private var pending = false
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    fun solicitar() {
        if (active) {
            pending = true
            return
        }
        active = true
        _cargando.value = true
        scope.launch {
            try {
                do {
                    pending = false
                    try {
                        attempt()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        onFailure(error)
                    }
                } while (pending)
            } finally {
                pending = false
                active = false
                _cargando.value = false
            }
        }
    }
}
