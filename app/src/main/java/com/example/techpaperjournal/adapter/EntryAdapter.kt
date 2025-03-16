package com.example.techpaperjournal.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.techpaperjournal.R
import com.example.techpaperjournal.data.model.Entry
import com.example.techpaperjournal.data.model.Paper

class EntryAdapter (private val context: Context, private val entries: List<Entry>) : RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {
    // Add a new entry to the list and notify the adapter
    fun updateEntries(newEntries: List<Entry>) {
        
    }

    // Inflate the item layout for each item and return a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryAdapter.EntryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_entry, parent, false)
        return EntryViewHolder(view)
    }

    // Bind the data for the current item to the ViewHolder
    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(entries[position])

    }

    // Return the total number of items in the list
    override fun getItemCount(): Int {
        return entries.size
    }

    // Bind the data to the corresponding UI elements in the item layout
    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(entry: Entry) {
            itemView.findViewById<TextView>(R.id.entry_title).text = entry.entryTitle
        }
    }
}