package com.example.techpaperjournal.features.library.papers

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.techpaperjournal.R
import com.example.techpaperjournal.features.journal.pages.PagesViewModel
import com.example.techpaperjournal.features.library.entries.EntriesViewModel
import com.example.techpaperjournal.features.library.entries.EntryAdapter
import com.example.techpaperjournal.features.library.entries.EntryWithPaper
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

abstract class BaseFragment: Fragment() {
    protected lateinit var papersViewModel: PapersViewModel
    protected lateinit var entriesViewModel: EntriesViewModel
    protected lateinit var pagesViewModel: PagesViewModel

    lateinit var papersRecyclerView: RecyclerView
    lateinit var paperAdapter: PaperAdapter
    lateinit var entriesRecyclerView: RecyclerView
    private lateinit var entryAdapter: EntryAdapter

    private val paperDetailsMap = mutableMapOf<String, String?>()
    private var lastPaperCount = 0

    /** --------------- Setup Methods --------------- **/

    // Set up the recycler view for displaying papers
    fun setupPaperRecyclerView(isHome: Boolean) {
        paperAdapter = PaperAdapter(
            requireContext(), mutableListOf(),
            onPaperClick = { paper -> PaperDetailsBottomSheet(paper).show(childFragmentManager, PaperDetailsBottomSheet.TAG) },
            onPaperLongClick = { paper -> showDeleteDialog("paper", paper.paperID) },
            isHome
        )
        papersRecyclerView.adapter = paperAdapter
        if (isHome)
            papersRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        else
            papersRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
    }

    // Set up the recycler view for displaying entries
    fun setupEntryRecyclerView(isHome: Boolean) {
        entryAdapter = EntryAdapter(requireContext(), mutableListOf(),
            onEntryClick = { entriesAndPapers -> navigateToEntry(entriesAndPapers.entry.entryID) },
            onEntryLongClick = { entriesAndPapers -> showDeleteDialog("entry", entriesAndPapers.entry.entryID) },
            isHome
        )
        entriesRecyclerView.adapter = entryAdapter
        if (isHome)
            entriesRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        else
            entriesRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    // Navigate to the entry fragment
    private fun navigateToEntry(entryId: String) {
        val bundle = Bundle().apply {
            putString("entryId", entryId)
        }
        findNavController().navigate(R.id.pageHostFragment, bundle)
    }

    // Observe changes in paper list and update the adapter
    fun observePaperViewModel(noPapersTv: TextView, isHome: Boolean) {
        //var papers: List<Paper>
        papersViewModel.paperListState.observe(viewLifecycleOwner) { uiState ->
            val papers = if (isHome) uiState.papers.take(8) else uiState.papers
            paperAdapter.updatePapers(papers)

            if (paperAdapter.itemCount == 0)
                noPapersTv.visibility = View.VISIBLE
            else
                noPapersTv.visibility = View.GONE
        }
        papersViewModel.fetchPapers()
    }

    // Observe changes in entry list and update the adapter
    fun observeEntryViewModel(noEntriesTv: TextView, isHome: Boolean) {
        var entries: List<EntryWithPaper>
        entriesViewModel.entryListState.observe(viewLifecycleOwner) { uiState ->
            entries = if (isHome) uiState.entriesWithPapers.take(8) else uiState.entriesWithPapers
            entryAdapter.updateEntries(entries)

            if (entryAdapter.itemCount == 0)
                noEntriesTv.visibility = View.VISIBLE
            else
                noEntriesTv.visibility = View.GONE
        }
        entriesViewModel.fetchEntriesAndPapers()
    }

    /** --------------- PDF Handling Methods --------------- **/

    // Launch the file picker
    private val pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleSelectedPdf(it) }
    }

    // Open the file picker
    private fun openFilePicker() {
        pdfPickerLauncher.launch("application/pdf")
    }

    // Handle the selected PDF
    private fun handleSelectedPdf(uri: Uri) {
        val fileBytes = getFileBytes(requireContext(), uri)
        val paperId = UUID.nameUUIDFromBytes(fileBytes).toString()

        lifecycleScope.launch {
            val isDuplicate = papersViewModel.isDuplicatePaper(paperId)
            if (isDuplicate) {
                Toast.makeText(context, "This paper has already been uploaded.", Toast.LENGTH_SHORT).show()
            } else {
                showPaperDetailsDialog(uri, paperId)
            }
        }
    }

    // Get the file bytes of the PDF
    private fun getFileBytes(context: Context, uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
    }

    // Extracts metadata and text from the PDF
    private fun extractPaperDetails(paperId: String, paperUri: Uri, context: Context) {
        try {
            PDFBoxResourceLoader.init(context)
            requireContext().contentResolver.openInputStream(paperUri)?.use { input ->
                val document: PDDocument = PDDocument.load(input)
                val metadata = document.documentInformation
                paperDetailsMap["PaperID"] = paperId
                paperDetailsMap["Title"] = metadata.title ?: ""
                paperDetailsMap["Author"] = metadata.author ?: ""
                paperDetailsMap["Topic"] = if (metadata.subject.isNullOrBlank()) metadata.keywords else if (metadata.keywords.isNullOrBlank()) metadata.subject else null
                paperDetailsMap["Pages"] = document.numberOfPages.toString()

                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val creationDate = metadata.creationDate ?: metadata.modificationDate
                paperDetailsMap["Publish Date"] = if (creationDate == null) "" else dateFormat.format(creationDate.time)

                val pdfStripper = PDFTextStripper()
                pdfStripper.startPage = 0
                pdfStripper.endPage = document.numberOfPages - 1
                paperDetailsMap["Content"] = pdfStripper.getText(document)
                document.close()
                Log.d("paperDetailsMap", paperDetailsMap.toString())
            }
        } catch (e: Exception) {
            Log.e("BaseFragment", "Error extracting PDF details: ${e.message}")
            Toast.makeText(context, "Failed to extract PDF details.", Toast.LENGTH_SHORT).show()
        }
    }

    /**--------------- Dialog Methods ---------------**/

    // Creates a custom dialog with a provided layout and setup function.
    private fun createCustomDialog(layoutRes: Int, setupView: (View, AlertDialog) -> Unit): AlertDialog {
        val view = LayoutInflater.from(context).inflate(layoutRes, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create().apply {
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        setupView(view, dialog)
        return dialog
    }

    // Displays a dialog for adding an entry or uploading a paper
    fun showAddDialog(addButton: View) {
        addButton.setOnClickListener {
            val dialog = createCustomDialog(R.layout.dialog_add_main) { addDialogView, dialog ->
                addDialogView.findViewById<TextView>(R.id.add_entry_button).setOnClickListener {
                    openAddEntryDialog()
                    dialog.dismiss()
                }

                addDialogView.findViewById<TextView>(R.id.upload_paper_dialog_button).setOnClickListener {
                    openFilePicker()
                    dialog.dismiss()
                }
            }
            dialog.show()
        }
    }

    // Displays a dialog for uploading a paper
    private fun showPaperDetailsDialog(uri: Uri, paperId: String) {
        createCustomDialog(R.layout.dialog_paper_details) { addPaperView, dialog ->
            extractPaperDetails(paperId, uri, requireContext())
            populatePaperDetails(addPaperView)
            dialog.show()

            addPaperView.findViewById<MaterialButton>(R.id.add_tag_button).setOnClickListener {
                addNewTags(addPaperView)
            }

            addPaperView.findViewById<MaterialButton>(R.id.upload_paper_button).setOnClickListener {
                getUpdatedMetadata(addPaperView)
                showUploadDialog()
                papersViewModel.uploadPaper(uri, paperDetailsMap)
                dialog.dismiss()
            }
        }
    }

    // Populates the paper dialog with the metadata
    private fun populatePaperDetails(view: View) {
        val titleEditText = view.findViewById<EditText>(R.id.title_input)
        val authorEditText = view.findViewById<EditText>(R.id.author_input)
        val datePublishedEditText = view.findViewById<EditText>(R.id.date_published_input)
        val container = view.findViewById<FlexboxLayout>(R.id.topics_input_container)

        titleEditText.setText(paperDetailsMap["Title"] ?: "Untitled")
        authorEditText.setText(paperDetailsMap["Author"] ?: "Unknown")
        datePublishedEditText.setText(paperDetailsMap["Publish Date"] ?: "Unknown")
        paperDetailsMap["Topic"]?.split(",")?.forEach { topic ->
            val tagView = TextView(context).apply {
                text = topic
                setPadding(20, 10, 20, 10)
                setTextColor(ContextCompat.getColor(context, R.color.beige))
                typeface = ResourcesCompat.getFont(context, R.font.poppins_medium)
                textSize = 14f
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bckg_topic_tag)
                setOnClickListener {
                    showEditTagDialog(topic) { editedTopic -> text = editedTopic }
                }
                setOnLongClickListener {
                    paperDetailsMap["Topic"]?.let { it1 -> paperDetailsMap["Topic"]?.drop(it1.indexOf(topic)) }
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
    }

    // Get the updated metadata from the dialog
    private fun getUpdatedMetadata(view: View) {
        val titleEditText = view.findViewById<EditText>(R.id.title_input)
        val authorEditText = view.findViewById<EditText>(R.id.author_input)
        val datePublishedEditText = view.findViewById<EditText>(R.id.date_published_input)
        if (titleEditText.text.toString().isBlank())
            titleEditText.error = "Title is required"
        if (authorEditText.text.toString().isBlank())
            authorEditText.error = "Author is required"
        if (datePublishedEditText.text.toString().isBlank())
            datePublishedEditText.error = "Date published is required"
        paperDetailsMap["Title"] = titleEditText.text.toString()
        paperDetailsMap["Author"] = authorEditText.text.toString()
        paperDetailsMap["Publish Date"] = datePublishedEditText.text.toString()
    }

    // Displays a dialog for adding new tags
    private fun addNewTags(view : View) {
        val dialog = createCustomDialog(R.layout.dialog_tag) { addTagView, dialog ->
            val inputField = addTagView.findViewById<EditText>(R.id.tag_input)
            val positiveButton = addTagView.findViewById<Button>(R.id.positive_button)
            val negativeButton = addTagView.findViewById<Button>(R.id.negative_button)
            val container = view.findViewById<FlexboxLayout>(R.id.topics_input_container)

            positiveButton.setOnClickListener {
                val topic = inputField.text.toString().trim()
                if (topic.isNotEmpty()) {
                    paperDetailsMap["Topic"] = (paperDetailsMap["Topic"] ?: "") + ", $topic"
                    addTagToContainer(container, topic)
                }
                dialog.dismiss()
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
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
                paperDetailsMap["Topic"]?.let { it1 -> paperDetailsMap["Topic"]?.drop(it1.indexOf(topic)) }
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

    // Displays a dialog for editing a tag
    private fun showEditTagDialog(topic: String, onTopicUpdated: (String)-> Unit) {
        val dialog = createCustomDialog(R.layout.dialog_tag) { editTagView, dialog ->
            val positiveButton = editTagView.findViewById<Button>(R.id.positive_button)
            val negativeButton = editTagView.findViewById<Button>(R.id.negative_button)
            editTagView.findViewById<TextView>(R.id.dialog_prompt_tag).text = "Edit Tag"
            positiveButton.text = "Save"
            val inputField = editTagView.findViewById<EditText>(R.id.tag_input)
            inputField.setText(topic)

            positiveButton.setOnClickListener {
                val editedTopic = inputField.text.toString().trim()
                if (editedTopic.isNotEmpty()) {
                    paperDetailsMap["Topic"] =
                        (paperDetailsMap["Topic"] ?: topic).replace(topic, editedTopic)
                    onTopicUpdated(editedTopic)
                }
                dialog.dismiss()
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // Displays a dialog for deleting an entry or paper
    private fun showDeleteDialog(item: String, id: String) {
        val dialog = createCustomDialog(R.layout.dialog_delete) { deleteDialogView, dialog ->
            val prompt = if (item == "paper") "Delete Paper" else "Delete Entry"
            val message =
                if (item == "paper") "Are you sure you want to delete this paper? The associated entry will also be deleted." else "Are you sure you want to delete this entry?"
            deleteDialogView.findViewById<TextView>(R.id.dialog_prompt).apply { text = prompt }
            deleteDialogView.findViewById<TextView>(R.id.dialog_message).apply { text = message }
            val positiveButton =
                deleteDialogView.findViewById<Button>(R.id.positive_button).apply { text = "Yes" }
            val negativeButton =
                deleteDialogView.findViewById<Button>(R.id.negative_button).apply { text = "No" }

            positiveButton.setOnClickListener {
                if (item == "paper") {
                    papersViewModel.deletePaper(id)
                    entriesViewModel.deletePapersEntry(id)
                    pagesViewModel.deletePages(id)
                } else {
                    entriesViewModel.deleteEntry(id)
                    pagesViewModel.deletePages(id)
                }
                dialog.dismiss()
            }
            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // Displays a loading dialog while uploading a paper
    private fun showUploadDialog() {
        val dialog = createCustomDialog(R.layout.dialog_progress_bar) { loadingDialog, dialog ->
            val icon = loadingDialog.findViewById<ImageView>(R.id.pdf_icon)
            val progressBar = loadingDialog.findViewById<ProgressBar>(R.id.progress_bar)

            var startedAnimation = false

            papersViewModel.paperListState.observe(viewLifecycleOwner) { state ->
                if (state.isUploading && !startedAnimation) {
                    startedAnimation = true

                    val animator = ValueAnimator.ofInt(0, 100)
                    animator.duration = 10000
                    animator.addUpdateListener {
                        val progress = it.animatedValue as Int
                        progressBar.progress = progress

                        val progressRatio = progress / 100f
                        val maxTranslation = progressBar.width - icon.width
                        icon.translationX = maxTranslation * progressRatio
                    }
                    animator.start()
                }

                if (state.uploadSuccess == true) {
                    dialog.dismiss()
                    lifecycleScope.launch {
                        papersViewModel.paperListState.observe(viewLifecycleOwner) { uiState ->
                            val papers = uiState.papers

                            if (papers.size > lastPaperCount && papers.isNotEmpty()) {
                                PaperDetailsBottomSheet(papers.first()).show(
                                    childFragmentManager,
                                    PaperDetailsBottomSheet.TAG
                                )
                            }
                            lastPaperCount = papers.size
                        }
                    }
                }

                if (state.uploadSuccess == false) {
                    dialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        state.errorMessage ?: "Upload failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        dialog.show()
    }

    // Displays a dialog for adding an entry
    private fun openAddEntryDialog() {
        val dialog = createCustomDialog(R.layout.dialog_add_entry) { addEntryDialog, dialog ->
            var paperID: String? = null
            lifecycleScope.launch {
                entriesViewModel.entryListState.observe(viewLifecycleOwner) { entryState ->
                    val usedPaperIds = entryState.entries.map { it.paperID }
                    papersViewModel.paperListState.observe(viewLifecycleOwner) { paperState ->
                        val papers = paperState.papers.filter { it.paperID !in usedPaperIds }
                        if (papers.isEmpty()) {
                            dialog.dismiss()
                            Toast.makeText(
                                context,
                                "No papers available to add entry",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Populate the paper chooser with filtered papers
                        val paperTitles = papers.map { it.title }
                        val adapter = ArrayAdapter(
                            requireContext(),
                            R.layout.item_entry_dropdown,
                            paperTitles
                        )
                        val paperChooser =
                            addEntryDialog.findViewById<AutoCompleteTextView>(R.id.paper_chooser)
                        paperChooser.threshold = 1
                        paperChooser.setAdapter(adapter)

                        // Handle paper selection
                        paperChooser.setOnItemClickListener { _, _, position, _ ->
                            val selectedTitle = adapter.getItem(position)
                            val selectedPaper = papers.find { it.title == selectedTitle }

                            selectedPaper?.let {
                                paperID = it.paperID
                            }
                        }
                    }
                }
                addEntryDialog.findViewById<MaterialButton>(R.id.add_entry_button)
                    .setOnClickListener {
                        addEntry(paperID)
                        dialog.dismiss()
                    }
            }
        }
        dialog.show()
    }

    // Add the entry and associated pages to the database
    private fun addEntry(paperID: String?) {
        lifecycleScope.launch {
            try {
                val entryID = entriesViewModel.addEntry(paperID)
                if (entryID != null) {
                    pagesViewModel.addPage(
                        mutableMapOf(
                            "entryID" to entryID,
                            "type" to "about",
                            "title" to "About",
                            "content" to "",
                            "pageOrder" to 0
                        )
                    )
                    pagesViewModel.addPage(
                        mutableMapOf(
                            "entryID" to entryID,
                            "type" to "notes",
                            "title" to "Notes",
                            "content" to "",
                            "pageOrder" to 1
                        )
                    )
                    entriesViewModel.updateEntryLastUpdated(entryID)
                } else {
                    Toast.makeText(context, "Failed to add entry", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error adding entry: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}