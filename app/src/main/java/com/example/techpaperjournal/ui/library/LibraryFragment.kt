package com.example.techpaperjournal.ui.library

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.techpaperjournal.R
import com.example.techpaperjournal.databinding.FragmentLibraryBinding
import com.example.techpaperjournal.ui.entries.EntriesFragment
import com.example.techpaperjournal.ui.papers.PaperDetailsBottomSheet
import com.example.techpaperjournal.ui.papers.PapersFragment
import com.example.techpaperjournal.ui.papers.PapersViewModel
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


// Request code for selecting a PDF document.
const val PICK_PDF_FILE = 2

class LibraryFragment : Fragment() {
    private var _binding: FragmentLibraryBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private lateinit var libraryViewModel : LibraryViewModel
    private lateinit var papersViewModel: PapersViewModel
    private val paperDetailsMap = mutableMapOf<String, String?>()
    private var lastPaperCount = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        libraryViewModel = ViewModelProvider(this)[LibraryViewModel::class.java]
        papersViewModel = ViewModelProvider(this)[PapersViewModel::class.java]
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Set up the add button click listener
        binding.addButton.setOnClickListener {
            val addDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_button, null)
            val dialog = AlertDialog.Builder(context)
                .setView(addDialogView)
                .create()
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()

            // Set up the add entry button click listener
            addDialogView.findViewById<TextView>(R.id.add_entry_button).setOnClickListener {
                dialog.dismiss()
                val addEntryView = LayoutInflater.from(context).inflate(R.layout.dialog_add_entry, null)
                val entryDialog = AlertDialog.Builder(context)
                    .setView(addEntryView)
                    .create()
                entryDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                entryDialog.show()
            }

            // Set up the upload paper button click listener
            addDialogView.findViewById<TextView>(R.id.upload_paper_dialog_button).setOnClickListener {
                openFilePicker()
                dialog.dismiss()
            }

        }

        binding.searchButton.setOnClickListener {
            binding.searchBar.visibility = View.VISIBLE
            binding.root.setOnClickListener {
                binding.searchBar.visibility = View.INVISIBLE
            }
        }

        replaceFragment(PapersFragment())
        binding.papersTab.setOnClickListener {
            replaceFragment(PapersFragment())
            binding.entriesTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.bckg_entry_tab_unselected)
            binding.papersTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.bckg_papers_tab_selected)
        }

        binding.entriesTab.setOnClickListener {
            replaceFragment(EntriesFragment())
            binding.entriesTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.bckg_entry_tab_selected)
            binding.papersTab.background = ContextCompat.getDrawable(requireContext(), R.drawable.bckg_papers_tab_unselected)
        }

        return root
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // open the file picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        startActivityForResult(intent, com.example.techpaperjournal.ui.home.PICK_PDF_FILE)
    }

    // Handle the result of the file picker
    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                val addPaperView = LayoutInflater.from(context).inflate(R.layout.dialog_upload_paper, null)
                val paperDialog = AlertDialog.Builder(context)
                    .setView(addPaperView)
                    .create()
                paperDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                extractPaperMetadata(uri)
                populatePaperDialog(addPaperView)
                paperDialog.show()
                addPaperView.findViewById<MaterialButton>(R.id.add_tag_button).setOnClickListener {
                    addTags(addPaperView)
                }
                addPaperView.findViewById<MaterialButton>(R.id.upload_paper_button).setOnClickListener {
                    getUpdatedMetadata(addPaperView)
                    showUploadDialog()
                    libraryViewModel.uploadPaper(uri, paperDetailsMap)
                    paperDialog.dismiss()
                    //val initialPaperCount = papersViewModel.paperListState.value?.papers?.size ?: 0

                }
            }
        }
    }

    // Extract the metadata from the PDF
    private fun extractPaperMetadata(paperUri: Uri) {
        PDFBoxResourceLoader.init(context)
        requireContext().contentResolver.openInputStream(paperUri)?.use { input ->
            val document: PDDocument = PDDocument.load(input)
            val metadata = document.documentInformation
            paperDetailsMap["Title"] = metadata.title ?: "Untitled"
            paperDetailsMap["Author"] = metadata.author ?: "Unknown"
            paperDetailsMap["Topic"] = if (metadata.subject.isNullOrBlank()) metadata.keywords else if (metadata.keywords.isNullOrBlank()) metadata.subject else null
            paperDetailsMap["Pages"] = document.numberOfPages.toString()

            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            paperDetailsMap["Publish Date"] = dateFormat.format(metadata.creationDate.time)

            val pdfStripper = PDFTextStripper()
            pdfStripper.startPage = 0
            pdfStripper.endPage = document.numberOfPages - 1
            paperDetailsMap["Content"] = pdfStripper.getText(document)
            document.close()
        }
    }

    // Populate the paper dialog with the metadata
    private fun populatePaperDialog(view: View) {
        val titleEditText = view.findViewById<EditText>(R.id.title_input)
        val authorEditText = view.findViewById<EditText>(R.id.author_input)
        val datePublishedEditText = view.findViewById<EditText>(R.id.date_published_input)
        val container = view.findViewById<FlexboxLayout>(R.id.topics_input_container)

        titleEditText.setText(paperDetailsMap["Title"] ?: "Untitled")
        authorEditText.setText(paperDetailsMap["Author"] ?: "Unknown")
        datePublishedEditText.setText(paperDetailsMap["Publish Date"] ?: "")
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
                    showEditDialog(topic) { editedTopic -> text = editedTopic }
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

        paperDetailsMap["Title"] = titleEditText.text.toString()
        paperDetailsMap["Author"] = authorEditText.text.toString()
        paperDetailsMap["Publish Date"] = datePublishedEditText.text.toString()
    }

    // Show the add tag dialog
    private fun addTags(view : View) {
        val customView = layoutInflater.inflate(R.layout.dialog_tag_input, null)
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
                paperDetailsMap["Topic"] = (paperDetailsMap["Topic"] ?: "") + ", $topic"
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
                showEditDialog(topic) { editedTopic -> text = editedTopic }
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

    private fun showEditDialog(topic: String, onTopicUpdated: (String)-> Unit) {
        val customView = layoutInflater.inflate(R.layout.dialog_tag_input, null)
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
                paperDetailsMap["Topic"] = (paperDetailsMap["Topic"] ?: topic).replace(topic, editedTopic)
                onTopicUpdated(editedTopic)
            }
            dialog.dismiss()
        }

        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showUploadDialog() {
        val customView = layoutInflater.inflate(R.layout.dialog_progress_bar, null)
        val icon = customView.findViewById<ImageView>(R.id.pdf_icon)
        val progressBar = customView.findViewById<ProgressBar>(R.id.progress_bar)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(customView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        Toast.makeText(requireContext(), "Dialog shown", Toast.LENGTH_SHORT).show()

        var startedAnimation = false

        libraryViewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.isUploading && !startedAnimation) {
                startedAnimation = true

                val animator = ValueAnimator.ofInt(0, 100)
                animator.duration = 8000
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
                Toast.makeText(requireContext(), state.errorMessage ?: "Upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}