FROM alpine:latest

RUN apk upgrade --no-cache \
    && apk add --no-cache bash curl gcompat libstdc++ maven openjdk11

RUN mkdir -p /app/target
WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY mps-transformation-terraform ./mps-transformation-terraform
RUN rm -rf ./mps-transformation-terraform/build
RUN mkdir -p /app/mps-transformation-terraform/transformationInput

RUN mvn clean package -DskipTests

CMD java -jar target/terraform-mps-plugin-0.0.1-SNAPSHOT.jar
