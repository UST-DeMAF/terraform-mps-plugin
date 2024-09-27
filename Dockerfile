# Stage 1: Build the application using Maven with Eclipse Temurin JDK 11
FROM maven:3.8.6-eclipse-temurin-11 AS build

# Set the working directory for the build stage
WORKDIR /app

# Copy the Maven project file and source code into the container
COPY pom.xml .
COPY src ./src
COPY mps-transformation-terraform ./mps-transformation-terraform

# Create the necessary directory for transformation input
RUN mkdir -p /app/mps-transformation-terraform/transformationInput

# Conditionally comment out the line 'executor.execute(prepareMps)' if SLIM=1
ARG SLIM
RUN if [ "$SLIM" != "1" ]; then \
    sed -i 's/executor\.execute(prepareMps)/\/\/ executor.execute(prepareMps)/' /app/src/main/java/ust/tad/terraformmpsplugin/analysis/TransformationService.java; \
    fi

# Build the project, using multiple threads and skipping tests
RUN mvn -T 2C -q clean package -DskipTests

# Download MPS iff SLIM!=1
RUN if [ "$SLIM" != "1" ]; then \
    cd mps-transformation-terraform && \
    ./gradlew prepareMps; \
    fi

# Remove JBR tarball and MPS/Plugin zips
RUN rm -rf /app/mps-transformation-terraform/build/download

# Stage 2: Create a minimal runtime image using OpenJDK 11 JRE
FROM openjdk:11-jre-slim

# Set the working directory for the runtime stage
WORKDIR /app

# Copy all built files from the build stage to the runtime stage
COPY --from=build /app /app

# Set the command to run the application
CMD ["java", "-jar", "/app/target/terraform-mps-plugin-0.0.1-SNAPSHOT.jar"]
