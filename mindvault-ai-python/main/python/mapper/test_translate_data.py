import os
import pymysql
from pymilvus import MilvusClient, CollectionSchema, FieldSchema, DataType
import dashscope
from datetime import datetime

# ========== 配置 ==========
MYSQL_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "mindvault",
    "charset": "utf8mb4"
}

COLLECTION_NAME = "documents"

dashscope.api_key = os.getenv("DASHSCOPE_API_KEY")
EMBED_MODEL = "text-embedding-v1"
VECTOR_DIM = 768

# ========== 1. 连接 MySQL ==========
conn = pymysql.connect(**MYSQL_CONFIG)
cursor = conn.cursor()

# ========== 2. 创建 Milvus Collection（自定义 Schema）==========
client = MilvusClient(uri="http://localhost:19530")

if COLLECTION_NAME in client.list_collections():
    client.drop_collection(COLLECTION_NAME)

fields = [
    FieldSchema(name="id", dtype=DataType.VARCHAR, is_primary=True, max_length=50),
    FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=VECTOR_DIM),
    FieldSchema(name="user_id", dtype=DataType.VARCHAR, max_length=20),
    FieldSchema(name="source_type", dtype=DataType.VARCHAR, max_length=10),
    FieldSchema(name="text", dtype=DataType.VARCHAR, max_length=2000),  # 调大长度
    FieldSchema(name="created_at", dtype=DataType.INT64),
]
schema = CollectionSchema(fields, description="笔记和帖子向量库")
client.create_collection(collection_name=COLLECTION_NAME, schema=schema)
print(f"集合 {COLLECTION_NAME} 创建成功")

# ========== 3. 嵌入函数 ==========
def get_embedding(text):
    truncated = str(text)[:300] if text else ""
    resp = dashscope.TextEmbedding.call(
        model=EMBED_MODEL,
        input=truncated
    )
    if resp.status_code == 200:
        full_vector = resp.output["embeddings"][0]["embedding"]
        return full_vector[:VECTOR_DIM]
    else:
        raise Exception(f"Embedding 失败: {resp.message}")

# ========== 4. 同步笔记 ==========
def sync_documents():
    cursor.execute(
        "SELECT id, user_id, title, content, create_time "
        "FROM document "
        "WHERE is_deleted = 0"
    )
    docs = cursor.fetchall()
    print(f"共找到 {len(docs)} 条笔记")

    data = []
    for doc_id, user_id, title, content, create_time in docs:
        text = f"{title}: {content or ''}"
        try:
            vector = get_embedding(text)
            data.append({
                "id": f"note_{doc_id}",
                "vector": vector,
                "user_id": str(user_id),
                "source_type": "note",
                "text": (str(text)[:500] if text else ""),
                "created_at": int(create_time.timestamp()) if create_time else int(datetime.now().timestamp())
            })
            print(f"  ✓ 笔记 {doc_id}: {title}")
        except Exception as e:
            print(f"  ✗ 笔记 {doc_id} 处理失败: {e}")

    if data:
        client.insert(collection_name=COLLECTION_NAME, data=data)
        print(f"成功写入 {len(data)} 条笔记向量")
    return len(data)

# ========== 5. 同步帖子 ==========
def sync_posts():
    cursor.execute(
        "SELECT id, author_id, title, content, create_time "
        "FROM post "
        "WHERE visibility = 0"
    )
    posts = cursor.fetchall()
    print(f"共找到 {len(posts)} 条帖子")

    data = []
    for post_id, author_id, title, content, create_time in posts:
        text = f"{title}: {content or ''}"
        try:
            vector = get_embedding(text)
            data.append({
                "id": f"post_{post_id}",
                "vector": vector,
                "user_id": str(author_id),
                "source_type": "post",
                "text": (str(text)[:500] if text else ""),
                "created_at": int(create_time.timestamp()) if create_time else int(datetime.now().timestamp())
            })
            print(f"  ✓ 帖子 {post_id}: {title}")
        except Exception as e:
            print(f"  ✗ 帖子 {post_id} 处理失败: {e}")

    if data:
        client.insert(collection_name=COLLECTION_NAME, data=data)
        print(f"成功写入 {len(data)} 条帖子向量")
    return len(data)

# ========== 6. 执行同步 ==========
try:
    note_count = sync_documents()
    post_count = sync_posts()
    print(f"\n全量同步完成！笔记 {note_count} 条，帖子 {post_count} 条")

    print("正在建立索引...")
    client.create_index(
        collection_name=COLLECTION_NAME,
        field_name="vector",
        index_type="IVF_FLAT",
        metric_type="COSINE",
        params={"nlist": 128}
    )
    print("索引建立完成")
finally:
    cursor.close()
    conn.close()
    print("数据库连接已关闭")