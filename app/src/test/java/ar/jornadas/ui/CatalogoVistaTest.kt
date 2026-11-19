package ar.jornadas.ui

import ar.jornadas.data.Charla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogoVistaTest {
    private val charlaDiaUno = charla("11", 1, "Android")
    private val charlaDatosUno = charla("21", 2, "Datos")
    private val charlaDatosDos = charla("22", 2, "Datos")
    private val charlaCalidad = charla("23", 2, "Calidad")

    @Test
    fun dayAndTopicFiltersKeepOnlyMatchingTalks() {
        val vista = vistaCatalogo(
            listOf(charlaDiaUno, charlaDatosUno, charlaDatosDos, charlaCalidad),
            dia = 2, tema = "Datos", edicionGuardada = true, aviso = ""
        )

        assertEquals(listOf("21", "22"), vista.charlas.map { it.id })
        assertNull(vista.mensajeVacio)
    }

    @Test
    fun failedRefreshKeepsSavedTalksAndAnnouncesSavedData() {
        val vista = vistaCatalogo(
            listOf(charlaDatosUno), dia = 0, tema = "Todos",
            edicionGuardada = true, aviso = "No se pudo consultar el programa"
        )

        assertEquals(listOf("21"), vista.charlas.map { it.id })
        assertTrue(vista.mensajeError.orEmpty().contains("datos guardados"))
        assertTrue(vista.puedeReintentar)
    }

    @Test
    fun punctuatedFailureMessageHasOneSeparatorBeforeSavedDataNotice() {
        val vista = vistaCatalogo(
            listOf(charlaDatosUno), dia = 0, tema = "Todos", edicionGuardada = true,
            aviso = "No se pudo actualizar el programa."
        )

        assertEquals(
            "No se pudo actualizar el programa. Se están usando datos guardados.",
            vista.mensajeError
        )
    }

    @Test
    fun savedDataNoticeAppearsInDetailAndAgendaWithoutDuplicatingCatalogNotice() {
        val aviso = "No se pudo actualizar el programa."
        val esperado = "No se pudo actualizar el programa. Se están usando datos guardados."

        assertEquals(esperado, mensajeErrorPrograma(aviso, edicionGuardada = true))
        assertEquals(esperado, avisoFueraDelCatalogo("Programa", "11", aviso, true))
        assertEquals(esperado, avisoFueraDelCatalogo("Mi agenda", null, aviso, true))
        assertNull(avisoFueraDelCatalogo("Programa", null, aviso, true))
        assertEquals(
            esperado,
            vistaCatalogo(listOf(charlaDatosUno), 0, "Todos", true, aviso).mensajeError
        )
    }

    @Test
    fun failedFirstDownloadOffersRetryWithoutEmptyEditionMessage() {
        val vista = vistaCatalogo(
            emptyList(), dia = 0, tema = "Todos",
            edicionGuardada = false, aviso = "No se pudo consultar el programa"
        )

        assertTrue(vista.charlas.isEmpty())
        assertNull(vista.mensajeVacio)
        assertTrue(vista.puedeReintentar)
    }

    @Test
    fun savedEditionWithNoTalksIsAValidEmptyProgram() {
        val vista = vistaCatalogo(
            emptyList(), dia = 0, tema = "Todos", edicionGuardada = true, aviso = ""
        )

        assertEquals("sin charlas programadas", vista.mensajeVacio)
        assertFalse(vista.puedeReintentar)
    }

    @Test
    fun emptyFilterDoesNotClaimProgramHasNoTalks() {
        val vista = vistaCatalogo(
            listOf(charlaDiaUno), dia = 2, tema = "Datos", edicionGuardada = true, aviso = ""
        )

        assertEquals("No hay charlas para estos filtros.", vista.mensajeVacio)
    }

    private fun charla(id: String, dia: Int, tema: String) = Charla(
        id = id, titulo = "Charla $id", tema = tema, dia = dia,
        hora = "10:00", sala = "Sala", ponente = "Ponente", resumen = "Resumen"
    )
}
