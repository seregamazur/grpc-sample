# Build the JAR
FROM maven:3.6.3-openjdk-11 as build
WORKDIR /workspace/app
COPY kotlin /workspace/app
COPY proto /workspace/proto
RUN mvn clean install -DskipTests

# Copy the JAR and run it
FROM adoptopenjdk:11-jdk-hotspot
WORKDIR /app
COPY --from=build /workspace/app/target/kotlin-1.0-SNAPSHOT.jar /app/kotlin-1.0-SNAPSHOT.jar
COPY tls_credentials /app/tls_credentials
COPY retrying_config.json /app/retrying_config.json
CMD ["sh", "-c", "java -cp /app/kotlin-1.0-SNAPSHOT.jar org.demo.client.GrpcResilientClient"]
