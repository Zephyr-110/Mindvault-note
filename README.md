
采用 **API / Service 分离** 的多模块 Maven 架构，接口与实现解耦，依赖方向可控。

---

## 🚀 快速开始

### 环境要求

- JDK 25+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.9+
- Node.js 20+

### 1. 克隆项目

```bash
git clone https://github.com/Zephyr-110/Mindvault-note.git
cd Mindvault-note
```

### 2. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS mindvault DEFAULT CHARSET utf8mb4;
```

项目启动后 MyBatis-Plus 会自动建表。

### 3. 配置密钥

创建 `mindvault-app/src/main/resources/application-local.yaml`：

```yaml
spring:
  datasource:
    password: 你的数据库密码
  data:
    redis:
      password: 你的Redis密码

aliyun:
  oss:
    access-key-id: 你的AccessKey
    access-key-secret: 你的Secret

jwt:
  secret: 你的JWT密钥
```

### 4. 启动后端

```bash
cd mindvault-app
mvn spring-boot:run
```

后端运行在 `http://localhost:8082`，API 文档：`http://localhost:8082/swagger-ui.html`

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`

---

## 📡 API 概览

| 模块 | 端点前缀 | 功能 |
|------|----------|------|
| 用户 | `/api/user/*` | 注册、登录、登出、密码修改 |
| 个人资料 | `/api/user/profile/*` | 资料查询与编辑 |
| 文档 | `/api/document/*` | 笔记 CRUD + 回收站 |
| 目录 | `/api/category/*` | 目录树管理 |
| 标签 | `/api/tag/*` | 标签管理 |
| 文件 | `/api/file/*` | OSS 文件上传 |
| 帖子 | `/api/community/post/*` | 帖子发布、详情、Feed 流 |
| 评论 | `/api/community/comment/*` | 评论管理 |
| 点赞 | `/api/community/like-record/*` | 点赞切换 |
| 收藏 | `/api/community/favorite/*` | 收藏管理 |
| 关注 | `/api/community/follow/*` | 关注/粉丝 |
| 私信 | `/api/community/message/*` | 一对一私信 |
| 通知 | `/api/community/notification/*` | 系统通知 |

---

## 🧩 设计亮点

- **策略模式权限校验** — `ResourceOwnerResolver` 接口 + `@CheckOwner` 注解，不同模块可插拔式注入
- **AOP 双层防护** — 接口限流 + 资源鉴权双切面，声明式注解即可启用
- **避免重复查库** — `ValidatedEntityHolder` (ThreadLocal) 缓存已校验实体
- **JWT 黑名单** — 登出即失效，Redis 兜底
- **软删除 + 回收站** — 数据保护，支持恢复

---

## 📊 项目规模

- 后端 Java 源文件：**155 个**，约 **5,400 行**
- 前端 Vue/JS 源文件：**15 个**，约 **5,000 行**
- MyBatis Mapper XML：**8 个**，约 **310 行**
- **总计约 10,800 行代码**

---

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源。

---

<p align="center">
  <b>🧠 用 MindVault，构建你的第二大脑</b>
</p>