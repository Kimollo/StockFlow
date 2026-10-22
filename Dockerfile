FROM node:22-alpine AS frontend
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS api
WORKDIR /src
COPY backend/pom.xml .
COPY backend/src ./src
COPY --from=frontend /src/dist ./src/main/resources/static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=api /src/target/stockflow-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
