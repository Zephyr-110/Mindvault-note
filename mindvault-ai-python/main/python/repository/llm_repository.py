from typing import Generator, Optional, List

from openai import OpenAI

from main.python.config.settings import settings


class LLMRepository:
    def __init__(self):
        self.client = OpenAI(
            api_key=settings.LLM_API_KEY,
            base_url=settings.LLM_BASE_URL,
        )

    def stream_chat(self, messages: List[dict]) -> Generator[str, None, None]:
        try:
            response = self.client.chat.completions.create(
                model=settings.LLM_MODEL,
                messages=messages,
                stream=True,
                temperature=settings.LLM_TEMPERATURE,
                max_tokens=settings.LLM_MAX_TOKENS,
            )
            for chunk in response:
                if chunk.choices and chunk.choices[0].delta.content:
                    yield chunk.choices[0].delta.content
            yield None
        except Exception as e:
            print(f"[LLMRepository] stream_chat 异常: {e}")
            yield f"ERROR: {str(e)}"

    def chat(self, messages: List[dict]) -> Optional[str]:
        try:
            response = self.client.chat.completions.create(
                model=settings.LLM_MODEL,
                messages=messages,
                temperature=settings.LLM_TEMPERATURE,
                max_tokens=settings.LLM_MAX_TOKENS,
            )
            return response.choices[0].message.content
        except Exception as e:
            print(f"[LLMRepository] chat 异常: {e}")
            return f"ERROR: {str(e)}"


llm_repository = LLMRepository()