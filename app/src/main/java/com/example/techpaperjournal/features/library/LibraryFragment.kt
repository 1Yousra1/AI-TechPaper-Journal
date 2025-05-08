package com.example.techpaperjournal.features.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.techpaperjournal.R
import com.example.techpaperjournal.databinding.FragmentLibraryBinding
import com.example.techpaperjournal.features.journal.pages.PagesViewModel
import com.example.techpaperjournal.features.library.entries.EntriesFragment
import com.example.techpaperjournal.features.library.entries.EntriesViewModel
import com.example.techpaperjournal.features.library.papers.BaseFragment
import com.example.techpaperjournal.features.library.papers.PapersFragment
import com.example.techpaperjournal.features.library.papers.PapersViewModel

class LibraryFragment : BaseFragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        papersViewModel = ViewModelProvider(this)[PapersViewModel::class.java]
        entriesViewModel = ViewModelProvider(this)[EntriesViewModel::class.java]
        pagesViewModel = ViewModelProvider(this)[PagesViewModel::class.java]
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        showAddDialog(binding.addButton)
        papersViewModel.fetchPapers()
        entriesViewModel.fetchEntriesAndPapers()

        // Set up the search button click listener
        binding.searchButton.setOnClickListener {
            binding.searchBar.visibility = View.VISIBLE
            binding.fragmentContainer.setOnClickListener {
                binding.searchBar.visibility = View.INVISIBLE
            }
        }

        // Set up the tab click listeners
        replaceFragment(PapersFragment())
        binding.papersTab.setOnClickListener {
            replaceFragment(PapersFragment())
            binding.entriesTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.bckg_entry_tab_unselected)
            binding.papersTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.bckg_papers_tab_selected)
        }

        binding.entriesTab.setOnClickListener {
            replaceFragment(EntriesFragment())
            binding.entriesTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.bckg_entry_tab_selected)
            binding.papersTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.bckg_papers_tab_unselected)
        }

        return root
    }

    // Replace the fragment in the container
    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}