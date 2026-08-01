import os
from typing import Generator

from pymilvus import MilvusClient
import requests
import httpx
import json

# 1. 导入你写好的路由（路径根据你实际情况微调）
from main.python.models.chat_message import ChatMessage
from main.python.models.chat_request import ChatRequest
from main.python.common.vector.vector_util import VectorUtil


class AgentMapper:
    # ========== 类变量（只放常量，不放需要初始化的对象）==========

    BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    MODEL = "deepseek-v4-pro"

    SYSTEM_PROMPT = """你是一个智能笔记助手，名叫 MindVault AI。
    你可以帮助用户：
    1. 总结和整理他们的笔记
    2. 根据笔记内容回答问题
    3. 搜索和推荐相关的笔记、帖子
    4. 提供写作建议和思路

    注意：笔记只能搜索自己的笔记，帖子是都可以看的，千万不要越权搜索。

    请用中文回答，语气友好专业。如果用户的问题和他们的笔记无关，也能正常搜索或者聊天。
    """

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

    # ========== 实例初始化（需要运行时环境的东西放这里）==========

    def __init__(self):
        self.tool_map = {
            "search_notes": self.search_notes,
            "search_posts": self.search_posts
        }

        self.client = MilvusClient()
        self._init_collections()

        self.API_KEY = os.getenv("OPENAI_API_KEY")
        self.headers = {
            "Authorization": f"Bearer {self.API_KEY}",
            "Content-Type": "application/json"
        }

        self.vector_util = VectorUtil()

    def _init_collections(self):
        index_params = self.client.prepare_index_params()
        index_params.add_index(
            field_name="vector",
            index_type="AUTOINDEX",
            metric_type="COSINE"
        )
        for collection_name in ["documents", "conversation_memory"]:
            try:
                self.client.create_index(collection_name=collection_name, index_params=index_params)
            except Exception:
                pass
        try:
            self.client.load_collection(collection_name="documents")
            self.client.load_collection(collection_name="conversation_memory")
        except Exception:
            pass

    # ========== 工具执行 ==========
    def search_notes(self, query: str, user_id: str, top_k: int = 5) -> str:
        vector = self.vector_util.get_embedding(query)
        results = self.client.search(
            collection_name="documents",
            data=[vector],
            limit=top_k,
            filter=f'source_type == "note" and user_id == "{user_id}"',
            output_fields=["id", "text"]
        )
        hits = results[0]
        items = []
        for hit in hits:
            items.append({
                "id": hit["id"],
                "text": hit["entity"]["text"][:200],
                "score": hit["distance"]
            })
        return f"找到以下相关笔记：{items}"

    def search_posts(self, query: str, top_k: int = 5) -> str:
        vector = self.vector_util.get_embedding(query)
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
        return f"找到以下相关帖子：{items}"

    def search_memories(self, text: str, user_id: str, session_id: str, top_k: int = 5) -> str:
        vector = self.vector_util.get_embedding(text)
        result = self.client.search(
            collection_name="conversation_memory",
            data=[vector],
            limit=top_k,
            filter=f'user_id == "{user_id}" and session_id == "{session_id}"',
            output_fields=["text", "timestamp"]
        )
        hits = result[0]
        if not hits:
            return ""
        lines = []
        for i, hit in enumerate(hits, 1):
            text = hit["entity"]["text"]
            lines.append(f"记忆{i}: {text}")
        return "\n".join(lines)

    # 调用模型，返回ChatMessage对象
    def chat(self, messages: list[ChatMessage], tools=None) -> ChatMessage:
        data = {
            "model": self.MODEL,
            "messages": [msg.model_dump() for msg in messages],
            "temperature": 0.7
        }
        if tools:
            data["tools"] = tools
            data["tool_choice"] = "auto"
        response = requests.post(f"{self.BASE_URL}/chat/completions", headers=self.headers, json=data)
        response.raise_for_status()
        return ChatMessage(**response.json()["choices"][0]["message"])

    MAX_LOOPS = 5

    def chat_agent(self, request: ChatRequest) -> str:
        system_prompt = ChatMessage(
            role="system",
            content=self.SYSTEM_PROMPT)
        history = ""
        if request.user_id and request.session_id:
            history = self.search_memories(request.messages[-1].content, request.user_id, request.session_id, 5)
        system_prompt.content += f"\n{history}"
        messages = [system_prompt] + request.messages
        loop_count = 0
        while loop_count < self.MAX_LOOPS:
            loop_count += 1
            print(f"= = =第 {loop_count} 次循环= = =")
            response = self.chat(messages, tools=self.tools)

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
                        role="tool",
                        tool_call_id=tool_call["id"],
                        content=result
                    ))
                    print(f"工具 {func_name} 返回结果：{result}")

                continue
            else:
                final_answer = response.content
                print(f"最终回答：{final_answer}")
                return final_answer
        return "达到最大循环次数，未能完成任务。"

    def chat_stream(self, messages, tools=None) -> Generator[dict, None, None]:
        # 构建请求数据
        data = {
            "model": self.MODEL,
            "messages": [msg.model_dump() for msg in messages],
            "temperature": 0.7,
            "stream": True
        }
        if tools:
            data["tools"] = tools
            data["tool_choice"] = "auto"
        # 发送请求,拿到流式响应（用 httpx 替换 requests 以避免事件循环阻塞）
        with httpx.Client(timeout=httpx.Timeout(60.0, connect=5.0)) as client:
            with client.stream(
                "POST",
                f"{self.BASE_URL}/chat/completions",
                headers=self.headers,
                json=data
            ) as response:
                response.raise_for_status()
                print(
                    f"[chat_stream] 响应头: Transfer-Encoding={response.headers.get('Transfer-Encoding')}, Content-Type={response.headers.get('Content-Type')}")
                # 初始化消息文本
                full_content = ""
                # 初始化工具调用缓冲区
                tool_calls_buffer = {}
                # 遍历流式响应
                line_count = 0
                for line in response.iter_lines():
                    line_count += 1
                    print(f"[chat_stream] 收到第{line_count}行: {line[:120]}")
                    if not line:
                        continue
                    if isinstance(line, bytes):
                        line = line.decode("utf-8")
                    if line.startswith("data: "):
                        line = line[6:]
                    if line == "[DONE]":
                        yield {"type": "done", "full_content": full_content}
                        return
                    chunk_data = json.loads(line)
                    # 拿到delta事件中的内容
                    delta = chunk_data["choices"][0].get("delta", {})
                    # 如果有推理内容（DeepSeek思考链），返回reasoning事件
                    if delta.get("reasoning_content"):
                        print(f"[chat_stream] 推理: {delta['reasoning_content']}")
                        yield {"type": "reasoning",
                               "text": delta["reasoning_content"]}
                    # 如果内容不为空，则说明没有调用工具，直接返回
                    if delta.get("content"):
                        full_content += delta["content"]
                        print(f"[chat_stream] 内容: {delta['content']}")
                        yield {"type": "content",
                               "text": delta["content"]}
                    # 如果调用工具，需要缓冲工具调用信息
                    if delta.get("tool_calls"):
                        # 遍历工具栏
                        for tc in delta["tool_calls"]:
                            # 拿到工具调用索引
                            index = tc["index"]
                            # 如果索引不在缓冲区中，初始化一个空字典
                            if index not in tool_calls_buffer:
                                tool_calls_buffer[index] = {
                                    "id": "",
                                    "type": "function",
                                    "function": {
                                        "name": "",
                                        "arguments": ""
                                    }
                                }
                            # 如果工具调用id不为空，添加到缓冲区
                            if tc.get("id"):
                                tool_calls_buffer[index]["id"] = tc["id"]
                            # 如果工具调用函数名称不为空，添加到缓冲区
                            if tc.get("function", {}).get("name"):
                                tool_calls_buffer[index]["function"]["name"] += tc["function"]["name"]
                            # 如果工具调用参数不为空，添加到缓冲区
                            if tc.get("function", {}).get("arguments"):
                                tool_calls_buffer[index]["function"]["arguments"] += tc["function"]["arguments"]
                    # 拿到模型完成调用工具的原因
                    finish_reason = chunk_data["choices"][0].get("finish_reason")
                    # 如果模型调用工具，返回工具调用信息
                    if finish_reason == "tool_calls":
                        print(f"[chat_stream] 工具调用: {list(tool_calls_buffer.values())}")
                        yield {"type": "tool_calls",
                               "calls": list(tool_calls_buffer.values())}
                        return
                    elif finish_reason is not None:
                        print(f"[chat_stream] 完成: {finish_reason}")
                        yield {"type": "done",
                               "full_content": full_content}
                        return

    def stream_chat_agent(self, request: ChatRequest) -> Generator[dict, None, None]:
        # 初始化系统提示词
        system_prompt = ChatMessage(role="system", content=self.SYSTEM_PROMPT)
        # 初始化会话历史
        history = ""
        # 如果用户id和会话id都不为空
        if request.user_id and request.session_id:
            # 搜索会话历史
            history = self.search_memories(request.messages[-1].content, request.user_id, request.session_id, 5)
        # 系统提示此后添加会话历史
        system_prompt.content += f"\n{history}"
        # 将用户新消息插到最后
        messages = [system_prompt] + request.messages
        # 循环初始轮次数
        loop_count = 0
        while loop_count < self.MAX_LOOPS:
            loop_count += 1
            print(f"= = =第 {loop_count} 次循环= = =")
            # 发送思考中事件
            yield {"type": "thinking",
                   "content": "思考中..."}
            # 调用模型，返回流
            response = self.chat_stream(messages, tools=self.tools)
            # 遍历流，解析每个delta事件
            for chunk in response:
                # 如果是内容事件，直接返回内容
                if chunk.get("type") == "content":
                    yield chunk
                # 如果是工具调用事件，需要缓冲工具调用信息
                elif chunk.get("type") == "tool_calls":
                    # 拼接工具调用信息
                    assistant_msg = ChatMessage(
                        role="assistant",
                        content="",
                        tool_calls=chunk["calls"]
                    )
                    # 添加到消息列表
                    messages.append(assistant_msg)
                    # 遍历工具调用信息
                    for tc in chunk["calls"]:
                        # 拿到工具调用函数名称
                        func_name = tc["function"]["name"]
                        # 解析工具调用参数
                        arguments = json.loads(tc["function"]["arguments"])
                        # 如果是搜索笔记工具，添加用户id参数
                        if func_name == "search_notes":
                            arguments["user_id"] = request.user_id
                        # 返回工具调用事件
                        yield {"type": "tool_call",
                               "name": func_name,
                               "arg": arguments}
                        # 拿到工具调用结果
                        result = self.tool_map[func_name](**arguments)
                        # 返回工具调用结果事件以及内容
                        yield {"type": "tool_result",
                               "name": func_name,
                               "result": result}
                        # 添加到消息列表
                        messages.append(ChatMessage(
                            role="tool",
                            tool_call_id=tc["id"],
                            content=result
                        ))
                # 如果是完成事件，直接返回完成事件
                elif chunk.get("type") == "done":
                    yield chunk
                    return
                # 其他事件，直接返回
                else:
                    yield chunk