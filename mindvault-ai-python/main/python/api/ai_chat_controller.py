from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from fastapi import FastAPI

from main.python.models.chat_request import ChatRequest
from main.python.models.chat_response import ChatResponse
from main.python.service.chat_service import ChatService
from main.python.models.rerank_dto import RerankDTO



class AIChatController:

    # 实例化 FastAPI 实例
    app = FastAPI()
    chat_service = ChatService()

    @staticmethod
    @app.post("/api/ai/chat")
    def chat(request: ChatRequest) -> ChatResponse:
        return AIChatController.chat_service.chat(request)

    @staticmethod
    @app.post("/api/ai/stream_chat")
    def stream_chat(request: ChatRequest) -> StreamingResponse:
                                                                        #该参数对浏览器说明，该接口返回的是流，需要SSE支持
        return StreamingResponse(AIChatController.chat_service.stream_chat(request), media_type="text/event-stream")

    @staticmethod
    @app.post("/api/ai/rerank")
    def rerank(request: RerankDTO) -> StreamingResponse:
                                                                       #该参数对浏览器说明，该接口返回的是流，需要SSE支持
        return StreamingResponse(AIChatController.chat_service.rerank(request), media_type="text/event-stream")

    @staticmethod
    @app.get("/api/ai/health")
    def health():
        return {"status": "ok"}