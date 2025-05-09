package com.example.techpaperjournal.features.library

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.SearchView
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
    private var activeFragment: Fragment = PapersFragment()
    private var isVisible = false

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
        setupTabs()
        setupSearchButton()
        setupSearch()
        replaceFragment(PapersFragment())

        return root
    }

    // Set up the tabs to switch between fragments
    private fun setupTabs() {
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
    }

    // Set up the search button click listener
    private fun setupSearchButton() {
        binding.searchButtonM.setOnClickListener {
            isVisible = !isVisible
            if (isVisible) {
                binding.searchBar.visibility = View.VISIBLE
                binding.tabBar.visibility = View.GONE
                binding.searchButtonM.setIconResource(R.drawable.ic_hide_search)
                binding.searchBar.requestFocus()
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(binding.searchBar, InputMethodManager.SHOW_IMPLICIT)
            } else {
                binding.searchBar.setQuery("", false)
                binding.searchBar.clearFocus()
                binding.searchBar.visibility = View.GONE
                binding.tabBar.visibility = View.VISIBLE
                binding.searchButtonM.setIconResource(R.drawable.ic_search)
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(binding.searchBar.windowToken, 0)
            }
        }
    }

    // Set up the search bar
    private fun setupSearch() {
        binding.searchBar.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                performSearch(newText)
                return true
            }
        })
        val searchText = binding.searchBar.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(ContextCompat.getColor(requireContext(), R.color.beige))
        searchText.hint = "Search..."
    }

    // Replace the fragment in the container
    private fun replaceFragment(fragment: Fragment) {
        activeFragment = fragment
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Perform the search in the active fragment
    private fun performSearch(query: String?) {
        val searchQuery = query?.trim() ?: ""
        Log.d("LibraryFragment", "Performing search with: '$searchQuery'")

        activeFragment.let { fragment ->
            if (fragment is Searchable && fragment.isAdded) {
                fragment.search(searchQuery)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}