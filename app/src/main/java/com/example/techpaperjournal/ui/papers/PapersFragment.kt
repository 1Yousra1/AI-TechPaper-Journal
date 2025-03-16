package com.example.techpaperjournal.ui.papers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.techpaperjournal.adapter.PaperAdapter
import com.example.techpaperjournal.databinding.FragmentPapersBinding
import kotlinx.coroutines.launch

class PapersFragment : Fragment() {
    private var _binding: FragmentPapersBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var papersRecyclerView: RecyclerView
    private lateinit var paperAdapter: PaperAdapter
    private lateinit var papersViewModel: PapersViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPapersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        papersViewModel = ViewModelProvider(this)[PapersViewModel::class.java]
        papersRecyclerView = binding.papersRv
        paperAdapter = PaperAdapter(requireContext(), mutableListOf())
        papersRecyclerView.adapter = paperAdapter
        papersRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        lifecycleScope.launch {
            papersViewModel.uiState.observe(viewLifecycleOwner) { uiState ->
                paperAdapter.updatePapers(uiState.papers)
            }
        }
        papersViewModel.fetchPapers()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}