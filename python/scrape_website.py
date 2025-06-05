import logging
import os
import re
import uuid
import time
from collections import deque
from typing import List, Tuple
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup
import numpy as np
import qdrant_client
from qdrant_client.http.models import Distance, VectorParams
from openai import OpenAI
from dotenv import load_dotenv
import tiktoken

# --- Configuration ---
# Load environment variables from .env file
load_dotenv()

# These can be overridden with environment variables for quick tuning.
MAX_PAGES = 50  # crawl limit per website
CHUNK_SIZE = 800  # in tokens (approx.)
CHUNK_OVERLAP = 40 # overlap between chunks
COLLECTION_NAME = "docs"
QDRANT_URL = "http://localhost:6333"
EMBED_MODEL = "text-embedding-3-small"
SITE_TO_INDEX = "https://ruter.no"

# Setup basic logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# --- Utility Functions ---
def _normalize_url(url: str) -> str:
    """Normalize URL by removing fragments and query parameters, adding trailing slash for directories."""
    # TODO: Implement URL normalization to ensure consistency.
    #       This helps in avoiding duplicate processing of URLs that point to the same content.
    #       Useful functions: `from urllib.parse import urlparse, urlunparse` (ensure this import is at the top of the file)
    #       Also, `import os` might be needed for `os.path.splitext`.
    # 1. Parse the URL using `parsed_url = urlparse(url)`.
    #    This breaks the URL into components: scheme, netloc, path, params, query, fragment.
    # 2. Create a new path ensuring it has a trailing slash if it doesn't point to a specific file (heuristic: no extension).
    #    `path = parsed_url.path`
    #    `if path and not os.path.splitext(path)[1] and not path.endswith('/'):`
    #    `    path += '/'`
    # 3. Reconstruct the URL without the fragment and query, and with the potentially modified path.
    #    `normalized_uri_tuple = parsed_url._replace(path=path, query='', fragment='')`
    # 4. Convert the named tuple back to a URL string: `final_url = urlunparse(normalized_uri_tuple)`.
    # 5. Return the `final_url`.
    return "" # Placeholder: Replace with your implementation

def scrape_site(base_url: str, max_pages: int = MAX_PAGES) -> List[Tuple[str, str]]:
    """Scrape a website and return list of (url, text_content) tuples."""
    logging.info(f"Starting to scrape {base_url}, up to {max_pages} pages.")
    
    # TODO: Implement website scraping
    # 1. Initialize session, seen URLs set, result list, and queue
    # 2. Get domain from base URL for filtering
    # 3. Create extract_text function using BeautifulSoup:
    #    - Remove unwanted tags (script, style, nav, header, footer, etc.)
    #    - Extract clean text content
    #    - Clean up whitespace
    # 4. Implement main scraping loop:
    #    - Process URLs from queue while under max_pages limit
    #    - Skip already visited URLs and external domains
    #    - Fetch page content with proper error handling
    #    - Extract text and add to results if not empty
    #    - Find and enqueue internal links
    
    session = requests.Session()
    seen, result = set(), []
    queue = deque([base_url])
    domain = urlparse(base_url).netloc
    pages_processed = 0

    def extract_text(html: str) -> str:
        """Extract clean text from HTML using BeautifulSoup."""
        # TODO: Implement text extraction using BeautifulSoup.
        #       The goal is to get meaningful text content for the RAG model, excluding irrelevant parts.
        #       Make sure you have `from bs4 import BeautifulSoup` (at the top of the file).
        #       You might also need `import re` for advanced whitespace cleanup.
        # 1. Create a BeautifulSoup object to parse the HTML:
        #    `soup = BeautifulSoup(html, 'html.parser')`
        # 2. Remove unwanted HTML tags that usually don't contain main content.
        #    Common tags to remove: <script>, <style>, <nav>, <header>, <footer>, <aside>.
        #    Example for removing all 'script' and 'style' tags:
        #    `for RAG_ignore_tag in soup(['script', 'style', 'nav', 'header', 'footer', 'aside']):`
        #    `    RAG_ignore_tag.decompose()` # .decompose() removes the tag and its content.
        # 3. Get all text from the modified soup. `soup.get_text()` is useful here.
        #    - `separator=' '` helps ensure spaces between text from different tags.
        #    - `strip=True` removes leading/trailing whitespace from individual text pieces.
        #    `text = soup.get_text(separator=' ', strip=True)`
        # 4. Optional: Further clean the text by removing excessive newlines or multiple spaces.
        #    `text = re.sub(r'\s+', ' ', text).strip()` (requires `import re`)
        # 5. Return the cleaned text string.
        return "" # Placeholder: Replace with your implementation

    while queue and len(result) < max_pages:
        url = queue.popleft()
        # Ensure URL is normalized *before* adding to seen set and processing. This avoids duplicate work.
        normalized_url = _normalize_url(url)
        
        # TODO: Implement URL filtering and page processing logic.
        # This is the core of the scraper. Follow these steps carefully.
        # 0. Basic check: if `normalized_url` is empty or invalid, `continue`.
        #    `if not normalized_url: continue`
        # 1. Skip if `normalized_url` is already in the `seen` set (we've processed or queued it).
        #    `if normalized_url in seen: continue`
        # 2. Add `normalized_url` to the `seen` set to mark it as processed/queued.
        #    `seen.add(normalized_url)`
        # 3. Skip if the URL's domain is different from the `base_url`'s domain (to stay on the same site).
        #    `if urlparse(normalized_url).netloc != domain: continue` (ensure `from urllib.parse import urlparse` is imported)
        # 4. Fetch page content using the `session` object.
        #    - Use `session.get(url, timeout=10)` (10-second timeout).
        #    - Wrap this in a `try-except requests.RequestException` block to handle network errors, timeouts, etc.
        #    - If an error occurs, log a warning (`logging.warning(...)`) and `continue` to the next URL.
        #    - After a successful request, check `response.raise_for_status()` to catch HTTP errors (4xx, 5xx). Also in try-except.
        #    `try:`
        #    `    response = session.get(url, timeout=10)`
        #    `    response.raise_for_status() # Raises HTTPError for bad responses (4XX or 5XX)`
        #    `except requests.RequestException as e:`
        #    `    logging.warning(f"Failed to fetch {url}: {e}")`
        #    `    continue`
        # 5. Ensure the content type is HTML before trying to parse it.
        #    `content_type = response.headers.get('Content-Type', '')`
        #    `if 'text/html' not in content_type: continue`
        # 6. Extract text from `response.text` (the HTML content) using the `extract_text()` function you defined above.
        #    `text_content = extract_text(response.text)`
        # 7. If extracted `text_content` is not empty (i.e., `if text_content:`), add the tuple `(normalized_url, text_content)` to the `result` list.
        # 8. Parse the fetched HTML (`response.text`) with BeautifulSoup again (or use the soup from `extract_text` if structured that way) to find all links.
        #    `page_soup = BeautifulSoup(response.text, 'html.parser')`
        # 9. Find all anchor (`<a>`) tags that have an `href` attribute.
        #    `for link_tag in page_soup.find_all('a', href=True):`
        #10. For each found link:
        #    a. Get the raw `href` value: `href = link_tag['href']`
        #    b. Construct an absolute URL from `base_url` and `href`: `absolute_link = urljoin(base_url, href)`. (ensure `from urllib.parse import urljoin` is imported)
        #    c. Normalize this `absolute_link` using `_normalize_url()`. Let's call it `normalized_absolute_link`.
        #    d. Check if this `normalized_absolute_link` should be added to the queue:
        #       - Its domain must match the original `domain` (`urlparse(normalized_absolute_link).netloc == domain`).
        #       - It must NOT already be in the `seen` set.
        #       - If both conditions are true, add `normalized_absolute_link` to the `queue`.
        #11. Increment `pages_processed` counter (already exists below this comment block in the template).
        
        if normalized_url in seen: # Example for step 1
            continue
        # Add the rest of the scraping logic here based on the detailed hints above
        
    logging.info(f"Scraping complete. Found {len(result)} pages with text.")
    return result

def chunk_text(text: str, size: int = CHUNK_SIZE, overlap: int = CHUNK_OVERLAP) -> List[str]:
    """Split text into overlapping chunks of approximately `size` tokens using tiktoken."""
    # TODO: Implement token-based text chunking using tiktoken
    # 1. Import tiktoken: `import tiktoken`
    # 2. Get the encoding for the model (e.g., cl100k_base for text-embedding-3-small):
    #    `encoding = tiktoken.get_encoding("cl100k_base")` (or use tiktoken.encoding_for_model(EMBED_MODEL))
    # 3. Encode the input text into token IDs:
    #    `tokens = encoding.encode(text)`
    # 4. If the total number of tokens is less than or equal to `size`, return the original text as a single chunk:
    #    `if len(tokens) <= size: return [text] if text.strip() else []`
    # 5. Calculate the step size for chunks, considering the overlap:
    #    `step = size - overlap`
    #    `if step <= 0: raise ValueError("Overlap cannot be greater than or equal to chunk size.")`
    # 6. Create chunks by sliding a window over the token IDs:
    #    `chunks = []`
    #    `for i in range(0, len(tokens), step):`
    #    `    token_chunk = tokens[i:i + size]`
    #    `    # Ensure the last chunk is not too small or handle as needed, though typically we take what's left.
    #    `    if not token_chunk: continue`
    #    `    chunk_text = encoding.decode(token_chunk)`
    #    `    if chunk_text.strip(): chunks.append(chunk_text.strip())`
    # 7. Handle potential errors during decoding if any invalid token sequences occur, though `decode` is generally robust.
    # 8. Return the list of text chunks.
    
    # Remove old approximation-based logic
    # words = text.split()
    # approx_tokens_per_word = 0.75 
    
    # TODO: Implement chunking logic here using tiktoken as per the hints above
    
    return []

# --- API Clients Setup ---
openai_api_key = os.getenv("OPENAI_API_KEY")
anthropic_api_key = os.getenv("ANTHROPIC_API_KEY")

if not openai_api_key:
    logging.error("OPENAI_API_KEY environment variable not found. Please set it in your .env file.")
    exit(1)

openai_client = OpenAI(api_key=openai_api_key)
qdrant = qdrant_client.QdrantClient(url=QDRANT_URL)

# --- Core Indexing Logic ---
def _ensure_collection(vector_size: int = 1536):
    """Ensure Qdrant collection exists with proper configuration."""
    logging.info(f"Ensuring Qdrant collection '{COLLECTION_NAME}' exists with vector size {vector_size}.")
    
    # TODO: Implement Qdrant collection creation/recreation.
    #       This function ensures that the Qdrant collection is ready for new data.
    #       For a workshop, recreating the collection ensures a clean state each time the script runs.
    #       Required imports: `from qdrant_client.http import models as rest` (or use `qdrant_client.models` directly if preferred)
    #       Reference the `qdrant` client initialized earlier.
    #       The `VECTOR_SIZE` needs to match your embedding model. For OpenAI's "text-embedding-3-small" (EMBED_MODEL), this is 1536.
    # 1. Attempt to recreate the collection. This is often simpler than checking existence and then creating/updating.
    #    `qdrant.recreate_collection(`
    #    `    collection_name=COLLECTION_NAME,`
    #    `    vectors_config=qdrant_client.models.VectorParams(size=vector_size, distance=qdrant_client.models.Distance.COSINE)`
    #    `)`
    #    - `collection_name`: Use the `COLLECTION_NAME` constant.
    #    - `vectors_config`: Defines the properties of the vectors to be stored.
    #        - `size`: Must match the dimensionality of the embeddings (e.g., 1536 for `text-embedding-3-small`). Pass the `vector_size` argument.
    #        - `distance`: The metric to measure similarity. `qdrant_client.models.Distance.COSINE` is common for text embeddings.
    # 2. Log a success message after creation.
    #    `logging.info(f"Collection '{COLLECTION_NAME}' created/recreated successfully.")`
    # 3. Add a short delay (`time.sleep(2)`) to allow Qdrant to stabilize after collection operations, as suggested in the original hints.
    #    (ensure `import time` is at the top of the file)
    # 4. Wrap the Qdrant call in a `try-except` block to catch potential errors.
    #    `try:`
    #    `    ... qdrant.recreate_collection(...) ...`
    #    `except Exception as e:`
    #    `    logging.error(f"Critical error during collection recreation for '{COLLECTION_NAME}': {e}")`
    #    `    # Propagate the error to stop the script if the collection can't be set up.`
    #    `    raise`
    
    try:
        # Example: (Replace VECTOR_SIZE with the actual variable if you define it, or use the vector_size parameter)
        logging.info(f"Recreating Qdrant collection: '{COLLECTION_NAME}' with vector size {vector_size} and COSINE distance.")
        qdrant.recreate_collection(
            collection_name=COLLECTION_NAME,
            vectors_config=qdrant_client.models.VectorParams(size=vector_size, distance=qdrant_client.models.Distance.COSINE)
        )
        logging.info(f"Collection '{COLLECTION_NAME}' created successfully.")
        
        time.sleep(2) # Delay for stability
    except Exception as e:
        logging.error(f"Critical error during collection recreation for '{COLLECTION_NAME}': {e}")
        # TODO: Add additional error handling and info gathering if needed. For now, re-raising is good.
        raise

def index_website(base_url: str):
    """Main indexing function that orchestrates the entire process."""
    logging.info(f"Starting indexing process for {base_url}...")
    
    # TODO: Implement the complete indexing workflow step-by-step.
    # This function combines scraping, chunking, embedding, and uploading.

    # 1. Scrape the website to get pages.
    #    - Call `pages = scrape_site(base_url, max_pages=MAX_PAGES)` (as already implemented below).
    #    - Check if `pages` is empty. If so, log a warning and return early (already implemented).

    # 2. Initialize lists to store data for Qdrant: `all_texts_to_embed = []`, `point_ids = []`, `payloads = []`.
    #    (The original template uses `texts, ids, payloads` which is fine too.)

    # 3. Process each scraped page to create chunks.
    #    `logging.info(f"Processing {len(pages)} scraped pages into chunks...")`
    #    `for page_url, page_content in pages:`
    #    `    # Chunk the text content of the page using the `chunk_text` function.`
    #    `    text_chunks = chunk_text(page_content, size=CHUNK_SIZE, overlap=CHUNK_OVERLAP)`
    #    `    for chunk in text_chunks:`
    #    `        if not chunk.strip(): continue # Skip empty chunks`
    #    `        all_texts_to_embed.append(chunk)`
    #    `        # Create a unique ID for each chunk. `uuid.uuid4()` is good for this.`
    #    `        # Ensure `import uuid` is at the top of the file.
    #    `        point_ids.append(str(uuid.uuid4()))`
    #    `        # Create the payload for Qdrant. This stores metadata associated with the vector.`
    #    `        # It should include the source URL and the chunk text itself for context retrieval.
    #    `        payloads.append({"url": page_url, "text": chunk, "original_url": page_url}) # or any other metadata`
    #    (The original template initializes `texts, ids, payloads` and has a TODO for processing pages - these hints replace that.)

    # 4. Check if any text chunks were generated.
    #    - If `all_texts_to_embed` is empty, log a warning and return (already implemented with `texts`).

    # 5. Generate embeddings for all text chunks in batches.
    #    `logging.info(f"Generating embeddings for {len(all_texts_to_embed)} text chunks...")`
    #    `vectors = []`
    #    `BATCH_SIZE_EMBEDDING = 100 # Define how many texts to embed in one API call.`
    #    `for i in range(0, len(all_texts_to_embed), BATCH_SIZE_EMBEDDING):`
    #    `    batch_texts = all_texts_to_embed[i:i + BATCH_SIZE_EMBEDDING]`
    #    `    try:`
    #    `        # Use the openai_client to create embeddings.`
    #    `        response = openai_client.embeddings.create(model=EMBED_MODEL, input=batch_texts)`
    #    `        # Extract the embedding vectors from the response.`
    #    `        batch_vectors = [item.embedding for item in response.data]`
    #    `        vectors.extend(batch_vectors)`
    #    `        logging.info(f"Embedded batch {i // BATCH_SIZE_EMBEDDING + 1}/{(len(all_texts_to_embed) -1) // BATCH_SIZE_EMBEDDING + 1}")`
    #    `    except Exception as e:`
    #    `        logging.error(f"Error embedding batch starting at index {i}: {e}")`
    #    `        # Decide on error strategy: skip batch, stop all, etc. For now, log and continue might mean partial indexing.`
    #    `        # To be robust, you might add retries or save failed batches for later processing.`
    #    (The original template has a TODO for batch embedding generation - these hints replace that.)

    # 6. Verify that embeddings were generated correctly.
    #    - If `vectors` is empty or `len(vectors) != len(all_texts_to_embed)`, log an error and return (already implemented).

    # 7. Ensure the Qdrant collection exists.
    #    - Determine the vector size from your embeddings (e.g., `vector_dim = len(vectors[0]) if vectors else 1536`).
    #    - Call `_ensure_collection(vector_size=vector_dim)`. This will create/recreate the collection.

    # 8. Upload all data (points with vectors and payloads) to Qdrant in batches.
    #    `logging.info(f"Uploading {len(vectors)} points to Qdrant collection '{COLLECTION_NAME}'...")`
    #    `BATCH_SIZE_QDRANT = 100 # Define how many points to upload in one Qdrant API call.`
    #    `for i in range(0, len(vectors), BATCH_SIZE_QDRANT):`
    #    `    batch_ids = point_ids[i:i + BATCH_SIZE_QDRANT]`
    #    `    batch_vectors = vectors[i:i + BATCH_SIZE_QDRANT]`
    #    `    batch_payloads = payloads[i:i + BATCH_SIZE_QDRANT]`
    #    `    # Create Qdrant PointStructs.`
    #    `    # Ensure `from qdrant_client.http.models import PointStruct` (or `from qdrant_client import models`)
    #    `    points_to_upload = [`
    #    `        qdrant_client.models.PointStruct(id=pid, vector=vec, payload=pl)`
    #    `        for pid, vec, pl in zip(batch_ids, batch_vectors, batch_payloads)`
    #    `    ]`
    #    `    try:`
    #    `        # Upsert the points to Qdrant.`
    #    `        qdrant.upsert(collection_name=COLLECTION_NAME, points=points_to_upload, wait=True)`
    #    `        logging.info(f"Uploaded batch {i // BATCH_SIZE_QDRANT + 1}/{(len(vectors)-1) // BATCH_SIZE_QDRANT + 1} to Qdrant.")`
    #    `    except Exception as e:`
    #    `        logging.error(f"Error uploading batch to Qdrant starting at index {i}: {e}")`
    #    (The original template has a TODO for setting up collection and uploading data - these hints replace that.)
    
    pages = scrape_site(base_url, max_pages=MAX_PAGES)
    if not pages:
        logging.warning(f"No pages scraped from {base_url}. Nothing to index.")
        return

    texts, ids, payloads = [], [], []
    
    # TODO: Process pages and create chunks
    # Based on the detailed hints above, this involves iterating through `pages`,
    # calling `chunk_text`, and populating `texts` (or `all_texts_to_embed`), `ids`, and `payloads`.
    
    if not texts:
        logging.warning("No text chunks available for embedding after processing all pages.")
        return

    logging.info(f"Generated {len(texts)} text chunks for embedding.")

    # TODO: Generate embeddings in batches
    # Based on the detailed hints (Step 5), this involves iterating through `texts` (or `all_texts_to_embed`)
    # in batches, calling `openai_client.embeddings.create`, and populating `vectors`.
    
    vectors = []
    BATCH_SIZE_EMBEDDING = 100
    
    # TODO: Implement batch embedding generation
    
    if not vectors or len(vectors) != len(texts):
        logging.error(f"Embedding failed or produced incomplete results. Expected {len(texts)} vectors, got {len(vectors)}. Aborting upload.")
        return

    logging.info(f"Successfully embedded {len(vectors)} chunks.")
    
    # TODO: Setup collection and upload data
    # Based on the detailed hints (Step 7 & 8):
    # 1. Determine vector dimension: `vector_dim = len(vectors[0]) if vectors else 1536`
    # 2. Call `_ensure_collection(vector_size=vector_dim)`
    # 3. Prepare `PointStructs` and upload to Qdrant in batches using `qdrant.upsert`.
    

if __name__ == "__main__":
    logging.info(f"--- Starting Website Indexing Script ---")
    
    # TODO: Implement startup checks and main execution flow.
    # 1. Check Qdrant connection health before starting the indexing process.
    #    - A simple way is to try to get cluster info: `qdrant.get_collections()` or `qdrant.cluster_info()`.
    #    - Wrap this in a `try-except` block. If it fails, log an error and exit (`exit(1)`).
    #    (The existing try-except block below can be adapted for this.)
    # 2. Define the `site_to_index` (using the `SITE_TO_INDEX` constant is good).
    # 3. Call the main `index_website(site_to_index)` function.
    # 4. Wrap the `index_website` call in a `try-except` block to catch any errors during the main process.
    #    Log a generic error message if something goes wrong.
    
    try:
        # TODO: Check Qdrant connection using a client method like `qdrant.get_collections()`
        # Example:
        # collections_response = qdrant.get_collections()
        # logging.info(f"Qdrant collections: {collections_response.collections}")
        # This confirms the client can communicate with the server.
        # If an exception occurs, it will be caught by the except block below.
        
        logging.info("Successfully connected to Qdrant (or client initialized - actual check recommended).")
    except Exception as e:
        logging.error(f"Could not connect to Qdrant at {QDRANT_URL}. Please ensure Qdrant is running. Error: {e}")
        exit(1)
        
    # TODO: Run indexing for the SITE_TO_INDEX
    # Example:
    # try:
    #     index_website(SITE_TO_INDEX)
    #     logging.info(f"Indexing process for {SITE_TO_INDEX} completed.")
    # except Exception as e:
    #     logging.error(f"An error occurred during the indexing process for {SITE_TO_INDEX}: {e}")
    #     # Optionally, re-raise or exit with an error code if it's a critical failure for an automated script
    
    logging.info(f"--- Indexing Script Finished ---")