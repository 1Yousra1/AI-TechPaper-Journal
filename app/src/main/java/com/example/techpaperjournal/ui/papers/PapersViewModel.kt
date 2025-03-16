package com.example.techpaperjournal.ui.papers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techpaperjournal.data.model.Paper
import com.example.techpaperjournal.data.repository.PaperRepository
import kotlinx.coroutines.launch

data class PaperUiState (
    val papers: List<Paper> = emptyList(),
    val title: String = "",
    val authors: String = "",
    val topics: List<String> = emptyList(),
    val pageCount: Int = 0,
    val summary: String = "",
    val pdfUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PapersViewModel : ViewModel() {
    private val _uiState = MutableLiveData<PaperUiState>(PaperUiState())
    val uiState: LiveData<PaperUiState> = _uiState
    private val paperRepository = PaperRepository()

    // Fetch all papers
    fun fetchPapers() {
        _uiState.value = _uiState.value?.copy(isLoading = true)

        viewModelScope.launch {
            try {
                /*paperRepository.getPapers().collect { papers ->
                    _uiState.value = _uiState.value?.copy(
                        papers = papers,
                        isLoading = false,
                        errorMessage = null
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

    // Fetch a specific paper
    fun fetchPaper(paperId: String) {
        _uiState.value = PaperUiState(isLoading = true)

        viewModelScope.launch {
            try {
                /*val paper = paperRepository.getPaper(paperId)
                _uiState.value = PaperUiState(papers = listOf(paper))*/
            } catch (e: Exception) {
                _uiState.value = PaperUiState(errorMessage = e.message)
            }
        }
    }

    // Add a new paper
    fun uploadPaper(paper: Paper) {

    }

    // Update an existing paper
    fun updatePaper(paper: Paper) {

    }

    // Delete a paper
    fun deletePaper(paperId: String) {

    }
}