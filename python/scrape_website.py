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
    # TODO: Implement URL normalization
    # 1. Remove fragment (#) and query (?) parts
    # 2. Add trailing slash for URLs without file extensions
    return ""

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
        """Extract clean text from HTML."""
        # TODO: Implement text extraction using BeautifulSoup
        return ""

    while queue and len(result) < max_pages:
        url = queue.popleft()
        normalized_url = _normalize_url(url)
        
        # TODO: Implement URL filtering and processing logic
        # Check if URL already seen or external domain
        # Fetch page, extract text, find links
        
        if normalized_url in seen:
            continue
        
        # TODO: Complete the scraping logic
        
    logging.info(f"Scraping complete. Found {len(result)} pages with text.")
    return result

def chunk_text(text: str, size: int = CHUNK_SIZE, overlap: int = CHUNK_OVERLAP) -> List[str]:
    """Split text into overlapping chunks of approximately `size` tokens."""
    # TODO: Implement text chunking
    # 1. Split text into words
    # 2. Calculate approximate tokens per word (use 0.75 as rough estimate)
    # 3. Convert token sizes to word counts
    # 4. Calculate step size for overlapping chunks
    # 5. Create chunks using sliding window approach
    # 6. Return list of non-empty chunks
    
    words = text.split()
    approx_tokens_per_word = 0.75 
    
    # TODO: Implement chunking logic
    
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
    
    # TODO: Implement collection management
    # 1. Use qdrant.recreate_collection() to create fresh collection
    # 2. Configure with proper vector size and cosine distance
    # 3. Set appropriate timeout for robustness
    # 4. Add delay for Qdrant to stabilize
    # 5. Handle exceptions and provide detailed error messages
    
    try:
        # TODO: Implement collection recreation
        
        time.sleep(2) # Delay for stability
    except Exception as e:
        logging.error(f"Critical error during collection recreation for '{COLLECTION_NAME}': {e}")
        # TODO: Add additional error handling and info gathering
        raise

def index_website(base_url: str):
    """Main indexing function that orchestrates the entire process."""
    logging.info(f"Starting indexing process for {base_url}...")
    
    # TODO: Implement complete indexing workflow
    # 1. Scrape the website to get pages
    # 2. Check if any pages were found
    # 3. Process each page:
    #    - Chunk the text content
    #    - Create unique IDs for each chunk
    #    - Build payload with source URL and text
    # 4. Generate embeddings for all text chunks:
    #    - Use batch processing for efficiency
    #    - Handle API errors gracefully
    # 5. Ensure Qdrant collection exists
    # 6. Upload all data to Qdrant
    
    pages = scrape_site(base_url, max_pages=MAX_PAGES)
    if not pages:
        logging.warning(f"No pages scraped from {base_url}. Nothing to index.")
        return

    texts, ids, payloads = [], [], []
    
    # TODO: Process pages and create chunks
    
    if not texts:
        logging.warning("No text chunks available for embedding after processing all pages.")
        return

    logging.info(f"Generated {len(texts)} text chunks for embedding.")

    # TODO: Generate embeddings in batches
    vectors = []
    BATCH_SIZE_EMBEDDING = 100
    
    # TODO: Implement batch embedding generation
    
    if not vectors or len(vectors) != len(texts):
        logging.error(f"Embedding failed or produced incomplete results. Expected {len(texts)} vectors, got {len(vectors)}. Aborting upload.")
        return

    logging.info(f"Successfully embedded {len(vectors)} chunks.")
    
    # TODO: Setup collection and upload data
    

if __name__ == "__main__":
    logging.info(f"--- Starting Website Indexing Script ---")
    
    # TODO: Implement startup checks and main execution
    # 1. Check if Qdrant is available and accessible
    # 2. Run the indexing process for the target site
    # 3. Handle any critical errors gracefully
    
    try:
        # TODO: Check Qdrant connection
        
        logging.info("Successfully connected to Qdrant.")
    except Exception as e:
        logging.error(f"Could not connect to Qdrant at {QDRANT_URL}. Please ensure Qdrant is running. Error: {e}")
        exit(1)
        
    # TODO: Run indexing
    
    logging.info(f"--- Indexing Script Finished ---")