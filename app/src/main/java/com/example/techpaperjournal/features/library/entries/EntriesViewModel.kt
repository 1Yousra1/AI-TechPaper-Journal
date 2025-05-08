package com.example.techpaperjournal.features.library.entries

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techpaperjournal.core.model.Entry
import com.example.techpaperjournal.core.model.Paper
import com.example.techpaperjournal.core.repository.EntryRepository
import com.example.techpaperjournal.core.repository.PaperRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class EntryListUiState(
    val entries: List<Entry> = emptyList(),
    val entriesWithPapers: List<EntryWithPaper> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean? = null,
    val errorMessage: String? = null
)

data class EntryWithPaper(
    val entry: Entry,
    val paper: Paper
)

class EntriesViewModel : ViewModel() {
    private val _entryListState = MutableLiveData(EntryListUiState())
    val entryListState: LiveData<EntryListUiState> = _entryListState
    private val entryRepository = EntryRepository()
    private val paperRepository = PaperRepository()

    // Add a new entry
    suspend fun addEntry(paperID: String?): String? {
        _entryListState.value = _entryListState.value?.copy(isLoading = true, isUploading = true, uploadSuccess = null)
        return try {
            val entryID = entryRepository.addEntry(paperID)
            _entryListState.value = if (entryID != null) {
                _entryListState.value?.copy(
                    isUploading = false,
                    uploadSuccess = true
                )
            } else {
                throw Exception("Upload failed")
            }
            entryID
        } catch (e: Exception) {
            _entryListState.value = _entryListState.value?.copy(
                isUploading = false,
                uploadSuccess = false,
                errorMessage = "Upload failed"
            )
            null
        }
    }

    // Fetch entries with their associated papers
    fun fetchEntriesAndPapers() {
        viewModelScope.launch {
            try {
                combine(
                    entryRepository.getEntries(),
                    paperRepository.getPapers()
                ) { entries, papers ->
                    _entryListState.value = _entryListState.value?.copy(entries = entries)
                    val entriesWithPapers = entries.mapNotNull { entry ->
                        papers.find { it.paperID == entry.paperID }?.let { paper ->
                            EntryWithPaper(entry, paper)

                        }

                    }
                    entriesWithPapers
                }.collect { entriesWithPapers ->
                    _entryListState.value = _entryListState.value?.copy(
                        entriesWithPapers = entriesWithPapers,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _entryListState.value = _entryListState.value?.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
                Log.e("ViewModel", "Failed to fetch entries: ${e.message}")
            }
        }
    }

    // Fetch a specific entry and its associated paper
    fun fetchEntryAndPaper(entryId: String) {
        viewModelScope.launch {
            try {
                entryRepository.getEntry(entryId).collect { entry ->
                    if (entry != null) {
                        paperRepository.getPaper(entry.paperID).collect { paper ->
                            if (paper != null) {
                                _entryListState.value = _entryListState.value?.copy(
                                    entriesWithPapers = listOf(EntryWithPaper(entry, paper)),
                                    isLoading = false,
                                    errorMessage = null
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _entryListState.value = _entryListState.value?.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    // Update an entry's last updated date
    fun updateEntryLastUpdated(entryId: String) {
        viewModelScope.launch {
            try {
                entryRepository.updateEntryLastUpdated(entryId)
                entryRepository.getEntry(entryId).collect { entry ->
                    entry?.let {
                        val updatedEntries = _entryListState.value?.entries?.map { e ->
                            if (e.entryID == entryId) it else e
                        } ?: emptyList()

                        _entryListState.value = _entryListState.value?.copy(entries = updatedEntries)
                    }
                }
            } catch (e: Exception) {
                _entryListState.value = _entryListState.value?.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    // Delete a entry
    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            try {
                entryRepository.deleteEntry(entryId)
                _entryListState.value = _entryListState.value?.copy(
                    entries = _entryListState.value?.entries?.filter { it.entryID != entryId } ?: emptyList(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _entryListState.value = _entryListState.value?.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    // Delete a papers entry
    fun deletePapersEntry(paperId: String) {
        viewModelScope.launch {
            try {
                val entryId = entryRepository.deletePapersEntry(paperId)
                _entryListState.value = _entryListState.value?.copy(
                    entries = _entryListState.value?.entries?.filter { it.entryID != entryId } ?: emptyList(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _entryListState.value = _entryListState.value?.copy(
                    errorMessage = e.message
                )
            }
        }
    }
}