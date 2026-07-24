from typing import Optional

from pydantic import BaseModel


class ChatResponse(BaseModel):
    content: str
    done: bool = False
    error: Optional[str] = None