# Use an official OpenJDK runtime as the base image
FROM openjdk:18-jdk

# Set the working directory inside the container
WORKDIR /app

COPY build-file/ ./build-file
COPY src/main/resources/application.properties ./application.properties
COPY keystore.p12 ./keystore.p12

RUN cat ./build-file/splited-app-* > ./app.jar
RUN rm -r build-file
RUN mkdir -p src/main/resources/file_data
RUN mkdir -p model

EXPOSE 8085



