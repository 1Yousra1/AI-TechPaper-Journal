package com.example.techpaperjournal.ui.papers

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.techpaperjournal.R
import com.example.techpaperjournal.data.model.Paper
import com.example.techpaperjournal.databinding.DialogBottomSheetBinding
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.storage.FirebaseStorage
import java.io.File


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
        var isExpanded = false

        binding.paperTitle.text = paper.title
        binding.paperAuthor.text = paper.author
        binding.paperPublishDate.text = paper.publishDate
        binding.paperSummary.text = paper.summaryText
        binding.paperSummary.setOnClickListener {
            if (!isExpanded) {
                binding.paperSummary.maxLines = Int.MAX_VALUE
                binding.paperSummary.ellipsize = null
                isExpanded = true
            } else {
                binding.paperSummary.maxLines = 15
                binding.paperSummary.ellipsize = TextUtils.TruncateAt.END
                isExpanded = false
            }
        }
        val topicList = if (paper.topic?.size!! > 4)  paper.topic.subList(0,4) + listOf("+${paper.topic.size - 4}") else paper.topic
        setTags(topicList, binding.paperTopicsContainer)
        binding.readPaperButton.setOnClickListener {
            FirebaseStorage.getInstance()
                .getReference("papers/${paper.paperID}.pdf")
                .downloadUrl
                .addOnSuccessListener { uri ->
                    val contentUri = Uri.parse(uri.toString())
                    openPdfInExternalApp(contentUri)
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load PDF", Toast.LENGTH_SHORT).show()
                }
        }
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

    private fun openPdfInExternalApp(firebaseUri: Uri) {
        if (!isAdded) {
            Log.w("PaperDetailsBottomSheet", "Fragment not attached, ignoring PDF open request.")
            return
        }

        val context = requireContext()

        val tempFile = File.createTempFile(
            "temp_${System.currentTimeMillis()}",
            ".pdf",
            context.cacheDir
        ).apply { deleteOnExit() }

        FirebaseStorage.getInstance()
            .getReferenceFromUrl(firebaseUri.toString())
            .getFile(tempFile)
            .addOnSuccessListener {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    tempFile
                )

                val intentPdf = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val intentAny = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                try {
                    if (intentPdf.resolveActivity(context.packageManager) != null) {
                        startActivity(intentPdf)
                    } else if (intentAny.resolveActivity(context.packageManager) != null) {
                        startActivity(intentAny)
                    } else {
                        showPdfReaderInstallPrompt()
                    }
                } catch (e: ActivityNotFoundException) {
                    showPdfReaderInstallPrompt()
                } catch (e: SecurityException) {
                    Toast.makeText(context, "Permission denied by PDF reader", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPdfReaderInstallPrompt() {
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_install_pdf_reader, null)
        val positiveButton = customView.findViewById<Button>(R.id.positive_button)
        val negativeButton = customView.findViewById<Button>(R.id.negative_button)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(customView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        positiveButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.adobe.reader")))
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.adobe.reader")))
            }
        }
        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

}