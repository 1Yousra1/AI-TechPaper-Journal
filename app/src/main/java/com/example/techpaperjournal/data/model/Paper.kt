package com.example.techpaperjournal.data.model

import com.google.firebase.Timestamp

data class Paper(
    val paperID: String = "",
    val title: String = "",
    val author: String = "",
    val publishDate: String = "",
    val uploadDate: Timestamp = Timestamp.now(),
    val topic: List<String>? = null,
    val fileUrl: String = "",
    val summaryText: String = ""
)