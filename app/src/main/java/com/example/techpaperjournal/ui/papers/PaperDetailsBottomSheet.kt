package com.example.techpaperjournal.ui.papers

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.techpaperjournal.R
import com.example.techpaperjournal.data.model.Paper
import com.example.techpaperjournal.databinding.DialogBottomSheetBinding
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PaperDetailsBottomSheet(private val paper: Paper) : BottomSheetDialogFragment() {
    private lateinit var binding : DialogBottomSheetBinding

    companion object {
        const val TAG = "ModalBottomSheetDialog"
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogBottomSheetBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { it ->
            val d = it as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update placeholders with actual data
        binding.paperTitle.text = paper.title
        binding.paperAuthor.text = paper.author
        binding.paperPublishDate.text = paper.publishDate
        val topicList = if (paper.topic?.size!! > 4)  paper.topic.subList(0,4) + listOf("+${paper.topic.size - 4}") else paper.topic
        setTags(topicList, binding.paperTopicsContainer)
    }

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