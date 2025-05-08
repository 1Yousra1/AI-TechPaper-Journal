package com.example.techpaperjournal.features.journal.pages

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.techpaperjournal.R
import com.example.techpaperjournal.databinding.FragmentPageNotesBinding
import com.example.techpaperjournal.features.library.entries.EntriesViewModel
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.github.dhaval2404.colorpicker.model.ColorSwatch
import com.google.android.material.button.MaterialButton
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotesPageFragment: Fragment() {
    private var _binding: FragmentPageNotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagesViewModel: PagesViewModel
    private lateinit var entriesViewModel: EntriesViewModel
    private val entryId = arguments?.getString("entryId")
    private val pageId = arguments?.getString("pageId")

    private lateinit var mEditor: RichEditor
    private var autoSaveJob: Job? = null
    private var isEditable: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        pagesViewModel = ViewModelProvider(this)[PagesViewModel::class.java]
        entriesViewModel = ViewModelProvider(this)[EntriesViewModel::class.java]
        _binding = FragmentPageNotesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        mEditor = binding.notesContent
        setupEditor()
        observePageContent()

        return root
    }

    // Initialize the editor
    private fun setupEditor() {
        mEditor.setEditorHeight(200)
        mEditor.setEditorFontSize(16)
        mEditor.setEditorFontColor(Color.DKGRAY)
        mEditor.setPadding(10, 10, 10, 10)
        mEditor.setPlaceholder("Insert text here...")
        setupEditorActions()
    }

    // Observe the page content and update the editor
    private fun observePageContent() {
        if (pageId != null) pagesViewModel.fetchPage(pageId)
        pagesViewModel.uiState.observe(viewLifecycleOwner) { pageUiState ->
            val page = pageUiState.page
            if (page != null) {
                mEditor.html = page.content
            }
        }

        mEditor.setOnTextChangeListener { text ->
            autoSaveJob?.cancel()
            autoSaveJob = lifecycleScope.launch {
                delay(2000)
                if (pageId != null) {
                    pagesViewModel.saveNote(pageId, text)
                    entriesViewModel.updateEntryLastUpdated(entryId!!)
                }

            }
        }
    }

    // Set up the editor actions
    private fun setupEditorActions() {
        binding.actionBack.setOnClickListener {
            mEditor.loadUrl("file:///android_asset/editor.html")
            mEditor.html = mEditor.html
        }
        binding.actionUndo.setOnClickListener { mEditor.undo() }
        binding.actionRedo.setOnClickListener { mEditor.redo() }
        binding.actionEdit.setOnClickListener {
            mEditor.setInputEnabled(!isEditable)
            isEditable = !isEditable
        }
        binding.actionErase.setOnClickListener { mEditor.removeFormat() }
        binding.actionBold.setOnClickListener { mEditor.setBold() }
        binding.actionItalic.setOnClickListener { mEditor.setItalic() }
        binding.actionSubscript.setOnClickListener { mEditor.setSubscript() }
        binding.actionSuperscript.setOnClickListener { mEditor.setSuperscript() }
        binding.actionStrikethrough.setOnClickListener { mEditor.setStrikeThrough() }
        binding.actionUnderline.setOnClickListener{ mEditor.setUnderline() }
        binding.actionHeading1.setOnClickListener { mEditor.setHeading(1) }
        binding.actionHeading2.setOnClickListener { mEditor.setHeading(2) }
        binding.actionHeading3.setOnClickListener { mEditor.setHeading(3) }
        binding.actionHeading4.setOnClickListener{ mEditor.setHeading(4) }
        binding.actionHeading5.setOnClickListener { mEditor.setHeading(5) }
        binding.actionHeading6.setOnClickListener{ mEditor.setHeading(6) }

        binding.actionTxtColor.setOnClickListener {
            //mEditor.setTextColor(if(isColorChanged) Color.BLACK else Color.BLUE)
            showColorPicker { selectedColor ->
                mEditor.setTextColor(selectedColor)
                binding.actionTxtColor.setColorFilter(selectedColor)
            }
            //isColorChanged = !isColorChanged
        }

        binding.actionBgColor.setOnClickListener {
            //mEditor.setTextBackgroundColor(if(isHighlightChanged) Color.WHITE else Color.YELLOW)
            showColorPicker { selectedColor ->
                mEditor.setTextBackgroundColor(selectedColor)
                binding.actionBgColor.setColorFilter(selectedColor)
            }
            //isHighlightChanged = !isHighlightChanged
        }

        binding.actionIndent.setOnClickListener { mEditor.setIndent() }
        binding.actionOutdent.setOnClickListener{ mEditor.setOutdent() }
        binding.actionAlignLeft.setOnClickListener { mEditor.setAlignLeft() }
        binding.actionAlignCenter.setOnClickListener{ mEditor.setAlignCenter() }
        binding.actionAlignRight.setOnClickListener { mEditor.setAlignRight() }
        binding.actionBlockquote.setOnClickListener { mEditor.setBlockquote(); }
        binding.actionInsertBullets.setOnClickListener { mEditor.setBullets() }
        binding.actionInsertNumbers.setOnClickListener { mEditor.setNumbers() }

        binding.actionInsertLink.setOnClickListener {
            showInsertLinkDialog()
        }

        binding.actionInsertCheckbox.setOnClickListener { mEditor.insertTodo() }
    }

    private fun showColorPicker(onColorSelected: (Int) -> Unit) {
        // Kotlin Code
        MaterialColorPickerDialog
            .Builder(requireContext())
            .setTitle("Pick Text Color")
            .setColorShape(ColorShape.SQAURE)
            .setColorSwatch(ColorSwatch._300)
            .setDefaultColor(R.color.white)
            .setColorListener { color, _ ->
                onColorSelected(color)
            }
            .show()
    }

    private fun showInsertLinkDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_insert_link, null)
        val linkEditText = dialogView.findViewById<EditText>(R.id.link_input)
        val titleEditText = dialogView.findViewById<EditText>(R.id.title_input)
        val createLinkButton = dialogView.findViewById<MaterialButton>(R.id.create_link_button)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            createLinkButton.setOnClickListener {
                val url = linkEditText.text.toString()
                val title = titleEditText.text.toString()
                if (url.isNotEmpty() && title.isNotEmpty()) {
                    mEditor.focusEditor()
                    mEditor.insertLink(url, title)
                }
                dialog.dismiss()
            }

        dialog.show()
    }
}