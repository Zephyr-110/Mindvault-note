-- ============================================
-- MindVault 数据库初始化脚本
-- MySQL 容器首次启动时自动执行
-- ============================================

CREATE DATABASE IF NOT EXISTS mindvault DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mindvault;

-- ─────────── 用户模块 ───────────

CREATE TABLE IF NOT EXISTS `user` (
                                      `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `user_profile` (
                                              `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
                                              `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
                                              `nickname`    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(512) DEFAULT NULL COMMENT '头像',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `gender`      BIGINT       DEFAULT NULL COMMENT '性别',
    `age`         BIGINT       DEFAULT NULL COMMENT '年龄',
    `region`      VARCHAR(128) DEFAULT NULL COMMENT '区域',
    `bio`         VARCHAR(512) DEFAULT NULL COMMENT '个性签名',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

CREATE TABLE IF NOT EXISTS `user_block` (
                                            `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `user_id`        BIGINT   NOT NULL COMMENT '用户ID',
                                            `blocked_user_id` BIGINT  NOT NULL COMMENT '被屏蔽用户ID',
                                            `create_time`    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_blocked` (`user_id`, `blocked_user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户黑名单表';

CREATE TABLE IF NOT EXISTS `user_setting` (
                                              `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                              `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
                                              `setting_key`   VARCHAR(64)  NOT NULL COMMENT '设置键',
    `setting_value` VARCHAR(512) DEFAULT NULL COMMENT '设置值',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_setting` (`user_id`, `setting_key`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表';

-- ─────────── 笔记模块 ───────────

CREATE TABLE IF NOT EXISTS `category` (
                                          `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `name`        VARCHAR(128) NOT NULL COMMENT '分类名',
    `parent_id`   BIGINT       DEFAULT NULL COMMENT '父分类ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `is_deleted`  TINYINT      DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
    `deleted_at`  DATETIME     DEFAULT NULL COMMENT '删除时间',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类目录表';

CREATE TABLE IF NOT EXISTS `document` (
                                          `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `title`       VARCHAR(256) NOT NULL COMMENT '标题',
    `content`     LONGTEXT     DEFAULT NULL COMMENT '内容',
    `category_id` BIGINT       DEFAULT NULL COMMENT '分类ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `is_deleted`  TINYINT      DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
    `deleted_at`  DATETIME     DEFAULT NULL COMMENT '删除时间',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    FULLTEXT KEY `ft_title_content` (`title`, `content`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

CREATE TABLE IF NOT EXISTS `tag` (
                                     `id`      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `name`    VARCHAR(64) NOT NULL COMMENT '标签名',
    `user_id` BIGINT      NOT NULL COMMENT '用户ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tag` (`user_id`, `name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

CREATE TABLE IF NOT EXISTS `document_tag` (
                                              `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                                              `document_id` BIGINT NOT NULL COMMENT '文档ID',
                                              `tag_id`      BIGINT NOT NULL COMMENT '标签ID',
                                              PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_tag` (`document_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档-标签关联表';

-- ─────────── 社区模块 ───────────

CREATE TABLE IF NOT EXISTS `post` (
                                      `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `title`           VARCHAR(256) NOT NULL COMMENT '标题',
    `content`         LONGTEXT     DEFAULT NULL COMMENT '内容',
    `author_id`       BIGINT       NOT NULL COMMENT '作者ID',
    `visibility`      BIGINT       DEFAULT 0 COMMENT '可见性: 0=公开, 1=仅好友',
    `original_post_id` BIGINT      DEFAULT NULL COMMENT '原始帖子ID(转发)',
    `note_accessory`  TEXT         DEFAULT NULL COMMENT '笔记附件JSON',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_visibility` (`visibility`),
    KEY `idx_create_time` (`create_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

CREATE TABLE IF NOT EXISTS `comment` (
                                         `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `post_id`     BIGINT       NOT NULL COMMENT '帖子ID',
                                         `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
                                         `parent_id`   BIGINT       DEFAULT NULL COMMENT '父评论ID',
                                         `content`     TEXT         NOT NULL COMMENT '评论内容',
                                         `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

CREATE TABLE IF NOT EXISTS `like_record` (
                                             `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型: post/comment',
    `target_id`   BIGINT      NOT NULL COMMENT '目标ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';

CREATE TABLE IF NOT EXISTS `favorite` (
                                          `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `post_id`     BIGINT   NOT NULL COMMENT '帖子ID',
                                          `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
                                          `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`user_id`, `post_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

CREATE TABLE IF NOT EXISTS `follow` (
                                        `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
                                        `follower_id` BIGINT   NOT NULL COMMENT '粉丝ID',
                                        `followee_id` BIGINT   NOT NULL COMMENT '被关注者ID',
                                        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follower_followee` (`follower_id`, `followee_id`),
    KEY `idx_followee_id` (`followee_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注表';

CREATE TABLE IF NOT EXISTS `message` (
                                         `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                         `sender_id`   BIGINT       NOT NULL COMMENT '发送者ID',
                                         `receiver_id` BIGINT       NOT NULL COMMENT '接收者ID',
                                         `content`     TEXT         NOT NULL COMMENT '消息内容',
                                         `is_read`     TINYINT(1)   DEFAULT 0 COMMENT '是否已读',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_receiver_id` (`receiver_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信表';

CREATE TABLE IF NOT EXISTS `notification` (
                                              `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
                                              `user_id`         BIGINT      NOT NULL COMMENT '用户ID',
                                              `type`            VARCHAR(32) NOT NULL COMMENT '通知类型',
    `trigger_user_id` BIGINT      DEFAULT NULL COMMENT '触发用户ID',
    `target_id`       BIGINT      DEFAULT NULL COMMENT '目标ID',
    `is_read`         TINYINT(1)  DEFAULT 0 COMMENT '是否已读',
    `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ─────────── AI 模块 ───────────

CREATE TABLE IF NOT EXISTS `ai_chat_session` (
                                                 `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                 `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
                                                 `title`       VARCHAR(256) DEFAULT NULL COMMENT '会话标题',
    `summary`     TEXT         DEFAULT NULL COMMENT '会话摘要',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';

CREATE TABLE IF NOT EXISTS `ai_chat_history` (
                                                 `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                 `session_id`  BIGINT       NOT NULL COMMENT '会话ID',
                                                 `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
                                                 `role`        VARCHAR(32)  NOT NULL COMMENT '角色: user/assistant',
    `content`     TEXT         NOT NULL COMMENT '消息内容',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话历史表';