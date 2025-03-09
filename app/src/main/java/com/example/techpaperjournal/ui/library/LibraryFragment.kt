package com.example.techpaperjournal.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.techpaperjournal.R
import com.example.techpaperjournal.databinding.FragmentLibraryBinding
import com.example.techpaperjournal.ui.entries.EntriesFragment
import com.example.techpaperjournal.ui.papers.PapersFragment

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val libraryViewModel = ViewModelProvider(this)[LibraryViewModel::class.java]

        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        replaceFragment(PapersFragment())
        binding.papersTab.setOnClickListener {
            replaceFragment(PapersFragment())
            binding.entriesTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.entry_tab_unselected)
            binding.papersTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.papers_tab_selected)
        }

        binding.entriesTab.setOnClickListener {
            replaceFragment(EntriesFragment())
            binding.entriesTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.entry_tab_selected)
            binding.papersTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.papers_tab_unselected)
        }

        return root
    }

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