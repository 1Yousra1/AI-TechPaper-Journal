package com.example.techpaperjournal.ui.home

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techpaperjournal.data.model.Paper
import com.example.techpaperjournal.data.repository.PaperRepository
import com.example.techpaperjournal.ui.papers.PaperItemUiState
import kotlinx.coroutines.launch

data class HomeUiState(
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean? = null,
    val errorMessage: String? = null,
    val papers: List<Paper> = emptyList(),
    val paperItemState: PaperItemUiState? = null
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> get() = _uiState
    private val paperRepository = PaperRepository()

    // Upload a PDF file
    fun uploadPaper(pdfUri: Uri, metadata: Map<String, String?>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isUploading = true, uploadSuccess = null, errorMessage = null)
            val success = paperRepository.addPaper(pdfUri, metadata)
            _uiState.value = if (success) {
                _uiState.value?.copy(isUploading = false, uploadSuccess = true)
            } else {
                _uiState.value?.copy(isUploading = false, uploadSuccess = false, errorMessage = "Upload failed")
            }
        }
    }

    // Fetch a specific paper
    fun fetchPaper(paperId: String) {
        viewModelScope.launch {
            try {
                paperRepository.getPaper(paperId).collect { paper ->
                    if (paper != null) {
                        _uiState.value = _uiState.value?.copy(
                            paperItemState = paper.topic?.let {
                                PaperItemUiState(
                                    title = paper.title,
                                    authors = paper.author,
                                    topics = it,
                                    publishDate = paper.publishDate,
                                    summary = paper.summaryText,
                                    pdfUrl = paper.fileUrl
                                )
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                //_paperItemState.value = PaperUiState(errorMessage = e.message)
            }
        }
    }
}