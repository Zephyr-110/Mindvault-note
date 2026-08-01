from typing import Generator

from main.python.mapper.llm_mapper import ChatMapper
from main.python.models.chat_request import ChatRequest
from main.python.models.chat_response import ChatResponse
from main.python.mapper.mapper_Test import AgentMapper


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

    def stream_agent_chat(self, request: ChatRequest) -> Generator[dict, None, None]:
        yield from self.agent_mapper.stream_chat_agent(request)