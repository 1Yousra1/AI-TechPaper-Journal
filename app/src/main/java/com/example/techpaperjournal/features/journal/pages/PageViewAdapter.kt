package com.example.techpaperjournal.features.journal.pages

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.techpaperjournal.core.model.Page

class PageViewAdapter(
    private val entryId: String?,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private var pages: List<Page> = emptyList()

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        return when (pages[position].type) {
            "about" -> AboutPageFragment()
            //"prompt" -> PromptPageFragment()
            else -> NotesPageFragment()
        }.apply {
            arguments = bundleOf(
                "entryId" to entryId,
                "pageId" to pages[position].pageID
            )
        }
    }

    fun submitPages(newPages: List<Page>) {
        this.pages = newPages
        notifyDataSetChanged()
    }
}