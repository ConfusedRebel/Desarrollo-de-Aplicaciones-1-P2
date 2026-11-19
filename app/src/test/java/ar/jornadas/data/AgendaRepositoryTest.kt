package ar.jornadas.data

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AgendaRepositoryTest {
    @Test
    fun `changed metadata keeps the favorite of a stable id`() = runBlocking {
        val dao = MemoryCarteleraDao(
            Charla("11", "Compose en producción", "Android", 1, "10:00", "Sala Centro", "Ana", "Resumen", true),
        )
        val repository = AgendaRepository(
            object : ProgramaApi {
                override suspend fun consultar() =
                    listOf(ficha("11", "Compose en producción avanzada", "Sala Puerto"))
            },
            dao,
        )

        repository.sincronizar()

        assertEquals(
            listOf(Charla("11", "Compose en producción avanzada", "Android", 1, "10:00", "Sala Puerto", "Ana", "Resumen", true)),
            dao.observar().value,
        )
    }

    @Test
    fun `retired talk disappears and returns without its old mark`() = runBlocking {
        val dao = MemoryCarteleraDao(
            Charla("11", "Old talk", "Android", 1, "10:00", "Sala Centro", "Ana", "Resumen", true),
            Charla("12", "Other talk", "Android", 1, "11:00", "Sala Centro", "Ana", "Resumen", true),
        )
        var snapshot = listOf(ficha("12", "Other talk", "Sala Centro"), ficha("90", "New talk", "Sala Puerto"))
        val repository = AgendaRepository(object : ProgramaApi {
            override suspend fun consultar() = snapshot
        }, dao)

        repository.sincronizar()
        assertEquals(setOf("12", "90"), dao.observar().value.map(Charla::id).toSet())
        assertTrue(dao.observar().value.single { it.id == "12" }.favorita)
        assertFalse(dao.observar().value.single { it.id == "90" }.favorita)

        snapshot = snapshot + ficha("11", "Talk returns", "Sala Sur")
        repository.sincronizar()
        assertFalse(dao.observar().value.single { it.id == "11" }.favorita)
    }

    @Test
    fun `mark changed while request is pending survives the response`() = runBlocking {
        for (originallyFavorite in listOf(false, true)) {
            val dao = MemoryCarteleraDao(
                Charla("11", "Old talk", "Android", 1, "10:00", "Sala Centro", "Ana", "Resumen", originallyFavorite),
            )
            val started = CompletableDeferred<Unit>()
            val response = CompletableDeferred<List<FichaRemota>>()
            val repository = AgendaRepository(object : ProgramaApi {
                override suspend fun consultar(): List<FichaRemota> {
                    started.complete(Unit)
                    return response.await()
                }
            }, dao)

            val request = async { repository.sincronizar() }
            started.await()
            repository.cambiarFavorita("11")
            response.complete(listOf(ficha("11", "Updated talk", "Sala Puerto")))
            request.await()

            assertEquals(!originallyFavorite, dao.observar().value.single().favorita)
            assertEquals("Updated talk", dao.observar().value.single().titulo)
        }
    }

    @Test
    fun `empty successful snapshot is saved and clears old talks`() = runBlocking {
        val dao = MemoryCarteleraDao(
            Charla("11", "Old talk", "Android", 1, "10:00", "Sala Centro", "Ana", "Resumen", true),
        )
        val repository = AgendaRepository(object : ProgramaApi {
            override suspend fun consultar(): List<FichaRemota> = emptyList()
        }, dao)

        assertFalse(repository.edicionGuardada.first())
        repository.sincronizar()

        assertTrue(dao.observar().value.isEmpty())
        assertTrue(repository.edicionGuardada.first())
    }

    @Test
    fun `failed request leaves saved talks and saved state intact`() = runBlocking {
        val dao = MemoryCarteleraDao(
            Charla("11", "Old talk", "Android", 1, "10:00", "Sala Centro", "Ana", "Resumen", true),
            savedInitially = true,
        )
        val repository = AgendaRepository(object : ProgramaApi {
            override suspend fun consultar(): List<FichaRemota> = throw IOException("offline")
        }, dao)

        try {
            repository.sincronizar()
            fail("Expected the service failure to reach the caller")
        } catch (_: IOException) {
            assertEquals("Old talk", dao.observar().value.single().titulo)
            assertTrue(dao.observar().value.single().favorita)
            assertTrue(repository.edicionGuardada.first())
        }
    }

    private fun ficha(id: String, titulo: String, sala: String) =
        FichaRemota(id, titulo, "Android", 1, "10:00", sala, "Ana", "Resumen")
}

private class MemoryCarteleraDao(vararg initial: Charla, savedInitially: Boolean = false) : CarteleraDao {
    private val rows = MutableStateFlow(initial.toList())
    private val saved = MutableStateFlow(savedInitially)

    override fun observar(): MutableStateFlow<List<Charla>> = rows
    override fun observarEdicionGuardada(): MutableStateFlow<Boolean> = saved

    override suspend fun idsFavoritos(): List<String> = rows.value.filter(Charla::favorita).map(Charla::id)

    override suspend fun borrar() {
        rows.value = emptyList()
    }

    override suspend fun guardar(fichas: List<Charla>) {
        rows.value = (rows.value.filterNot { existing -> fichas.any { it.id == existing.id } } + fichas)
            .sortedWith(compareBy(Charla::dia, Charla::hora, Charla::sala))
    }

    override suspend fun alternar(id: String) {
        rows.value = rows.value.map { if (it.id == id) it.copy(favorita = !it.favorita) else it }
    }

    override suspend fun registrarEdicionGuardada(edicion: EdicionGuardada) {
        saved.value = true
    }
}
