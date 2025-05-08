package com.example.techpaperjournal.features.library.papers

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.techpaperjournal.R
import com.example.techpaperjournal.core.model.Paper

class PaperAdapter (
    private val context: Context,
    private var papers: MutableList<Paper>,
    private val onPaperClick: (Paper) -> Unit,
    private val onPaperLongClick: (Paper) -> Unit,
    private val isFromHome: Boolean
) : RecyclerView.Adapter<PaperAdapter.PaperViewHolder>() {
    // Add a new paper to the list and notify the adapter
    fun updatePapers(newPapers: List<Paper>) {
        papers = newPapers.toMutableList()
        notifyDataSetChanged()
    }

    // Inflate the item layout for each item and return a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaperViewHolder {
        if (isFromHome) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_paper_home, parent, false)
            return PaperViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_paper_library, parent, false)
            return PaperViewHolder(view)
        }
    }

    // Bind the data for the current item to the ViewHolder
    override fun onBindViewHolder(holder: PaperViewHolder, position: Int) {
        holder.bind(papers[position])
    }

    // Return the total number of items in the list
    override fun getItemCount(): Int {
        Log.d("PaperAdapter", "Item count: ${papers.size}")
        return papers.size
    }

    // Bind the data to the corresponding UI elements in the item layout
     inner class PaperViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(paper: Paper) {
            itemView.findViewById<TextView>(R.id.paper_title).text = paper.title
            itemView.setOnClickListener { onPaperClick(paper) }
            itemView.setOnLongClickListener {
                onPaperLongClick(paper)
                true
            }
        }
    }
}