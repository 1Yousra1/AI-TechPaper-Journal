package com.example.techpaperjournal.features.library.papers

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techpaperjournal.core.model.Paper
import com.example.techpaperjournal.core.repository.PaperRepository
import kotlinx.coroutines.launch

data class PaperUiState (
    val papers: List<Paper> = emptyList(),
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PapersViewModel : ViewModel() {
    private val _paperListState = MutableLiveData(PaperUiState())
    val paperListState: LiveData<PaperUiState> = _paperListState
    private val paperRepository = PaperRepository()

    // Fetch all papers
    fun fetchPapers() {
        _paperListState.value = _paperListState.value?.copy(isLoading = true)
        viewModelScope.launch {
            try {
                paperRepository.getPapers().collect { papers ->
                    _paperListState.value = _paperListState.value?.copy(
                        papers = papers,
                        isLoading = false,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                _paperListState.value = _paperListState.value?.copy(
                    errorMessage = e.message,
                    isLoading = false
                )
            }
        }
    }

    // Upload a new paper
    fun uploadPaper(pdfUri: Uri, metadata: Map<String, String?>) {
        _paperListState.value = _paperListState.value?.copy(isUploading = true, uploadSuccess = null)
        viewModelScope.launch {
            val success = paperRepository.addPaper(pdfUri, metadata)
            _paperListState.value = _paperListState.value?.copy(
                isUploading = false,
                uploadSuccess = success,
                errorMessage = if (!success) "Upload failed" else null
            )
        }
    }

    // Check if paper already exists
    suspend fun isDuplicatePaper(paperId: String): Boolean {
        return paperRepository.isDuplicatePaper(paperId)
    }

    // Update an existing paper
    fun updatePaper(paperId: String, paperDetails: Map<String, Any?>) {
        viewModelScope.launch {
            try {
                paperRepository.updatePaper(paperId, paperDetails)
                fetchPapers()
            } catch (e: Exception) {
                _paperListState.value = _paperListState.value?.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    // Delete a paper
    fun deletePaper(paperId: String) {
        viewModelScope.launch {
            try {
                paperRepository.deletePaper(paperId)
                fetchPapers()
            } catch (e: Exception) {
                _paperListState.value = _paperListState.value?.copy(
                    errorMessage = e.message
                )
            }
        }
    }
}