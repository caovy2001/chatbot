# Use an official OpenJDK runtime as the base image
FROM openjdk:18-jdk

# Set the working directory inside the container
WORKDIR /app

COPY build-file/ ./build-file
COPY src/main/resources/application.properties ./application.properties

RUN cat ./build-file/splited-app-* > ./app.jar
RUN rm -r build-file
RUN mkdir mkdir -p src/main/resources/file_data

EXPOSE 8085



