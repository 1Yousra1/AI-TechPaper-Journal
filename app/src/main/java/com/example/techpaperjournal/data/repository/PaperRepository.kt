package com.example.techpaperjournal.data.repository

import android.net.Uri
import com.example.techpaperjournal.data.model.Paper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID


class PaperRepository {
    private val db = FirebaseFirestore.getInstance()
    private val papersCollection = db.collection("papers")
    private val storage = FirebaseStorage.getInstance()

    // Fetch all papers
    fun getPapers(): Flow<List<Paper>> = callbackFlow {
        val listener = papersCollection.orderBy("uploadDate", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val papers = snapshot?.toObjects(Paper::class.java) ?: emptyList()
            trySend(papers)
        }

        awaitClose { listener.remove() }
    }


    // Fetch a specific paper
     fun getPaper(id: String): Flow<Paper?> = flow {
        try {
            val paper = papersCollection.document(id).get().await().toObject(Paper::class.java)
            emit(paper)
        } catch (e: Exception) {
            emit(null)
        }
    }

    // Add a new paper
    suspend fun addPaper(paperUri: Uri, metadata: Map<String, String?>) : Boolean {
        return try {
            val paperID = UUID.randomUUID().toString()
            val fileName = "papers/$paperID.pdf"
            val storageRef = storage.reference.child(fileName)
            storageRef.putFile(paperUri).await()
            val pdfUrl = storageRef.downloadUrl.await().toString()

            val paper = Paper(
                paperID = paperID,
                title = metadata["Title"] ?: "Untitled",
                author = metadata["Author"] ?: "Unknown",
                publishDate = metadata["Publish Date"] ?: "",
                uploadDate = Timestamp.now(),
                topic = if (metadata["Topic"]?.isBlank() == true)  null else metadata["Topic"]?.split(","),
                fileUrl = pdfUrl,
                summaryText = "Not generated yet."
            )
            papersCollection.document(paper.paperID).set(paper).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    /* Update an existing paper
    suspend fun updatePaper(paper: Paper) : Boolean {
    }*/

    // Delete a paper
    suspend fun deletePaper(paperId: String) {
        try {
            papersCollection.document(paperId).delete().await()
            val storageRef = storage.reference.child("papers/$paperId.pdf")
            storageRef.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}