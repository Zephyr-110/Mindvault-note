import os
import sys
from pathlib import Path

# ========== 🔍 关键：先手动加载 .env ==========
try:
    from dotenv import load_dotenv

    env_path = Path(__file__).parent.parent.parent / ".env"  # 根据你项目结构调整路径
    print(f"📂 尝试加载 .env 文件: {env_path}")
    print(f"📂 .env 文件是否存在: {env_path.exists()}")
    if env_path.exists():
        with open(env_path, "r", encoding="utf-8") as f:
            print("=" * 60)
            print("📄 .env 文件原始内容（前 500 字符）:")
            content = f.read()
            print(content[:500])
            print("=" * 60)
        load_dotenv(env_path, override=True)
        print("✅ .env 文件加载成功")
    else:
        print("❌ .env 文件不存在！")
except Exception as e:
    print(f"❌ 加载 .env 失败: {e}")

# ========== 🔍 检查环境变量 ==========
print("\n" + "=" * 60)
print("🔍 环境变量 OPENAI_API_KEY:")
raw = os.getenv("OPENAI_API_KEY")
print(f"   repr: {repr(raw)}")
print(f"   类型: {type(raw)}")
if raw:
    print(f"   长度: {len(raw)}")
    non_ascii = [(i, c, hex(ord(c))) for i, c in enumerate(raw) if ord(c) > 127]
    if non_ascii:
        print(f"   ⚠️ 发现非 ASCII 字符: {non_ascii}")
    else:
        print(f"   ✅ 全部为 ASCII 字符")
print("=" * 60)

# ========== 🔍 实例化 AgentMapper，触发诊断 ==========
print("\n🚀 开始实例化 AgentMapper...")
try:
    from main.python.mapper.agent_mapper import AgentMapper

    mapper = AgentMapper()
    print("✅ AgentMapper 实例化成功！")

    # 测试发送一个最简单的请求
    from main.python.models.chat_message import ChatMessage
    from main.python.models.chat_request import ChatRequest

    print("\n🚀 发送测试流式请求...")
    req = ChatRequest(
        messages=[ChatMessage(role="user", content="你好")],
        user_id="test_user",
        session_id="test_session"
    )

    count = 0
    for chunk in mapper.stream_chat_agent(req):
        count += 1
        print(f"📦 收到第 {count} 个 chunk: {chunk}")
        if count > 10:
            print("... 只显示前 10 个 chunk")
            break
    print(f"✅ 流式请求完成，共收到 {count} 个 chunk")

except Exception as e:
    import traceback

    print(f"❌ 出错了: {type(e).__name__}: {e}")
    traceback.print_exc()