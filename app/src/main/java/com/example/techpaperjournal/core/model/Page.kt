package com.example.techpaperjournal.core.model

data class Page (
    val pageID: String = "",
    val entryID: String = "",
    val type: String = "",
    val title: String = "",
    val content: String = "",
    val pageOrder: Int = 0
)