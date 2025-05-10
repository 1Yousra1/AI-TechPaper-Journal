package com.example.techpaperjournal.core.repository

import android.util.Log
import com.example.techpaperjournal.core.model.Entry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EntryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val entriesCollection = db.collection("entries")

    // Add a new entry
    suspend fun addEntry(paperID: String?) : String? {
        return try {
            val entryID = UUID.randomUUID().toString()
            val entry = Entry(
                entryID = entryID,
                paperID = paperID!!,
                creationDate = Timestamp.now(),
                lastUpdated = Timestamp.now()
            )
            entriesCollection.document(entry.entryID).set(entry).await()
            Log.d("Entries (Radd)", "Entry added successfully: $entry")
            entryID
        } catch (e: Exception) {
            Log.e("EntryRepository", "Error adding entry: ${e.message}", e)
            null
        }
    }

    // Fetch all entries
    fun getEntries(): Flow<List<Entry>> = callbackFlow {
        val listener = entriesCollection.orderBy("lastUpdated", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val entries = snapshot?.toObjects(Entry::class.java) ?: emptyList()
            Log.d("Entries (RepoGet)", "Fetched $entries")
            trySend(entries)
        }
        awaitClose { listener.remove() }
    }

    // Fetch a specific entry
    fun getEntry(id: String): Flow<Entry?> = flow {
        try {
            val entry = entriesCollection.document(id).get().await().toObject(Entry::class.java)
            emit(entry)
        } catch (e: Exception) {
            emit(null)
        }
    }

    // Update an entry last updated date
    suspend fun updateEntryLastUpdated(entryId: String) {
        try {
            entriesCollection.document(entryId).update("lastUpdated", Timestamp.now()).await()
        } catch (e: Exception) {
            Log.e("EntryRepository", "Error updating entry: ${e.message}", e)
        }
    }

    // Delete a entry
    suspend fun deleteEntry(entryId: String) {
        try {
            entriesCollection.document(entryId).delete().await()
        } catch (e: Exception) {
            Log.e("EntryRepository", "Error deleting entry: ${e.message}", e)
        }
    }

    // Delete an entry associated with a paper
    suspend fun deletePapersEntry(paperId: String): String? {
        return try {
            val querySnapshot = entriesCollection.whereEqualTo("paperID", paperId).get().await()
            querySnapshot.documents.forEach { it.reference.delete().await() }
            return querySnapshot.documents.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("EntryRepository", "Error deleting entry: ${e.message}", e)
            null
        }
    }
}