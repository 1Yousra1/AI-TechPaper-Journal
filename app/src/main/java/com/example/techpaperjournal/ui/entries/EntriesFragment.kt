package com.example.techpaperjournal.ui.entries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.techpaperjournal.adapter.EntryAdapter
import com.example.techpaperjournal.adapter.PaperAdapter
import com.example.techpaperjournal.databinding.FragmentEntriesBinding
import com.example.techpaperjournal.databinding.FragmentHomeBinding
import com.example.techpaperjournal.ui.home.HomeViewModel
import com.example.techpaperjournal.ui.papers.PapersViewModel
import kotlinx.coroutines.launch

class EntriesFragment : Fragment() {
    private var _binding: FragmentEntriesBinding? = null

    private lateinit var entriesRecyclerView: RecyclerView
    private lateinit var entryAdapter: EntryAdapter
    private lateinit var entriesViewModel: EntriesViewModel

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntriesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        entriesViewModel = ViewModelProvider(this)[EntriesViewModel::class.java]
        entriesRecyclerView = binding.entriesRv
        entryAdapter = EntryAdapter(requireContext(), mutableListOf())
        entriesRecyclerView.adapter = entryAdapter
        entriesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)


        lifecycleScope.launch {
            entriesViewModel.uiState.observe(viewLifecycleOwner) { uiState ->
                entryAdapter.updateEntries(uiState.entries)
            }
        }
        entriesViewModel.fetchEntries()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}