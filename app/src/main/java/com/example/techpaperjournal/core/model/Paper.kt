package com.example.techpaperjournal.core.model

import com.google.firebase.Timestamp

data class Paper(
    val paperID: String = "",
    val title: String = "",
    val author: String = "",
    val publishDate: String = "",
    val lastAccessed: Timestamp = Timestamp.now(),
    val uploadDate: Timestamp = Timestamp.now(),
    val topic: List<String>? = null,
    val numOfPages: Int = 1,
    val fileUrl: String = "",
    val summaryText: String = ""
)