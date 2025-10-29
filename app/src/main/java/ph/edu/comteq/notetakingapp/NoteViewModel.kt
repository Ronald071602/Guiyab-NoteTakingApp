package ph.edu.comteq.notetakingapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteDAO: NoteDAO = AppDatabase.getDatabase(application).noteDAO()

    // Track what the user is searching for
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // NEW: Category filter
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Smart notes: shows all notes OR search results
    val allNotes: Flow<List<Note>> = searchQuery.flatMapLatest { query ->
        val category = _selectedCategory.value

        when {
            // Both search and category
            query.isNotBlank() && category != null -> {
                // We'll need to add this query if you want both filters
                noteDAO.searchNotes(query)  // For now, just search
            }
            // Just search
            query.isNotBlank() -> noteDAO.searchNotes(query)
            // Just category
            category != null -> noteDAO.getNotesByCategory(category)
            // No filters
            else -> noteDAO.getAllNotes()
        }
    }

    // NEW: All notes WITH their tags
    val allNotesWithTags: Flow<List<NoteWithTags>> = noteDAO.getAllNotesWithTags()

    // NEW: All available categories
    val allCategories: Flow<List<String>> = noteDAO.getAllCategories()

    // NEW: All available tags
    val allTags: Flow<List<Tag>> = noteDAO.getAllTags()

    // Call this when user types in search box
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Call this to clear the search
    fun clearSearch() {
        _searchQuery.value = ""
    }

    // NEW: Filter by category
    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun clearCategoryFilter() {
        _selectedCategory.value = null
    }

    fun insert(note: Note) = viewModelScope.launch {
        noteDAO.insertNote(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        // Update the updatedAt timestamp
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        noteDAO.updateNote(updatedNote)
    }

    fun delete(note: Note) = viewModelScope.launch {
        noteDAO.deleteNote(note)
    }

    suspend fun getNoteById(id: Int): Note? {
        return noteDAO.getNoteById(id)
    }

    suspend fun getNoteWithTags(noteId: Int): NoteWithTags? {
        return noteDAO.getNoteWithTags(noteId)
    }



    fun insertTag(tag: Tag) = viewModelScope.launch {
        noteDAO.insertTag(tag)
    }

    fun updateTag(tag: Tag) = viewModelScope.launch {
        noteDAO.updateTag(tag)
    }

    fun deleteTag(tag: Tag) = viewModelScope.launch {
        noteDAO.deleteTag(tag)
    }


    // Add a tag to a note
    fun addTagToNote(noteId: Int, tagId: Int) = viewModelScope.launch {
        noteDAO.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
    }

    // Remove a tag from a note
    fun removeTagFromNote(noteId: Int, tagId: Int) = viewModelScope.launch {
        noteDAO.deleteNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
    }

    // Get all notes that have a specific tag
    fun getNotesWithTag(tagId: Int): Flow<List<Note>> {
        return noteDAO.getNotesWithTag(tagId)
    }

    // Create a note and attach selected tags
    fun addNoteWithTags(
        title: String,
        content: String,
        category: String,
        selectedTagIds: List<Int>
    ) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val note = Note(
            title = title,
            content = content,
            category = category,
            createdAt = now,
            updatedAt = now
        )
        val newId = noteDAO.insertNote(note).toInt()
        selectedTagIds.forEach { tagId ->
            noteDAO.insertNoteTagCrossRef(NoteTagCrossRef(newId, tagId))
        }
    }
}