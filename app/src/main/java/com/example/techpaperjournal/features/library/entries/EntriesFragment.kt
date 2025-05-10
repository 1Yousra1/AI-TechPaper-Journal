package com.example.techpaperjournal.features.library.entries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.techpaperjournal.databinding.FragmentLibraryEntriesBinding
import com.example.techpaperjournal.features.library.FilterAndSortable
import com.example.techpaperjournal.features.library.Searchable
import com.example.techpaperjournal.features.library.papers.BaseFragment

class EntriesFragment : BaseFragment(), Searchable, FilterAndSortable {
    private var _binding: FragmentLibraryEntriesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        entriesViewModel = ViewModelProvider(this)[EntriesViewModel::class.java]
        _binding = FragmentLibraryEntriesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        entriesRecyclerView = binding.entriesRv
        setupEntryRecyclerView(false)

        observeEntryViewModel(binding.noEntriesTv, false)

        return root
    }

    // Search entries by paper title or author
    override fun search(query: String) {
        val filteredEntries = originalEntries.filter {
            it.paper.title.contains(query, true) || it.paper.author.contains(query, true)
        }
        entryAdapter.updateEntries(filteredEntries)
    }

    // Update the viewmodel with the filter option
    override fun filter(option: String) {
        entriesViewModel.setFilter(option)
    }

    // Update the viewmodel with the sort option
    override fun sort(option: String) {
        entriesViewModel.setSort(option)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}