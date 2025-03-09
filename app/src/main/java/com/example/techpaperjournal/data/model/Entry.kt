package com.example.techpaperjournal.data.model

import com.google.firebase.Timestamp

data class Entry (
    val entryID: String = "",
    val paperID: String = "",
    val entryTitle: String = "",
    val creationDate: Timestamp = Timestamp.now()
)