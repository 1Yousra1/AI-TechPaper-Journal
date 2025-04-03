package com.example.techpaperjournal.ui.papers

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techpaperjournal.data.model.Paper
import com.example.techpaperjournal.data.repository.PaperRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

data class PaperUiState (
    val papers: List<Paper> = emptyList(),
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class PaperItemUiState(
    val title: String = "",
    val authors: String = "",
    val topics: List<String> = emptyList(),
    val publishDate: String = Timestamp.now().toString(),
    val pageCount: Int = 0,
    val summary: String = "",
    val pdfUrl: String? = null
)

class PapersViewModel : ViewModel() {
    private val _paperListState = MutableLiveData<PaperUiState>(PaperUiState())
    val paperListState: LiveData<PaperUiState> = _paperListState
    private val _paperItemState = MutableLiveData<PaperItemUiState>()
    val paperItemState: LiveData<PaperItemUiState> = _paperItemState
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

    // Fetch a specific paper
    fun fetchPaper(paperId: String) {
        viewModelScope.launch {
            try {
                paperRepository.getPaper(paperId).collect { paper ->
                    if (paper != null) {
                        _paperItemState.value = paper.topic?.let {
                            _paperItemState.value?.copy(
                                title = paper.title,
                                authors = paper.author,
                                topics = it,
                                publishDate = paper.publishDate,
                                summary = paper.summaryText,
                                pdfUrl = paper.fileUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                //_paperItemState.value = _paperListState.value?.copy(errorMessage = e.message)
            }
        }
    }

    // Update an existing paper
    fun updatePaper(paper: Paper) {

    }

    // Delete a paper
    fun deletePaper(paperId: String) {
        viewModelScope.launch {
            try {
                paperRepository.deletePaper(paperId)
                _paperListState.value = _paperListState.value?.copy(
                    papers = _paperListState.value?.papers?.filter { it.paperID != paperId } ?: emptyList(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _paperListState.value = _paperListState.value?.copy(
                    errorMessage = e.message
                )
            }
        }
    }
}