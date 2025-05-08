package com.example.techpaperjournal.core.model

import com.google.firebase.Timestamp

data class Entry (
    val entryID: String = "",
    val paperID: String = "",
    val creationDate: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now()
)