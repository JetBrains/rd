FROM adoptopenjdk/openjdk11:x86_64-ubuntu-jdk-11.0.11_9

WORKDIR /rd
COPY . .

ENV TEAMCITY_VERSION=1
RUN ./gradlew assemble
ENTRYPOINT ./gradlew build
