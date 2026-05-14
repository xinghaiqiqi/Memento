-- 创建数据库
CREATE DATABASE IF NOT EXISTS memento_ai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE memento_ai_db;

-- 表1：users（用户表）
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户唯一标识',
    nickname VARCHAR(50) NOT NULL DEFAULT '记忆收藏家' COMMENT '用户昵称',
    avatar VARCHAR(255) COMMENT '头像路径（本地或base64）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '首次初始化时间',
    memory_count INT DEFAULT 0 COMMENT '记忆总数（冗余字段）'
) ENGINE=InnoDB COMMENT='用户表';

-- 表2：memories（记忆表）
CREATE TABLE IF NOT EXISTS memories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记忆唯一标识',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    title VARCHAR(200) NOT NULL COMMENT '记忆标题',
    content TEXT NOT NULL COMMENT '记忆详细内容',
    event_date DATE NOT NULL COMMENT '事件发生日期',
    tags VARCHAR(255) COMMENT '逗号分隔的标签，如“旅行,开心”',
    sentiment_score DECIMAL(3,2) DEFAULT 0 COMMENT '情感分数，范围-1到1',
    sentiment_analyzed_at TIMESTAMP NULL COMMENT '情感分析时间',
    embedding JSON COMMENT '文本嵌入向量（用于聚类，存储为数组）',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '软删除标记',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_date (user_id, event_date),
    INDEX idx_user_deleted (user_id, is_deleted),
    INDEX idx_sentiment (sentiment_score)
) ENGINE=InnoDB COMMENT='记忆表';

-- 表3：topic_clusters（主题簇表）
CREATE TABLE IF NOT EXISTS topic_clusters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主题唯一标识',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    name VARCHAR(100) NOT NULL COMMENT '主题名称，如“我的旅行故事”',
    description TEXT COMMENT '主题描述',
    memory_ids TEXT COMMENT '该主题包含的记忆ID列表（JSON数组格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='主题簇表';

-- 表4：milestones（重要节点表）
CREATE TABLE IF NOT EXISTS milestones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '节点唯一标识',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    memory_id BIGINT NOT NULL COMMENT '关联的记忆',
    node_type VARCHAR(50) NOT NULL COMMENT '节点类型：毕业/入职/旅行/分手等',
    custom_title VARCHAR(200) COMMENT '自定义节点标题',
    event_date DATE NOT NULL COMMENT '节点发生日期',
    is_auto BOOLEAN DEFAULT TRUE COMMENT 'TRUE为AI自动识别，FALSE为用户手动添加',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_date (user_id, event_date)
) ENGINE=InnoDB COMMENT='重要节点表';

-- 表5：generated_narratives（叙事生成表）
CREATE TABLE IF NOT EXISTS generated_narratives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '叙事唯一标识',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    title VARCHAR(200) NOT NULL COMMENT '叙事标题，如“我的2024年回忆”',
    content TEXT NOT NULL COMMENT '叙事正文（长文本）',
    condition_json TEXT COMMENT '生成条件JSON，记录用户选择的时间段/主题',
    word_count INT COMMENT '字数统计',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB COMMENT='叙事生成表';

-- 表6：import_logs（导入日志表）
CREATE TABLE IF NOT EXISTS import_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志唯一标识',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    success_count INT DEFAULT 0 COMMENT '成功导入条数',
    fail_count INT DEFAULT 0 COMMENT '失败条数',
    error_message TEXT COMMENT '错误详情（如有）',
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间'
) ENGINE=InnoDB COMMENT='导入日志表';

-- 表7：export_tasks（导出任务表）
CREATE TABLE IF NOT EXISTS export_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务唯一标识',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/processing/completed/failed',
    file_path VARCHAR(500) COMMENT '生成的PDF文件路径',
    condition_json TEXT COMMENT '导出条件JSON',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间'
) ENGINE=InnoDB COMMENT='导出任务表';

-- 表8：app_config（应用配置表）
CREATE TABLE IF NOT EXISTS app_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='应用配置表';

-- 预设配置项
INSERT INTO app_config (config_key, config_value) VALUES 
('is_initialized', 'false'),
('ai_model', 'deepseek'),
('api_key', '');
