package com.example.techpaperjournal.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.techpaperjournal.R
import com.example.techpaperjournal.adapter.PaperAdapter
import com.example.techpaperjournal.data.model.Paper
import com.example.techpaperjournal.databinding.FragmentHomeBinding
import com.example.techpaperjournal.ui.papers.PaperDetailsBottomSheet
import com.example.techpaperjournal.ui.papers.PapersViewModel
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// Request code for selecting a PDF document.
const val PICK_PDF_FILE = 2

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private lateinit var homeViewModel : HomeViewModel
    private val metadataMap = mutableMapOf<String, String?>()

    private lateinit var papersRecyclerView: RecyclerView
    private lateinit var paperAdapter: PaperAdapter
    private lateinit var papersViewModel: PapersViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
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

        papersViewModel = ViewModelProvider(this)[PapersViewModel::class.java]
        papersRecyclerView = binding.papersRv
        paperAdapter = PaperAdapter(requireContext(), mutableListOf(),
            { paper -> PaperDetailsBottomSheet(paper).show(childFragmentManager, PaperDetailsBottomSheet.TAG) },
            { paper -> showDeleteDialog(paper) })
        papersRecyclerView.adapter = paperAdapter
        papersRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        lifecycleScope.launch {
            papersViewModel.paperListState.observe(viewLifecycleOwner) { uiState ->
                paperAdapter.updatePapers(uiState.papers)

                if (paperAdapter.itemCount == 0)
                    binding.noPapersTv.visibility = View.VISIBLE
                else
                    binding.noPapersTv.visibility = View.GONE
            }
        }
        papersViewModel.fetchPapers()

        return root
    }

    // Open the file selection dialog
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        startActivityForResult(intent, PICK_PDF_FILE)
    }

    // Handle the result of the file picker
    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == com.example.techpaperjournal.ui.library.PICK_PDF_FILE && resultCode == Activity.RESULT_OK) {
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
                    homeViewModel.uploadPaper(uri, metadataMap)
                    paperDialog.dismiss()
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
            metadataMap["Title"] = metadata.title ?: "Untitled"
            metadataMap["Author"] = metadata.author ?: "Unknown"
            metadataMap["Topic"] = if (metadata.subject.isNullOrBlank()) metadata.keywords else if (metadata.keywords.isNullOrBlank()) metadata.subject else null

            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            metadataMap["Publish Date"] = dateFormat.format(metadata.creationDate.time)
            document.close()
        }
    }

    // Populate the paper dialog with the metadata
    private fun populatePaperDialog(view: View) {
        val titleEditText = view.findViewById<EditText>(R.id.title_input)
        val authorEditText = view.findViewById<EditText>(R.id.author_input)
        val datePublishedEditText = view.findViewById<EditText>(R.id.date_published_input)
        val container = view.findViewById<FlexboxLayout>(R.id.topics_input_container)

        titleEditText.setText(metadataMap["Title"] ?: "Untitled")
        authorEditText.setText(metadataMap["Author"] ?: "Unknown")
        datePublishedEditText.setText(metadataMap["Publish Date"] ?: "")
        metadataMap["Topic"]?.split(",")?.forEach { topic ->
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
                    metadataMap["Topic"]?.let { it1 -> metadataMap["Topic"]?.drop(it1.indexOf(topic)) }
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

        metadataMap["Title"] = titleEditText.text.toString()
        metadataMap["Author"] = authorEditText.text.toString()
        metadataMap["Publish Date"] = datePublishedEditText.text.toString()

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
                metadataMap["Topic"] = (metadataMap["Topic"] ?: "") + ", $topic"
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
                metadataMap["Topic"]?.let { it1 -> metadataMap["Topic"]?.drop(it1.indexOf(topic)) }
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
                metadataMap["Topic"] = (metadataMap["Topic"] ?: topic).replace(topic, editedTopic)
                onTopicUpdated(editedTopic)
            }
            dialog.dismiss()
        }

        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }

    // Show the delete dialog for the paper
    private fun showDeleteDialog(paper: Paper) {
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_paper, null)
        val positiveButton = customView.findViewById<Button>(R.id.positive_button).apply { text = "Yes" }
        val negativeButton = customView.findViewById<Button>(R.id.negative_button).apply { text = "No" }

        val dialog = AlertDialog.Builder(context)
            .setView(customView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        positiveButton.setOnClickListener {
            papersViewModel.deletePaper(paper.paperID)
            dialog.dismiss()
        }
        negativeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}