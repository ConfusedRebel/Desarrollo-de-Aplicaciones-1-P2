package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

@Entity(tableName = "edicion_guardada")
data class EdicionGuardada(@PrimaryKey val id: Int = 1)

@Dao
interface CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    fun observar(): Flow<List<Charla>>

    @Query("SELECT EXISTS(SELECT 1 FROM edicion_guardada WHERE id = 1)")
    fun observarEdicionGuardada(): Flow<Boolean>

    @Query("SELECT id FROM charlas WHERE favorita = 1")
    suspend fun idsFavoritos(): List<String>

    @Query("DELETE FROM charlas")
    suspend fun borrar()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(fichas: List<Charla>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun registrarEdicionGuardada(edicion: EdicionGuardada)

    @Transaction
    suspend fun reemplazarEdicion(fichas: List<Charla>) {
        val favoritas = idsFavoritos().toSet()
        borrar()
        guardar(fichas.map { it.copy(favorita = it.id in favoritas) })
        registrarEdicionGuardada(EdicionGuardada())
    }

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    suspend fun alternar(id: String)
}

@Database(entities = [Charla::class, EdicionGuardada::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao

    companion object {
        val MIGRACION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `edicion_guardada` (`id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO `edicion_guardada` (`id`) SELECT 1 WHERE EXISTS (SELECT 1 FROM `charlas`)")
            }
        }
    }
}
