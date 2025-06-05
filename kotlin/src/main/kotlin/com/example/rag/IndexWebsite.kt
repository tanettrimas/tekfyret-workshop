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
    // TODO: Implement URL normalization.
    //       Normalization is key for deduplication and consistent processing in web scraping.
    //       Aim to: Remove fragments (#) and query parameters (?).
    //       Consider adding a trailing slash for directory-like URLs (those without apparent file extensions)
    //       to treat `http://example.com/path` and `http://example.com/path/` as equivalent if desired.
    //       Java's `java.net.URI` class can be helpful here for parsing and reconstruction.
    //       Example steps:
    //       1. Create a `URI` object: `val uri = URI(url.trim())`
    //       2. Reconstruct path: `var path = uri.path; if (path != null && !path.endsWith("/") && !path.matches(Regex(".*\\.[a-zA-Z0-9]+$"))) { path += "/"; }`
    //       3. Create new URI without query/fragment: `return URI(uri.scheme, uri.authority, path, null, null).toString()`
    return "" // Placeholder
}

private fun isSameBaseDomain(url1: String, url2: String): Boolean {
    // TODO: Implement domain comparison.
    //       This function ensures the scraper stays within the target website's domain boundaries.
    //       The `java.net.URI` class is suitable for this.
    //       1. Parse both URLs into `URI` objects: `val uri1 = URI(url1)`, `val uri2 = URI(url2)`.
    //       2. Extract the host from each: `val host1 = uri1.host`, `val host2 = uri2.host`.
    //       3. Handle potential www. prefix for more lenient comparison if needed (e.g. remove it from both hosts before comparing).
    //       4. Compare hosts: `return host1 != null && host2 != null && host1.equals(host2, ignoreCase = true)`
    //          (or `host1.endsWith(host2)` or vice-versa if comparing subdomains to a base domain).
    return false // Placeholder
}

// --- Scraping Logic ---
private fun scrapeWebsite(baseUrl: String, maxPages: Int = MAX_PAGES_TO_SCRAPE): List<Pair<String, String>> {
    println("[Scraper] Starting scrape of $baseUrl, up to $maxPages pages.")
    val queue: ArrayDeque<String> = ArrayDeque(listOf(baseUrl))
    val visitedUrls = mutableSetOf<String>()
    val scrapedPages = mutableListOf<Pair<String, String>>() // Stores (URL, TextContent)
    var pagesProcessedCount = 0
    
    val baseDomainUri = URI(baseUrl)
    val targetHost = baseDomainUri.host ?: run {
        println("[Scraper] Error: Could not determine host from base URL: $baseUrl")
        return emptyList()
    }

    while (queue.isNotEmpty() && scrapedPages.size < maxPages) {
        val currentUrlRaw = queue.removeFirst()
        val currentUrlNormalized = normalizeUrl(currentUrlRaw) // Normalize before any processing
        
        // TODO: Implement URL filtering and page processing logic.
        //       This loop drives the web scraping process.
        // 1. Basic URL Validation & Filtering:
        //    - If `currentUrlNormalized` is blank or already in `visitedUrls`, continue.
        //    - Use `isSameBaseDomain(currentUrlNormalized, baseUrl)` (or compare `URI(currentUrlNormalized).host` with `targetHost`)
        //      to ensure we are not straying to external domains. If external, log and continue.
        // 2. Mark as Visited & Increment Counter:
        //    - Add `currentUrlNormalized` to `visitedUrls`.
        //    - Increment `pagesProcessedCount`.
        // 3. Fetch and Parse HTML using Jsoup:
        //    - `val doc = Jsoup.connect(currentUrlNormalized).timeout(10000).get()` (10s timeout)
        //    - Wrap in try-catch for `java.io.IOException` or other Jsoup/network exceptions. Log errors and continue.
        // 4. Extract Meaningful Text Content:
        //    - Select relevant parts of the document. E.g., `doc.body()` or specific CSS selectors like `doc.select("main article")`.
        //    - Remove common non-content elements (headers, footers, navs, scripts, styles, asides) to reduce noise for the RAG model.
        //      `selectedElement.select("header, footer, nav, script, style, aside").remove()`
        //    - Get the clean text: `val textContent = selectedElement.text().replace("\s+".toRegex(), " ").trim()`
        // 5. Store Scraped Content:
        //    - If `textContent` is not blank, add `Pair(currentUrlNormalized, textContent)` to `scrapedPages`.
        // 6. Discover and Enqueue New Internal Links:
        //    - Find all `<a>` tags with `href` attributes: `val links = doc.select("a[href]")`
        //    - For each link:
        //      a. Get the absolute URL: `val absoluteLink = link.absUrl("href")` (Jsoup handles relative to absolute conversion).
        //      b. Normalize `absoluteLink` using `normalizeUrl()`.
        //      c. If `absoluteLinkNormalized` is valid, internal (check domain again), and not in `visitedUrls` or `queue`, add it to `queue`.
        // 7. Handle exceptions gracefully throughout the process (network errors, parsing errors).
        
        if (currentUrlNormalized.isBlank() || currentUrlNormalized in visitedUrls) continue

        try {
            val currentUri = URI(currentUrlNormalized)
            if (currentUri.host == null || !currentUri.host.equals(targetHost, ignoreCase = true)) {
                // println("[Scraper] Skipping external or invalid host URL: $currentUrlNormalized") // Optional: for verbose logging
                continue
            }
        } catch (e: java.net.URISyntaxException) {
            println("[Scraper] Skipping invalid URL syntax: $currentUrlNormalized - ${e.message}")
            continue
        }

        visitedUrls.add(currentUrlNormalized)
        pagesProcessedCount++
        
        if (pagesProcessedCount == 1 || pagesProcessedCount % 10 == 0 || pagesProcessedCount == maxPages || scrapedPages.size % 10 == 0) {
            println("[Scraper] Processed $pagesProcessedCount pages, Enqueued ${queue.size}, Scraped ${scrapedPages.size}. Current: $currentUrlNormalized")
        }
        
        try {
            val doc = Jsoup.connect(currentUrlNormalized).timeout(10000).get() // 10-second timeout
            
            // Remove common irrelevant sections. Adjust selectors based on typical website structures.
            doc.select("nav, header, footer, script, style, aside, .sidebar, #sidebar, .ad, .banner").remove()
            
            val mainContentElement = doc.body() ?: doc // Fallback to whole doc if body is null
            val textContent = mainContentElement.text().replace(Regex("\s{2,}"), " ").trim() // Normalize whitespace

            if (textContent.isNotBlank()) {
                scrapedPages.add(Pair(currentUrlNormalized, textContent))
            }

            if (scrapedPages.size < maxPages) { // Only find more links if we haven't hit the page cap
                val linksOnPage = doc.select("a[href]")
                for (link in linksOnPage) {
                    val absoluteLink = link.absUrl("href") // Jsoup resolves relative to absolute
                    val normalizedLink = normalizeUrl(absoluteLink)
                    if (normalizedLink.isNotBlank() && URI(normalizedLink).host.equals(targetHost, ignoreCase = true) &&
                        normalizedLink !in visitedUrls && !queue.contains(normalizedLink) && queue.size < maxPages * 2) { // Prevent queue from growing excessively
                        queue.add(normalizedLink)
                    }
                }
            }
        } catch (e: Exception) {
            println("[Scraper] Warning: Failed to fetch/process $currentUrlNormalized: ${e.message?.take(150)}")
        }
    }
    
    println("[Scraper] Scraping complete. Found ${scrapedPages.size} pages with text from $pagesProcessedCount processed URLs.")
    return scrapedPages
}

// --- Text Chunking Logic (Using jtokkit for token-based chunking) ---
private fun chunkTextContent(text: String, chunkSizeInTokens: Int = CHUNK_SIZE_TOKENS, overlapInTokens: Int = CHUNK_OVERLAP_TOKENS): List<String> {
    if (text.isBlank()) return emptyList()
    
    // TODO: Implement token-based text chunking using jtokkit.
    //       The goal is to divide text into semantically coherent segments that fit the embedding model's context window.
    //       `jtokkit` (like `tiktoken` in Python) accurately counts tokens for OpenAI models.
    // 1. Obtain a `jtokkit` `Encoding` instance for `EncodingType.CL100K_BASE` (used by text-embedding-ada-002, text-embedding-3-small, etc.).
    //    `val registry = Encodings.newDefaultEncodingRegistry()`
    //    `val encoding: Encoding = registry.getEncoding(EncodingType.CL100K_BASE)`
    // 2. Encode the input `text` into a list of token IDs: `val tokens = encoding.encode(text)`
    // 3. If `tokens.size <= chunkSizeInTokens`, no chunking is needed; return the original text as a single-element list (if not blank).
    // 4. Calculate `stepSize = chunkSizeInTokens - overlapInTokens`. Ensure `stepSize > 0` to prevent infinite loops.
    // 5. Iterate from `0` to `tokens.size - 1` with the `stepSize`.
    //    In each iteration, define a `chunkEnd = min(i + chunkSizeInTokens, tokens.size)`.
    //    Extract the `tokenChunk = tokens.subList(i, chunkEnd)`.
    // 6. Decode each `tokenChunk` back to a string: `val chunkText = encoding.decode(tokenChunk)`.
    // 7. Add `chunkText.trim()` to your list of results if it's not blank.
    // 8. Return the list of decoded text chunks.
    
    val registry = Encodings.newDefaultEncodingRegistry() // Hint: Part of step 1
    val encoding: Encoding = registry.getEncoding(EncodingType.CL100K_BASE) // Hint: Part of step 1
    
    // TODO: Implement the core chunking logic here based on steps 2-8 above.
    
    return emptyList() // Placeholder: Replace with your list of chunks
}

// --- Embedding Logic ---
suspend fun generateEmbeddings(texts: List<String>): List<List<Float>> {
    if (texts.isEmpty()) return emptyList()
    println("[Embedder] Generating embeddings for ${texts.size} text chunks...")
    
    // TODO: Implement embedding generation via OpenAI API.
    //       Embeddings convert text into numerical vectors, capturing semantic meaning for similarity searches.
    //       Use Ktor HTTP client for the API call.
    // 1. Prepare the request for OpenAI's embedding API (e.g., "/v1/embeddings").
    //    - Body: `IdxOpenAiEmbeddingRequest(model = EMBED_MODEL, input = texts_batch)`.
    //    - Headers: `Authorization: Bearer $openAiKey`, `Content-Type: application/json`.
    // 2. Implement batching: OpenAI's API might have limits on input size or number of texts per request.
    //    Process `texts` in batches (e.g., 50-200 texts per batch, depending on average text length).
    // 3. For each batch:
    //    a. Make an HTTP POST request to `https://api.openai.com/v1/embeddings`.
    //    b. Deserialize the `IdxOpenAiEmbeddingResponse`.
    //    c. Extract the embedding vectors: `response.data.map { it.embedding.map { it.toFloat() } }`.
    //       Note the conversion from `Double` (OpenAI's output) to `Float` (Qdrant typically uses Float32).
    //    d. Add these batch embeddings to a main list.
    // 4. Handle potential exceptions during API calls (e.g., network issues, API errors) and log them.
    //    Return an empty list or partially generated list upon failure, or rethrow critical exceptions.
    
    // TODO: Implement the batch embedding generation logic here.
    
    return emptyList() // Placeholder: Replace with your list of embeddings
}

// --- Qdrant Logic ---
suspend fun ensureQdrantCollection(vectorSize: Int) {
    println("[Qdrant] Ensuring collection '$COLLECTION_NAME' exists with vector size $vectorSize.")
    
    // TODO: Implement Qdrant collection management using Ktor HTTP client.
    //       A Qdrant collection stores your vectors and their associated metadata (payloads).
    //       For this workshop, we'll recreate the collection for a fresh start each time.
    // 1. Attempt to delete the collection if it exists (to ensure a clean state for the workshop).
    //    - HTTP DELETE to `"$QDRANT_URL/collections/$COLLECTION_NAME?timeout=$QDRANT_RECREATE_TIMEOUT_SECONDS"`.
    //    - This might return 404 if the collection doesn't exist, which is acceptable.
    //      Catch exceptions (e.g., `ClientRequestException` for 404) and log them informatively.
    // 2. Create a new collection.
    //    - HTTP PUT to `"$QDRANT_URL/collections/$COLLECTION_NAME?timeout=$QDRANT_RECREATE_TIMEOUT_SECONDS"`.
    //    - Body: `IdxQdrantCollectionCreateRequest(vectors = IdxQdrantVectorParams(size = vectorSize, distance = "Cosine"))`.
    //      - `size`: Must match the dimensionality of your embeddings (from `EMBED_MODEL`).
    //      - `distance`: "Cosine" is standard for text embeddings, measuring similarity by angle.
    // 3. Add a short delay (`kotlinx.coroutines.delay(2000)`) after creation to allow Qdrant to stabilize.
    // 4. Handle exceptions robustly. If collection setup fails, it's a critical error for the indexing process.
    
    // TODO: Implement the collection deletion and creation logic here.
    //       Remember to handle exceptions and add a delay.
    try {
        // Example of how you might structure the try-catch for deletion, then creation
        // httpClient.delete(...) 
        // httpClient.put(...) 
        kotlinx.coroutines.delay(2000) // Short delay for Qdrant to stabilize
    } catch (e: Exception) {
        println("[Qdrant] Critical error during collection management for '$COLLECTION_NAME': ${e.message}")
        throw e // Re-throw to stop the process if collection setup fails
    }
}

private suspend fun uploadToQdrant(points: List<IdxQdrantPoint>) {
    if (points.isEmpty()) {
        println("[Qdrant] No points to upload.")
        return
    }
    println("[Qdrant] Uploading ${points.size} points to collection '$COLLECTION_NAME'...")
    
    // TODO: Implement point upload to Qdrant using Ktor HTTP client.
    //       Qdrant "points" consist of a vector, a unique ID, and a payload (metadata).
    //       The payload is crucial for retrieving the original text and source URL during search.
    // 1. Prepare the request for Qdrant's points upsert API.
    //    - HTTP PUT to `"$QDRANT_URL/collections/$COLLECTION_NAME/points?wait=true"`.
    //      `wait=true` makes the operation synchronous, ensuring data is indexed before the call returns.
    //    - Body: `IdxQdrantUpsertPointsRequest(points = batch_of_points)`.
    // 2. Implement batching for uploading points if `points` list is very large (e.g., >100-500 points per batch).
    //    This improves reliability and performance.
    // 3. For each batch:
    //    a. Make the HTTP PUT request.
    //    b. Check the response status for success.
    // 4. Handle exceptions (network, Qdrant errors) and log them.
    
    // TODO: Implement the batch upload logic here.
    try {
        // Example structure: Iterate through batches and make httpClient.put calls
    } catch (e: Exception) {
        println("[Qdrant] Error uploading points to Qdrant: ${e.message}")
    }
}

// --- Main Indexing Process ---
fun main(args: Array<String>) = runBlocking {
    println("--- Kotlin Website Indexing Script ---")
    
    // TODO: Implement the main indexing workflow, orchestrating all steps.
    //       This function ties together scraping, chunking, embedding, and Qdrant operations.
    // 1. Validate Command Line Arguments: Expect a base URL to scrape.
    //    If not provided, print usage instructions and exit.
    // 2. Scrape Website: Call `scrapeWebsite(baseUrlToIndex)` to get a list of (URL, TextContent) pairs.
    //    If no pages are scraped, log and exit gracefully.
    // 3. Prepare Data for Indexing:
    //    - Initialize `allTextChunks = mutableListOf<String>()`
    //    - Initialize `pointPayloads = mutableListOf<Map<String, String>>()` (for Qdrant payloads)
    //    - Initialize `pointIds = mutableListOf<String>()` (for Qdrant point IDs)
    // 4. Chunk Content: Iterate through scraped pages.
    //    For each page's content, call `chunkTextContent()`.
    //    For each resulting chunk: add it to `allTextChunks`, generate a unique ID (`UUID.randomUUID().toString()`) for `pointIds`,
    //    and create a payload map `mapOf("url" to pageUrl, "text" to chunk, "title" to Jsoup.parse(pageContent).title())` for `pointPayloads`.
    // 5. Generate Embeddings: Call `generateEmbeddings(allTextChunks)`.
    //    If embedding generation fails or returns an inconsistent number of vectors, log and exit.
    //    The size of the first embedding vector determines the `vectorSize` for Qdrant collection.
    // 6. Ensure Qdrant Collection: Call `ensureQdrantCollection(vectorSize)`.
    // 7. Create Qdrant Points: Iterate from `0` to `embeddings.size - 1`.
    //    For each index `i`, create an `IdxQdrantPoint(id = pointIds[i], vector = embeddings[i], payload = pointPayloads[i])`.
    //    Add these points to a list.
    // 8. Upload to Qdrant: Call `uploadToQdrant(listOfQdrantPoints)`.
    // 9. Close Ktor `httpClient` in a `finally` block to ensure it's always closed.
    
    if (args.isEmpty()) {
        println("Usage: ./gradlew runIndex --args=\"<base-url-to-index>\"")
        println("Example: ./gradlew runIndex --args=\"https://www.ruter.no\"")
        // httpClient.close() // Will be closed in finally block
        return@runBlocking
    }
    
    val baseUrlToIndex = args[0]
    println("[Main] Target website for indexing: $baseUrlToIndex")

    try {
        // TODO: Implement steps 2-8 from the main workflow hint above.
        // Example structure:
        // val scrapedPages = scrapeWebsite(...)
        // if (scrapedPages.isEmpty()) { ... return@runBlocking }
        // ... prepare lists for chunks, ids, payloads ...
        // ... loop through scrapedPages, call chunkTextContent, populate lists ...
        // if (allTextChunks.isEmpty()) { ... return@runBlocking }
        // val embeddings = generateEmbeddings(...)
        // if (embeddings.isEmpty() || ...) { ... return@runBlocking }
        // val vectorSize = embeddings.firstOrNull()?.size ?: ...
        // ensureQdrantCollection(vectorSize)
        // ... create qdrantPoints list ...
        // uploadToQdrant(qdrantPoints)

        println("[Main] --- Indexing process complete for $baseUrlToIndex ---")

    } catch (e: Exception) {
        println("[Main] An unhandled error occurred during the indexing process: ${e.message}")
        e.printStackTrace()
    } finally {
        httpClient.close()
        println("[Main] HTTP client closed.")
    }
}