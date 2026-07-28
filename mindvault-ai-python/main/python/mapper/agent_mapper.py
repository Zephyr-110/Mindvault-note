import os

from pymilvus import MilvusClient
import requests
import json

import dashscope

from main.python.models.chat_message import ChatMessage
from main.python.models.chat_request import ChatRequest
from main.python.models.to_embedding_dto import ToEmbeddingDTO


class AgentMapper:

    def __init__(self):
        # 工具映射
        self.tool_map = {
            "search_notes": self.search_notes,
            "search_posts": self.search_posts
        }

    # 初始化Milvus客户端
    client = MilvusClient()
    # 声明索引
    index_params = client.prepare_index_params()
    index_params.add_index(
        field_name = "vector",
        index_name = "AUTOINDEX",
        metric_type = "COSINE"
    )
    # 创建索引
    client.create_index(collection_name="documents", index_params=index_params)
    # 加载集合到内存
    client.load_collection(collection_name="documents")
    # 初始化LLM各项参数
    API_KEY = os.getenv("OPENAI_API_KEY")
    BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    MODEL = "deepseek-v4-pro"
    # 初始化请求头
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }
    # 定义工具
    tools = [
        {
            "type": "function",
            "function": {
                "name": "search_notes",
                "description": "搜索用户的笔记，返回相关笔记的标题和内容片段",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "query": {
                            "type": "string",
                            "description": "用于搜索的关键词"
                        },
                        "top_k": {
                            "type": "integer",
                            "description": "返回的相关笔记数量",
                            "default": 5
                        }
                    }
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "search_posts",
                "description": "搜索用户的帖子，返回相关帖子的标题和内容片段",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "query": {
                            "type": "string",
                            "description": "用于搜索的关键词"
                        },
                        "top_k": {
                            "type": "integer",
                            "description": "返回的相关帖子数量",
                            "default": 5
                        }
                    }
                }
            }
        }
    ]
    # 初始化系统提示词
    SYSTEM_PROMPT = """你是一个智能笔记助手，名叫 MindVault AI。
    你可以帮助用户：
    1. 总结和整理他们的笔记
    2. 根据笔记内容回答问题
    3. 搜索和推荐相关的笔记、帖子
    4. 提供写作建议和思路
    
    注意：笔记只能搜索自己的笔记，帖子是都可以看的，千万不要越权搜索。

    请用中文回答，语气友好专业。如果用户的问题和他们的笔记无关，也能正常搜索或者聊天。
    """


    # 得到文本的向量表示
    def get_embedding(self, text: str) -> list[float]:
        # 截断文本，避免超出模型最大输入长度
        truncated = str(text)[:300] if text else ""
        # 获取向量
        response = dashscope.TextEmbedding.call(
            model = "text-embedding-v1",
            input = truncated
        )
        # 检查响应状态码
        if response.status_code == 200:
            # 提取向量并返回
            full_vector = response.output["embeddings"][0]["embedding"]
            return full_vector
        else:
            # 抛出异常
            raise Exception(f"Embedding 失败 : {response.message}")


    # ========== 工具执行 ==========
    def search_notes(self, query: str, user_id: str, top_k: int = 5) -> str:
        # 获取关键词向量
        vector = self.get_embedding(query)
        # 搜索相关笔记
        results = self.client.search(
            collection_name = "documents",
            data = [vector],
            limit = top_k,
            filter = f'source_type == "note" and user_id == {user_id}',
            output_fields = ["id", "text"]
        )
        hits = results[0]
        items = []
        for hit in hits:
            items.append({
                "id": hit["id"],
                "text": hit["entity"]["text"][:200],
                "score": hit["distance"]
            })
        # 返回结果
        return f"找到以下相关笔记：{items}"

    def search_posts(self, query: str, top_k: int = 5) -> str:
        # 获取关键词向量
        vector = self.get_embedding(query)
        # 搜索相关帖子
        result = self.client.search(
            collection_name="documents",
            data=[vector],
            limit=top_k,
            filter='source_type == "post"',
            output_fields=["id", "text"]
        )
        hits = result[0]
        items = []
        for hit in hits:
            items.append({
                "id": hit["id"],
                "text": hit["entity"]["text"][:200],
                "score": hit["distance"]
            })
        # 返回结果
        return f"找到以下相关帖子：{items}"

    # 调用模型，返回ChatMessage对象
    def chat(self, messages: list[ChatMessage], tools=None) -> ChatMessage:
        # 构建请求数据
        data = {
            "model": self.MODEL,
            "messages": [msg.model_dump() for msg in messages],
            "temperature": 0.7
        }
        # 如果提供了工具，添加到请求数据中
        if tools:
            data["tools"] = tools
            data["tool_choice"] = "auto"
        # 发送请求
        response = requests.post(f"{self.BASE_URL}/chat/completions", headers=self.headers, json=data)
        # 检查是否成功返回响应
        response.raise_for_status()
        return ChatMessage(**response.json()["choices"][0]["message"])

    MAX_LOOPS = 5

    def chat_agent(self, request: ChatRequest ) -> str:
        # 初始化系统提示词
        system_prompt = ChatMessage(role="system", content=self.SYSTEM_PROMPT)
        # 系统提示词插到消息列表开头
        messages = [system_prompt] + request.messages
        loop_count = 0
        while loop_count < self.MAX_LOOPS:
            loop_count += 1
            print(f"= = =第 {loop_count} 次循环= = =")
            response = self.chat(messages, tools = self.tools)

            if response.tool_calls:
                tool_calls = response.tool_calls
                messages.append(response)

                for tool_call in tool_calls:
                    func_name = tool_call["function"]["name"]
                    arguments = json.loads(tool_call["function"]["arguments"])
                    print(f"调用工具 {func_name}，参数：{arguments}")
                    if func_name == "search_notes":
                        arguments["user_id"] = request.user_id
                    result = self.tool_map[func_name](**arguments)

                    messages.append(ChatMessage(
                        role = "tool",
                        tool_call_id = tool_call["id"],
                        content = result
                    ))
                    print(f"工具 {func_name} 返回结果：{result}")

                continue
            else:
                final_answer = response.content
                print(f"最终回答：{final_answer}")
                return final_answer
        return "达到最大循环次数，未能完成任务。"


    def set_embedding(self, dto: ToEmbeddingDTO):
        text = f"{dto.title}\n{dto.content}"
        vector = self.get_embedding(text)
        data = {
            "id": f"note_{dto.id}",  # 主键，前缀区分来源
            "vector": vector,  # 向量
            "user_id": str(dto.user_id),  # 用户 ID
            "source_type": dto.source_type,  # 数据类型
            "text": text,  # 原文（用于展示）
            "created_at": dto.created_at  # 时间戳
        }
        self.client.insert(
            collection_name = "documents",
            data = [data],
        )
        print(f"成功插入资源 {dto.source_type}: id = {dto.id}")
