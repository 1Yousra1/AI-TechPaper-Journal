package com.example.techpaperjournal.features.journal.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.techpaperjournal.core.model.Page
import com.example.techpaperjournal.databinding.FragmentPageContentBinding
import com.example.techpaperjournal.features.library.entries.EntriesViewModel
import com.example.techpaperjournal.features.library.papers.PapersViewModel
import kotlinx.coroutines.launch

class PageContentFragment: Fragment() {
    private var _binding: FragmentPageContentBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(entryId: String, selectedPageId: String?): PageContentFragment {
            return PageContentFragment().apply {
                arguments = Bundle().apply {
                    putString("entryId", entryId)
                    selectedPageId?.let { putString("selectedPageId", it) }
                }
            }
        }
    }

    private lateinit var entriesViewModel: EntriesViewModel
    private lateinit var papersViewModel: PapersViewModel
    private lateinit var pagesViewModel: PagesViewModel

    private fun getEntryId(): String = arguments?.getString("entryId")!!
    private fun getSelectedPageId(): String? = arguments?.getString("selectedPageId")

    private lateinit var pageAdapter: PageViewAdapter
    private lateinit var pageContainer: ViewPager2

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        entriesViewModel = ViewModelProvider(this)[EntriesViewModel::class.java]
        papersViewModel = ViewModelProvider(this)[PapersViewModel::class.java]
        pagesViewModel = ViewModelProvider(this)[PagesViewModel::class.java]
        _binding = FragmentPageContentBinding.inflate(inflater, container, false)
        val root: View = binding.root
        setupPageView()
        observePages()
        return root
    }

    // Set up the page view
    private fun setupPageView() {
        pageContainer = binding.pageContentContainer
        pageAdapter = PageViewAdapter(getEntryId(), childFragmentManager, lifecycle)
        pageContainer.adapter = pageAdapter
    }

    // Observe the pages and update the adapter
    private fun observePages() {
        lifecycleScope.launch {
            pagesViewModel.fetchPages(getEntryId())
            pagesViewModel.uiState.observe(viewLifecycleOwner) { pageUiState ->
                pageUiState.pages.let { pages ->
                    pageAdapter.submitPages(pages)
                    setInitialPage(pages)
                    if (pages.isNotEmpty()) {
                        setPageTitle(pages)
                    }
                }
            }
        }
    }

    // Set the initial page and update the title
    private fun setInitialPage(pages: List<Page>) {
        getSelectedPageId().let { pageId ->
            val startPosition = pages.indexOfFirst { it.pageID == pageId }
            if (startPosition >= 0) {
                pageContainer.setCurrentItem(startPosition, false)
                (parentFragment as? PageHostFragment)?.updateTitle(
                    when (pages[startPosition].type) {
                        "about" -> "About"
                        "prompt" -> "Prompt"
                        else -> pages[startPosition].title
                    }
                )
            }
        }
    }

    // Set the page title
    private fun setPageTitle(pages: List<Page>) {
        binding.pageContentContainer.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val title = when (pages[position].type) {
                    "about" -> "About"
                    "prompt" -> "Prompt"
                    else -> pages[position].title
                }
                (parentFragment as? PageHostFragment)?.updateTitle(title)
            }
        })
    }
}