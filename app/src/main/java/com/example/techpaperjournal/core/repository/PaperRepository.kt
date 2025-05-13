package com.example.techpaperjournal.core.repository

import android.net.Uri
import android.util.Log
import com.example.techpaperjournal.core.openai.OpenAIClient
import com.example.techpaperjournal.core.openai.Message
import com.example.techpaperjournal.core.openai.OpenAIRequest
import com.example.techpaperjournal.core.model.Paper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.util.UUID

class PaperRepository {
    private val db = FirebaseFirestore.getInstance()
    private val papersCollection = db.collection("papers")
    private val storage = FirebaseStorage.getInstance()

    // Fetch all papers
    fun getPapers(): Flow<List<Paper>> = callbackFlow {
        val listener = papersCollection.orderBy("lastAccessed", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
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
    suspend fun addPaper(paperUri: Uri, details: Map<String, String?>) : Boolean {
        return try {
            val paperID = details["PaperID"]
            val fileName = "papers/$paperID.pdf"
            val storageRef = storage.reference.child(fileName)
            storageRef.putFile(paperUri).await()
            val pdfUrl = storageRef.downloadUrl.await().toString()
            val content = details["Content"]
            val summary = if (!details["Content"].isNullOrBlank()) {
                generateSummary(content!!)
            } else {
                "No content available to summarize."
            }

            val paper = Paper(
                paperID = paperID ?: UUID.randomUUID().toString(),
                title = details["Title"] ?: "Untitled",
                author = details["Author"] ?: "Unknown",
                publishDate = details["Publish Date"] ?: "Unknown",
                lastAccessed = Timestamp.now(),
                uploadDate = Timestamp.now(),
                topic = if (details["Topic"]?.isBlank() == true)  null else details["Topic"]?.split(","),
                numOfPages = details["Pages"].toString().toInt(),
                fileUrl = pdfUrl,
                summaryText = summary!!
            )
            papersCollection.document(paper.paperID).set(paper).await()
            true
        } catch (e: Exception) {
            Log.e("PaperRepository", "Error adding paper: ${e.message}", e)
            false
        }
    }

    // Check if paper already exists
    suspend fun isDuplicatePaper(paperId: String): Boolean {
        val snapshot = papersCollection.whereEqualTo("paperID", paperId).get().await()
        return !snapshot.isEmpty
    }

    // Update an existing paper
    suspend fun updatePaper(paperId: String, paperDetails: Map<String, Any?>) {
        papersCollection.document(paperId).update(paperDetails).await()
    }

    // Update a papers last accessed date
    suspend fun updatePaperLastAccessed(paperId: String) {
        try {
            papersCollection.document(paperId).update("lastAccessed", Timestamp.now()).await()
        } catch (e: Exception) {
            Log.e("EntryRepository", "Error updating paper: ${e.message}", e)
        }
    }

    // Delete a paper
    suspend fun deletePaper(paperId: String) {
        try {
            papersCollection.document(paperId).delete().await()
            storage.reference.child("papers/$paperId.pdf").delete().await()
        } catch (e: Exception) {
            Log.e("PaperRepository", "Error deleting paper: ${e.message}", e)
        }
    }

    // Generate a summary of the paper's content
    private suspend fun generateSummary(content: String): String? {
        val safeContent = content.take(5000)
        val messages = listOf(
            Message(
                role = "user",
                content = "Generate a comprehensive, clear, and concise summary of the following paper. " +
                        "Ensure that the summary captures the key concepts, arguments, findings, and conclusions " +
                        "in a detailed and thorough manner without sacrificing readability or coherence. " +
                        "Only include the summary and omit empty lines. It must be 250 words.: $safeContent")
        )

        val request = OpenAIRequest(
            model = "gpt-4o-mini",
            messages = messages,
            temperature = 0.7,
            maxTokens = 350
        )

        return try {
            Log.d("OpenAI", "Request: $request")
            val response = OpenAIClient.service.generateSummary(request)
            Log.d("OpenAI", "Response: $response")
            response.choices.firstOrNull()?.message?.content
        } catch (e: HttpException) {
            Log.e("OpenAI", "HTTP Error: ${e.code()} - ${e.message()}")
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("OpenAI", "Error Body: $errorBody")
            null
        } catch (e: Exception) {
            Log.e("OpenAI", "General Error: ${e.message}", e)
            null
        }
    }
}