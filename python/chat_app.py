import asyncio
import logging
import os
from typing import List, Tuple

import qdrant_client
from openai import OpenAI
import anthropic
import chainlit as cl
from dotenv import load_dotenv

# --- Configuration ---
# Load environment variables from .env file
load_dotenv()

COLLECTION_NAME = "docs"
QDRANT_URL = "http://localhost:6333"
EMBED_MODEL = "text-embedding-3-small"
LLM_MODEL = "claude-sonnet-4-20250514"

# Setup basic logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# --- Initialize Clients ---
# TODO: Implement client initialization with proper error handling
# 1. Check for required API keys in environment variables
# 2. Initialize OpenAI, Anthropic, and Qdrant clients
# 3. Handle initialization errors gracefully
# 4. Consider how to inform users of setup issues

if not os.getenv("OPENAI_API_KEY"):
    logging.error("OPENAI_API_KEY environment variable not found. Please set it in your .env file.")
if not os.getenv("ANTHROPIC_API_KEY"):
    logging.error("ANTHROPIC_API_KEY environment variable not found. Please set it in your .env file.")

try:
    # TODO: Initialize API clients
    # openai_client = OpenAI()
    # anthropic_client = anthropic.Anthropic()
    # qdrant = qdrant_client.QdrantClient(url=QDRANT_URL)
    pass
except Exception as e:
    logging.error(f"Failed to initialize API clients: {e}")
    raise

SYSTEM_PROMPT = (
    "You are an assistant that answers **only** from the provided <context>. "
    "If the answer cannot be found, simply reply with `I don't know`."
)

def check_qdrant_collection():
    """Checks if the Qdrant collection exists and has points."""
    # TODO: Implement Qdrant collection status check
    # 1. Try to get collection information from Qdrant
    # 2. Check if collection exists and has points
    # 3. Return tuple of (success_boolean, status_message)
    # 4. Handle exceptions for non-existent collections
    
    try:
        # TODO: Get collection info and check points count
        
        return True, f"Collection '{COLLECTION_NAME}' found with points."
    except Exception as e:
        logging.error(f"Could not get Qdrant collection info for '{COLLECTION_NAME}': {e}")
        return False, f"Could not connect to or find Qdrant collection '{COLLECTION_NAME}'. Please ensure Qdrant is running and the indexing script has been run."


def answer_query(query: str, top_k: int = 5) -> Tuple[str, List[str]]:
    """Retrieve context from Qdrant and ask Anthropic; return (answer, sources)."""
    logging.info(f"Embedding query: '{query[:50]}...'")
    
    # TODO: Implement complete RAG pipeline
    # 1. Generate embedding for user query using OpenAI
    # 2. Search Qdrant for relevant documents using the embedding
    # 3. Extract context texts and source URLs from search results
    # 4. Create prompt with context and user query
    # 5. Get answer from Anthropic using the context
    # 6. Handle errors at each step gracefully
    # 7. Return tuple of (answer_text, list_of_sources)
    
    try:
        # TODO: Generate query embedding
        # v = openai_client.embeddings.create(model=EMBED_MODEL, input=[query]).data[0].embedding
        pass
    except Exception as e:
        logging.error(f"Failed to embed query with OpenAI: {e}")
        return f"Error: Could not embed your question. {e}", []

    logging.info(f"Searching Qdrant collection '{COLLECTION_NAME}' for top {top_k} results.")
    try:
        # TODO: Search Qdrant for relevant documents
        # hits = qdrant.search(...)
        pass
    except Exception as e:
        logging.error(f"Failed to search Qdrant: {e}")
        return f"Error: Could not retrieve information from the knowledge base. {e}", []

    # TODO: Process search results
    # Check if hits were found
    # Extract sources and context texts
    # Handle empty results

    # TODO: Generate answer using Anthropic
    # Create context string and user message
    # Call Anthropic API with system prompt
    # Extract and return the response

    return "Implementation needed", []


# --- Chainlit Callbacks ---

@cl.on_chat_start
async def on_chat_start():
    """Initialize the chat session."""
    logging.info("New chat session started.")
    
    # TODO: Implement chat session initialization
    # 1. Check Qdrant collection status asynchronously
    # 2. Send welcome message based on collection status
    # 3. Set session state for chat readiness
    # 4. Verify all API clients are initialized
    
    collection_ok, status_message = await asyncio.get_event_loop().run_in_executor(None, check_qdrant_collection)
    
    if collection_ok:
        await cl.Message(
            f"👋 Welcome! I'm ready to answer questions about the indexed content. {status_message}"
        ).send()
        cl.user_session.set("ready_to_chat", True)
    else:
        await cl.Message(
            f"⚠️ Welcome! There seems to be an issue with the knowledge base. {status_message}"
        ).send()
        cl.user_session.set("ready_to_chat", False)
    
    # TODO: Add client verification check


@cl.on_message
async def on_message(message: cl.Message):
    """Handle incoming user messages."""
    # TODO: Implement message handling
    # 1. Check if chat is ready (knowledge base available)
    # 2. Extract user query from message
    # 3. Call answer_query function asynchronously
    # 4. Format response with sources as clickable links
    # 5. Handle errors gracefully
    # 6. Update the message with final content
    
    if not cl.user_session.get("ready_to_chat", False):
        await cl.Message(
            "I'm not ready to chat yet. Please check the initial messages or server logs for issues."
        ).send()
        return

    query = message.content.strip()
    logging.info(f"Received query: '{query}'")

    msg = cl.Message(content="")
    await msg.send()

    try:
        # TODO: Process query and get answer
        # answer_text, sources = await asyncio.get_event_loop().run_in_executor(None, answer_query, query)
        
        # TODO: Format response with sources
        # Create markdown formatted response with clickable source links
        
        # TODO: Update message with final content
        
        pass

    except Exception as e:
        logging.error(f"Error processing message: {e}")
        await cl.Message(content=f"❌ An unexpected error occurred: {e}").send()