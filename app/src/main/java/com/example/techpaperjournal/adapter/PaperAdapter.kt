package com.example.techpaperjournal.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.techpaperjournal.R
import com.example.techpaperjournal.data.model.Paper

class PaperAdapter (private val context: Context, private val papers: MutableList<Paper>) : RecyclerView.Adapter<PaperAdapter.PaperViewHolder>() {
    // Add a new paper to the list and notify the adapter
    fun updatePapers(newPapers: List<Paper>) {

    }

    // Inflate the item layout for each item and return a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaperAdapter.PaperViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_paper, parent, false)
        return PaperViewHolder(view)
    }

    // Bind the data for the current item to the ViewHolder
    override fun onBindViewHolder(holder: PaperViewHolder, position: Int) {
        holder.bind(papers[position])
    }

    // Return the total number of items in the list
    override fun getItemCount(): Int {
        return papers.size
    }

    // Bind the data to the corresponding UI elements in the item layout
    class PaperViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(paper: Paper) {
            itemView.findViewById<TextView>(R.id.paper_title).text = paper.title
        }
    }
}