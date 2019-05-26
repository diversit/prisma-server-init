FROM oracle/graalvm-ce:19.0.0 AS builder

WORKDIR /build

RUN gu install native-image

# Add target folder to build dir
ADD target target

RUN native-image -cp target/classes eu.diversit.k8s.prisma.server.InitConfig /build/prisma-init-config

FROM oraclelinux:7-slim AS runner

ENV SECRETS_FOLDER=/secrets
ENV PRISMA_CONFIG=/prisma/prisma.config

WORKDIR /app

COPY --from=builder /build/prisma-init-config /usr/local/bin

#ENTRYPOINT ["/app/prisma-init-config"]

#CMD ["$SECRETS_FOLDER", ">", "$PRISMA_CONFIG"]