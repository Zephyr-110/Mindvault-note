from typing import Generator


from main.python.models.rerank_dto import RerankDTO
from main.python.mapper.llm_mapper import ChatMapper
from main.python.models.chat_request import ChatRequest
from main.python.models.chat_response import ChatResponse




class ChatService:

    def __init__(self):
        self.llm_mapper = ChatMapper()

    def chat(self, request: ChatRequest) -> ChatResponse:
        return self.llm_mapper.chat(request)

    def stream_chat(self, request: ChatRequest) -> Generator[str, None, None]:
        for chunk in self.llm_mapper.stream_chat(request):
            yield chunk

    def rerank(self, request: RerankDTO) -> Generator[str, None, None]:
        for chunk in self.llm_mapper.rerank(request):
            yield chunk

