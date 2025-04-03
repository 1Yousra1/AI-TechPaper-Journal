package com.example.techpaperjournal.ui.papers

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.techpaperjournal.R
import com.example.techpaperjournal.adapter.PaperAdapter
import com.example.techpaperjournal.data.model.Paper
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
        paperAdapter = PaperAdapter(requireContext(), mutableListOf(),
            { paper -> PaperDetailsBottomSheet(paper).show(childFragmentManager, PaperDetailsBottomSheet.TAG) },
            { paper -> showDeleteDialog(paper) })
        papersRecyclerView.adapter = paperAdapter
        papersRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        lifecycleScope.launch {
            papersViewModel.paperListState.observe(viewLifecycleOwner) { uiState ->
                paperAdapter.updatePapers(uiState.papers)

                if (paperAdapter.itemCount == 0)
                    binding.noPapersTv.visibility = View.VISIBLE
                else
                    binding.noPapersTv.visibility = View.GONE
            }
        }
        papersViewModel.fetchPapers()

        return root
    }

    // Show the delete dialog for the paper
    private fun showDeleteDialog(paper: Paper) {
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_paper, null)
        val positiveButton = customView.findViewById<Button>(R.id.positive_button).apply { text = "Yes" }
        val negativeButton = customView.findViewById<Button>(R.id.negative_button).apply { text = "No" }

        val dialog = AlertDialog.Builder(context)
            .setView(customView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        positiveButton.setOnClickListener {
            papersViewModel.deletePaper(paper.paperID)
            dialog.dismiss()
        }
        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}