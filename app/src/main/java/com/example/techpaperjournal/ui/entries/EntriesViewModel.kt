package com.example.techpaperjournal.ui.entries

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techpaperjournal.data.model.Entry
import com.example.techpaperjournal.data.repository.EntryRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

data class EntryUiState (
    val entries: List<Entry> = emptyList(),
    val title: String = "",
    val lastUpdated: Timestamp = Timestamp.now(),
    val topics: List<String> = emptyList(),
    val summary: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class EntriesViewModel : ViewModel() {
    private val _uiState = MutableLiveData<EntryUiState>()
    val uiState: LiveData<EntryUiState> = _uiState
    private val entryRepository = EntryRepository()

    // Fetch all entries
    fun fetchEntries() {
        _uiState.value = EntryUiState(isLoading = true)

        viewModelScope.launch {
            try {
                /*entryRepository.getEntries().collect { entries ->
                    _uiState.value = _uiState.value?.copy(
                        entries = entries,
                        isLoading = false
                    )
                }*/

            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    errorMessage = e.message,
                    isLoading = false
                )
            }
        }
    }

    // Fetch a specific entry
    fun fetchEntry(entryId: String) {
        _uiState.value = EntryUiState(isLoading = true)

        viewModelScope.launch {
            try {
                /*val entry = entryRepository.getEntry(entryId)
                _uiState.value = EntryUiState(entries = listOf(entry))*/
            } catch (e: Exception) {
                _uiState.value = EntryUiState(errorMessage = e.message)
            }
        }
    }

    // Add a new entry
    fun uploadEntry(entry: Entry) {

    }

    // Update an existing entry
    fun updateEntry(entry: Entry) {

    }

    // Delete a entry
    fun deleteEntry(entryId: String) {

    }
}