ARG SKYWALKING_AGENT_IMAGE
ARG JAVA_RUNTIME_IMAGE
FROM ${SKYWALKING_AGENT_IMAGE} AS skywalking-agent
FROM ${JAVA_RUNTIME_IMAGE}
ARG TRAVEL_PRODUCT_PORT

RUN apt-get update && apt-get install -y openjdk-17-jdk && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=skywalking-agent /skywalking/agent /skywalking/agent
COPY target/travel-product-1.0.0.jar app.jar

EXPOSE ${TRAVEL_PRODUCT_PORT}

ENTRYPOINT ["java", "-javaagent:/skywalking/agent/skywalking-agent.jar", "-jar", "app.jar"]
