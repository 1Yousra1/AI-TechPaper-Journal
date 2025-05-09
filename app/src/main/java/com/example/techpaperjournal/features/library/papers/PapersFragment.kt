package com.example.techpaperjournal.features.library.papers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.techpaperjournal.databinding.FragmentLibraryPapersBinding
import com.example.techpaperjournal.features.journal.pages.PagesViewModel
import com.example.techpaperjournal.features.library.Searchable
import com.example.techpaperjournal.features.library.entries.EntriesViewModel

class PapersFragment : BaseFragment(), Searchable {
    private var _binding: FragmentLibraryPapersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        papersViewModel = ViewModelProvider(this)[PapersViewModel::class.java]
        entriesViewModel = ViewModelProvider(this)[EntriesViewModel::class.java]
        pagesViewModel = ViewModelProvider(this)[PagesViewModel::class.java]
        _binding = FragmentLibraryPapersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Set up the recycler view
        papersRecyclerView = binding.papersRv
        setupPaperRecyclerView(false)

        // Observe the view model
        observePaperViewModel(binding.noPapersTv, false)

        return root
    }

    override fun search(query: String) {
        val filteredPapers = originalPapers.filter {
            it.title.contains(query, true) || it.author.contains(query, true)
        }
        paperAdapter.updatePapers(filteredPapers)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}