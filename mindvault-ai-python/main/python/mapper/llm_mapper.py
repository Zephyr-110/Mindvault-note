import json
from typing import Generator, List
from sentence_transformers import SentenceTransformer, util

from main.python.models.chat_request import ChatRequest
from main.python.models.chat_response import ChatResponse
from main.python.repository.llm_repository import llm_repository
from main.python.models.rerank_dto import RerankDTO
from main.python.models.chat_message import ChatMessage



SYSTEM_PROMPT = """你是一个智能笔记助手，名叫 MindVault AI。
你可以帮助用户：
1. 总结和整理他们的笔记
2. 根据笔记内容回答问题
3. 搜索和推荐相关的笔记、帖子、用户
4. 提供写作建议和思路

请用中文回答，语气友好专业。如果用户的问题和他们的笔记无关，也能正常搜索。"""



class ChatMapper:


    model = SentenceTransformer("shibing624/text2vec-base-chinese", local_files_only=True)

    """
    负责将用户请求转换为LLM的messages字段输入格式
    """

    def build_messages(self, request: ChatRequest, system_prompt: str) -> list[dict]:
        #创建一个新列表，用于存储消息
        messages = []
        #如果第一条是系统消息并且消息列表不为空，则将系统消息添加到列表中
        if request.messages and request.messages[0].role == "system":
            context = request.messages[0].content
            messages.append(
                {
                    "role": "system",
                    "content": system_prompt + "\n\n## 用户前文摘要：\n" + context
                }
            )
            # 从第二条消息开始，将消息赋值给history变量
            history = request.messages[1:]
        #如果没有系统消息，或者消息列表为空，则将系统消息添加到列表中
        else:
            messages.append({"role": "system", "content": system_prompt})
            # 直接将参数中的消息列表赋值给history变量
            history = request.messages
        # 遍历history变量，将每条消息添加到messages列表中
        for msg in history:
            messages.append({"role": msg.role, "content": msg.content})
        #返回的messages列表，就是调用LLM的接口参数中的messages字段，用于生成响应
        return messages

    def intention_check_build_messages(self, request: ChatRequest) -> list[dict]:
        #创建一个新列表，用于存储消息
        messages = []
        #如果第一条是系统消息并且消息列表不为空，则将系统消息添加到列表中
        if request.messages and request.messages[0].role == "system":
            context = request.messages[0].content
            messages.append(
                {
                    "role": "system",
                    "content": context
                }
            )
            # 从第二条消息开始，将消息赋值给history变量
            history = request.messages[1:]
        #如果没有系统消息，或者消息列表为空，则将系统消息添加到列表中
        else:
            messages.append({"role": "system", "content": SYSTEM_PROMPT})
            # 直接将参数中的消息列表赋值给history变量
            history = request.messages
        # 遍历history变量，将每条消息添加到messages列表中
        for msg in history:
            messages.append({"role": msg.role, "content": msg.content})
        #返回的messages列表，就是调用LLM的接口参数中的messages字段，用于生成响应
        return messages


    def chat(self, request: ChatRequest) -> ChatResponse :
        # 调用build_messages方法，将用户请求转换为LLM的messages字段输入格式
        messages = self.intention_check_build_messages(request)
        # 调用LLM的chat方法，拼接响应，拿到返回结果
        result = llm_repository.chat(messages)
        # 判断返回结果，如果为None，则返回错误信息
        if result is None:
            return ChatResponse(error="LLM调用失败")
        # 如果返回结果以ERROR:开头，则返回错误信息
        if result.startswith("ERROR:"):
            return ChatResponse(error=result[6:])
        # 否则，返回正常结果
        return ChatResponse(content=result)


    def stream_chat(self, request: ChatRequest) -> Generator[str, None, None]:
        # 调用build_messages方法，将用户请求转换为LLM的messages字段输入格式
        messages = self.build_messages(request, SYSTEM_PROMPT)
        # 调用LLM的stream_chat方法，返回一个生成器，用于逐个返回结果
        for chunk in llm_repository.stream_chat(messages):
            # 如果chunk为None，则返回一个结束消息，并结束生成器，即结束标志
            if chunk is None:
                yield f"data: {json.dumps({'done': True})}\n\n"
                break
            # 如果chunk以ERROR:开头，则返回一个错误消息，并结束生成器，即错误标志
            if chunk.startswith("ERROR:"):
                yield f"data: {json.dumps({'error': chunk[6:]})}\n\n"
                break
            # 否则，返回一个正常消息
            yield f"data: {json.dumps({'content': chunk})}\n\n"





    def rerank(self, request: RerankDTO) -> Generator[str, None, None]:
        # 创建一个空列表，用于存储资源
        resources = []
        for item in request.sources:
            # 根据资源类型，生成对应的描述
            if item.type == "post":
                doc = f"[帖子] {item.title}: {item.content}"
            elif item.type == "note":
                doc = f"[笔记] {item.title}: {item.content}"
            else:
                doc = item.content
            # 将描述添加到资源列表中
            resources.append(doc)
        # 把query向量化
        query_embedding = self.model.encode(request.query)
        # 把资源列表向量化
        doc_embeddings = self.model.encode(resources)
        # 计算相似度，获取相似度分数
        scores = util.cos_sim(query_embedding, doc_embeddings)[0]
        # 创建一个空列表，用于存储排序后的资源
        ranked = []
        for i, score in enumerate(scores):
            # 拿到对应索引的资源
            item = request.sources[i]
            # 将相似度和资源绑定，然后添加到列表中
            ranked.append((score.item(), item))
        # 对资源进行排序，降序
        ranked.sort(key=lambda x: x[0], reverse=True)
        top_k = request.top_k
        # 只取前top_k个资源
        ranked = ranked[:top_k]
        # 创建一个系统提示，用于告诉LLM
        search_prompt = SYSTEM_PROMPT + "\n\n## 搜索结果："
        for item in ranked:
            search_prompt += f"\n\n### 资源 {item[1].type} {item[1].id} {item[1].title}\n{item[1].content}\n"
        # 创建两个消息，用于告诉LLM
        message1 = ChatMessage(role="system", content=search_prompt)
        message2 = ChatMessage(role="user", content=request.query)
        messages = [message1.model_dump(), message2.model_dump()]
        # 调用LLM的stream_chat方法，返回一个生成器，用于逐个返回结果
        for chunk in llm_repository.stream_chat(messages):
            # 如果chunk为None，则返回一个结束消息，并结束生成器，即结束标志
            if chunk is None:
                yield f"data: {json.dumps({'done': True})}\n\n"
                break
            # 如果chunk以ERROR:开头，则返回一个错误消息，并结束生成器，即错误标志
            if chunk.startswith("ERROR:"):
                yield f"data: {json.dumps({'error': chunk[6:]})}\n\n"
                break
            # 否则，返回一个正常消息
            yield f"data: {json.dumps({'content': chunk})}\n\n"