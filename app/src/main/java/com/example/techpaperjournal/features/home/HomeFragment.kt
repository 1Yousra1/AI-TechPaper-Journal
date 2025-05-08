package com.example.techpaperjournal.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.techpaperjournal.databinding.FragmentHomeBinding
import com.example.techpaperjournal.features.journal.pages.PagesViewModel
import com.example.techpaperjournal.features.library.entries.EntriesViewModel
import com.example.techpaperjournal.features.library.papers.BaseFragment
import com.example.techpaperjournal.features.library.papers.PapersViewModel

class HomeFragment : BaseFragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        papersViewModel = ViewModelProvider(this)[PapersViewModel::class.java]
        entriesViewModel = ViewModelProvider(this)[EntriesViewModel::class.java]
        pagesViewModel = ViewModelProvider(this)[PagesViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        showAddDialog(binding.addButton)

        // Set up the papers and entries recycler view
        papersRecyclerView = binding.papersRv
        entriesRecyclerView = binding.entriesRv
        setupPaperRecyclerView(true)
        setupEntryRecyclerView(true)

        // Observe the view models
        observePaperViewModel(binding.noPapersTv, true)
        observeEntryViewModel(binding.noEntriesTv, true)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}