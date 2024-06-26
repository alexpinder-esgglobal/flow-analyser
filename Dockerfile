# Start with a base image containing Java runtime (JDK)
FROM openjdk:17-jdk-slim

# Add Maintainer Info
LABEL maintainer=""

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8888

# The application's jar file
ARG JAR_FILE=target/flow-mo-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
ADD ${JAR_FILE} flow-mo-0.0.1-SNAPSHOT.jar

# Run the jar file
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/flow-mo-0.0.1-SNAPSHOT.jar"]
