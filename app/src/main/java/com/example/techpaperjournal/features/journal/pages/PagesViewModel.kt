package com.example.techpaperjournal.features.journal.pages

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techpaperjournal.core.model.Page
import com.example.techpaperjournal.core.repository.PageRepository
import kotlinx.coroutines.launch

data class PageUIState(
    val pages: List<Page> = emptyList(),
    val page: Page? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class PagesViewModel: ViewModel() {
    private val _uiState = MutableLiveData(PageUIState())
    val uiState: LiveData<PageUIState> = _uiState
    private val pageRepository = PageRepository()

    /** --------------- Adding Pages --------------- **/

    // Add a new page
    fun addPage(pageMap: MutableMap<String, Any?>) {
        Log.d("PagesViewModel", "Adding page: $pageMap")
        viewModelScope.launch {
            _uiState.value = PageUIState(loading = true)
            try {
                pageRepository.addPage(pageMap)
                _uiState.value = PageUIState(
                    loading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = PageUIState(
                    error = e.message,
                    loading = false
                )
            }

        }
    }

    /** --------------- Fetching Pages --------------- **/

    // Fetch all pages in an entry
    fun fetchPages(entryId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = PageUIState(loading = true)
                pageRepository.fetchPages(entryId).collect { pages ->
                    _uiState.value = PageUIState(
                        pages = pages,
                        loading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = PageUIState(
                    error = e.message,
                    loading = false
                )
            }
        }
    }

    // Fetch a specific page
    fun fetchPage(pageId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = PageUIState(loading = true)
                pageRepository.fetchPage(pageId).collect { page ->
                    _uiState.value = PageUIState(
                        page = page,
                        loading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = PageUIState(
                    error = e.message,
                    loading = false
                )
            }
        }
    }

    /** --------------- Modifying Pages --------------- **/

    // Save a page's notes
    fun saveNote(pageId: String, notes: String) {
        viewModelScope.launch {
            try {
                _uiState.value = PageUIState(loading = true)
                pageRepository.saveNote(pageId, notes)
                _uiState.value = PageUIState(
                    loading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = PageUIState(
                    error = e.message,
                    loading = false
                )
            }
        }
    }

    // Update a page's title
    fun updatePageTitle(pageId: String, title: String) {
        viewModelScope.launch {
            try {
                _uiState.value = PageUIState(loading = true)
                pageRepository.updatePageTitle(pageId, title)
                _uiState.value = PageUIState(
                    loading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = PageUIState(
                    error = e.message,
                    loading = false
                )
            }
        }
    }

    /** --------------- Deleting Pages --------------- **/

    // Delete all pages in an entry
    fun deletePages(entryId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = PageUIState(loading = true)
                pageRepository.deletePages(entryId)
                _uiState.value = PageUIState(
                    loading = false,
                    error = null
                )
        } catch (e: Exception) {
                _uiState.value = PageUIState(
                    error = e.message,
                    loading = false
                )
            }
        }
    }

    // Delete a page
    fun deletePage(pageId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = PageUIState(loading = true)
                pageRepository.deletePage(pageId)
                _uiState.value = PageUIState(
                    loading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = PageUIState(
                    error = e.message,
                    loading = false
                )
            }
        }
    }
}