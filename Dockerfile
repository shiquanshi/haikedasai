# 构建前端
FROM node:18-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# 构建后端
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM nginx:alpine

# 安装 Java 运行时和 supervisor
RUN apk add --no-cache openjdk17-jre supervisor

# 复制前端构建文件
COPY --from=frontend-build /frontend/dist /usr/share/nginx/html

# 复制后端 JAR
COPY --from=backend-build /app/target/question-card-1.0.0.jar /app/app.jar

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制 supervisor 配置
COPY supervisord.conf /etc/supervisord.conf

# 暴露端口
EXPOSE 8080

# 使用 supervisor 同时运行 nginx 和 Spring Boot
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]