package com.example.techpaperjournal.features.journal.grid

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.techpaperjournal.R
import com.example.techpaperjournal.core.model.Page
import com.example.techpaperjournal.databinding.FragmentPageGridviewBinding
import com.example.techpaperjournal.features.journal.pages.PageHostFragment
import com.example.techpaperjournal.features.journal.pages.PagesViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class GridViewFragment: Fragment() {
    private var _binding: FragmentPageGridviewBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(entryId: String): GridViewFragment {
            return GridViewFragment().apply {
                arguments = Bundle().apply {
                    putString("entryId", entryId)
                }
            }
        }
    }

    private lateinit var pagesViewModel: PagesViewModel
    private lateinit var adapter: GridViewAdapter
    private var entryID: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        pagesViewModel = ViewModelProvider(this)[PagesViewModel::class.java]
        _binding = FragmentPageGridviewBinding.inflate(inflater, container, false)
        val root: View = binding.root
        entryID = arguments?.getString("entryId")

        setupRecyclerView()
        observePages()

        return root
    }

    /** --------------- Setup Methods --------------- **/

    private fun setupRecyclerView() {
        val gridView = binding.pagesRv
        adapter = GridViewAdapter(requireContext(), mutableListOf(),
            { page ->
                val bundle = Bundle().apply {
                    putString("entryId", page.entryID)
                    putString("selectedPageId", page.pageID)
                }
                findNavController().navigate(R.id.action_gridViewFragment_to_entryPageHostFragment, bundle)
            },
            { page -> showEditDialog(page) },
            { size -> showAddPageDialog(size) },
            { pageId -> showDeleteDialog(pageId) })
        gridView.adapter = adapter
        gridView.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun observePages() {
        (parentFragment as? PageHostFragment)?.updateTitle("GridView")
        lifecycleScope.launch {
            pagesViewModel.uiState.observe(viewLifecycleOwner) { uiState ->
                Log.d("GridViewFragment", "Pages Updated: ${uiState.pages}")
                if (uiState.pages.isNotEmpty()) {
                    adapter.updatePages(uiState.pages)
                }
            }
        }
        pagesViewModel.fetchPages(entryID!!)
    }

    /** --------------- Dialog Methods --------------- **/

    // Show the delete dialog
     private fun showDeleteDialog(id: String) {
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_delete, null)
        customView.findViewById<TextView>(R.id.dialog_prompt).apply { text = "Delete Page" }
        customView.findViewById<TextView>(R.id.dialog_message).apply { text = "Are you sure you want to delete this page?" }
        val positiveButton = customView.findViewById<Button>(R.id.positive_button).apply { text = "Yes" }
        val negativeButton = customView.findViewById<Button>(R.id.negative_button).apply { text = "No" }

        val dialog = AlertDialog.Builder(context)
            .setView(customView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        positiveButton.setOnClickListener {
            pagesViewModel.deletePage(id)
            dialog.dismiss()
        }
        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showEditDialog(page: Page) {
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_title_input, null)
        customView.findViewById<TextView>(R.id.dialog_prompt).text = "Edit Page Title"
        val title = customView.findViewById<TextView>(R.id.title_input)
        title.text = page.title
        customView.findViewById<MaterialButton>(R.id.save_title_button).text = "Save"
        val dialog = AlertDialog.Builder(context)
            .setView(customView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customView.findViewById<Button>(R.id.save_title_button).setOnClickListener {
            val titleInput = title.text.toString()
            pagesViewModel.updatePageTitle(page.pageID, titleInput)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddPageDialog(size: Int) {
        Log.d("GridViewFragment", "Add Page Button clicked at position $size")
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_title_input, null)
        val dialog = AlertDialog.Builder(context)
            .setView(customView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customView.findViewById<Button>(R.id.save_title_button).setOnClickListener {
            val title = customView.findViewById<TextView>(R.id.title_input).text.toString()
            pagesViewModel.addPage(
                mutableMapOf(
                    "entryID" to entryID,
                    "type" to "notes",
                    "title" to title,
                    "content" to "",
                    "pageOrder" to size + 1
                )
            )
            dialog.dismiss()
        }
        dialog.show()
    }
}