package com.example.techpaperjournal.data.model

import com.google.firebase.Timestamp
import com.google.type.Date
import org.w3c.dom.Text

data class Paper (
    val paperID : String = "",
    val title : String = "",
    val author : String = "",
    val publishDate : Timestamp = Timestamp.now(),
    val uploadDate : Timestamp = Timestamp.now(),
    val topic : List<String> = emptyList(),
    val fileUrl : String = "",
    val summaryText : String = ""
)