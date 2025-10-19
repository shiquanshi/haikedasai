# 智能知识问答卡片系统

## 项目简介

智能知识问答卡片系统是一个基于 Vue 3 + Java Spring Boot 的全栈应用，旨在帮助用户通过问答卡片的形式进行知识学习和记忆辅助。

### 核心功能

1. **主题输入**: 用户输入学习主题
2. **题库选择**: 三种方式生成题库
   - AI智能生成
   - 系统推荐题库
   - 自定义文档上传
3. **卡片交互**: 翻转查看答案的互动学习体验
4. **Demo题库**: 预置财务和税务两个专业题库

## 技术栈

### 前端
- Vue 3.3.4
- TypeScript
- Vite
- Vue Router
- Pinia (状态管理)
- Element Plus (UI组件库)
- Axios (HTTP客户端)

### 后端
- Java 17
- Spring Boot 3.2.0
- MyBatis 3.0.2
- MySQL 8.0+
- Lombok
- Apache POI (文档解析)
- PDF Box (PDF解析)

## 项目结构

```
haikedasai/
├── frontend/              # 前端项目
│   ├── src/
│   │   ├── api/          # API接口
│   │   ├── router/       # 路由配置
│   │   ├── views/        # 页面组件
│   │   ├── App.vue       # 根组件
│   │   ├── main.ts       # 入口文件
│   │   └── style.css     # 全局样式
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
│
├── backend/              # 后端项目
│   ├── src/main/java/com/knowledge/questioncard/
│   │   ├── common/       # 通用类
│   │   ├── config/       # 配置类
│   │   ├── controller/   # 控制器
│   │   ├── dto/          # 数据传输对象
│   │   ├── entity/       # 实体类
│   │   ├── repository/   # 数据访问层
│   │   ├── service/      # 业务逻辑层
│   │   └── QuestionCardApplication.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
└── README.md
```

## 快速开始

### 环境要求

- Node.js 16+
- Java 17+
- Maven 3.6+

### 安装步骤

#### 1. 启动后端

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

#### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端应用将在 `http://localhost:3000` 启动

### 访问应用

打开浏览器访问: `http://localhost:3000`

## API 接口

### 题库相关接口

- `POST /api/question-bank/generate` - AI生成题库
- `GET /api/question-bank/system?topic={topic}` - 获取系统推荐题库
- `GET /api/question-bank/{bankId}/cards` - 获取指定题库的卡片
- `POST /api/question-bank/upload` - 上传自定义文档生成题库

## 功能特性

### 1. AI智能生成
- 输入主题后，系统自动生成相关问答卡片
- 支持多种主题领域

### 2. 系统推荐题库
- 预置专业题库（财务、税务）
- 精选高质量问答内容

### 3. 自定义文档上传
- 支持 TXT、PDF、DOC/DOCX 格式
- 自动解析文档内容生成题库

### 4. 卡片交互
- 点击翻转查看答案
- 流畅的动画效果
- 响应式设计

## 开发说明

### 前端开发

```bash
cd frontend
npm run dev      # 开发模式
npm run build    # 生产构建
npm run preview  # 预览构建结果
```

### 后端开发

```bash
cd backend
mvn spring-boot:run  # 启动应用
mvn test            # 运行测试
mvn clean package   # 打包应用
```

## 数据库配置

项目使用 MySQL 数据库 + MyBatis 框架。

### 数据库初始化

1. 确保 MySQL 服务已启动

2. 执行初始化脚本创建数据库和表：

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

或者手动在 MySQL 中执行：

```sql
CREATE DATABASE IF NOT EXISTS question_card_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE question_card_db;
-- 然后执行 schema.sql 中的表创建语句
```

3. 修改 `backend/src/main/resources/application.yml` 中的数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/question_card_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: your_username  # 修改为你的MySQL用户名
    password: your_password  # 修改为你的MySQL密码
```

4. 启动后端应用，系统会自动加载财务和税务 Demo 题库数据

### 数据库表结构

- `question_banks`: 题库表
- `question_cards`: 问答卡片表

## 未来规划

- [ ] 用户系统和学习进度跟踪
- [ ] 更多主题题库
- [ ] 学习统计和分析
- [ ] 移动端适配
- [ ] 社区分享功能

## 许可证

MIT License

## 联系方式

如有问题或建议，欢迎提 Issue。