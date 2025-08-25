FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copier d'abord les fichiers de dépendances pour optimiser le cache Docker
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar

# Changer vers l'utilisateur non-root
USER appuser

# Cloud Run utilise la variable PORT
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]