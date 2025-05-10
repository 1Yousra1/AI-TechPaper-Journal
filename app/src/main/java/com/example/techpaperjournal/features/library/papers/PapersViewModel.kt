package com.example.techpaperjournal.features.library.papers

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techpaperjournal.core.model.Paper
import com.example.techpaperjournal.core.repository.PaperRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class PaperUiState (
    val papers: List<Paper> = emptyList(),
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class FilterUIState (
    val lastFilterChecked: String = "all",
    val lastSortChecked: String = "accessed"
)

class PapersViewModel : ViewModel() {
    private val _paperListState = MutableLiveData(PaperUiState())
    val paperListState: LiveData<PaperUiState> = _paperListState

    private val _filterState = MutableLiveData(FilterUIState())
    private var originalPapers: List<Paper> = emptyList()

    val filterState: LiveData<FilterUIState> = _filterState
    private val paperRepository = PaperRepository()

    fun setFilter(filter: String) {
        _filterState.value = _filterState.value?.copy(lastFilterChecked = filter)
        applyFiltersAndSort()
    }

    fun setSort(sort: String) {
        _filterState.value = _filterState.value?.copy(lastSortChecked = sort)
        applyFiltersAndSort()
    }

    // Fetch all papers
    fun fetchPapers() {
        _paperListState.value = _paperListState.value?.copy(isLoading = true)
        viewModelScope.launch {
            try {
                paperRepository.getPapers().collect { papers ->
                    originalPapers = papers
                    applyFiltersAndSort()
                    _paperListState.value = _paperListState.value?.copy(
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

    private fun applyFiltersAndSort() {
        val filtered = applyFilters()
        val sorted = applySort(filtered)
        _paperListState.value = _paperListState.value?.copy(papers = sorted)
    }

    private fun applyFilters(): List<Paper> {
        return when (_filterState.value?.lastFilterChecked ?: "all") {
            "all" -> originalPapers
            "recent" -> {
                val thresholdDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                }.time
                originalPapers.filter { it.lastAccessed.toDate() > thresholdDate }
            }
            else -> originalPapers.filter {
                it.topic?.contains(_filterState.value?.lastFilterChecked) ?: false
            }
        }
    }

    private fun applySort(papers: List<Paper>): List<Paper> {
        return when (_filterState.value?.lastSortChecked ?: "accessed") {
            "accessed" -> papers.sortedByDescending { it.lastAccessed }
            "upload" -> papers.sortedByDescending { it.uploadDate }
            "date" -> papers.sortedByDescending {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(it.publishDate)
            }
            "title" -> papers.sortedBy { it.title }
            else -> papers
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

    // Update a paper's last accessed date
    fun updatePaperLastAccessed(paperId: String) {
        viewModelScope.launch {
            try {
                paperRepository.updatePaperLastAccessed(paperId)
                paperRepository.getPaper(paperId).collect { paper ->
                    paper?.let {
                        val updatedPapers = _paperListState.value?.papers?.map { e ->
                            if (e.paperID == paperId) it else e
                        } ?: emptyList()

                        _paperListState.value = _paperListState.value?.copy(papers = updatedPapers)
                    }
                }
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