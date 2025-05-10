package com.example.techpaperjournal.features.library

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.RadioGroup
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
import com.google.android.material.button.MaterialButton

class LibraryFragment : BaseFragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private var activeFragment: Fragment = PapersFragment()
    private var isVisible = false
    private var lastFilterChecked = "all"
    private var lastSortChecked = "accessed"

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
        setupFilterAndSort()
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
                binding.filterButton.visibility = View.VISIBLE
                binding.tabBar.visibility = View.GONE
                binding.searchButtonM.setIconResource(R.drawable.ic_hide_search)
                binding.searchBar.requestFocus()
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(binding.searchBar, InputMethodManager.SHOW_IMPLICIT)
            } else {
                binding.searchBar.setQuery("", false)
                binding.searchBar.clearFocus()
                binding.searchBar.visibility = View.GONE
                binding.filterButton.visibility = View.GONE
                binding.tabBar.visibility = View.VISIBLE
                binding.searchButtonM.setIconResource(R.drawable.ic_search)
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(binding.searchBar.windowToken, 0)
            }
        }
    }

    // Replace the fragment in the container
    private fun replaceFragment(fragment: Fragment) {
        activeFragment = fragment
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
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

    // Perform the search in the active fragment
    private fun performSearch(query: String?) {
        val searchQuery = query?.trim() ?: ""
        activeFragment.let { fragment ->
            if (fragment is Searchable && fragment.isAdded) {
                fragment.search(searchQuery)
            }
        }
    }

    private fun setupFilterAndSort() {
        binding.filterButton.setOnClickListener {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_filter, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(view)
                .create().apply {
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            dialog.show()
            papersViewModel.filterState.value?.let { state ->
                when (state.lastFilterChecked) {
                    "all" -> view.findViewById<RadioButton>(R.id.filter_all).isChecked = true
                    "recent" -> view.findViewById<RadioButton>(R.id.filter_recent).isChecked = true
                    else -> {
                        view.findViewById<RadioButton>(R.id.filter_topic).isChecked = true
                        val topicInput = view.findViewById<TextView>(R.id.topic_input)
                        topicInput.visibility = View.VISIBLE
                        topicInput.text = state.lastFilterChecked
                    }
                }
                when (state.lastSortChecked) {
                    "accessed" -> view.findViewById<RadioButton>(R.id.sort_accessed_updated).isChecked =
                        true

                    "upload" -> view.findViewById<RadioButton>(R.id.sort_upload).isChecked = true
                    "date" -> view.findViewById<RadioButton>(R.id.sort_date).isChecked = true
                    "title" -> view.findViewById<RadioButton>(R.id.sort_title).isChecked = true
                }
                view.findViewById<RadioButton>(R.id.filter_topic)
                    .setOnCheckedChangeListener { _, isChecked ->
                        view.findViewById<TextView>(R.id.topic_input).visibility =
                            if (isChecked) View.VISIBLE else View.GONE
                    }
            }
            view.findViewById<MaterialButton>(R.id.cancel_button).setOnClickListener { dialog.dismiss() }
            view.findViewById<MaterialButton>(R.id.apply_button).setOnClickListener {
                getFilterAndSortOptions(view)
                dialog.dismiss()
            }
        }
    }

    private fun getFilterAndSortOptions(view: View) {
        val filterGroup = view.findViewById<RadioGroup>(R.id.filter_radio_group)
        val topicInput = view.findViewById<TextView>(R.id.topic_input)
        val filterOption = when (filterGroup.checkedRadioButtonId) {
            R.id.filter_all -> { lastFilterChecked = "all"; "all" }
            R.id.filter_recent -> { lastFilterChecked = "recent"; "recent" }
            R.id.filter_topic -> { lastFilterChecked = "topic"; topicInput.text.toString() }
            else -> "all"
        }

        val sortGroup = view.findViewById<RadioGroup>(R.id.sort_radio_group)
        val sortOption = when (sortGroup.checkedRadioButtonId) {
            R.id.sort_accessed_updated -> { lastSortChecked = "accessed"; "accessed" }
            R.id.sort_upload -> { lastSortChecked = "upload"; "upload" }
            R.id.sort_date -> { lastSortChecked = "date"; "date" }
            R.id.sort_title -> { lastSortChecked = "title"; "title" }
            else -> "accessed"
        }
        papersViewModel.setFilter(filterOption)
        papersViewModel.setSort(sortOption)
        entriesViewModel.setFilter(filterOption)
        entriesViewModel.setSort(sortOption)
        performFilterSort(filterOption, sortOption)
    }

    private fun performFilterSort(filterOpt: String, sortOpt: String) {
        val filterOption = filterOpt.trim()
        val sortOption = sortOpt.trim()
        activeFragment.let { fragment ->
            if (fragment is FilterAndSortable && fragment.isAdded) {
                fragment.filter(filterOption)
                fragment.sort(sortOption)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}