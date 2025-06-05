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
LLM_MODEL = "claude-3-haiku-20240307"

# Setup basic logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# --- Initialize Clients ---
# TODO: Implement client initialization with proper error handling
# 1. Check for required API keys in environment variables
# 2. Initialize OpenAI, Anthropic, and Qdrant clients
# 3. Handle initialization errors gracefully
# 4. Consider how to inform users of setup issues

# Example global variable declarations (you'll assign them in the try block)
openai_client = None
anthropic_client = None
qdrant = None # Renaming for consistency with later usage

if not os.getenv("OPENAI_API_KEY"):
    logging.error("CRITICAL: OPENAI_API_KEY environment variable not found. Set it in your .env file.")
    # For a real app, you might raise SystemExit here to prevent startup.
if not os.getenv("ANTHROPIC_API_KEY"):
    logging.error("CRITICAL: ANTHROPIC_API_KEY environment variable not found. Set it in your .env file.")
    # For a real app, you might raise SystemExit here.

try:
    # Initialize API clients here
    openai_client = OpenAI() # API key is read from OPENAI_API_KEY environment variable by default
    anthropic_client = anthropic.Anthropic() # API key is read from ANTHROPIC_API_KEY by default
    qdrant = qdrant_client.QdrantClient(url=QDRANT_URL) # Use QDRANT_URL constant
    
    logging.info("OpenAI, Anthropic, and Qdrant clients initialized successfully.")

except Exception as e:
    logging.error(f"CRITICAL: Failed to initialize one or more API clients during setup: {e}", exc_info=True)
    raise SystemExit(f"Application cannot start due to API client initialization failure: {e}")

SYSTEM_PROMPT = (
    "You are an assistant that answers **only** from the provided <context>. "
    "If the answer cannot be found, simply reply with `I don't know`."
)

def check_qdrant_collection() -> Tuple[bool, str]:
    """Checks if the Qdrant collection exists and has points."""
    # TODO: 1. Verify that `qdrant` client is initialized
    # TODO: 2. Call Qdrant to fetch collection metadata for COLLECTION_NAME
    # TODO: 3. Determine if the collection exists (404 vs success)
    # TODO: 4. Check `points_count` > 0
    # TODO: 5. Return (True, status_message) or (False, error_message)
    # TODO: 6. Wrap all calls in try/except and log any errors
    pass


def answer_query(query: str, top_k: int = 5) -> Tuple[str, List[str]]:
    """Retrieve context from Qdrant and ask Anthropic; return (answer, sources)."""
    # TODO: 0. Verify that openai_client, anthropic_client and qdrant are all initialized
    
    # TODO: 1. Generate an embedding for `query` using OpenAI
    #    - Use openai_client.embeddings.create(...)
    #    - Extract the vector from the response
    
    # TODO: 2. Search Qdrant with that embedding
    #    - Use qdrant.search(...)
    #    - Request payloads so you get stored text and URLs
    
    # TODO: 3. Extract and assemble:
    #    - A list of source URLs
    #    - A combined context string from the returned document texts
    
    # TODO: 4. Build your prompt:
    #    - Wrap your context in <context>…</context>
    #    - Append “User Question: {query}”
    #    - Apply your SYSTEM_PROMPT
    
    # TODO: 5. Send the prompt to Anthropic for completion
    #    - Use anthropic_client.messages.create(...)
    #    - Extract the answer text
    
    # TODO: 6. Add proper try/except around each external call
    #    - Log or return useful error messages if something fails
    
    # TODO: 7. Return a tuple of (answer_text, sources_list)
    #    - Ensure you return unique/filtered URLs
    
    pass


# --- Chainlit Callbacks ---

@cl.on_chat_start
async def on_chat_start():
    """Initialize the chat session."""
    # TODO: 1. Verify that OpenAI, Anthropic and Qdrant clients are all initialized
    # TODO: 2. Run `check_qdrant_collection` asynchronously
    # TODO: 3. Based on its result, send either a welcome or warning message
    # TODO: 4. Set `cl.user_session.set("ready_to_chat", True/False)` accordingly
    # TODO: 5. Handle and log any unexpected exceptions
    pass


@cl.on_message
async def on_message(message: cl.Message):
    """Handle incoming user messages."""
    # TODO: 1. Check `cl.user_session.get("ready_to_chat")` and bail if False
    # TODO: 2. Validate/extract the text query from `message.content`
    # TODO: 3. Send a “Thinking…” placeholder response
    # TODO: 4. Call `answer_query` in a background thread
    # TODO: 5. Format the returned answer and sources into Markdown
    # TODO: 6. Update or replace the placeholder with the final content
    # TODO: 7. Wrap each step in try/except to catch/log errors and inform the user
    #await cl.Message(content=).send()