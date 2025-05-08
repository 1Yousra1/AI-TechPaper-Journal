package com.example.techpaperjournal.features.journal.grid

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.techpaperjournal.R
import com.example.techpaperjournal.core.model.Page

class GridViewAdapter(
    private val context: Context,
    private var pages: List<Page>,
    private val onPageClick: (Page) -> Unit,
    private val onPageLongClick: (Page) -> Unit,
    private val onAddPageClick: (Int) -> Unit,
    private val onDeleteClick: (String) -> Unit
):RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_PAGE = 0
        const val TYPE_ADD_BUTTON = 1
    }

    // Update the list of pages and notify the adapter that the data has changed
    fun updatePages(newPages: List<Page>) {
        pages = newPages.toMutableList()
        notifyDataSetChanged()
    }

    // Return the type of view for the current position
    override fun getItemViewType(position: Int): Int {
        return if (position < pages.size) TYPE_PAGE else TYPE_ADD_BUTTON
    }

    // Inflate the item layout for each item and return a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_PAGE) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_page, parent, false)
            return PageViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_add_page, parent, false)
            return AddPageViewHolder(view)
        }
    }

    // Bind the data for the current item to the ViewHolder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PageViewHolder -> holder.bind(pages[position])
            is AddPageViewHolder -> holder.bind(pages.size)
        }
    }

    // Return the total number of items in the list
    override fun getItemCount(): Int = pages.size + 1

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(page: Page) {
            itemView.findViewById<TextView>(R.id.page_title).text = page.title
            if (page.type == "about")
                itemView.findViewById<ImageButton>(R.id.delete_button).visibility = View.GONE
            else
                itemView.findViewById<ImageButton>(R.id.delete_button).visibility = View.VISIBLE

            itemView.findViewById<ImageButton>(R.id.delete_button).setOnClickListener {
                onDeleteClick(page.pageID)
            }

            itemView.setOnClickListener { onPageClick(page) }
            itemView.setOnLongClickListener {
                onPageLongClick(page)
                true
            }
        }
    }

    inner class AddPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(size: Int) {
            itemView.findViewById<ImageButton>(R.id.add_page).setOnClickListener {
                Log.d("GridViewAdapter", "Add Page Button clicked at position $size")
                onAddPageClick(size)
            }
        }
    }
}