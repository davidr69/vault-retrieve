FROM registry:5000/openjdk:21-ea-27-slim

COPY build/libs/vault-retrieve-4.0.3.jar /app/
WORKDIR /app

ENTRYPOINT ["/bin/sh", "-c", "java -jar /app/vault-retrieve-4.0.4.jar"]
