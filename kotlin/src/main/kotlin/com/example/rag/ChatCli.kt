package com.example.rag

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive


// --- Configuration ---
@OptIn(ExperimentalSerializationApi::class)
private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; explicitNulls = false }

private val dotenv = dotenv {
    ignoreIfMissing = true 
}

private val openAiKey = dotenv["OPENAI_API_KEY"]
    ?: run {
        println("Error: OPENAI_API_KEY not found in .env file or as environment variable.")
        kotlin.system.exitProcess(1)
    }
private val anthropicKey = dotenv["ANTHROPIC_API_KEY"]
    ?: run {
        println("Error: ANTHROPIC_API_KEY not found in .env file or as environment variable.")
        kotlin.system.exitProcess(1)
    }

private const val EMBED_MODEL = "text-embedding-3-small"
private const val LLM_MODEL = "claude-sonnet-4-20250514" 
private const val QDRANT_URL = "http://localhost:6333"
private const val COLLECTION_NAME = "docs_kt"
private const val TOP_K_RESULTS = 5
private const val MAX_TOKENS_ANTHROPIC = 1024

// Ktor HTTP Client
private val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) { json(json) }
    expectSuccess = true 
}

// --- Data classes for ChatCli (prefixed with Chat) ---
@Serializable
private data class ChatOpenAiEmbeddingRequest(val model: String, val input: List<String>)

@Serializable
private data class ChatEmbeddingData(val embedding: List<Double>)

@Serializable
private data class ChatOpenAiEmbeddingResponse(val data: List<ChatEmbeddingData>)

@Serializable
private data class ChatQdrantSearchRequest(
    val vector: List<Float>,
    val limit: Int,
    @SerialName("with_payload") val withPayload: Boolean
)

@Serializable
private data class ChatQdrantSearchHit(val id: JsonElement, val score: Float, val payload: Map<String, JsonElement>?)

@Serializable
private data class ChatQdrantSearchResponse(val result: List<ChatQdrantSearchHit>)

@Serializable
private data class ChatAnthropicMessage(val role: String, val content: String)

@Serializable
private data class ChatAnthropicRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String? = null, 
    val messages: List<ChatAnthropicMessage>
)

@Serializable
private data class ChatAnthropicContentBlock(val type: String, val text: String)

@Serializable
private data class ChatAnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ChatAnthropicContentBlock>,
)

private const val ANTHROPIC_SYSTEM_PROMPT = "You are an assistant that answers **only** from the provided <context>. " +
                                          "If the answer cannot be found, simply reply with I don't know. " +
                                          "Format your answer clearly and concisely."

// --- Core Chat Logic ---
suspend fun generateQueryEmbedding(query: String): List<Float> {
    // TODO: Implement query embedding generation
    // 1. Make HTTP POST request to OpenAI embeddings API
    // 2. Include Authorization header with Bearer token
    // 3. Send the user query for embedding
    // 4. Extract embedding vector from response
    // 5. Convert from Double to Float
    // 6. Handle errors gracefully and return empty list on failure
    
    try {
        // TODO: Implement API call to OpenAI
        
        return emptyList()
    } catch (e: Exception) {
        println("[Embedder] Error: ${e.message}")
        return emptyList()
    }
}

suspend fun searchQdrant(queryVector: List<Float>): List<String> {
    if (queryVector.isEmpty()) { 
        return emptyList() 
    }
    
    // TODO: Implement Qdrant vector search
    // 1. Make HTTP POST request to Qdrant search endpoint
    // 2. Send the query vector with search parameters
    // 3. Request payload data to get the text content
    // 4. Extract text from each search hit's payload
    // 5. Return list of context texts
    // 6. Handle errors and return empty list on failure
    
    try {
        // TODO: Implement search logic
        
        return emptyList()
    } catch (e: Exception) { 
        println("[Qdrant] Error during Qdrant search: ${e.message}")
        return emptyList() 
    }
}

suspend fun getAnthropicAnswer(userQuery: String, context: String): String {
    // TODO: Implement Anthropic API call for answer generation
    // 1. Create request with system prompt and user message
    // 2. Include the context and user query in the message
    // 3. Make HTTP POST request to Anthropic messages API
    // 4. Include required headers (x-api-key, anthropic-version)
    // 5. Extract text content from response
    // 6. Handle errors gracefully
    
    val requestBody = ChatAnthropicRequest(
        model = LLM_MODEL, 
        maxTokens = MAX_TOKENS_ANTHROPIC, 
        system = ANTHROPIC_SYSTEM_PROMPT,
        messages = listOf(ChatAnthropicMessage(
            role = "user", 
            content = "<context>\n${context.trim()}\n</context>\n\nUser question: $userQuery"
        ))
    )
    
    try {
        // TODO: Implement API call to Anthropic
        
        return "Sorry, I encountered an error trying to get an answer from the AI model."
    } catch (e: Exception) {
        println("[LLM] Error during Anthropic call: ${e.message}")
        return "Sorry, I encountered an error trying to get an answer from the AI model."
    }
}

// --- Main CLI Loop ---
fun main() = runBlocking {
    println("--- Kotlin RAG Chat CLI ---")
    println("Type your questions to query the indexed website content. Type 'exit' or 'quit' to stop.")

    // TODO: Implement connection check to Qdrant
    // 1. Make HTTP GET request to check if collection exists
    // 2. Print success or error message
    // 3. Exit if connection fails
    
    try {
        // TODO: Check Qdrant connection
        
    } catch (e: Exception) {
        println("[System] Error: Could not connect to Qdrant or collection '$COLLECTION_NAME' does not exist.")
        println("Please ensure Qdrant is running and you have run IndexWebsite.kt first.")
        println("Error details: ${e.message}")
        return@runBlocking
    }

    // TODO: Implement main chat loop
    // 1. Read user input in a loop
    // 2. Handle exit commands (exit, quit)
    // 3. For each user query:
    //    a. Generate embedding for the query
    //    b. Search Qdrant for relevant context
    //    c. Get answer from Anthropic using the context
    //    d. Display the answer to the user
    // 4. Handle errors gracefully for each step
    // 5. Close HTTP client when done
    
    while (true) {
        print("\n> Question: ")
        val userInput = readLine()
        
        if (userInput.isNullOrBlank() || userInput.equals("exit", ignoreCase = true) || userInput.equals("quit", ignoreCase = true)) {
            break
        }

        // TODO: Implement the RAG pipeline:
        // 1. Embed user query
        // 2. Search Qdrant for context
        // 3. Get answer from Anthropic
        // 4. Display result
    }
    
    println("Exiting chat. Goodbye!")
    httpClient.close()
}