package com.example.techpaperjournal.core.repository

import android.util.Log
import com.example.techpaperjournal.core.model.Page
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PageRepository {
    private val db = FirebaseFirestore.getInstance()
    private val pagesCollection = db.collection("pages")

    /** --------------- Adding Pages --------------- **/

    // Add a new page
    suspend fun addPage(pageMap: MutableMap<String, Any?>): Boolean {
        return try {
            val pageID = UUID.randomUUID().toString()
            val page = Page(
                pageID = pageID,
                entryID = pageMap["entryID"] as String,
                type = pageMap["type"] as String,
                title = pageMap["title"] as String,
                content = pageMap["content"] as String,
                pageOrder = pageMap["pageOrder"] as Int
            )
            pagesCollection.document(page.pageID).set(page).await()
            Log.d("PageRepository", "Added page: $page")
            true
        } catch (e: Exception) {
            Log.e("PageRepository", "Error adding page: ${e.message}")
            false
        }
    }

    /** --------------- Fetching Pages --------------- **/

    // Fetch all pages in an entry
     fun fetchPages(entryId: String): Flow<List<Page>> = callbackFlow {
         try {
            val listener = pagesCollection
                .whereEqualTo("entryID", entryId)
                .orderBy("pageOrder")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        Log.e("PageRepository", "Error fetching pages: ${error.message}")
                        return@addSnapshotListener
                    }
                    val pages = snapshot?.toObjects(Page::class.java) ?: emptyList()
                    trySend(pages)
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e("PageRepository", "Error fetching pages: ${e.message}")
        }
    }

    // Fetch a specific page
    fun fetchPage(pageId: String): Flow<Page?> = flow {
        try {
            val page = pagesCollection.document(pageId).get().await().toObject(Page::class.java)
            emit(page)
        } catch (e: Exception) {
            Log.e("PageRepository", "Error fetching page: ${e.message}")
            emit(null)
        }
    }

    /** --------------- Modifying Pages --------------- **/

    // Update a page's content
    suspend fun saveNote(pageId: String, notes: String) {
        pagesCollection.document(pageId).update("content", notes).await()
    }

    // Update a page's title
    suspend fun updatePageTitle(pageId: String, title: String) {
        pagesCollection.document(pageId).update("title", title).await()
    }

    /** --------------- Deleting Pages --------------- **/

    // Delete all pages in an entry
    suspend fun deletePages(entryId: String) {
        pagesCollection.whereEqualTo("entryID", entryId).get().await().documents.forEach {
            it.reference.delete()
        }
    }

    // Delete a page
    suspend fun deletePage(pageId: String) {
        pagesCollection.document(pageId).delete().await()
    }
}