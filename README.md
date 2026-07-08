🚀 v0.0.2 — 首个公开版本
MindVault 是一个面向个人知识管理与社区分享的在线笔记平台，支持 Markdown 编辑、全文搜索、树形目录、社区互动与实时私信。

✨ 已实现功能
📝 笔记管理
功能	说明
文档 CRUD	创建、编辑、删除、查看文档
全文搜索	MySQL FULLTEXT 索引，支持搜索片段高亮
树形目录	多级分类，支持 2D 列表 / 3D 力导向图两种视图
标签管理	文档支持多标签关联
回收站	软删除，支持恢复
👥 社区互动
功能	说明
帖子	发布帖子，可关联文档附件
评论	二级评论，支持回复
互动	点赞、收藏、关注
通知	点赞/评论/关注通知，支持已读标记
💬 实时通信
一对一私信聊天，基于 WebSocket 实时推送
👤 用户系统
注册 / 登录 / JWT 认证
个人资料编辑（昵称、头像、邮箱、性别、简介）
偏好设置（主题、字体、编辑器视图、隐私控制）
用户拉黑
🏗️ 技术栈
层级	技术
后端框架	Spring Boot 4.0.6 + Spring MVC
ORM	MyBatis-Plus 3.5.16
认证	JWT 0.12.6（自动续期 + Redis 黑名单）
缓存	Spring Cache + Redis
数据库	MySQL 8.0
对象存储	阿里云 OSS
实时通信	WebSocket（Spring STOMP over SockJS）
前端	Vue 3 + Vite + Element Plus + Three.js
API 文档	SpringDoc OpenAPI（Swagger UI）
🔒 安全特性
声明式资源权限校验（@CheckOwner + 策略模式）
接口限流（@RateLimit）
JWT 自动续期 + 登出黑名单
🚀 快速开始

bash
git clone https://github.com/Zephyr-110/Mindvault-note.git
cd Mindvault-note

# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS mindvault DEFAULT CHARSET utf8mb4;"

# 配置 application-local.yaml

# 启动后端（8082）
cd mindvault-app && mvn spring-boot:run

# 启动前端（5173）
cd ../frontend && npm install && npm run dev
📝 后续计划
笔记协作编辑
笔记版本历史
AI 辅助写作
移动端适配
Docker 一键部署
