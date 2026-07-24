from typing import List, Optional
from pydantic import BaseModel, Field
from main.python.models.rerank_source_item import RerankSourceItem

class RerankDTO(BaseModel):
    query: str # 用户搜的关键词
    sources: List[RerankSourceItem] # 待排序的文档列表
    top_k: Optional[int] = Field(default=5, alias="topK") # 返回前几条
    user_id: Optional[str] = Field(default=None, alias="userId") # 谁在搜（可选）