package ph.edu.comteq.notetakingapp

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDAO {

    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    // 🔍 Search notes by title or content, sorted by last update
    @Query("""
        SELECT * FROM notes 
        WHERE title LIKE '%' || :searchQuery || '%' 
        OR content LIKE '%' || :searchQuery || '%' 
        ORDER BY updated_at DESC
    """)
    fun searchNotes(searchQuery: String): Flow<List<Note>>


    @Query("SELECT * FROM notes WHERE category = :category ORDER BY updated_at DESC")
    fun getNotesByCategory(category: String): Flow<List<Note>>


    @Query("SELECT DISTINCT category FROM notes WHERE category != '' ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Int): Tag?


    // Connect a note to a tag
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    // Disconnect a note from a tag
    @Delete
    suspend fun deleteNoteTagCrossRef(crossRef: NoteTagCrossRef)

    // Get a note with all its tags
    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteWithTags(noteId: Int): NoteWithTags?

    // Get all notes with their tags
    @Transaction
    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>>

    // Search notes (with tags)
    @Transaction
    @Query("""
        SELECT * FROM notes 
        WHERE title LIKE '%' || :searchQuery || '%' 
        OR content LIKE '%' || :searchQuery || '%' 
        ORDER BY updated_at DESC
    """)
    fun searchNotesWithTags(searchQuery: String): Flow<List<NoteWithTags>>

    // Get all notes that have a specific tag
    @Transaction
    @Query("""
        SELECT * FROM notes 
        INNER JOIN note_tag_cross_ref 
        ON notes.id = note_tag_cross_ref.note_id
        WHERE note_tag_cross_ref.tag_id = :tagId
        ORDER BY updated_at DESC
    """)
    fun getNotesWithTag(tagId: Int): Flow<List<Note>>
}
