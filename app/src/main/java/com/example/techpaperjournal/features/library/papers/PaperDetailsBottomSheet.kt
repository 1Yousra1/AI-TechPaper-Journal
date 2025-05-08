package com.example.techpaperjournal.features.library.papers

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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import com.example.techpaperjournal.R
import com.example.techpaperjournal.core.model.Paper
import com.example.techpaperjournal.databinding.DialogBottomSheetBinding
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.storage.FirebaseStorage
import java.io.File

open class PaperDetailsBottomSheet(private val paper: Paper) : BottomSheetDialogFragment() {
    private lateinit var binding : DialogBottomSheetBinding
    private lateinit var papersViewModel: PapersViewModel
    private lateinit var paperDetailsMap : MutableMap<String, Any?>
    private var topics : MutableList<String>? = null
    private var isExpanded = false
    private var isEdited = false

    companion object {
        const val TAG = "ModalBottomSheetDialog"
    }

    // Inflate the layout for the bottom sheet
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogBottomSheetBinding.inflate(inflater, container, false)
        papersViewModel = ViewModelProvider(this)[PapersViewModel::class.java]
        paperDetailsMap = mutableMapOf()
        topics = paper.topic as MutableList<String>?
        return binding.root
    }

    // Set up the bottom sheet behavior for expanding
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    //
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateBottomSheet()
        setupListeners()
    }

    /** --------------- UI Setup Methods --------------- **/

    // Populate the bottom sheet with paper details
    private fun populateBottomSheet() {
        binding.paperTitle.text = if (isEdited) paperDetailsMap["title"] as String else paper.title
        binding.paperAuthor.text = if (isEdited) paperDetailsMap["author"] as String else paper.author
        binding.paperPublishDate.text = if (isEdited) paperDetailsMap["publishDate"] as String else paper.publishDate
        binding.paperSummary.text = paper.summaryText

        setupSummaryExpandable()
        if (topics != null) {
            val topics = if (isEdited) paperDetailsMap["topic"] as List<*> else paper.topic
            val topicList = if (topics?.size!! > 4) topics.subList(0, 4) + listOf("+${topics.size - 4}") else topics
            binding.paperTopicsContainer.removeAllViews()
            setTags(topicList as MutableList<String>?, binding.paperTopicsContainer)
        }
    }

    // Expand or collapse the paper summary
    private fun setupSummaryExpandable() {
        binding.paperSummary.setOnClickListener {
            isExpanded = !isExpanded
            binding.paperSummary.maxLines = if (isExpanded) Int.MAX_VALUE else 15
            binding.paperSummary.ellipsize = if (isExpanded) null else TextUtils.TruncateAt.END
            isExpanded = true
        }
    }

    // Setup click listeners for buttons
    private fun setupListeners() {
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

        binding.editButton.setOnClickListener { showEditPaperDialog() }
    }

    /** --------------- Edit Paper Methods --------------- **/

    // Displays the edit paper details dialog
    private fun showEditPaperDialog() {
        val editPaperView = LayoutInflater.from(context).inflate(R.layout.dialog_paper_details, null)
        editPaperView.findViewById<TextView>(R.id.dialog_prompt).text = "Edit Paper Details"
        editPaperView.findViewById<MaterialButton>(R.id.upload_paper_button).text = "Save"
        populateEditPaperDetailsDialog(editPaperView)

        val paperDialog = AlertDialog.Builder(context)
            .setView(editPaperView)
            .create()
        paperDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        editPaperView.findViewById<MaterialButton>(R.id.add_tag_button).setOnClickListener { addNewTags(editPaperView) }
        editPaperView.findViewById<MaterialButton>(R.id.upload_paper_button).setOnClickListener {
            paperDetailsMap = mutableMapOf<String, Any?>().apply {
                this["title"] = editPaperView.findViewById<EditText>(R.id.title_input).text.toString()
                this["author"] = editPaperView.findViewById<EditText>(R.id.author_input).text.toString()
                this["publishDate"] = editPaperView.findViewById<EditText>(R.id.date_published_input).text.toString()
                this["topic"] = topics
            }
            papersViewModel.updatePaper(paper.paperID, paperDetailsMap)
            isEdited = true
            populateBottomSheet()
            paperDialog.dismiss()
        }
        paperDialog.show()
    }

    // Populate the edit paper dialog with the metadata
    private fun populateEditPaperDetailsDialog(view: View) {
        view.findViewById<EditText>(R.id.title_input).setText(paper.title)
        view.findViewById<EditText>(R.id.author_input).setText(paper.author)
        view.findViewById<EditText>(R.id.date_published_input).setText(paper.publishDate)
        setTags(topics, view.findViewById(R.id.topics_input_container))
    }

    /** --------------- Tag Methods --------------- **/

    // Set the tags in the container
    private fun setTags(topics : MutableList<String>?, container: FlexboxLayout) {
        topics?.forEach { topic -> addTagToContainer(container, topic) }
    }

    // Displays a dialog to add a new tag
    private fun addNewTags(view : View) {
        val customView = layoutInflater.inflate(R.layout.dialog_tag, null)
        val inputField = customView.findViewById<EditText>(R.id.tag_input)
        val positiveButton = customView.findViewById<Button>(R.id.positive_button)
        val negativeButton = customView.findViewById<Button>(R.id.negative_button)
        val container = view.findViewById<FlexboxLayout>(R.id.topics_input_container)

        val dialog = AlertDialog.Builder(context)
            .setView(customView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        positiveButton.setOnClickListener {
            val topic = inputField.text.toString().trim()
            if (topic.isNotEmpty()) {
                topics?.plusAssign(topic)
                addTagToContainer(container, topic)
            }
            dialog.dismiss()
        }

        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    // Add a tag to the container
    private fun addTagToContainer(container: FlexboxLayout, topic: String) {
        val tagView = TextView(context).apply {
            text = topic
            setPadding(20, 10, 20, 10)
            setTextColor(ContextCompat.getColor(context, R.color.beige))
            typeface = ResourcesCompat.getFont(context, R.font.poppins_medium)
            textSize = 14f
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bckg_topic_tag)
            setOnClickListener {
                showEditTagDialog(topic) { editedTopic -> text = editedTopic }
            }
            setOnLongClickListener {
                topics.let { it1 -> it1?.let { it2 -> topics?.drop(it2.indexOf(topic)) } }
                container.removeView(this)
                true
            }
        }

        val layoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = 10
            bottomMargin = 10
        }
        tagView.layoutParams = layoutParams

        container.addView(tagView, container.childCount - 1)
    }

    // Displays a dialog to edit a tag
    private fun showEditTagDialog(topic: String, onTopicUpdated: (String)-> Unit) {
        val customView = layoutInflater.inflate(R.layout.dialog_tag, null)
        customView.findViewById<TextView>(R.id.dialog_prompt_tag).apply { text = "Edit Tag"}
        customView.findViewById<MaterialButton>(R.id.positive_button).apply { text = "Save" }
        val inputField = customView.findViewById<EditText>(R.id.tag_input)
        val positiveButton = customView.findViewById<Button>(R.id.positive_button)
        val negativeButton = customView.findViewById<Button>(R.id.negative_button)

        val dialog = AlertDialog.Builder(context)
            .setView(customView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        inputField.setText(topic)

        positiveButton.setOnClickListener {
            val editedTopic = inputField.text.toString().trim()
            if (editedTopic.isNotEmpty()) {
                val index = topics?.indexOf(topic)
                if (index != null) {
                    topics?.set(index, editedTopic)
                }
                onTopicUpdated(editedTopic)
            }
            dialog.dismiss()
        }

        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }

    /** --------------- PDF Handling Methods --------------- **/

    // Open the PDF in an external app
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

    // Displays a prompt to install a PDF reader
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