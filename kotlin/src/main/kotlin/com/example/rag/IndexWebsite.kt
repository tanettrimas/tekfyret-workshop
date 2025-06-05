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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.net.URI
import java.util.*
import kotlin.math.min

// jtokkit imports for token-based chunking
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingType


// --- Configuration ---
@OptIn(ExperimentalSerializationApi::class)
private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

// Load environment variables from .env file using dotenv-kotlin
private val dotenv = dotenv {
    ignoreIfMissing = true
}

private val openAiKey = dotenv["OPENAI_API_KEY"]
    ?: run {
        println("Error: OPENAI_API_KEY not found in .env file or as environment variable.")
        kotlin.system.exitProcess(1)
    }


private const val EMBED_MODEL = "text-embedding-3-small"
private const val QDRANT_URL = "http://localhost:6333"
private const val COLLECTION_NAME = "docs_kt" 
private const val MAX_PAGES_TO_SCRAPE = 50
private const val CHUNK_SIZE_TOKENS = 800
private const val CHUNK_OVERLAP_TOKENS = 100
private const val QDRANT_RECREATE_TIMEOUT_SECONDS = 20L

// Ktor HTTP Client for network requests
private val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) { json(json) }
    expectSuccess = true
}

// --- Data classes for OpenAI Embeddings ---
@Serializable
private data class IdxOpenAiEmbeddingRequest(val model: String, val input: List<String>)

@Serializable
private data class IdxEmbeddingData(val embedding: List<Double>)

@Serializable
private data class IdxOpenAiEmbeddingResponse(val data: List<IdxEmbeddingData>)

// --- Data classes for Qdrant ---
@Serializable
private data class IdxQdrantVectorParams(val size: Int, val distance: String)

@Serializable
private data class IdxQdrantCollectionCreateRequest(val vectors: IdxQdrantVectorParams)

@Serializable
private data class IdxQdrantPoint(val id: String, val vector: List<Float>, val payload: Map<String, String>)

@Serializable
private data class IdxQdrantUpsertPointsRequest(val points: List<IdxQdrantPoint>)

// --- Utility Functions ---
private fun normalizeUrl(url: String): String {
    // TODO: Implement URL normalization
    // Remove fragments (#) and query parameters (?)
    // Add trailing slash for directory URLs
    return ""
}

private fun isSameBaseDomain(url1: String, url2: String): Boolean {
    // TODO: Implement domain comparison
    // Use URI.host to compare if two URLs belong to the same domain
    return false
}

// --- Scraping Logic ---
private fun scrapeWebsite(baseUrl: String, maxPages: Int = MAX_PAGES_TO_SCRAPE): List<Pair<String, String>> {
    println("[Scraper] Starting scrape of $baseUrl, up to $maxPages pages.")
    val queue: ArrayDeque<String> = ArrayDeque(listOf(baseUrl))
    val visitedUrls = mutableSetOf<String>()
    val scrapedPages = mutableListOf<Pair<String, String>>()
    var pagesProcessedCount = 0
    
    while (queue.isNotEmpty() && scrapedPages.size < maxPages) {
        val currentUrl = normalizeUrl(queue.removeFirst())
        
        // TODO: Implement URL filtering and processing logic
        // 1. Skip if already visited or external domain
        // 2. Add to visited set and increment counter
        // 3. Use Jsoup to fetch and parse the HTML
        // 4. Remove unwanted elements (header, footer, nav, script, style, etc.)
        // 5. Extract clean text content
        // 6. Find all links and add internal ones to the queue
        // 7. Handle exceptions gracefully
        
        if (currentUrl in visitedUrls || !isSameBaseDomain(currentUrl, baseUrl)) continue
        visitedUrls.add(currentUrl)
        pagesProcessedCount++
        
        if (pagesProcessedCount == 1 || pagesProcessedCount % 20 == 0 || pagesProcessedCount == maxPages) {
            println("[Scraper] Processing page $pagesProcessedCount/$maxPages: $currentUrl")
        }
        
        try {
            // TODO: Use Jsoup.connect() to fetch the page
            // TODO: Remove unwanted HTML elements
            // TODO: Extract and clean text content
            // TODO: Add to scrapedPages if text is not empty
            // TODO: Find and enqueue internal links
            
        } catch (e: Exception) {
            println("[Scraper] Warning: Failed to fetch/process $currentUrl: ${e.message?.take(100)}")
        }
    }
    
    println("[Scraper] Scraping complete. Found ${scrapedPages.size} pages with text.")
    return scrapedPages
}

// --- Text Chunking Logic (Using jtokkit for token-based chunking) ---
private fun chunkTextContent(text: String, chunkSizeInTokens: Int = CHUNK_SIZE_TOKENS, overlapInTokens: Int = CHUNK_OVERLAP_TOKENS): List<String> {
    if (text.isBlank()) return emptyList()
    
    // TODO: Implement token-based text chunking
    // 1. Initialize jtokkit encoding registry and get CL100K_BASE encoding
    // 2. Encode the text to get token IDs
    // 3. If text fits in one chunk, return as single item
    // 4. Calculate step size for overlapping chunks
    // 5. Create chunks by sliding window over token IDs
    // 6. Decode each chunk back to text
    
    val registry = Encodings.newDefaultEncodingRegistry()
    val encoding: Encoding = registry.getEncoding(EncodingType.CL100K_BASE)
    
    // TODO: Implement the chunking logic here
    
    return emptyList()
}

// --- Embedding Logic ---
suspend fun generateEmbeddings(texts: List<String>): List<List<Float>> {
    if (texts.isEmpty()) return emptyList()
    println("[Embedder] Generating embeddings for ${texts.size} text chunks...")
    
    // TODO: Implement embedding generation
    // 1. Make HTTP POST request to OpenAI embeddings API
    // 2. Use the configured embedding model
    // 3. Include Authorization header with API key
    // 4. Handle the response and extract embeddings
    // 5. Convert Double to Float for Qdrant compatibility
    // 6. Handle exceptions gracefully
    
    val embeddings = mutableListOf<List<Float>>()
    try {
        // TODO: Implement API call to OpenAI
        
    } catch (e: Exception) {
        println("[Embedder] Error generating embeddings: ${e.message}")
    }
    return embeddings
}

// --- Qdrant Logic ---
suspend fun ensureQdrantCollection(vectorSize: Int) {
    println("[Qdrant] Ensuring collection '$COLLECTION_NAME' exists with vector size $vectorSize.")
    
    // TODO: Implement Qdrant collection management
    // 1. Try to delete existing collection (for fresh start in workshop)
    // 2. Handle 404 errors if collection doesn't exist
    // 3. Create new collection with proper vector configuration
    // 4. Use Cosine distance for similarity search
    // 5. Add delay for Qdrant to stabilize
    
    try {
        // TODO: Delete existing collection if it exists
        
        // TODO: Create new collection
        
        kotlinx.coroutines.delay(2000) // Short delay for Qdrant to stabilize
    } catch (e: Exception) {
        println("[Qdrant] Error ensuring collection '$COLLECTION_NAME': ${e.message}")
        throw e // Re-throw to stop the process if collection setup fails
    }
}

private suspend fun uploadToQdrant(points: List<IdxQdrantPoint>) {
    if (points.isEmpty()) {
        println("[Qdrant] No points to upload.")
        return
    }
    println("[Qdrant] Uploading ${points.size} points to collection '$COLLECTION_NAME'...")
    
    // TODO: Implement point upload to Qdrant
    // 1. Make HTTP PUT request to Qdrant points endpoint
    // 2. Use wait=true for synchronous indexing
    // 3. Send all points in the request body
    // 4. Handle errors gracefully
    
    try {
        // TODO: Implement upload logic
        
    } catch (e: Exception) {
        println("[Qdrant] Error uploading points to Qdrant: ${e.message}")
    }
}

// --- Main Indexing Process ---
fun main(args: Array<String>) = runBlocking {
    println("--- Kotlin Website Indexing Script ---")
    
    // TODO: Implement main indexing workflow
    // 1. Validate command line arguments (URL to index)
    // 2. Scrape the website to get page content
    // 3. Check if any content was found
    // 4. Chunk all page content into smaller pieces
    // 5. Generate embeddings for all chunks
    // 6. Ensure Qdrant collection exists
    // 7. Create Qdrant points with embeddings and metadata
    // 8. Upload everything to Qdrant
    
    if (args.isEmpty()) {
        println("Usage: ./gradlew runIndex --args=\"<base-url>\"")
        return@runBlocking
    }
    
    val baseUrlToIndex = args[0]
    println("Target website: $baseUrlToIndex")
    
    // TODO: Complete the indexing workflow
    
    println("--- Indexing process complete for $baseUrlToIndex ---")
}