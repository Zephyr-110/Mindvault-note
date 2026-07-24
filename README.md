# 🧠 MindVault — 智能笔记管理平台

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-4fc08d.svg)](https://vuejs.org/)
[![Python](https://img.shields.io/badge/Python-3.12+-blue.svg)](https://www.python.org/)

MindVault 是一个集**笔记管理、知识图谱、社区互动、AI 智能助手**于一体的全栈知识管理平台。

---

## ✨ 核心功能

### 📝 笔记管理
- **Markdown 编辑器** — 所见即所得编辑，支持图片粘贴上传
- **分类目录** — 无限层级目录树，拖拽排序
- **标签系统** — 灵活的多维度标签管理
- **知识图谱 3D** — 基于 Three.js 的力导向图，可视化笔记关联
- **思维导图** — 目录结构一键转思维导图
- **回收站** — 删除保留，支持恢复与彻底删除

### 👥 社区互动
- **帖子发布** — Markdown 格式，支持公开/仅好友可见
- **评论 & 回复** — 多层嵌套评论，楼中楼回复
- **点赞 & 收藏** — 点赞帖子/评论，收藏优质内容
- **关注系统** — 关注/取关，基于 Redis Bitmap 的高性能批量查询
- **私信聊天** — 用户间实时私信，WebSocket 推送
- **通知中心** — 点赞、评论、关注、私信实时通知，未读角标

### 🤖 AI 智能助手
- **智能问答** — RAG 检索增强，用你的笔记和社区内容回答问题
- **意图识别** — 自动判断是否需要检索知识库
- **流式对话 (SSE)** — 打字机效果的实时响应
- **Rerank 重排序** — 语义重排序提升回答质量
- **会话管理** — 多轮对话上下文保持，历史回溯

### 🔐 用户系统
- **注册/登录** — JWT 无状态认证，自动续期
- **个人设置** — 昵称、头像、主题、隐私偏好
- **黑名单** — 屏蔽骚扰用户
- **接口限流** — 基于注解的 RateLimit，防止接口滥用

---

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────┐
│                  前端 (Vue 3 + Vite)                  │
│       Element Plus / ECharts / Three.js              │
│         Markdown-it / Axios / WebSocket              │
└──────────────────────┬──────────────────────────────┘
                       │  HTTP / SSE / WebSocket
┌──────────────────────▼──────────────────────────────┐
│              mindvault-app (Spring Boot 4)           │
│  ┌──────────┬──────────┬──────────┬──────────────┐  │
│  │  user    │   note   │community │     ai       │  │
│  │ service  │ service  │ service  │   service    │  │
│  └──────────┴──────────┴──────────┴──────┬───────┘  │
│                  ┌───────────────────────▼────────┐  │
│                  │      mindvault-common          │  │
│                  │  JWT / RateLimit / OSS / ...   │  │
│                  └───────────────────────────────┘  │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│                   数据 & 基础设施                      │
│      MySQL 8       Redis       Aliyun OSS            │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│            Python AI 微服务 (FastAPI)                 │
│    Ollama (qwen2.5)  +  sentence-transformers       │
│             向量检索 / RAG / Rerank                   │
└─────────────────────────────────────────────────────┘
```

| 层次 | 技术 |
|------|------|
| 后端框架 | Spring Boot 4.0.6 / Java 25 |
| ORM | MyBatis-Plus 3.5.16 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 对象存储 | Aliyun OSS |
| 认证 | JJWT 0.12.6 |
| API 文档 | SpringDoc OpenAPI 3.0 |
| 前端框架 | Vue 3 + Vite |
| UI 库 | Element Plus |
| 可视化 | Three.js / ECharts |
| AI 框架 | FastAPI + Ollama + OpenAI SDK |
| 向量模型 | BAAI/bge-m3 |
| LLM | Qwen2.5 (本地部署) |

---

## 📦 项目结构

```
MindVault/
├── mindvault-common/            # 公共模块：JWT、限流、异常处理、OSS 配置
├── mindvault-user-api/          # 用户模块接口定义 (DTO/VO/Service)
├── mindvault-user-service/      # 用户模块实现：注册登录、设置、黑名单
├── mindvault-note-api/          # 笔记模块接口定义
├── mindvault-note-service/      # 笔记模块实现：文档、分类、标签、知识图谱
├── mindvault-community-api/     # 社区模块接口定义
├── mindvault-community-service/ # 社区模块实现：帖子、评论、关注、私信、通知
├── mindvault-ai-api/            # AI 模块接口定义
├── mindvault-ai-service/        # AI 模块实现：RAG、意图识别、SSE 流式响应
├── mindvault-ai-python/         # Python AI 微服务：FastAPI + Ollama
├── mindvault-app/               # 主启动模块，聚合所有子模块
├── frontend/                    # Vue 3 前端
└── pom.xml                      # Maven 父 POM
```

---

## 🚀 快速开始

### 环境要求

- **JDK** 25+
- **Maven** 3.9+
- **MySQL** 8.0+
- **Redis** 7.0+
- **Node.js** 18+ (前端)
- **Python** 3.12+ (AI 服务)
- **Ollama** (可选，本地 LLM)

### 1. 克隆项目

```bash
git clone https://github.com/your-username/MindVault.git
cd MindVault
```

### 2. 准备数据库

```sql
CREATE DATABASE IF NOT EXISTS mindvault DEFAULT CHARACTER SET utf8mb4;
```

### 3. 配置环境变量

```bash
# 数据库
export DB_PASSWORD=your_password
export REDIS_PASSWORD=your_redis_password

# 阿里云 OSS（可选，不配置则使用默认 bucket）
export OSS_ACCESS_KEY=your_ak
export OSS_ACCESS_SECRET=your_sk
export OSS_BUCKET=mindvault-files

# JWT
export JWT_SECRET=your_jwt_secret_key
```

### 4. 启动后端

```bash
cd mindvault-app
mvn spring-boot:run
# 启动在 http://localhost:8082
# Swagger 文档: http://localhost:8082/swagger-ui.html
```

### 5. 启动 Python AI 服务（可选）

```bash
cd mindvault-ai-python
pip install -r requirements.txt
python -m main.python.main
# 启动在 http://localhost:8000
```

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
# 启动在 http://localhost:5173
```

---

## 🔧 开发指南

### API 模块分离设计

项目采用 **API/Service 分离** 的模块设计：

- `*-api` 模块：定义接口契约（DTO、VO、Service 接口），不依赖数据库
- `*-service` 模块：实现业务逻辑，依赖对应 api 模块和 common 模块

这使得模块间依赖清晰，也方便未来拆分为微服务。

### 接口限流

```java
@RateLimit(key = "register", limit = 3, window = 300)
@PostMapping("/user/register")
public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
    // 5 分钟内最多 3 次注册
}
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

---

## 📝 后续计划

- [ ] 笔记协作编辑 (WebSocket 实时同步)
- [ ] 笔记版本历史 & Diff 对比
- [ ] AI 辅助写作 / 自动摘要 / 标签推荐
- [ ] 移动端适配 (PWA)
- [ ] Docker 一键部署
- [ ] 全文检索 (Elasticsearch)

---

## 📄 License

MIT License

---