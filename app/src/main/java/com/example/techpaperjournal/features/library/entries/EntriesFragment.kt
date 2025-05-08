package com.example.techpaperjournal.features.library.entries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.techpaperjournal.databinding.FragmentLibraryEntriesBinding
import com.example.techpaperjournal.features.library.papers.BaseFragment

class EntriesFragment : BaseFragment() {
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

        // Set up RecyclerView for entries
        entriesRecyclerView = binding.entriesRv
        setupEntryRecyclerView(false)

        // Observe entries view model
        observeEntryViewModel(binding.noEntriesTv, false)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}