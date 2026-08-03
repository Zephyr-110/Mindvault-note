# ============================================
# 阶段一：构建阶段 — 用 Maven 编译打包
# ============================================
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# 先复制 pom.xml 文件，利用 Docker 缓存层加速构建
COPY pom.xml .
COPY mindvault-common/pom.xml mindvault-common/
COPY mindvault-user-api/pom.xml mindvault-user-api/
COPY mindvault-user-service/pom.xml mindvault-user-service/
COPY mindvault-note-api/pom.xml mindvault-note-api/
COPY mindvault-note-service/pom.xml mindvault-note-service/
COPY mindvault-community-api/pom.xml mindvault-community-api/
COPY mindvault-community-service/pom.xml mindvault-community-service/
COPY mindvault-ai-api/pom.xml mindvault-ai-api/
COPY mindvault-ai-service/pom.xml mindvault-ai-service/
COPY mindvault-app/pom.xml mindvault-app/

# 下载依赖（这层会被缓存，除非 pom.xml 变更）
RUN mvn dependency:go-offline -B -q

# 复制源代码
COPY mindvault-common/src mindvault-common/src
COPY mindvault-user-api/src mindvault-user-api/src
COPY mindvault-user-service/src mindvault-user-service/src
COPY mindvault-note-api/src mindvault-note-api/src
COPY mindvault-note-service/src mindvault-note-service/src
COPY mindvault-community-api/src mindvault-community-api/src
COPY mindvault-community-service/src mindvault-community-service/src
COPY mindvault-ai-api/src mindvault-ai-api/src
COPY mindvault-ai-service/src mindvault-ai-service/src
COPY mindvault-app/src mindvault-app/src

# 编译打包，跳过测试（测试应在 CI 中跑）
RUN mvn package -DskipTests -B -q

# ============================================
# 阶段二：运行阶段 — 只保留 JRE + jar 包
# ============================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /build/mindvault-app/target/*.jar app.jar

# 暴露端口
EXPOSE 8082

# 启动命令（可通过环境变量覆盖 JVM 参数）
ENTRYPOINT ["java", "-jar", "app.jar"]