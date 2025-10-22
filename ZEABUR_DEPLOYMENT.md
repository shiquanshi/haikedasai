# Zeabur 部署指南

本项目支持在 Zeabur 平台上部署，Zeabur 会自动构建和部署应用。

## 部署架构

在 Zeabur 上，您需要部署以下服务：

1. **应用服务**（自动检测并构建）
2. **MySQL 数据库**（使用 Zeabur 预构建服务）
3. **MinIO 对象存储**（使用 Zeabur 预构建服务）

## 快速部署步骤

### 1. 创建项目

1. 登录 [Zeabur 控制台](https://zeabur.com)
2. 点击「新建项目」
3. 选择从 Git 仓库导入或直接上传代码

### 2. 添加 MySQL 服务

1. 在项目中点击「添加服务」
2. 选择「预构建服务」→「MySQL」
3. 等待 MySQL 服务启动
4. Zeabur 会自动生成以下环境变量：
   - `MYSQL_HOST`
   - `MYSQL_PORT`
   - `MYSQL_USERNAME`
   - `MYSQL_PASSWORD`
   - `MYSQL_DATABASE`

### 3. 添加 MinIO 服务

1. 在项目中点击「添加服务」
2. 选择「预构建服务」→「MinIO」
3. 等待 MinIO 服务启动
4. Zeabur 会自动生成以下环境变量：
   - `MINIO_ENDPOINT`
   - `MINIO_ROOT_USER`
   - `MINIO_ROOT_PASSWORD`

### 4. 配置应用服务

在应用服务的「变量」标签页中添加以下环境变量：

#### 必需的环境变量

```bash
# Spring 配置
SPRING_PROFILES_ACTIVE=zeabur

# 数据库配置（使用 Zeabur 自动注入的变量）
SPRING_DATASOURCE_URL=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=${MYSQL_USERNAME}
SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD}

# MinIO 配置（使用 Zeabur 自动注入的变量）
MINIO_ENDPOINT=${MINIO_ENDPOINT}
MINIO_ACCESS_KEY=${MINIO_ROOT_USER}
MINIO_SECRET_KEY=${MINIO_ROOT_PASSWORD}
MINIO_BUCKET_NAME=question-card-images

# 服务器端口（Zeabur 默认使用 8080）
SERVER_PORT=8080
```

#### 可选的环境变量

```bash
# JWT 配置（建议修改为自己的密钥）
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400000

# 火山引擎 API 配置（如需使用 AI 功能）
VOLC_ENDPOINT=your-volc-endpoint
VOLC_API_KEY=your-volc-api-key
VOLC_MODEL_NAME=your-model-name
```

### 5. 初始化 MinIO Bucket

由于 Zeabur 的 MinIO 服务需要手动创建 bucket，您需要：

1. 访问 MinIO Console（在 MinIO 服务详情中可以找到访问地址）
2. 使用 Zeabur 生成的 `MINIO_ROOT_USER` 和 `MINIO_ROOT_PASSWORD` 登录
3. 创建名为 `question-card-images` 的 bucket
4. 设置 bucket 为公开读取（Public Read）

### 6. 部署应用

1. 推送代码到 Git 仓库
2. Zeabur 会自动检测到 `zbpack.json` 配置
3. 自动构建并部署应用

## 配置文件说明

### zbpack.json

```json
{
  "root_directory": "backend"
}
```

这个配置告诉 Zeabur 从 `backend` 目录构建应用。

### application-zeabur.yml

创建 `backend/src/main/resources/application-zeabur.yml` 配置文件：

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

minio:
  endpoint: ${MINIO_ENDPOINT}
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket-name: ${MINIO_BUCKET_NAME:question-card-images}

server:
  port: ${SERVER_PORT:8080}

jwt:
  secret: ${JWT_SECRET:default-secret-key-change-in-production}
  expiration: ${JWT_EXPIRATION:86400000}

volc:
  endpoint: ${VOLC_ENDPOINT:}
  api-key: ${VOLC_API_KEY:}
  model-name: ${VOLC_MODEL_NAME:}

logging:
  level:
    com.knowledge.questioncard: INFO
```

## 域名配置

1. 在 Zeabur 项目设置中，可以为应用配置自定义域名
2. Zeabur 会自动处理 HTTPS 证书
3. 配置域名后，访问 `https://your-domain.com` 即可使用应用

## 数据库初始化

### 方式一：自动初始化（推荐）

在 `application-zeabur.yml` 中配置：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 自动更新表结构
```

### 方式二：手动执行 SQL

1. 使用 Zeabur 提供的数据库连接信息
2. 通过数据库客户端连接到 MySQL
3. 执行项目中的初始化 SQL 脚本

## 环境变量对照表

| 配置项 | 环境变量 | Zeabur 自动注入 | 说明 |
|--------|----------|-----------------|------|
| 数据库地址 | MYSQL_HOST | ✅ | MySQL 服务地址 |
| 数据库端口 | MYSQL_PORT | ✅ | MySQL 服务端口 |
| 数据库用户名 | MYSQL_USERNAME | ✅ | MySQL 用户名 |
| 数据库密码 | MYSQL_PASSWORD | ✅ | MySQL 密码 |
| 数据库名称 | MYSQL_DATABASE | ✅ | 数据库名 |
| MinIO 地址 | MINIO_ENDPOINT | ✅ | MinIO 服务地址 |
| MinIO Access Key | MINIO_ROOT_USER | ✅ | MinIO 访问密钥 |
| MinIO Secret Key | MINIO_ROOT_PASSWORD | ✅ | MinIO 私密密钥 |
| MinIO Bucket | MINIO_BUCKET_NAME | ❌ | 需手动设置 |
| JWT 密钥 | JWT_SECRET | ❌ | 需手动设置 |
| 火山引擎 API | VOLC_* | ❌ | 需手动设置 |

## 常见问题

### 1. 应用无法连接数据库

**原因**：环境变量配置错误或 MySQL 服务未启动

**解决方法**：
- 检查 MySQL 服务是否正常运行
- 验证环境变量是否正确配置
- 查看应用日志中的错误信息

### 2. 图片上传失败

**原因**：MinIO bucket 未创建或权限配置错误

**解决方法**：
- 登录 MinIO Console 检查 bucket 是否存在
- 确认 bucket 权限设置为公开读取
- 检查 MinIO 环境变量是否正确

### 3. 构建失败

**原因**：依赖下载失败或配置错误

**解决方法**：
- 检查 `zbpack.json` 配置
- 确认 `pom.xml` 依赖配置正确
- 查看构建日志中的详细错误信息

### 4. 应用启动慢

**原因**：首次部署需要下载依赖和构建

**解决方法**：
- 首次部署需要等待较长时间（5-10分钟）
- 后续部署会使用缓存，速度会快很多

## 监控和日志

### 查看应用日志

1. 在 Zeabur 控制台中选择应用服务
2. 点击「日志」标签
3. 实时查看应用运行日志

### 查看服务状态

1. 在项目页面可以看到所有服务的运行状态
2. 绿色表示服务正常运行
3. 红色表示服务异常，需要查看日志排查问题

## 成本优化建议

1. **合理选择服务规格**：根据实际流量选择合适的服务器规格
2. **使用 Zeabur 免费额度**：新用户有一定的免费额度
3. **定期清理无用数据**：清理数据库和 MinIO 中的过期数据
4. **配置自动休眠**：在低流量时段可以配置服务自动休眠

## 与 Docker 部署的区别

| 特性 | Zeabur 部署 | Docker 部署 |
|------|-------------|-------------|
| 部署方式 | 自动化部署 | 手动部署 |
| 数据库 | 预构建服务 | Docker 容器 |
| 对象存储 | 预构建服务 | Docker 容器 |
| 域名配置 | 一键配置 | 需手动配置 |
| HTTPS | 自动证书 | 需手动配置 |
| 扩展性 | 一键扩展 | 手动扩展 |
| 成本 | 按量付费 | 服务器成本 |
| 适用场景 | 快速上线、自动运维 | 完全控制、本地开发 |

## 生产环境建议

1. **修改默认密钥**：
   - 更改 JWT_SECRET
   - 配置强密码

2. **配置备份**：
   - 定期备份 MySQL 数据
   - 备份 MinIO 中的重要文件

3. **监控告警**：
   - 配置服务异常告警
   - 监控资源使用情况

4. **日志管理**：
   - 配置日志级别
   - 定期清理旧日志

5. **性能优化**：
   - 配置数据库连接池
   - 启用缓存机制
   - 优化 SQL 查询

## 从 Docker 迁移到 Zeabur

如果您之前使用 Docker 部署，迁移到 Zeabur 的步骤：

1. **导出数据**：
   ```bash
   # 导出 MySQL 数据
   docker-compose exec mysql mysqldump -uroot -pquanshi666 question_card_db > backup.sql
   
   # 导出 MinIO 数据
   docker-compose exec minio mc mirror minio/question-card-images ./minio-backup
   ```

2. **在 Zeabur 创建服务**（参考上述步骤）

3. **导入数据**：
   - 连接 Zeabur MySQL，导入 backup.sql
   - 通过 MinIO Console 上传文件到新的 bucket

4. **配置环境变量**（参考上述配置）

5. **测试验证**：确保所有功能正常工作

## 技术支持

- Zeabur 官方文档：https://zeabur.com/docs
- Zeabur Discord 社区：https://discord.gg/zeabur
- 项目 Issues：提交到项目的 GitHub Issues