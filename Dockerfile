# Use a base JDK image
FROM eclipse-temurin:21-jdk-alpine as builder

# Set workdir
WORKDIR /app

# Copy source files
COPY . .

# Make Maven wrapper executable and build the JAR
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

# Use a smaller JRE base image for runtime.
FROM eclipse-temurin:21-jre-alpine

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Set working directory
WORKDIR /home/spring

# Copy built jar
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]