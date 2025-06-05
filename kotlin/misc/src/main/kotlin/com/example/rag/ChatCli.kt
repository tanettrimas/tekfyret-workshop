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

// --- RAG Pipeline Functions ---
suspend fun getQueryEmbedding(query: String): List<Float>? {
    // TODO: Generate a vector embedding for the user's query using the OpenAI Embeddings API.
    //       This vector captures the semantic meaning of the query, enabling similarity search in Qdrant.
    //       - Request: POST to `https://api.openai.com/v1/embeddings` with `CliOpenAiEmbeddingRequest`.
    //       - Key parameters: `model` (e.g., `EMBED_MODEL`), `input` (the user query).
    //       - Response: Extract the embedding vector from `CliOpenAiEmbeddingResponse.data[0].embedding`.
    //       - Convert the `Double` vector elements to `Float` for Qdrant compatibility.
    //       - Ensure robust error handling (network, API errors) and return null on failure.
    println("   [RAG] Generating embedding for query: \"${query.take(50)}...\"")
    try {
        // TODO: Implement OpenAI API call for embedding
        // Example structure:
        // val requestBody = CliOpenAiEmbeddingRequest(model = EMBED_MODEL, input = listOf(query))
        // val response: CliOpenAiEmbeddingResponse = httpClient.post(...) { ... }.body()
        // val embedding = response.data.firstOrNull()?.embedding?.map { it.toFloat() }
        // if (embedding != null) { return embedding }
        println("   [RAG] Error: No embedding data. (This is a placeholder message)")
    } catch (e: Exception) {
        println("   [RAG] Error generating query embedding: ${e.message}")
    }
    return null
}

suspend fun searchQdrant(vector: List<Float>): List<CliQdrantPoint> {
    // TODO: Perform a semantic search in the Qdrant vector database using the query embedding.
    //       This retrieves the most relevant document chunks (points) from the indexed knowledge base.
    //       - Request: POST to `$QDRANT_URL/collections/$COLLECTION_NAME/points/search` with `CliQdrantSearchRequest`.
    //       - Key parameters: `vector` (the query embedding), `limit` (e.g., `TOP_K_RESULTS`), `with_payload = true` (to get text/URL).
    //       - Response: `CliQdrantSearchResponseResult.result` contains the list of `CliQdrantPoint`.
    //       - The `payload` in each point contains the original text and source URL.
    //       - Handle errors gracefully, returning an empty list if the search fails.
    println("   [RAG] Searching Qdrant for top $TOP_K_RESULTS results...")
    try {
        // TODO: Implement Qdrant search call
        // Example structure:
        // val requestBody = CliQdrantSearchRequest(vector = vector, limit = TOP_K_RESULTS, with_payload = true)
        // val response: CliQdrantSearchResponseResult = httpClient.post(...) { ... }.body()
        // return response.result
        println("   [RAG] Qdrant search placeholder. No actual search performed.")
    } catch (e: Exception) {
        println("   [RAG] Error searching Qdrant: ${e.message}")
    }
    return emptyList()
}

suspend fun getAnswerFromLLM(query: String, context: String): String {
    // TODO: Generate a natural language answer using Anthropic's Messages API, based on the query and retrieved context.
    //       The LLM synthesizes information from the context to respond to the user's query.
    //       - Request: POST to `https://api.anthropic.com/v1/messages` with `CliAnthropicRequest`.
    //       - Key parameters:
    //         - `model` (e.g., `LLM_MODEL`).
    //         - `messages`: A list containing a user message. The content of this message should be carefully constructed to include both the `context` (retrieved from Qdrant) and the original `query`.
    //           Example format: "<context>\n[context_string]\n</context>\n\nUser Question: [query_string]"
    //         - `system`: The `SYSTEM_PROMPT` to guide the LLM's behavior (e.g., to answer only from context).
    //         - `max_tokens`: To limit the length of the generated answer.
    //       - Response: Extract the answer text from `CliAnthropicResponse.content[0].text`.
    //       - Implement robust error handling. Return a fallback message if the API call fails.
    println("   [RAG] Asking LLM (Anthropic $LLM_MODEL) for an answer. Context length: ${context.length} chars.")
    
    // TODO: Construct user message and Anthropic API request body here, then make the call.
    // Example structure:
    // val userMessageContent = """<context>..."""
    // val requestMessages = listOf(CliAnthropicMessage(role = "user", content = userMessageContent))
    // val requestBody = CliAnthropicRequest(...)
    // try {
    //     val response: CliAnthropicResponse = httpClient.post(...) { ... }.body()
    //     val answerText = response.content.firstOrNull()?.text
    //     if (answerText != null) { return answerText }
    // } catch (e: Exception) { ... }

    return "I apologize, but I encountered an error while trying to generate an answer. (This is a placeholder response)"
}

// --- Main Chat Loop ---
fun main() = runBlocking {
    println("--- Kotlin RAG Chat CLI ---")
    println("Using OpenAI model: $EMBED_MODEL, Anthropic model: $LLM_MODEL, Qdrant collection: $COLLECTION_NAME")
    println("Type 'exit' or 'quit' to end the chat.")

    // TODO: Orchestrate the RAG pipeline within this main chat loop.
    //       This involves sequentially calling the functions above for each user query.
    //       Key RAG pipeline steps:
    //       1. Read user query.
    //       2. Generate query embedding: `getQueryEmbedding(query)`.
    //       3. Search Qdrant for relevant context: `searchQdrant(embeddingVector)`.
    //       4. Compile context: Extract text from Qdrant results (`hit.payload?.get("text")`) and format into a single string.
    //          Also, collect source URLs (`hit.payload?.get("url")`) for citation.
    //       5. Generate answer using LLM: `getAnswerFromLLM(query, compiledContext)`.
    //       6. Display answer and sources.
    //       - Ensure comprehensive error handling at each stage (e.g., if embedding fails, or Qdrant returns no hits).
    //       - The `SYSTEM_PROMPT` guides the LLM to respond "I don't know" if context is insufficient.

    while (true) {
        print("\nAsk a question: ")
        val query = readlnOrNull()?.trim()

        if (query.isNullOrEmpty()) {
            continue
        }
        if (query.equals("exit", ignoreCase = true) || query.equals("quit", ignoreCase = true)) {
            println("Exiting chat.")
            break
        }

        println("Processing your query... (This is a placeholder, RAG pipeline not yet implemented here)")

        // TODO: Implement the RAG pipeline steps as outlined in the main TODO comment above.
        // Example structure:
        // val queryVector = getQueryEmbedding(query)
        // if (queryVector == null) { /* handle error, continue */ }
        // val searchResults = searchQdrant(queryVector)
        // ... process searchResults to get contextString and sources ...
        // val answer = getAnswerFromLLM(query, contextString)
        // println("\n🤖 Answer: $answer")
        // ... print sources ...
        
        // Placeholder answer for now:
        println("\n🤖 Answer: This is a placeholder response. The RAG pipeline needs to be implemented.")

    }

    httpClient.close()
    println("--- Chat session ended ---")
}