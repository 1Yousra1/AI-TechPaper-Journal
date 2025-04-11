package com.example.techpaperjournal.data.repository

import android.net.Uri
import android.util.Log
import com.example.techpaperjournal.data.openai.OpenAIClient
import com.example.techpaperjournal.data.openai.Message
import com.example.techpaperjournal.data.openai.OpenAIRequest
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
import retrofit2.HttpException
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
    suspend fun addPaper(paperUri: Uri, details: Map<String, String?>) : Boolean {
        return try {
            val paperID = UUID.randomUUID().toString()
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
                paperID = paperID,
                title = details["Title"] ?: "Untitled",
                author = details["Author"] ?: "Unknown",
                publishDate = details["Publish Date"] ?: "",
                uploadDate = Timestamp.now(),
                topic = if (details["Topic"]?.isBlank() == true)  null else details["Topic"]?.split(","),
                numOfPages = details["Pages"].toString().toInt(),
                fileUrl = pdfUrl,
                summaryText = summary!!
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

    suspend fun generateSummary(content: String): String? {
        val safeContent = content.take(4000) // Roughly within OpenAI’s input limits
        val messages = listOf(
            Message(role = "user", content = "Summarize the following text in 200 words: $safeContent")
        )

        val request = OpenAIRequest(
            model = "gpt-4o-mini",
            messages = messages,
            temperature = 0.7,
            maxTokens = 300
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