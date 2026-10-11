# One file builds both halves. Compose picks a target per service:
#   docker build --target backend  .   # Spring Boot API
#   docker build --target frontend .   # React SPA behind nginx (proxies /graphql to the backend)

FROM eclipse-temurin:25-jdk AS backend-build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src/ src/
RUN ./mvnw package -DskipTests

FROM node:24-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# codegen reads ../src/main/resources/schema from /app, so the generated types always match the backend.
COPY src/main/resources/schema/ /src/main/resources/schema/
RUN npm run codegen && npm run build

FROM eclipse-temurin:25-jre AS backend
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM nginx:1-alpine AS frontend
COPY frontend/nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=frontend-build /app/dist /usr/share/nginx/html
# The backend's address; the official image substitutes it into the template at start.
ENV BACKEND_URL=http://app:8080
EXPOSE 80
