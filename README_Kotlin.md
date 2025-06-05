# Kotlin RAG Chatbot (CLI) Workshop Guide

**Goal:** Build a Command-Line Interface (CLI) RAG chatbot in Kotlin. It will answer questions based on a website you choose.

**Core Idea:**
1.  **Indexing:** Scrape Website -> Extract Text -> Chunk (Token-based) -> Embed (OpenAI) -> Store (Qdrant).
2.  **Chat:** User Query -> Embed Query (OpenAI) -> Search Qdrant -> Get Context -> Prompt LLM (Anthropic) -> Get Answer.

**Simplified Architecture:**

```text
                        [Indexing Pipeline: Populates Vector DB]
    +-----------------+      +-----------------+      +------------------+      +-----------------+      +-----------------+
    |    Website      |----->|     Scraper     |----->|     Chunker      |----->|   Embedding     |----->|   Vector DB     |
    |                 |      | (beautifulsoup  |      |(tiktoken/jtokkit)|      | (OpenAI API)    |      |    (Qdrant)     |
    |                 |      |    /Jsoup)      |      |                  |      |                 |      |                 |
    +-----------------+      +-----------------+      +------------------+      +-----------------+      +-----------------+
        (Data Source)             (Fetch HTML)         (Break into pieces)       (Text to Vectors)      (Store Vectors & Text)
                                                                                                                 ^
                                                                        _________________________________________|      
                                                                       |                                       
                                                                       |                                        
    +-----------------+      +-----------------+      +-----------------------+      +-----------------+      +-----------------+
    | User Asks       |----->|   Embedding     |----->|   Search Vector DB    |----->|   Augmented     |----->| LLM (Anthropic) |
    | Question        |      | (OpenAI API)    |      |       (Qdrant)        |      |   Prompt        |      | API             |------> Answer
    |                 |      |                 |      |                       |      |(Context + Query)|      |                 |
    +-----------------+      +-----------------+      +-----------------------+      +-----------------+      +-----------------+
      (User Interface)        (Query to Vector)      (Find relevant chunks)           (Prepare for LLM)        (Generate Answer)
```


---

## Building Steps (Kotlin)

### 1. Project Setup

*   **Start Qdrant (Vector Database):**
    ```bash
    mkdir -p ./qdrant_storage
    docker run -d --name rag-qdrant -p 6333:6333 -v "$(pwd)/qdrant_storage":/qdrant/storage qdrant/qdrant
    ```
    *Verify: `http://localhost:6333/dashboard` or `docker ps`.*
*   **API Keys:** Create `.env` in your project root with:
    ```env
    OPENAI_API_KEY="sk-YOUR_OPENAI_KEY_HERE"
    ANTHROPIC_API_KEY="sk-ant-YOUR_ANTHROPIC_KEY_HERE"
    ```
*   **Kotlin/Gradle Project:**
    *   Requires JDK (e.g., OpenJDK 21) and Gradle.
    *   Use the provided `build.gradle.kts`. Key dependencies:
        *   `kotlinx-serialization-json` (JSON)
        *   `ktor-client-*` (HTTP client)
        *   `jsoup` (HTML scraping)
        *   `dotenv-kotlin` (.env loading)
        *   `jtokkit` (Tokenization)
    *   Place Kotlin source files in `src/main/kotlin/com/example/rag/`.

### 2. Indexing Script (e.g., `IndexWebsite.kt`)

*   **Core Logic:**
    *   Load API keys from `.env` using `dotenv-kotlin`.
    *   Define configurations (QDRANT_URL, collection name e.g., `"docs_kt"`, model names).
    *   Initialize Ktor `HttpClient`.
    *   Create `@Serializable` data classes for API requests/responses.
*   **Functions to Implement:**
    *   `scrapeWebsite(baseUrl, maxPages)`: Fetch HTML with Jsoup, clean boilerplate (`<header>`, `<footer>`, `<nav>`, `<script>`), extract text, crawl links.
    *   `chunkTextContent(text, chunkSizeTokens, overlapTokens)`: Use `jtokkit` (e.g., `CL100K_BASE` encoding) to create token-based, overlapping text chunks.
    *   `generateEmbeddings(texts)`: Ktor POST to OpenAI embeddings API, get vectors.
    *   `ensureQdrantCollection()` & `uploadToQdrant()`: Use Ktor to interact with Qdrant HTTP API (create collection, upload points with ID, vector, payload).
*   **`main` function:** Call the above functions. Get website URL from command-line `args`.

### 3. Chat CLI Script (e.g., `ChatCli.kt`)

*   **Core Logic:**
    *   Similar setup (env vars, Ktor client, config, `@Serializable` data classes).
*   **Functions to Implement:**
    *   `generateQueryEmbedding(query)`: Embed user's query using OpenAI API.
    *   `searchQdrant(queryVector)`: Ktor POST to Qdrant search endpoint. Request payload with results.
    *   `getAnthropicAnswer(userQuery, context)`: Ktor POST to Anthropic messages API. Use `x-api-key` header for Anthropic.
*   **`main` function (REPL):**
    *   Loop: `print("> ")`, `readLine()`, process input.
    *   Call functions: embed, search, get LLM answer.
    *   Print answer. Handle `exit`/`quit`.
    *   Close `HttpClient` on exit.

### 4. Running Your Kotlin App (via Gradle)

1.  **Index Content (Run Once):**
    ```bash
    ./gradlew runIndex --args="YOUR_CHOSEN_WEBSITE_URL"
    ```
    *(Assumes `runIndex` task in `build.gradle.kts` for `IndexWebsiteKt`.)*
2.  **Run Chat CLI:**
    ```bash
    ./gradlew runChat -q
    ```
    *(Assumes `runChat` task for `ChatCliKt`. `-q` for quieter Gradle output.)*

**Tip:** Refer to library documentation for Ktor, Jsoup, Jtokkit, Qdrant, OpenAI, and Anthropic for specific API usage.

Good luck! 