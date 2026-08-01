import dashscope


class VectorUtil:

    @staticmethod
    def _truncate_by_bytes(text: str, max_bytes: int) -> str:
        """按 UTF-8 字节数截断文本，确保不截断在多字节字符中间"""
        if not text:
            return ""
        encoded = text.encode('utf-8')
        if len(encoded) <= max_bytes:
            return text
        return encoded[:max_bytes].decode('utf-8', errors='ignore')

    def get_embedding(self, text: str) -> list[float]:
        truncated = str(text)[:300] if text else ""
        response = dashscope.TextEmbedding.call(
            model="text-embedding-v1",
            input=truncated
        )
        if response.status_code == 200:
            full_vector = response.output["embeddings"][0]["embedding"]
            return full_vector[:768]
        else:
            raise Exception(f"Embedding 失败 : {response.message}")