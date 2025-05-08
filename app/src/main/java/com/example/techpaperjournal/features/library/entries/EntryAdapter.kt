package com.example.techpaperjournal.features.library.entries

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.techpaperjournal.R
import com.google.android.flexbox.FlexboxLayout
import java.text.SimpleDateFormat
import java.util.Locale

class EntryAdapter (
    private val context: Context,
    private var entries: MutableList<EntryWithPaper>,
    private val onEntryClick: (EntryWithPaper) -> Unit,
    private val onEntryLongClick: (EntryWithPaper) -> Unit,
    private val isFromHome: Boolean
) : RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {
    // Add a new entry to the list and notify the adapter
    fun updateEntries(newEntries: List<EntryWithPaper>) {
        Log.d("Entries", "updateEntries: $newEntries")
        entries = newEntries.toMutableList()
        Log.d("Entries", "updateEntries: $entries")
        notifyDataSetChanged()
    }

    // Inflate the item layout for each item and return a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        if (isFromHome) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_entry_home, parent, false)
            return EntryViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_entry_library, parent, false)
            return EntryViewHolder(view)
        }
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
    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(entryAndPaper: EntryWithPaper) {
            itemView.findViewById<TextView>(R.id.entry_title).text = entryAndPaper.paper.title
            val timestamp = entryAndPaper.entry.lastUpdated.toDate()
            val formatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            val formatted = formatter.format(timestamp)
            itemView.findViewById<TextView>(R.id.entry_updated).text = "Last updated $formatted"
            itemView.findViewById<TextView>(R.id.paper_summary).text = entryAndPaper.paper.summaryText
            val container = itemView.findViewById<FlexboxLayout>(R.id.topics_container)
            val topics = entryAndPaper.paper.topic
            if (topics.isNullOrEmpty()) {
                container.visibility = View.GONE
            } else {
                container.visibility = View.VISIBLE
                val visibleTags = mutableListOf<String>()
                val maxChars = if (isFromHome) 29 else 45
                var totalChars = 0

                for (tag in topics) {
                    totalChars += tag.length
                    if (totalChars <= maxChars) {
                        visibleTags.add(tag)
                    } else {
                        break
                    }
                }

                if (visibleTags.size < topics.size) {
                    visibleTags.add("+${topics.size - visibleTags.size}")
                }

                setTags(visibleTags, container)
            }
            itemView.setOnClickListener {
                onEntryClick(entryAndPaper)
            }

            itemView.setOnLongClickListener {
                onEntryLongClick(entryAndPaper)
                true
            }
        }
    }

    // Add topic tags to the container
    private fun setTags(tagList : List<String>?, container: FlexboxLayout) {
        container.removeAllViews()
        tagList?.forEach { tag ->
            val tagView = TextView(context).apply {
                text = tag
                setPadding(20, 10, 20, 10)
                setTextColor(ContextCompat.getColor(context, R.color.beige))
                textSize = 11f
                typeface = ResourcesCompat.getFont(context, R.font.poppins_medium)
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