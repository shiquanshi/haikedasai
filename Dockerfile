# 构建前端
FROM node:18-alpine AS frontend-build
WORKDIR /frontend

# 先复制 package.json 和锁文件（利用 Docker 缓存，避免每次修改代码都重新安装依赖）
COPY frontend/package.json frontend/package-lock.json ./

# 清理缓存并切换镜像源（解决国内下载慢问题）
RUN rm -rf node_modules package-lock.json || true && \
    npm cache clean --force && \
    npm config set registry https://registry.npm.taobao.org/

# 强制安装指定版本的 element-plus（确保依赖完整）
RUN npm install element-plus@2.3.14 --force && \
    npm install --force

# 复制所有前端代码（这一步放在依赖安装后，利用缓存）
COPY frontend/ ./

# 构建时清除 Rollup 缓存（解决 Element Plus 文件引用问题）
RUN npm run build -- --force  # 若项目用 Vite，--force 清除缓存；若用 Rollup，根据构建命令调整

# 构建后端
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app

# 先复制 pom.xml 并下载依赖（利用 Maven 缓存）
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B  # 提前下载所有依赖，后续修改代码无需重复下载

# 复制源代码并打包
COPY backend/src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM nginx:alpine

# 安装 Java 运行时和 supervisor（确保版本兼容）
RUN apk add --no-cache openjdk17-jre supervisor

# 复制前端构建产物到 Nginx 静态目录
COPY --from=frontend-build /frontend/dist /usr/share/nginx/html

# 复制后端 JAR 包
COPY --from=backend-build /app/target/question-card-1.0.0.jar /app/app.jar

# 复制 Nginx 配置（确保配置正确，避免 404 等问题）
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制 supervisor 配置（确保同时启动 Nginx 和后端服务）
COPY supervisord.conf /etc/supervisord.conf

# 设置环境变量（指定 Spring 环境）
ENV SPRING_PROFILES_ACTIVE=docker

# 暴露端口（Nginx 用 80，后端用 8080）
EXPOSE 80 8080

# 启动 supervisor 管理进程
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]