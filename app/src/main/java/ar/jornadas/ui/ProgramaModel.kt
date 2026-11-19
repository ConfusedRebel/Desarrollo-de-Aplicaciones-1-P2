package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgramaModel(app: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(app) {
    companion object {
        private var cargaAutomaticaSolicitada = false
    }

    private val servicios = Servicios(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val edicionGuardada = repo.edicionGuardada.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val dia = savedStateHandle.getStateFlow("dia", 0)
    val tema = savedStateHandle.getStateFlow("tema", "Todos")
    val aviso = MutableStateFlow("")
    private val cola = RefreshQueue(viewModelScope,
        attempt = {
            aviso.value = ""
            repo.sincronizar()
        },
        onFailure = { aviso.value = "No se pudo actualizar el programa." })
    val cargando = cola.cargando

    fun elegirDia(nuevoDia: Int) { savedStateHandle["dia"] = nuevoDia }
    fun elegirTema(nuevoTema: String) { savedStateHandle["tema"] = nuevoTema }

    fun cargarAlIniciar() {
        if (!cargaAutomaticaSolicitada) {
            cargaAutomaticaSolicitada = true
            recargar()
        }
    }

    fun recargar() = cola.solicitar()

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
