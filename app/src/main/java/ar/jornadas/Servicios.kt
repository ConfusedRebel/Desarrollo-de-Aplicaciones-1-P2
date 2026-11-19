package ar.jornadas

import android.content.Context
import androidx.room.Room
import ar.jornadas.data.*
import ar.jornadas.lab.ServicioLocal
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Servicios(context: Context) {
    private val recursos = obtenerRecursos(context.applicationContext)
    val mesa: ServicioLocal get() = recursos.mesa
    val agenda: AgendaRepository get() = recursos.agenda

    private class Recursos(context: Context) {
        val mesa = ServicioLocal(context)
        private val archivo = Room.databaseBuilder(context, Archivo::class.java, "cartelera.db")
            .addMigrations(Archivo.MIGRACION_1_2)
            .build()
        private val api = Retrofit.Builder().baseUrl("https://programa.invalid/")
            .client(OkHttpClient.Builder().addInterceptor(mesa).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(ProgramaApi::class.java)
        val agenda = AgendaRepository(api, archivo.cartelera())
    }

    companion object {
        @Volatile private var compartidos: Recursos? = null

        private fun obtenerRecursos(context: Context): Recursos = compartidos ?: synchronized(this) {
            compartidos ?: Recursos(context).also { compartidos = it }
        }
    }
}
