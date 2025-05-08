package com.example.techpaperjournal.features.journal.pages

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.techpaperjournal.R
import com.example.techpaperjournal.databinding.FragmentPageAboutBinding
import com.example.techpaperjournal.features.library.entries.EntriesViewModel
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.io.File

class AboutPageFragment: Fragment() {
    private var _binding: FragmentPageAboutBinding? = null
    private val binding get() = _binding!!

    private lateinit var entriesViewModel: EntriesViewModel
    private val entryId = arguments?.getString("entryId")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        entriesViewModel = ViewModelProvider(this)[EntriesViewModel::class.java]
        _binding = FragmentPageAboutBinding.inflate(inflater, container, false)
        val root: View = binding.root
        setupContent()
        return root
    }

    // Observe and set the content
    private fun setupContent() {
        lifecycleScope.launch {
            entriesViewModel.fetchEntryAndPaper(entryId!!)
            entriesViewModel.entryListState.observe(viewLifecycleOwner) { entryUiState ->
                entryUiState.entriesWithPapers.forEach { entryWithPaper ->
                    binding.datePublished.text = entryWithPaper.paper.publishDate
                    binding.pageNumber.text = "${entryWithPaper.paper.numOfPages} pages"
                    binding.paperSummary.text = entryWithPaper.paper.summaryText
                    binding.openPaperButton.setOnClickListener { openPdf(entryWithPaper.paper.paperID) }
                }
            }
        }
    }

    // Open the PDF in the external PDF reader
    private fun openPdf(paperID: String) {
        FirebaseStorage.getInstance()
            .getReference("papers/${paperID}.pdf")
            .downloadUrl
            .addOnSuccessListener { uri ->
                val contentUri = Uri.parse(uri.toString())
                openPdfInExternalApp(contentUri)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load PDF", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openPdfInExternalApp(firebaseUri: Uri) {
        if (!isAdded) {
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

    // Display a dialog to install the PDF reader
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