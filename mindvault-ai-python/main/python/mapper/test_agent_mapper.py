import os

from pymilvus import MilvusClient
import requests
import json

import dashscope


client = MilvusClient()

# 创建索引（Milvus 要求先建索引才能 load 和 search）
index_params = client.prepare_index_params()
index_params.add_index(
    field_name="vector",
    index_type="AUTOINDEX",
    metric_type="COSINE"
)
client.create_index(collection_name="documents", index_params=index_params)

client.load_collection("documents")

# ========== 1. 配置 ==========
API_KEY = os.getenv("OPENAI_API_KEY")
BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"  # 百炼兼容模式
MODEL = "deepseek-v4-pro"  # 或 qwen-max

headers = {
    "Authorization": f"Bearer {API_KEY}",
    "Content-Type": "application/json"
}

# ========== 2. 工具定义（OpenAI function calling 格式）==========
tools = [
    {
        "type": "function",
        "function": {
            "name": "search_notes",
            "description": "搜索用户的笔记，返回相关笔记的标题和内容片段",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "搜索关键词"},
                    "top_k": {"type": "integer", "description": "返回数量，默认3"}
                },
                "required": ["query"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "获取指定城市的天气",
            "parameters": {
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市名"}
                },
                "required": ["city"]
            }
        }
    }
]

def get_embedding(text):
    truncated = str(text)[:300] if text else ""
    resp = dashscope.TextEmbedding.call(
        model="text-embedding-v1",
        input=truncated
    )
    if resp.status_code == 200:
        full_vector = resp.output["embeddings"][0]["embedding"]
        return full_vector[:768]
    else:
        raise Exception(f"Embedding 失败: {resp.message}")

# ========== 3. 工具执行函数（模拟）==========
def search_notes(query, user_id, top_k=5):
    vector = get_embedding(query)
    result = client.search(
        collection_name="documents",
        data = [vector],
        limit = top_k,
        filter = f'source_type == "note" and user_id == {user_id}',
        output_fields = ["id", "text"]
    )
    hits = result[0]
    items = []
    for hit in hits:
        items.append({
            "id": hit["id"],
            "text": hit["entity"]["text"][:200],
            "score": hit["distance"]
        })
    return f"找到以下笔记：{items}"

def search_posts(query, top_k=5):
    vector = get_embedding(query)
    result = client.search(
        collection_name="posts",
        data = [vector],
        limit = top_k,
        filter = 'source_type == "post"',
        output_fields = ["id", "text"]
    )
    hits = result[0]
    items = []
    for hit in hits:
        items.append({
            "id": hit["id"],
            "text": hit["entity"]["text"][:200],
            "score": hit["distance"]
        })
    return f"找到以下帖子：{items}"

# 工具名 -> 函数映射
tool_map = {
    "search_notes": search_notes,
    "search_posts": search_posts
}

# ========== 4. 调用 LLM（非流式，用于演示）==========
def chat_completion(messages, tools=None):
    """发送请求到 LLM，返回完整响应"""
    data = {
        "model": MODEL,
        "messages": messages,
        "temperature": 0.7
    }
    if tools:
        data["tools"] = tools
        data["tool_choice"] = "auto"  # 让模型自己决定是否调用工具

    resp = requests.post(f"{BASE_URL}/chat/completions", headers=headers, json=data)
    resp.raise_for_status()
    return resp.json()["choices"][0]["message"]

# ========== 5. ReAct 循环 ==========
MAX_LOOPS = 5  # 防止死循环

def run_agent(user_message):
    # 系统提示词
    system_prompt = {
        "role": "system",
        "content": "你是一个智能助手，可以使用工具查找信息。如果需要搜索笔记或查询天气，请调用对应工具。"
    }
    messages = [system_prompt, {"role": "user", "content": user_message}]

    loop_count = 0
    while loop_count < MAX_LOOPS:
        loop_count += 1
        print(f"--- 第{loop_count}轮 ---")
        response = chat_completion(messages, tools=tools)

        # 判断 LLM 是否要调用工具
        if response.get("tool_calls"):
            # 工具调用列表
            tool_calls = response["tool_calls"]
            # 先把 LLM 的回复加入历史（包含 tool_calls）
            messages.append(response)

            # 执行每一个工具调用，并把结果以 "tool" 角色加入消息
            for tool_call in tool_calls:
                func_name = tool_call["function"]["name"]
                arguments = json.loads(tool_call["function"]["arguments"])
                print(f"调用工具: {func_name}({arguments})")

                # 执行工具函数
                result = tool_map[func_name](**arguments)

                # 工具返回消息
                messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call["id"],
                    "content": result
                })
                print(f"工具结果: {result}")

            # 继续循环，让 LLM 看结果再决定下一步
            continue
        else:
            # 没有工具调用，直接输出内容
            final_answer = response.get("content", "")
            print(f"最终回答: {final_answer}")
            return final_answer

    return "达到最大循环次数，未能完成任务。"

# ========== 6. 测试 ==========
if __name__ == "__main__":
    user_input = "帮我总结一下redis相关的笔记，我时间长不用了，有点忘了，谢谢你啦！"
    run_agent(user_input)