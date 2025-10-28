package ph.edu.comteq.notetakingapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Note::class,
        Tag::class,               // NEW: Tag entity
        NoteTagCrossRef::class    // NEW: Junction table
    ],
    version = 2,                 // IMPORTANT: upgraded from version 1 → 2
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDAO(): NoteDAO

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to notes table (if they don't already exist)
                database.execSQL("ALTER TABLE notes ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE notes ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")

                // Update existing notes with created_at timestamp as updated_at
                database.execSQL("UPDATE notes SET updated_at = created_at")

                // Create tags table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color TEXT NOT NULL DEFAULT '#6200EE'
                    )
                """)

                // Create note-tag cross reference table (many-to-many)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS note_tag_cross_ref (
                        note_id INTEGER NOT NULL,
                        tag_id INTEGER NOT NULL,
                        PRIMARY KEY(note_id, tag_id)
                    )
                """)
            }
        }


        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes_database"
                )
                    .addMigrations(MIGRATION_1_2)  // Apply migration
                    .build()
                this.instance = instance
                instance
            }
        }
    }
}
