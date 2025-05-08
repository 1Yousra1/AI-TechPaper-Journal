package com.example.techpaperjournal.features.journal.pages

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.techpaperjournal.R
import com.example.techpaperjournal.core.model.Paper
import com.example.techpaperjournal.databinding.FragmentPageHostBinding
import com.example.techpaperjournal.features.journal.grid.GridViewFragment
import com.example.techpaperjournal.features.library.entries.EntriesViewModel
import com.example.techpaperjournal.features.library.papers.PapersViewModel
import com.google.android.flexbox.FlexboxLayout

class PageHostFragment: Fragment() {
    private var _binding: FragmentPageHostBinding? = null
    private val binding get() = _binding!!

    private lateinit var entriesViewModel: EntriesViewModel
    private lateinit var papersViewModel: PapersViewModel
    private lateinit var pagesViewModel: PagesViewModel

    private val entryId: String? get() = arguments?.getString("entryId")
    private val selectedPageId: String? get() = arguments?.getString("selectedPageId")

    private val _pageTitle = MutableLiveData<String>()
    private val pageTitle: MutableLiveData<String> get() = _pageTitle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        entriesViewModel = ViewModelProvider(this)[EntriesViewModel::class.java]
        papersViewModel = ViewModelProvider(this)[PapersViewModel::class.java]
        pagesViewModel = ViewModelProvider(this)[PagesViewModel::class.java]
        _binding = FragmentPageHostBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupInitialFragment()
        setupHeaderButtons()
        observeEntryData()

        return root
    }

    /** --------------- Setup Methods --------------- **/

    // Set up the initial fragment
    private fun setupInitialFragment() {
        childFragmentManager.beginTransaction()
            .replace(R.id.page_fragment_container, PageContentFragment.newInstance(entryId!!, selectedPageId))
            .commit()
    }

    // Set up the header buttons
    private fun setupHeaderButtons() {
        binding.pageHeader.gridviewButton.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                navigateToGridview()
            }
        }
    }

    // Navigate to the gridview fragment
    private fun navigateToGridview() {
        val gridFragment = GridViewFragment.newInstance(entryId!!)
        childFragmentManager.beginTransaction()
            .replace(R.id.page_fragment_container, gridFragment)
            .addToBackStack(null)
            .commit()
        binding.pageHeader.gridviewButton.visibility = View.INVISIBLE
    }

    // Observe and fill the header with paper details
    private fun observeEntryData() {
        entriesViewModel.fetchEntryAndPaper(entryId!!)
        entriesViewModel.entryListState.observe(viewLifecycleOwner) { entryUiState ->
            entryUiState.entriesWithPapers.forEach { entryWithPaper ->
                val paper = entryWithPaper.paper
                fillPageHeader(paper)

            }
        }
    }

    /** --------------- Page Header Methods --------------- **/

    // Update the page title
    fun updateTitle(title: String) {
        _pageTitle.value = title
    }

    // Set the page header
    private fun fillPageHeader(paper: Paper) {
        pageTitle.observe(viewLifecycleOwner) { newTitle ->
            binding.pageHeader.pageTitle.text = newTitle
        }
        binding.pageHeader.paperTitle.text = paper.title
        binding.pageHeader.paperAuthor.text = paper.author
        setTags(paper.topic, binding.pageHeader.paperTopicsContainer)
        setupHeaderExpansion()
    }

    // Set up the header expansion
    private fun setupHeaderExpansion() {
        var isExpanded = false
        binding.pageHeader.root.setOnClickListener {
            isExpanded = !isExpanded
            binding.pageHeader.paperAuthor.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.pageHeader.paperTopicsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
    }

    // Set the topic tags
    private fun setTags(tagList : List<String>?, container: FlexboxLayout) {
        container.removeAllViews()
        tagList?.forEach { tag ->
            val tagView = TextView(requireContext()).apply {
                text = tag
                setPadding(20, 10, 20, 10)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.beige))
                textSize = 12f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bckg_topic_tag)
            }

            val layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 10
                bottomMargin = 10
            }
            tagView.layoutParams = layoutParams

            container.addView(tagView, container.childCount)
        }
    }
}