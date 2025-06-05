# Welcome to the RAG Chat Application!

This is a Retrieval Augmented Generation (RAG) chat application built with Chainlit. 

## How it works:

1. **Document Indexing**: The `scrape_website.py` script crawls a website, chunks the content, and stores embeddings in Qdrant vector database.

2. **Query Processing**: When you ask a question, the app:
   - Converts your question to an embedding
   - Searches for relevant content in the vector database
   - Uses the retrieved context to generate an accurate answer

3. **AI-Powered Responses**: The app uses Claude (Anthropic) to generate responses based only on the indexed content.

## Getting Started:

1. Make sure you have run the indexing script first
2. Ensure Qdrant is running locally
3. Start asking questions about the indexed content!

The app will only answer based on the information it has indexed, ensuring accurate and grounded responses.