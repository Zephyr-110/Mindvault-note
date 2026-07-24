from pydantic import BaseModel


class RerankSourceItem(BaseModel):
    type: str # "post" / "user" / "note"
    id: str # 唯一标识，跳转详情页用
    title: str # 标题 / 昵称
    content: str # 正文 / 简介 / 笔记内容
