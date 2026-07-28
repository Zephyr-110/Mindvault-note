from typing import Generator

from main.python.mapper import agent_mapper
from main.python.models.chat_message import ChatMessage
from main.python.models.rerank_dto import RerankDTO
from main.python.mapper.llm_mapper import ChatMapper
from main.python.models.chat_request import ChatRequest
from main.python.models.chat_response import ChatResponse
from main.python.mapper.agent_mapper import AgentMapper
from main.python.models.to_embedding_dto import ToEmbeddingDTO




class ChatService:


    def __init__(self):
        self.llm_mapper = ChatMapper()
        self.agent_mapper = AgentMapper()

    def chat(self, request: ChatRequest) -> ChatResponse:
        return self.llm_mapper.chat(request)

    def stream_chat(self, request: ChatRequest) -> Generator[str, None, None]:
        for chunk in self.llm_mapper.stream_chat(request):
            yield chunk

    def agent_chat(self, request: ChatRequest) -> ChatResponse:
        response = self.agent_mapper.chat_agent(request)
        return ChatResponse(content=response)

    def set_embedding(self, dto: ToEmbeddingDTO):
        agent_mapper.set_embedding(dto)



    def rerank(self, request: RerankDTO) -> Generator[str, None, None]:
        for chunk in self.llm_mapper.rerank(request):
            yield chunk

