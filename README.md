# AI Coder - 智能代码生成平台

基于 Spring Boot + Vue 3 的全栈 AI 代码生成项目，接入 DeepSeek API，支持自然语言描述自动生成前端项目代码。


<img width="1875" height="917" alt="image" src="https://github.com/user-attachments/assets/5d6577e0-eb8d-427d-93b6-dd389244a2de" />
<img width="1905" height="888" alt="image" src="https://github.com/user-attachments/assets/89f43490-5e0c-4e27-9379-e650529565f3" />
<img width="1920" height="927" alt="image" src="https://github.com/user-attachments/assets/df1c054f-48ff-435c-9087-3239ddd26216" />



## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2.2、MyBatis-Flex、LangChain4j |
| 前端 | Vue 3.5、Vite、Ant Design Vue 4、Pinia |
| 数据库 | MySQL 8、Redis |
| AI | DeepSeek API（Chat + Embedding） |
| JDK | **17**（非 21） |

## 环境要求

- **JDK 17**（Java 21 不兼容，项目中有虚拟线程等 API 仅支持 21，已改为 Thread）
- **MySQL 8**（本地运行，端口 3306）
- **Redis**（本地运行，端口 6379，**无密码**）
- **Node.js 22+**
- **Maven 3.9+**

## 快速启动

### 1. 启动 MySQL

确保本地 MySQL 运行在 3306 端口，创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS ai_code_mother DEFAULT CHARSET utf8mb4;
```

然后执行建表 SQL：`src/main/java/com/lmr/aicoder/sql/create_table.sql`

### 2. 启动 Redis

**关键：必须以无密码模式启动**（LangChain4j 社区版 Redis 模块存在 Bug，无法传递密码）。

```bash
# Windows
"D:/LiMengranDownload/Redis/Redis-x64-3.2.100/redis-server.exe" --port 6379
```

### 3. 创建本地配置文件

在 `src/main/resources/` 下创建 `application-local.yml`（**此文件已被 .gitignore 排除，不会提交到 Git**）：

```yaml
# AI 配置（DeepSeek）
ai:
  api-key: 你的DeepSeek_API_Key

# 腾讯云 COS 对象存储（代码部署用）
cos:
  client:
    secret-id: 你的SecretId
    secret-key: 你的SecretKey
    region: ap-beijing
    bucket: your-bucket-name
```

> 不创建此文件项目也能启动，但 AI 代码生成和 COS 上传功能无法使用。

### 4. 启动后端

在 IDEA 中创建 Run Configuration：

- **Main class**: `com.lmr.aicoder.AiCoderApplication`
- **Program arguments**: `--spring.profiles.active=local --server.port=8123`
- **JRE**: JDK 17

启动后访问：`http://localhost:8123/api`

Swagger 文档：`http://localhost:8123/api/doc.html`

### 5. 启动前端

```bash
cd ai-coder-frontend
npm install    # 首次运行
npm run dev
```

访问：`http://localhost:5173`

## 项目结构

```
ai-coder/
├── src/main/java/com/lmr/aicoder/
│   ├── ai/           # AI 服务（DeepSeek 对话、嵌入）
│   ├── config/       # Spring 配置
│   ├── controller/   # REST 接口
│   ├── core/         # 核心业务
│   ├── generator/    # 代码生成引擎
│   ├── mapper/       # MyBatis 数据访问
│   ├── model/        # 数据模型
│   ├── service/      # 业务逻辑
│   └── sql/          # 数据库建表脚本
├── ai-coder-frontend/ # Vue 3 前端
│   ├── src/api/      # API 调用
│   ├── src/pages/    # 页面组件
│   ├── src/stores/   # Pinia 状态管理
│   └── src/router/   # 路由配置
└── pom.xml
```

## 常见问题

### 启动报 `NOAUTH Authentication required`

Redis 设置了密码导致。LangChain4j 社区版 Redis 模块（beta7）无法正确传递密码。

**解决**：以无密码模式启动 Redis：
```bash
redis-server.exe --port 6379
```

### 编译报 `Thread.startVirtualThread()` 找不到

Java 版本不对。虚拟线程是 JDK 21 API，项目已改为 `new Thread().start()`，必须用 **JDK 17** 编译运行。

### 前端启动报错

```bash
cd ai-coder-frontend
rm -rf node_modules    # 清除旧的依赖
npm install
npm run dev
```

### API 密钥在哪里配置

所有敏感配置（DeepSeek Key、COS 密钥）都在 `application-local.yml` 中，该文件已被 `.gitignore` 排除，不会提交到 Git。首次克隆项目后需手动创建。
