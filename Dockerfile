#############
# Build and test Java sources
#############
FROM maven:3-jdk-8-alpine AS javaBuilder

WORKDIR /sources

ADD src src
ADD pom.xml .

RUN mvn clean test

#############
# Build Java application to native app
#############
FROM oracle/graalvm-ce:19.0.0 AS toolBuilder

WORKDIR /build

RUN gu install native-image

# Add target folder to build dir
COPY --from=javaBuilder /sources/target target

RUN native-image -cp target/classes eu.diversit.k8s.prisma.server.InitConfig /build/prisma-init-config

#############
# Build image with only native app
#############
FROM oraclelinux:7-slim AS runner

ENV SECRETS_FOLDER=/secrets
ENV PRISMA_CONFIG=/prisma/prisma.config

WORKDIR /app

# Copy app to a runnable location
COPY --from=toolBuilder /build/prisma-init-config /usr/local/bin
