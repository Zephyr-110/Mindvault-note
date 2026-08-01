import json
import sys
sys.stdout.reconfigure(line_buffering=True)

from fastapi.responses import StreamingResponse
from fastapi import APIRouter, FastAPI
import uvicorn

from main.python.models.chat_request import ChatRequest
from main.python.models.chat_response import ChatResponse
from main.python.mapper.service_test import ChatService
from models.agent_response import AgentChatResponse


ai_router = APIRouter()
chat_service = ChatService()


@ai_router.post("/api/ai/chat")
def chat(request: ChatRequest) -> ChatResponse:
    return chat_service.chat(request)


@ai_router.post("/api/ai/stream_chat")
def stream_chat(request: ChatRequest) -> StreamingResponse:
    return StreamingResponse(chat_service.stream_chat(request), media_type="text/event-stream")


@ai_router.post("/api/ai/agent_chat")
def agent_chat(request: ChatRequest) -> AgentChatResponse:
    response = chat_service.agent_chat(request)
    return AgentChatResponse(content=response.content, sessionId=request.session_id)

@ai_router.post("/api/ai/stream_agent_chat")
def stream_agent_chat(request: ChatRequest) -> StreamingResponse:
    return StreamingResponse(
        ("data: " + json.dumps(event, ensure_ascii=False) + "\n\n" for event in chat_service.stream_agent_chat(request)),
        media_type="text/event-stream"
    )

@ai_router.get("/api/ai/health")
def health():
    return {"status": "ok"}


app = FastAPI(title="MindVault AI API")
app.include_router(ai_router)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000, log_level="info")