# ============================================
# Python AI 微服务 — FastAPI + Ollama + Milvus
# ============================================
FROM python:3.12-slim

WORKDIR /app

# 安装系统依赖（编译 sentence-transformers 等需要）
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# 先复制依赖文件，利用 Docker 缓存层
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制源代码
COPY main/ main/

# 暴露端口
EXPOSE 8000

# 启动 FastAPI
CMD ["uvicorn", "main.python.app:app", "--host", "0.0.0.0", "--port", "8000"]
