ARG SKYWALKING_AGENT_IMAGE
ARG JAVA_RUNTIME_IMAGE
FROM ${SKYWALKING_AGENT_IMAGE} AS skywalking-agent
FROM ${JAVA_RUNTIME_IMAGE}
ARG TRAVEL_GATEWAY_PORT

RUN apt-get update && apt-get install -y openjdk-17-jdk && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=skywalking-agent /skywalking/agent /skywalking/agent
RUN cp /skywalking/agent/optional-plugins/apm-spring-cloud-gateway-4.x-plugin-*.jar /skywalking/agent/plugins/ \
    && cp /skywalking/agent/optional-plugins/apm-spring-webflux-6.x-plugin-*.jar /skywalking/agent/plugins/
COPY target/travel-gateway-1.0.0.jar app.jar

EXPOSE ${TRAVEL_GATEWAY_PORT}

ENTRYPOINT ["java", "-javaagent:/skywalking/agent/skywalking-agent.jar", "-jar", "app.jar"]
