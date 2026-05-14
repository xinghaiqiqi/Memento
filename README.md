# Memento AI (拾光记)

一款将个人碎片记忆自动转化为叙事回忆录的 AI 原生桌面软件。

## 项目架构

- **后端**: Spring Boot 2.7.x, MyBatis Plus, H2 Database (嵌入式)
- **前端**: Vue 3, Vite, Element Plus, Pinia, ECharts
- **AI**: DeepSeek API (支持对话与情感分析)

## 目录结构

- `memento-ai-backend/`: 后端项目源码
- `memento-ai-frontend/`: 前端项目源码
- `memento-ai-backend/src/main/resources/db/`: 数据库初始化脚本 (`schema.sql`)

## 快速启动

### 1. 后端启动
1. 在 `memento-ai-backend` 目录下，配置环境变量或直接修改 `application.yml`。
   - `DB_PATH`: 数据库文件存储路径 (默认 `./data/memento_db`)
   - `DEEPSEEK_API_KEY`: 你的 DeepSeek API Key
2. 运行 `mvn clean spring-boot:run` 或在 IDE 中启动 `MementoApplication`。
3. 数据库将自动初始化，无需手动安装 MySQL。
4. 访问 `http://localhost:8080/h2-console` 可进入数据库管理控制台。

### 2. 前端启动
1. 进入 `memento-ai-frontend` 目录。
2. 执行 `npm install` 安装依赖。
3. 执行 `npm run dev` 启动开发服务器。
4. 访问 `http://localhost:5173`。

## 环境变量说明

为了方便打包为 EXE，数据库已切换为 H2 嵌入式模式。

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_PATH` | H2 数据库文件路径 | `./data/memento_db` |
| `DB_USER` | 数据库用户 | `sa` |
| `DB_PASS` | 数据库密码 | (空) |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | (空) |

## 开发分工

- **成员A**: 用户与项目初始化、记忆管理 (CRUD)、文件导入、情感分析后端接口。
- **成员B**: 主题聚类、重要节点识别、时光画廊、时间轴视图。
- **成员C**: 叙事生成、情感光谱、记忆星云图 (加分项)、导出报告。
