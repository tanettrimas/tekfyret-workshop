# Python RAG Chatbot Workshop Guide

**Goal:** Build a Retrieval Augmented Generation (RAG) chatbot that answers questions based on the content of a website you choose.

**Core Idea:**
1.  **Indexing Pipeline:** Scrape website -> Extract Text -> Chunk Text -> Create Embeddings -> Store in Vector DB (Qdrant).
2.  **Chat Pipeline:** User Query -> Create Query Embedding -> Search Vector DB -> Get Relevant Chunks -> Combine with Query for LLM Prompt -> Get Answer from LLM (Anthropic).

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

## Building Steps (Python)

### 1. Project Setup

*   **Start Qdrant:** Use Docker for a local Qdrant instance.
    ```bash
    mkdir -p ./qdrant_storage
    docker run -d --name rag-qdrant -p 6333:6333 -v "$(pwd)/qdrant_storage":/qdrant/storage qdrant/qdrant
    ```
    *Verify: `http://localhost:6333/dashboard`*
*   **API Keys:** Create a `.env` file in your project root with `OPENAI_API_KEY` and `ANTHROPIC_API_KEY`.
*   **Python Environment:**
    ```bash
    python3 -m venv .venv
    source .venv/bin/activate
    ```
*   **Install Libraries:**
    ```bash
    pip install -U requests beautifulsoup4 openai qdrant-client anthropic chainlit python-dotenv numpy tiktoken
    ```

### 2. Indexing Pipeline (e.g., create `index_content.py`)

*   **Imports:** `os, requests, BeautifulSoup, OpenAI, QdrantClient, VectorParams, PointStruct, uuid, dotenv, tiktoken` (if using tiktoken for chunking).
*   **Load Env Vars:** `load_dotenv()`.
*   **Config:** API keys, QDRANT_URL, `COLLECTION_NAME` (e.g., "py_docs"), `EMBED_MODEL` (e.g., "text-embedding-3-small"), target `WEBSITE_URL`, `MAX_PAGES`.
*   **Initialize Clients:** `OpenAI()`, `QdrantClient(url=QDRANT_URL)`.
*   **`scrape_website(base_url, max_pages)` function:**
    *   Use `requests.get()` and `BeautifulSoup` to fetch and parse HTML.
    *   Extract main text content (e.g., from `<body>`), remove scripts, styles, navs, footers.
    *   Implement a basic same-domain link crawler (e.g., using a `deque` and `set` for visited URLs).
    *   Return `List[Tuple[str, str]]` (url, text).
*   **`chunk_text(text, model_name, chunk_size_tokens, overlap_tokens)` function:**
    *   Use `tiktoken.encoding_for_model(model_name)` to get tokenizer.
    *   Encode text to tokens. Create overlapping windows of tokens. Decode back to text strings.
    *   Return `List[str]` (chunks).
*   **`get_embeddings(text_chunks, model)` function:**
    *   Call `openai_client.embeddings.create(model=model, input=text_chunks)`.
    *   Return `List[List[float]]` (vectors).
*   **Main Indexing Logic in `if __name__ == "__main__":`**
    1.  Call `scrape_website()`.
    2.  Iterate through scraped pages, call `chunk_text()` for each page's text.
    3.  Collect all chunks and their source URLs.
    4.  Call `get_embeddings()` for all chunks.
    5.  `qdrant_client.recreate_collection(...)` (vector_size=1536 for `text-embedding-3-small`).
    6.  Create `PointStruct` objects (id, vector, payload with original text and source URL).
    7.  `qdrant_client.upsert(...)` points to Qdrant.

### 3. Chat Application (e.g., create `chat_ui.py` with Chainlit)

*   **Imports:** `os, chainlit as cl, OpenAI, QdrantClient, Anthropic, dotenv`.
*   **Load Env Vars & Config:** Similar to indexing script. Use the same `COLLECTION_NAME`.
*   **Initialize Clients.**
*   **`answer_question(user_query)` function:**
    1.  Get query embedding: `openai_client.embeddings.create(...)`.
    2.  Search Qdrant: `qdrant_client.search(collection_name=..., query_vector=..., limit=3, with_payload=True)`.
    3.  Construct `context` from search result payloads.
    4.  Create prompt for Anthropic (system prompt, context, user query).
    5.  Call `anthropic_client.messages.create(model=LLM_MODEL, ...)`.
    6.  Extract and return answer text and source URLs from results.
*   **Chainlit UI (`@cl.on_chat_start`, `@cl.on_message`):**
    *   `@cl.on_chat_start`: Send a welcome message.
    *   `@cl.on_message`: Get `message.content`, call `answer_question()` (use `await cl.make_async(answer_question)(...)`), send the answer, and display sources.

### 4. Running Your Python App

1.  `python index_content.py` (Run once, or to update index)
2.  `chainlit run chat_ui.py`

Good luck! This is a high-level guide; refer to library documentation for details. 