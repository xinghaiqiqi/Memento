-- H2 Schema for Memento AI

-- 表1：users（用户表）
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL DEFAULT '记忆收藏家',
    avatar VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    memory_count INT DEFAULT 0
);

-- 表2：memories（记忆表）
CREATE TABLE IF NOT EXISTS memories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    event_date DATE NOT NULL,
    tags VARCHAR(255),
    photo_url VARCHAR(500),
    sentiment_score DECIMAL(3,2) DEFAULT 0,
    sentiment_analyzed_at TIMESTAMP NULL,
    embedding JSON,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_date ON memories(user_id, event_date);
CREATE INDEX IF NOT EXISTS idx_user_deleted ON memories(user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_sentiment ON memories(sentiment_score);

-- 确保旧数据表包含 photo_url 字段
ALTER TABLE memories ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500);

-- 表3：topic_clusters（主题簇表）
CREATE TABLE IF NOT EXISTS topic_clusters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    memory_ids TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 表4：milestones（重要节点表）
CREATE TABLE IF NOT EXISTS milestones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    memory_id BIGINT NOT NULL,
    node_type VARCHAR(50) NOT NULL,
    custom_title VARCHAR(200),
    event_date DATE NOT NULL,
    is_auto BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ms_user_date ON milestones(user_id, event_date);

-- 表5：generated_narratives（叙事生成表）
CREATE TABLE IF NOT EXISTS generated_narratives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    condition_json TEXT,
    word_count INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_gn_user_created ON generated_narratives(user_id, created_at);

-- 确保旧数据表包含 type 字段
ALTER TABLE generated_narratives ADD COLUMN IF NOT EXISTS type VARCHAR(50);

-- 表6：import_logs（导入日志表）
CREATE TABLE IF NOT EXISTS import_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    success_count INT DEFAULT 0,
    fail_count INT DEFAULT 0,
    error_message TEXT,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 表7：export_tasks（导出任务表）
CREATE TABLE IF NOT EXISTS export_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    file_path VARCHAR(500),
    condition_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL
);

-- 表8：app_config（应用配置表）
CREATE TABLE IF NOT EXISTS app_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 预设配置项
MERGE INTO app_config (config_key, config_value) KEY(config_key) VALUES ('is_initialized', 'false');
MERGE INTO app_config (config_key, config_value) KEY(config_key) VALUES ('ai_model', 'deepseek');
MERGE INTO app_config (config_key, config_value) KEY(config_key) VALUES ('api_key', '');
