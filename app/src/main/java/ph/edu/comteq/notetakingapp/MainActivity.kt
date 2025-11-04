package ph.edu.comteq.notetakingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ph.edu.comteq.notetakingapp.ui.theme.NoteTakingAppTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteTakingAppTheme {
                var searchQuery by remember { mutableStateOf("") }
                var isSearchActive by remember { mutableStateOf(false) }
                val notes by viewModel.allNotes.collectAsState(initial = emptyList())

                var showAddDialog by remember { mutableStateOf(false) }
                var editingNoteId by remember { mutableStateOf<Int?>(null) }
                var noteToDelete by remember { mutableStateOf<Note?>(null) }
                val scope = rememberCoroutineScope()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (isSearchActive) {
                            // 🔍 Search Mode
                            SearchBar(
                                modifier = Modifier.fillMaxWidth(),
                                inputField = {
                                    SearchBarDefaults.InputField(
                                        query = searchQuery,
                                        onQueryChange = {
                                            searchQuery = it
                                            viewModel.updateSearchQuery(it)
                                        },
                                        onSearch = {},
                                        expanded = true,
                                        onExpandedChange = {
                                            if (!it) {
                                                isSearchActive = false
                                                searchQuery = ""
                                                viewModel.clearSearch()
                                            }
                                        },
                                        placeholder = { Text("Search notes") },
                                        leadingIcon = {
                                            IconButton(
                                                onClick = {
                                                    isSearchActive = false
                                                    searchQuery = ""
                                                    viewModel.clearSearch()
                                                }
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Close Search"
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(
                                                    onClick = {
                                                        searchQuery = ""
                                                        viewModel.clearSearch()
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Clear,
                                                        contentDescription = "Clear search"
                                                    )
                                                }
                                            }
                                        }
                                    )
                                },
                                expanded = true,
                                onExpandedChange = {
                                    if (!it) {
                                        isSearchActive = false
                                        searchQuery = ""
                                        viewModel.clearSearch()
                                    }
                                }
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    if (notes.isEmpty()) {
                                        item {
                                            Text(
                                                text = "No notes found",
                                                modifier = Modifier.padding(16.dp),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    } else {
                                        items(notes) { note ->
                                            NoteCard(
                                                note = note,
                                                onEditClick = {
                                                    editingNoteId = note.id
                                                    isSearchActive = false
                                                    searchQuery = ""
                                                    viewModel.clearSearch()
                                                    showAddDialog = true
                                                },
                                                onDeleteClick = {
                                                    noteToDelete = note
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            TopAppBar(
                                title = { Text("Notes") },
                                actions = {
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Filled.Search, contentDescription = "Search")
                                    }
                                }
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add note")
                        }
                    }
                ) { innerPadding ->
                    NoteListScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        onEditClick = { note ->
                            editingNoteId = note.id
                            showAddDialog = true
                        },
                        onDeleteClick = { note ->
                            noteToDelete = note
                        }
                    )
                }

                if (showAddDialog) {
                    AddNoteDialog(
                        viewModel = viewModel,
                        noteId = editingNoteId,
                        onDismiss = {
                            showAddDialog = false
                            editingNoteId = null
                        },
                        onSaved = {
                            showAddDialog = false
                            editingNoteId = null
                        }
                    )
                }

                // Delete confirmation dialog
                noteToDelete?.let { note ->
                    AlertDialog(
                        onDismissRequest = { noteToDelete = null },
                        title = { Text("Delete Note") },
                        text = { Text("Are you sure you want to delete \"${note.title}\"?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.delete(note)
                                    }
                                    noteToDelete = null
                                }
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { noteToDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier,
    onEditClick: (Note) -> Unit = {},
    onDeleteClick: (Note) -> Unit = {}
) {
    val notesWithTags by viewModel.allNotesWithTags.collectAsState(initial = emptyList())

    LazyColumn(modifier = modifier) {
        items(notesWithTags) { note ->
            NoteCard(
                note = note.note,
                tags = note.tags,
                onEditClick = { onEditClick(note.note) },
                onDeleteClick = { onDeleteClick(note.note) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: Note,
    tags: List<Tag> = emptyList(),
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onEditClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = DateUtils.formatDate(note.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row {
                    if (note.category.isNotBlank()) {
                        AssistChip(onClick = { }, label = { Text(note.category) })
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Text(
                text = note.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // tags
            if(tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag.name) },
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddNoteDialog(
    viewModel: NoteViewModel,
    noteId: Int? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val allTags by viewModel.allTags.collectAsState(initial = emptyList())
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedTagIds by remember { mutableStateOf(setOf<Int>()) }

    val isEditMode = noteId != null

    // Load note data when editing
    LaunchedEffect(noteId) {
        if (noteId != null) {
            val noteWithTags = viewModel.getNoteWithTags(noteId)
            if (noteWithTags != null) {
                title = noteWithTags.note.title
                content = noteWithTags.note.content
                category = noteWithTags.note.category
                selectedTagIds = noteWithTags.tags.map { it.id }.toSet()
            }
        } else {
            // Reset when switching to add mode
            title = ""
            content = ""
            category = ""
            selectedTagIds = emptySet()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && content.isNotBlank(),
                onClick = {
                    if (isEditMode && noteId != null) {
                        scope.launch {
                            viewModel.updateNoteWithTags(
                                noteId = noteId,
                                title = title.trim(),
                                content = content.trim(),
                                category = category.trim(),
                                selectedTagIds = selectedTagIds.toList()
                            )
                        }
                    } else {
                        viewModel.addNoteWithTags(
                            title = title.trim(),
                            content = content.trim(),
                            category = category.trim(),
                            selectedTagIds = selectedTagIds.toList()
                        )
                    }
                    onSaved()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(text = if (isEditMode) "Edit Note" else "Add Note") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("e.g., Personal, Work") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (categories.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow {
                        categories.forEach { cat ->
                            AssistChip(
                                onClick = { category = cat },
                                label = { Text(cat) },
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
                if (allTags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = "Tags", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    FlowRow {
                        allTags.forEach { tag ->
                            val selected = tag.id in selectedTagIds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedTagIds = if (selected) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                },
                                label = { Text(tag.name) },
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}
