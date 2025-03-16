package com.example.techpaperjournal.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.techpaperjournal.R
import com.example.techpaperjournal.databinding.FragmentHomeBinding

// Request code for selecting a PDF document.
const val PICK_PDF_FILE = 2

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.addButton.setOnClickListener {
            val addDialogView = LayoutInflater.from(context).inflate(R.layout.custom_add_dialog, null)
            val dialog = AlertDialog.Builder(context)
                .setView(addDialogView)
                .create()
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
            addDialogView.findViewById<TextView>(R.id.add_entry_button).setOnClickListener {
                dialog.dismiss()
                val addEntryView = LayoutInflater.from(context).inflate(R.layout.custom_add_entry_dialog, null)
                val entryDialog = AlertDialog.Builder(context)
                    .setView(addEntryView)
                    .create()
                entryDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                entryDialog.show()
            }
            addDialogView.findViewById<TextView>(R.id.upload_paper_button).setOnClickListener {
                openFile()
                dialog.dismiss()
                val addPaperView = LayoutInflater.from(context).inflate(R.layout.custom_upload_paper_dialog, null)
                val paperDialog = AlertDialog.Builder(context)
                    .setView(addPaperView)
                    .create()
                paperDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                paperDialog.show()
            }
        }

        return root
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

        }

        startActivityForResult(intent, PICK_PDF_FILE)
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                uploadPdf(uri)
            }
        }
    }

    private fun uploadPdf(pdfUri: Uri) {
        Toast.makeText(requireContext(), "PDF selected: $pdfUri", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}