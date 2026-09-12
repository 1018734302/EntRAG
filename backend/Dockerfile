# 多阶段构建：先 maven 编译，再交给精简 JRE 运行
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/enterprise-rag.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
